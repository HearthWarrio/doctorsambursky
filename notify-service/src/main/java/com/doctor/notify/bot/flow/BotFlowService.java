package com.doctor.notify.bot.flow;

import com.doctor.notify.bot.MedBratBot;
import com.doctor.notify.bot.TelegramSendService;
import com.doctor.notify.bot.state.*;
import com.doctor.notify.bot.ui.KeyboardFactory;
import com.doctor.notify.integration.booking.*;
import com.doctor.notify.integration.booking.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BotFlowService {

    private static final String TIME_PATTERN_HINT = "ЧЧ:ММ ДД:ММ:ГГ (пример: 14:00 25:06:25)";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.US);

    private final MedBratBot bot;
    private final TelegramSendService telegram;
    private final BookingClient booking;

    private final Map<Long, PatientSession> patientSessions = new ConcurrentHashMap<>();
    private final Map<Long, DoctorPendingAction> doctorPending = new ConcurrentHashMap<>();

    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            onCallback(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            onTextMessage(update.getMessage());
        }
    }

    public void onDoctorApprovalRequest(long doctorChatId, long appointmentId, String text) {
        telegram.sendText(doctorChatId, text, KeyboardFactory.doctorApproval(appointmentId));
    }

    private void onCallback(CallbackQuery cq) {
        telegram.answerCallback(cq.getId());

        Long chatId = cq.getMessage().getChatId();
        if (!chatId.equals(bot.getDoctorChatId())) {
            telegram.sendText(chatId, "Эта кнопка не для вас.");
            return;
        }

        String data = cq.getData();
        if (data == null) return;

        try {
            if (data.startsWith("APPT_CONFIRM:")) {
                long id = Long.parseLong(data.substring("APPT_CONFIRM:".length()));
                DoctorActionRequestDTO req = new DoctorActionRequestDTO();
                req.setAction(DoctorActionType.CONFIRM);
                AppointmentDTO a = booking.doctorAction(id, req);
                telegram.sendText(chatId, "Подтверждено. Запись #" + a.getId());
                return;
            }

            if (data.startsWith("APPT_DECLINE:")) {
                long id = Long.parseLong(data.substring("APPT_DECLINE:".length()));
                DoctorPendingAction p = new DoctorPendingAction();
                p.setType(DoctorPendingType.DECLINE_REASON);
                p.setAppointmentId(id);
                doctorPending.put(chatId, p);
                telegram.sendText(chatId, "Введите причину отказа или отправьте /skip");
                return;
            }

            if (data.startsWith("APPT_RESCHEDULE:")) {
                long id = Long.parseLong(data.substring("APPT_RESCHEDULE:".length()));
                DoctorPendingAction p = new DoctorPendingAction();
                p.setType(DoctorPendingType.RESCHEDULE_TIME);
                p.setAppointmentId(id);
                doctorPending.put(chatId, p);
                telegram.sendText(chatId, "Введите новое время: " + TIME_PATTERN_HINT + " или отправьте /any");
                return;
            }

            if (data.startsWith("APPT_CANCEL:")) {
                long id = Long.parseLong(data.substring("APPT_CANCEL:".length()));
                DoctorPendingAction p = new DoctorPendingAction();
                p.setType(DoctorPendingType.CANCEL_REASON);
                p.setAppointmentId(id);
                doctorPending.put(chatId, p);
                telegram.sendText(chatId, "Введите причину отмены или отправьте /skip");
            }
        } catch (ServiceUnavailableException ex) {
            telegram.sendText(chatId, "booking-service недоступен. Попробуйте позже.");
        } catch (BookingClientException ex) {
            telegram.sendText(chatId, "Ошибка: " + ex.getMessage());
        } catch (Exception ex) {
            telegram.sendText(chatId, "Ошибка обработки команды. Попробуйте ещё раз.");
        }
    }

    private void onTextMessage(Message msg) {
        Long chatId = msg.getChatId();
        String text = msg.getText().trim();

        if (chatId.equals(bot.getDoctorChatId())) {
            if (handleDoctorText(chatId, text)) return;

            if ("/schedule".equalsIgnoreCase(text)) {
                sendSchedule(chatId);
                return;
            }

            if (text.toLowerCase(Locale.ROOT).startsWith("/cancel")) {
                cancelByCommand(chatId, text);
                return;
            }

            if ("/start".equalsIgnoreCase(text)) {
                telegram.sendText(chatId, "Команды:\n/schedule\n/cancel <id> <reason|/skip>\n(кнопки по заявкам приходят автоматически)");
                return;
            }

            telegram.sendText(chatId, "Для расписания: /schedule. Для отмены: /cancel <id> <reason|/skip>.");
            return;
        }

        if (text.startsWith("/start")) {
            String arg = text.replace("/start", "").trim();
            if (arg.equalsIgnoreCase("booking") || arg.isBlank()) {
                PatientSession s = patientSessions.computeIfAbsent(chatId, k -> new PatientSession());
                s.setStep(PatientStep.WAIT_NAME);
                s.getDraft().setTelegramUsername(msg.getFrom() != null ? msg.getFrom().getUserName() : null);
                telegram.sendText(chatId, "Введите ФИО");
                return;
            }
        }

        PatientSession s = patientSessions.computeIfAbsent(chatId, k -> new PatientSession());
        if (s.getStep() == PatientStep.IDLE) {
            if (tryRescheduleFromIdle(chatId, s, text)) return;
            telegram.sendText(chatId, "Чтобы начать запись – нажмите кнопку на сайте или отправьте /start booking");
            return;
        }

        switch (s.getStep()) {
            case WAIT_NAME -> {
                s.getDraft().setPatientName(text);
                s.setStep(PatientStep.WAIT_PHONE);
                telegram.sendText(chatId, "Введите телефон");
            }
            case WAIT_PHONE -> {
                s.getDraft().setPhone(text);
                s.setStep(PatientStep.WAIT_ADDRESS);
                telegram.sendText(chatId, "Введите адрес (где принимать)");
            }
            case WAIT_ADDRESS -> {
                s.getDraft().setAddress(text);
                s.setStep(PatientStep.WAIT_WHATSAPP);
                telegram.sendText(chatId, "Введите WhatsApp (необязательно) или отправьте /skip");
            }
            case WAIT_WHATSAPP -> {
                if (!"/skip".equalsIgnoreCase(text)) {
                    s.getDraft().setWhatsappNumber(text);
                }
                s.setStep(PatientStep.WAIT_EMAIL);
                telegram.sendText(chatId, "Введите email (необязательно) или отправьте /skip");
            }
            case WAIT_EMAIL -> {
                if (!"/skip".equalsIgnoreCase(text)) {
                    s.getDraft().setEmail(text);
                }
                s.setStep(PatientStep.WAIT_TIME);
                telegram.sendText(chatId, "Введите желаемое время: " + TIME_PATTERN_HINT);
            }
            case WAIT_TIME -> createAppointment(chatId, s, text);
            default -> telegram.sendText(chatId, "Не понял. Попробуйте ещё раз.");
        }
    }

    private void sendSchedule(Long doctorChatId) {
        LocalDateTime from = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime to = from.plusDays(7);

        try {
            List<AppointmentDTO> items = booking.getSchedule(from.format(ISO), to.format(ISO));
            if (items == null || items.isEmpty()) {
                telegram.sendText(doctorChatId, "Расписание пустое: сейчас – +7 дней.");
                return;
            }

            List<AppointmentDTO> sorted = new ArrayList<>(items);
            sorted.sort(Comparator.comparing(this::sortKeyTime, Comparator.nullsLast(Comparator.naturalOrder())));

            List<String> lines = new ArrayList<>();
            for (AppointmentDTO a : sorted) {
                if (a == null || a.getId() == null) continue;
                lines.add(formatScheduleLineOneLine(a));
            }

            if (lines.isEmpty()) {
                telegram.sendText(doctorChatId, "Расписание пустое: сейчас – +7 дней.");
                return;
            }

            sendLinesChunked(doctorChatId, lines);

        } catch (ServiceUnavailableException ex) {
            telegram.sendText(doctorChatId, "booking-service недоступен. Попробуйте позже.");
        } catch (BookingClientException ex) {
            telegram.sendText(doctorChatId, "Ошибка: " + ex.getMessage());
        }
    }

    private LocalDateTime sortKeyTime(AppointmentDTO a) {
        String raw = pickDisplayTimeRaw(a);
        if (raw == null) return null;
        try {
            return LocalDateTime.parse(raw, ISO);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatScheduleLineOneLine(AppointmentDTO a) {
        String time = formatTime(pickDisplayTimeRaw(a));
        String st = normalizeStatus(a.getStatus());
        String name = (a.getPatientName() == null || a.getPatientName().isBlank()) ? "неизвестно" : a.getPatientName();

        return "#" + a.getId()
                + " – " + time
                + " – " + name
                + " – " + st;
    }

    private String normalizeStatus(String st) {
        if (st == null || st.isBlank()) return "UNKNOWN";
        return st.trim().toUpperCase(Locale.ROOT);
    }

    private String pickDisplayTimeRaw(AppointmentDTO a) {
        String st = normalizeStatus(a.getStatus());
        if ("RESCHEDULE_PROPOSED".equals(st) && a.getRescheduleProposedTime() != null && !a.getRescheduleProposedTime().isBlank()) {
            return a.getRescheduleProposedTime();
        }
        return a.getAppointmentTime();
    }

    private String formatTime(String raw) {
        if (raw == null) return "время неизвестно";
        try {
            LocalDateTime dt = LocalDateTime.parse(raw, ISO);
            return dt.format(HUMAN);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private void sendLinesChunked(Long chatId, List<String> lines) {
        String header = "Расписание (сейчас – +7 дней):";
        StringBuilder sb = new StringBuilder(header);

        for (String line : lines) {
            String next = "\n" + line;
            if (sb.length() + next.length() > 3500) {
                telegram.sendText(chatId, sb.toString());
                sb = new StringBuilder(header);
            }
            sb.append(next);
        }

        if (sb.length() > header.length()) {
            telegram.sendText(chatId, sb.toString());
        }
    }

    private void cancelByCommand(Long doctorChatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            telegram.sendText(doctorChatId, "Формат: /cancel <id> <reason|/skip>");
            return;
        }

        long id;
        try {
            id = Long.parseLong(parts[1].trim());
        } catch (Exception e) {
            telegram.sendText(doctorChatId, "id должен быть числом. Формат: /cancel <id> <reason|/skip>");
            return;
        }

        String reason = null;
        if (parts.length >= 3) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length)).trim();
            if ("/skip".equalsIgnoreCase(reason)) {
                reason = null;
            }
        }

        if (reason != null && !reason.isBlank()) {
            try {
                cancelNowAndNotify(doctorChatId, id, reason);
            } catch (ServiceUnavailableException ex) {
                telegram.sendText(doctorChatId, "booking-service недоступен. Попробуйте позже.");
            } catch (BookingClientException ex) {
                telegram.sendText(doctorChatId, ex.getMessage());
            } catch (Exception ex) {
                telegram.sendText(doctorChatId, "Ошибка обработки отмены. Попробуйте ещё раз.");
            }
            return;
        }

        DoctorPendingAction p = new DoctorPendingAction();
        p.setType(DoctorPendingType.CANCEL_REASON);
        p.setAppointmentId(id);
        doctorPending.put(doctorChatId, p);
        telegram.sendText(doctorChatId, "Введите причину отмены или отправьте /skip");
    }

    private boolean handleDoctorText(Long doctorChatId, String text) {
        DoctorPendingAction p = doctorPending.get(doctorChatId);
        if (p == null) return false;

        try {
            if (p.getType() == DoctorPendingType.DECLINE_REASON) {
                String reason = "/skip".equalsIgnoreCase(text) ? null : text;
                DoctorActionRequestDTO req = new DoctorActionRequestDTO();
                req.setAction(DoctorActionType.DECLINE);
                req.setReason(reason);
                AppointmentDTO a = booking.doctorAction(p.getAppointmentId(), req);
                doctorPending.remove(doctorChatId);
                telegram.sendText(doctorChatId, "Отклонено. Запись #" + a.getId());
                return true;
            }

            if (p.getType() == DoctorPendingType.RESCHEDULE_TIME) {
                DoctorActionRequestDTO req = new DoctorActionRequestDTO();
                req.setAction(DoctorActionType.RESCHEDULE);
                if (!"/any".equalsIgnoreCase(text)) {
                    req.setProposedTime(text);
                }
                AppointmentDTO a = booking.doctorAction(p.getAppointmentId(), req);
                doctorPending.remove(doctorChatId);
                telegram.sendText(doctorChatId, "Отправлено пациенту. Запись #" + a.getId());
                return true;
            }

            if (p.getType() == DoctorPendingType.CANCEL_REASON) {
                String reason = "/skip".equalsIgnoreCase(text) ? null : text;
                cancelNowAndNotify(doctorChatId, p.getAppointmentId(), reason);
                doctorPending.remove(doctorChatId);
                return true;
            }

            return false;

        } catch (ServiceUnavailableException ex) {
            telegram.sendText(doctorChatId, "booking-service недоступен. Попробуйте позже.");
            return true;
        } catch (BookingClientException ex) {
            telegram.sendText(doctorChatId, "Ошибка: " + ex.getMessage());
            doctorPending.remove(doctorChatId);
            return true;
        } catch (Exception ex) {
            telegram.sendText(doctorChatId, "Ошибка обработки. Попробуйте ещё раз.");
            doctorPending.remove(doctorChatId);
            return true;
        }
    }

    private void cancelNowAndNotify(Long doctorChatId, long appointmentId, String reason) {
        CancelAppointmentRequestDTO req = new CancelAppointmentRequestDTO();
        req.setReason(reason);

        AppointmentDTO a = booking.cancelAppointment(appointmentId, req);

        telegram.sendText(doctorChatId, "Отменено. Запись #" + a.getId());

        if (a.getPatientTelegramChatId() != null) {
            String time = formatTime(pickDisplayTimeRaw(a));
            StringBuilder sb = new StringBuilder();
            sb.append("Врач отменил запись ").append(time).append(".");
            if (reason != null && !reason.isBlank()) {
                sb.append("\nПричина: ").append(reason);
            }
            telegram.sendText(a.getPatientTelegramChatId(), sb.toString());
        }
    }

    private void createAppointment(Long chatId, PatientSession s, String timeText) {
        BotCreateAppointmentRequestDTO req = new BotCreateAppointmentRequestDTO();
        req.setPatientName(s.getDraft().getPatientName());
        req.setPhone(s.getDraft().getPhone());
        req.setAddress(s.getDraft().getAddress());
        req.setWhatsappNumber(s.getDraft().getWhatsappNumber());
        req.setEmail(s.getDraft().getEmail());
        req.setTelegramUsername(s.getDraft().getTelegramUsername());
        req.setPatientTelegramChatId(chatId);
        req.setRequestedTime(timeText);

        try {
            AppointmentDTO created = booking.createAppointment(req);
            s.setLastAppointmentId(created.getId());
            s.setStep(PatientStep.IDLE);
            telegram.sendText(chatId, "Запрос отправлен врачу. Номер заявки: #" + created.getId());
        } catch (SlotUnavailableException ex) {
            telegram.sendText(chatId, "Это время занято. Введите другое: " + TIME_PATTERN_HINT);
        } catch (BadRequestException ex) {
            telegram.sendText(chatId, "Неверный формат. Введите время как " + TIME_PATTERN_HINT);
        } catch (ServiceUnavailableException ex) {
            telegram.sendText(chatId, "Техническая проблема. Попробуйте позже.");
        } catch (BookingClientException ex) {
            telegram.sendText(chatId, "Ошибка: " + ex.getMessage());
        }
    }

    private boolean tryRescheduleFromIdle(Long chatId, PatientSession s, String timeText) {
        if (s.getLastAppointmentId() == null) return false;

        try {
            AppointmentDTO a = booking.getAppointment(s.getLastAppointmentId());
            if (a == null || a.getStatus() == null) return false;

            String st = a.getStatus().toUpperCase(Locale.ROOT);
            if (!st.equals("RESCHEDULE_REQUESTED") && !st.equals("RESCHEDULE_PROPOSED")) return false;

            PatientRescheduleRequestDTO req = new PatientRescheduleRequestDTO();
            req.setRequestedTime(timeText);
            AppointmentDTO updated = booking.patientReschedule(a.getId(), req);
            telegram.sendText(chatId, "Новое время отправлено врачу. Заявка: #" + updated.getId());
            return true;
        } catch (SlotUnavailableException ex) {
            telegram.sendText(chatId, "Это время занято. Введите другое: " + TIME_PATTERN_HINT);
            return true;
        } catch (BadRequestException ex) {
            telegram.sendText(chatId, "Неверный формат. Введите время как " + TIME_PATTERN_HINT);
            return true;
        } catch (BookingClientException ex) {
            return false;
        }
    }
}
