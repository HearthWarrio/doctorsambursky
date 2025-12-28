package com.doctor.booking.service;

import com.doctor.booking.dto.*;
import com.doctor.booking.entity.Appointment;
import com.doctor.booking.entity.AppointmentStatus;
import com.doctor.booking.entity.Patient;
import com.doctor.booking.exception.AppointmentNotFoundException;
import com.doctor.booking.exception.InvalidAppointmentStateException;
import com.doctor.booking.exception.SlotUnavailableException;
import com.doctor.booking.integration.notify.NotifyClient;
import com.doctor.booking.integration.notify.dto.DoctorApprovalNotificationDTO;
import com.doctor.booking.integration.notify.dto.SendMessageNotificationDTO;
import com.doctor.booking.mapper.AppointmentMapper;
import com.doctor.booking.repository.AppointmentRepository;
import com.doctor.booking.util.DateTimeFormatUtil;
import com.doctor.booking.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Set<AppointmentStatus> BUSY_STATUSES = EnumSet.of(
            AppointmentStatus.PENDING_DOCTOR,
            AppointmentStatus.CONFIRMED
    );

    private final PatientService patientService;
    private final AppointmentRepository apptRepo;
    private final AppointmentMapper mapper;
    private final NotifyClient notifyClient;

    @Value("${telegram.doctor.chat-id}")
    private Long doctorChatId;

    @Value("${booking.doctorDecisionTimeoutMinutes:120}")
    private int doctorDecisionTimeoutMinutes;

    /**
     * Web booking (no payments): patient proposes a slot, doctor decides via MedBrat.
     */
    @Transactional
    public AppointmentDTO book(BookingRequest req) {
        Patient p = patientService.findOrCreate(req.getName(), req.getPhone(), req.getEmail());

        LocalDateTime time = req.getAppointmentTime();
        if (apptRepo.existsByAppointmentTimeAndStatusIn(time, BUSY_STATUSES)
                || apptRepo.existsByRescheduleProposedTimeAndStatus(time, AppointmentStatus.RESCHEDULE_PROPOSED)) {
            throw new SlotUnavailableException("Время занято");
        }

        LocalDateTime now = LocalDateTime.now();

        Appointment a = new Appointment();
        a.setPatient(p);
        a.setAppointmentTime(time);
        a.setStatus(AppointmentStatus.PENDING_DOCTOR);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        a.setDoctorDecisionDeadlineAt(now.plusMinutes(doctorDecisionTimeoutMinutes));
        a.setDoctorNotified(false);
        a.setTimeoutDeclineNotified(false);

        a = apptRepo.save(a);

        boolean sent = false;
        if (doctorChatId != null) {
            sent = sendDoctorApprovalWithRetry(
                    a.getId(),
                    doctorChatId,
                    "Пациент " + p.getName() + " хочет записаться на " + DateTimeUtil.format(a.getAppointmentTime()) + ". Подтвердить?"
            );
        }

        if (sent) {
            LocalDateTime sentAt = LocalDateTime.now();
            a.setDoctorNotified(true);
            a.setDoctorNotifiedAt(sentAt);
            a.setUpdatedAt(sentAt);
            a = apptRepo.save(a);
        }

        return mapper.toDto(a);
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> getAvailable(LocalDateTime from, LocalDateTime to, int stepMinutes) {
        var slots = new ArrayList<LocalDateTime>();
        for (var t = from; !t.isAfter(to); t = t.plusMinutes(stepMinutes)) {
            slots.add(t);
        }

        var busy = apptRepo.findByAppointmentTimeBetweenAndStatusIn(from, to, BUSY_STATUSES)
                .stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        var proposedBusy = apptRepo.findByRescheduleProposedTimeBetweenAndStatus(from, to, AppointmentStatus.RESCHEDULE_PROPOSED)
                .stream()
                .map(Appointment::getRescheduleProposedTime)
                .collect(Collectors.toSet());

        busy.addAll(proposedBusy);

        return slots.stream()
                .filter(s -> !busy.contains(s))
                .map(s -> {
                    var slot = new AvailableSlotDTO();
                    slot.setAppointmentTime(s);
                    return slot;
                })
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void declineIfDoctorSilent() {
        LocalDateTime now = LocalDateTime.now();

        apptRepo.findByStatusAndDoctorDecisionDeadlineAtBefore(AppointmentStatus.PENDING_DOCTOR, now).forEach(a -> {
            a.setStatus(AppointmentStatus.DECLINED);
            a.setDeclineReason("Врач не ответил в течение 2 часов");
            a.setUpdatedAt(now);
            a = apptRepo.save(a);

            Long chatId = null;
            if (a.getPatient() != null) {
                chatId = a.getPatient().getTelegramChatId();
            }

            if (chatId == null) {
                a.setTimeoutDeclineNotified(true);
                a.setTimeoutDeclineNotifiedAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
                return;
            }

            boolean sent = sendPatientMessageWithRetry(
                    a,
                    "Запрос на запись " + DateTimeUtil.format(a.getAppointmentTime()) + " отклонён: врач не ответил вовремя."
            );

            if (sent) {
                a.setTimeoutDeclineNotified(true);
                a.setTimeoutDeclineNotifiedAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> listByDay(LocalDateTime day) {
        var start = day.withHour(0).withMinute(0).withSecond(0).withNano(0);
        var end = start.plusDays(1);
        return apptRepo.findByAppointmentTimeBetweenAndStatusIn(start, end, EnumSet.allOf(AppointmentStatus.class)).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDTO createFromBot(BotCreateAppointmentRequestDTO req) {
        LocalDateTime time = DateTimeFormatUtil.parseBotDateTime(req.getRequestedTime());

        if (apptRepo.existsByAppointmentTimeAndStatusIn(time, BUSY_STATUSES)
                || apptRepo.existsByRescheduleProposedTimeAndStatus(time, AppointmentStatus.RESCHEDULE_PROPOSED)) {
            throw new SlotUnavailableException("Время занято");
        }

        Patient patient = patientService.findOrCreateTelegramPatient(
                req.getPatientTelegramChatId(),
                req.getPatientName(),
                req.getPhone(),
                req.getAddress(),
                req.getTelegramUsername(),
                req.getWhatsappNumber(),
                req.getEmail()
        );

        LocalDateTime now = LocalDateTime.now();

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setAppointmentTime(time);
        a.setStatus(AppointmentStatus.PENDING_DOCTOR);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        a.setDoctorDecisionDeadlineAt(now.plusMinutes(doctorDecisionTimeoutMinutes));
        a.setDoctorNotified(false);
        a.setTimeoutDeclineNotified(false);

        a = apptRepo.save(a);

        boolean sent = false;
        if (doctorChatId != null) {
            sent = sendDoctorApprovalWithRetry(
                    a.getId(),
                    doctorChatId,
                    "Пациент " + patient.getName() + " хочет записаться на " + DateTimeUtil.format(a.getAppointmentTime()) + ". Подтвердить?"
            );
        }

        if (sent) {
            LocalDateTime sentAt = LocalDateTime.now();
            a.setDoctorNotified(true);
            a.setDoctorNotifiedAt(sentAt);
            a.setUpdatedAt(sentAt);
            a = apptRepo.save(a);
        }

        return mapper.toDto(a);
    }

    @Transactional
    public AppointmentDTO doctorAction(long appointmentId, DoctorActionRequestDTO req) {
        Appointment a = apptRepo.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Запись не найдена"));

        if (a.getStatus() != AppointmentStatus.PENDING_DOCTOR) {
            throw new InvalidAppointmentStateException("Запись не ожидает подтверждения врача");
        }

        if (LocalDateTime.now().isAfter(a.getDoctorDecisionDeadlineAt())) {
            a.setStatus(AppointmentStatus.DECLINED);
            a.setDeclineReason("Истёк срок подтверждения");
            a.setUpdatedAt(LocalDateTime.now());
            apptRepo.save(a);

            sendToPatientIfTelegramKnown(a,
                    "Запрос на запись " + DateTimeUtil.format(a.getAppointmentTime()) + " отклонён: истёк срок подтверждения.");

            throw new InvalidAppointmentStateException("Истёк срок подтверждения");
        }

        switch (req.getAction()) {
            case CONFIRM -> {
                a.setStatus(AppointmentStatus.CONFIRMED);
                a.setDeclineReason(null);
                a.setRescheduleProposedTime(null);
                a.setUpdatedAt(LocalDateTime.now());
                apptRepo.save(a);

                sendToPatientIfTelegramKnown(a,
                        "Запись подтверждена. Дата и время: " + DateTimeUtil.format(a.getAppointmentTime()));
            }
            case DECLINE -> {
                a.setStatus(AppointmentStatus.DECLINED);
                a.setDeclineReason(req.getReason() == null || req.getReason().isBlank() ? "Отклонено врачом" : req.getReason());
                a.setUpdatedAt(LocalDateTime.now());
                apptRepo.save(a);

                sendToPatientIfTelegramKnown(a,
                        "Запись отклонена. Причина: " + a.getDeclineReason());
            }
            case RESCHEDULE -> {
                if (req.getProposedTime() != null && !req.getProposedTime().isBlank()) {
                    var proposed = DateTimeFormatUtil.parseBotDateTime(req.getProposedTime());

                    if (apptRepo.existsByAppointmentTimeAndStatusIn(proposed, BUSY_STATUSES)
                            || apptRepo.existsByRescheduleProposedTimeAndStatus(proposed, AppointmentStatus.RESCHEDULE_PROPOSED)) {
                        throw new SlotUnavailableException("Предложенное время занято");
                    }

                    a.setStatus(AppointmentStatus.RESCHEDULE_PROPOSED);
                    a.setRescheduleProposedTime(proposed);
                    a.setUpdatedAt(LocalDateTime.now());
                    apptRepo.save(a);

                    sendToPatientIfTelegramKnown(a,
                            "Врач предлагает другое время: " + DateTimeUtil.format(proposed)
                                    + ". Если не подходит – пришлите другое в формате " + DateTimeFormatUtil.BOT_PATTERN);
                } else {
                    a.setStatus(AppointmentStatus.RESCHEDULE_REQUESTED);
                    a.setRescheduleProposedTime(null);
                    a.setUpdatedAt(LocalDateTime.now());
                    apptRepo.save(a);

                    sendToPatientIfTelegramKnown(a,
                            "Врач просит выбрать другое время. Пришлите в формате " + DateTimeFormatUtil.BOT_PATTERN);
                }
            }
        }

        return mapper.toDto(a);
    }

    @Transactional
    public AppointmentDTO patientReschedule(long appointmentId, PatientRescheduleRequestDTO req) {
        Appointment a = apptRepo.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Запись не найдена"));

        if (a.getStatus() != AppointmentStatus.RESCHEDULE_REQUESTED && a.getStatus() != AppointmentStatus.RESCHEDULE_PROPOSED) {
            throw new InvalidAppointmentStateException("Смена времени недоступна для текущего статуса");
        }

        var time = DateTimeFormatUtil.parseBotDateTime(req.getRequestedTime());

        if (apptRepo.existsByAppointmentTimeAndStatusIn(time, BUSY_STATUSES)
                || apptRepo.existsByRescheduleProposedTimeAndStatus(time, AppointmentStatus.RESCHEDULE_PROPOSED)) {
            throw new SlotUnavailableException("Время занято");
        }

        a.setAppointmentTime(time);
        a.setStatus(AppointmentStatus.PENDING_DOCTOR);
        a.setDeclineReason(null);
        a.setRescheduleProposedTime(null);
        a.setDoctorDecisionDeadlineAt(LocalDateTime.now().plusMinutes(doctorDecisionTimeoutMinutes));
        a.setUpdatedAt(LocalDateTime.now());
        a = apptRepo.save(a);

        var doc = new DoctorApprovalNotificationDTO();
        doc.setDoctorChatId(doctorChatId);
        doc.setAppointmentId(a.getId());
        doc.setText("Пациент " + a.getPatient().getName() + " хочет записаться на " + DateTimeUtil.format(a.getAppointmentTime()) + ". Подтвердить?");
        notifyClient.sendDoctorApproval(doc);

        return mapper.toDto(a);
    }

    @Transactional(readOnly = true)
    public AppointmentDTO getForBot(long appointmentId) {
        Appointment a = apptRepo.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Запись не найдена"));
        return mapper.toDto(a);
    }

    public List<AppointmentDTO> getSchedule(LocalDateTime from, LocalDateTime to) {
        Set<AppointmentStatus> statuses = EnumSet.of(
                AppointmentStatus.PENDING_DOCTOR,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.RESCHEDULE_REQUESTED,
                AppointmentStatus.RESCHEDULE_PROPOSED
        );

        List<Appointment> items = new ArrayList<>();
        items.addAll(apptRepo.findByAppointmentTimeBetweenAndStatusIn(from, to, statuses));
        items.addAll(apptRepo.findByRescheduleProposedTimeBetweenAndStatus(from, to, AppointmentStatus.RESCHEDULE_PROPOSED));

        Set<Long> seen = new HashSet<>();
        List<AppointmentDTO> out = new ArrayList<>();

        for (Appointment a : items) {
            if (a != null && a.getId() != null && seen.add(a.getId())) {
                out.add(mapper.toDto(a));
            }
        }

        return out;
    }

    public AppointmentDTO cancelByDoctor(long id, String reason) {
        Appointment a = apptRepo.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException("Запись не найдена: id=" + id));

        AppointmentStatus st = a.getStatus();
        if (st == AppointmentStatus.DECLINED || st == AppointmentStatus.CANCELLED || st == AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStateException("Нельзя отменить запись в статусе: " + st);
        }

        a.setCancelReason(reason);
        a.setStatus(AppointmentStatus.CANCELLED);

        Appointment saved = apptRepo.save(a);

        if (saved.getPatient() != null && saved.getPatient().getTelegramChatId() != null) {
            SendMessageNotificationDTO msg = new SendMessageNotificationDTO();
            msg.setChatId(saved.getPatient().getTelegramChatId());
            msg.setText("Запись #" + saved.getId() + " отменена врачом."
                    + (reason == null || reason.isBlank() ? "" : " Причина: " + reason));
            notifyClient.sendMessage(msg);
        }

        return mapper.toDto(saved);
    }

    private void sendToPatientIfTelegramKnown(Appointment a, String text) {
        if (a == null || a.getPatient() == null || a.getPatient().getTelegramChatId() == null) {
            return;
        }
        SendMessageNotificationDTO msg = new SendMessageNotificationDTO();
        msg.setChatId(a.getPatient().getTelegramChatId());
        msg.setText(text);
        notifyClient.sendMessage(msg);
    }

    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void sendPatientReminders() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime w24From = now.plusHours(24).minusMinutes(20);
        LocalDateTime w24To = now.plusHours(24).plusMinutes(20);

        LocalDateTime w2From = now.plusHours(2).minusMinutes(10);
        LocalDateTime w2To = now.plusHours(2).plusMinutes(10);

        List<Appointment> due24 = apptRepo.findByStatusAndAppointmentTimeBetweenAndReminder24SentAtIsNull(
                AppointmentStatus.CONFIRMED, w24From, w24To
        );

        for (Appointment a : due24) {
            Long chatId = (a.getPatient() == null) ? null : a.getPatient().getTelegramChatId();

            if (chatId == null) {
                a.setReminder24SentAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
                continue;
            }

            try {
                SendMessageNotificationDTO msg = new SendMessageNotificationDTO();
                msg.setChatId(chatId);
                msg.setText("Напоминание: у вас приём " + DateTimeUtil.format(a.getAppointmentTime()) + " (примерно через 24 часа).");
                notifyClient.sendMessage(msg);

                a.setReminder24SentAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
            } catch (Exception ignored) {

            }
        }

        List<Appointment> due2 = apptRepo.findByStatusAndAppointmentTimeBetweenAndReminder2hSentAtIsNull(
                AppointmentStatus.CONFIRMED, w2From, w2To
        );

        for (Appointment a : due2) {
            Long chatId = (a.getPatient() == null) ? null : a.getPatient().getTelegramChatId();

            if (chatId == null) {
                a.setReminder2hSentAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
                continue;
            }

            try {
                SendMessageNotificationDTO msg = new SendMessageNotificationDTO();
                msg.setChatId(chatId);
                msg.setText("Напоминание: у вас приём " + DateTimeUtil.format(a.getAppointmentTime()) + " (примерно через 2 часа).");
                notifyClient.sendMessage(msg);

                a.setReminder2hSentAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
            } catch (Exception ignored) {

            }
        }
    }

    private boolean sendDoctorApprovalWithRetry(Long appointmentId, Long doctorChatId, String text) {
        DoctorApprovalNotificationDTO doc = new DoctorApprovalNotificationDTO();
        doc.setDoctorChatId(doctorChatId);
        doc.setAppointmentId(appointmentId);
        doc.setText(text);

        for (int i = 0; i < 3; i++) {
            try {
                notifyClient.sendDoctorApproval(doc);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean sendPatientMessageWithRetry(Appointment a, String text) {
        if (a == null || a.getPatient() == null || a.getPatient().getTelegramChatId() == null) {
            return false;
        }

        SendMessageNotificationDTO msg = new SendMessageNotificationDTO();
        msg.setChatId(a.getPatient().getTelegramChatId());
        msg.setText(text);

        for (int i = 0; i < 3; i++) {
            try {
                notifyClient.sendMessage(msg);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void retryUndeliveredDoctorApprovals() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> pending = apptRepo.findByStatusAndDoctorNotifiedIsFalseAndDoctorDecisionDeadlineAtAfter(
                AppointmentStatus.PENDING_DOCTOR,
                now
        );

        for (Appointment a : pending) {
            boolean sent = sendDoctorApprovalWithRetry(a.getId(), doctorChatId,
                    "Пациент " + a.getPatient().getName() + " хочет записаться на " + DateTimeUtil.format(a.getAppointmentTime()) + ". Подтвердить?");

            if (sent) {
                a.setDoctorNotified(true);
                a.setDoctorNotifiedAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
            }
        }
    }

    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void retryUndeliveredTimeoutDeclines() {
        String reason = "Врач не ответил в течение 2 часов";
        List<Appointment> declined = apptRepo.findByStatusAndTimeoutDeclineNotifiedIsFalseAndDeclineReason(
                AppointmentStatus.DECLINED,
                reason
        );

        LocalDateTime now = LocalDateTime.now();
        for (Appointment a : declined) {
            boolean sent = sendPatientMessageWithRetry(a,
                    "Запрос на запись " + DateTimeUtil.format(a.getAppointmentTime()) + " отклонён: врач не ответил вовремя.");

            if (sent) {
                a.setTimeoutDeclineNotified(true);
                a.setTimeoutDeclineNotifiedAt(now);
                a.setUpdatedAt(now);
                apptRepo.save(a);
            }
        }
    }

}
