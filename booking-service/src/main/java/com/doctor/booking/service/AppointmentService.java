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

        Appointment a = new Appointment();
        a.setPatient(p);
        a.setAppointmentTime(time);
        a.setStatus(AppointmentStatus.PENDING_DOCTOR);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.setDoctorDecisionDeadlineAt(LocalDateTime.now().plusMinutes(doctorDecisionTimeoutMinutes));
        a = apptRepo.save(a);

        DoctorApprovalNotificationDTO doc = new DoctorApprovalNotificationDTO();
        doc.setDoctorChatId(doctorChatId);
        doc.setAppointmentId(a.getId());
        doc.setText("Пациент " + p.getName() + " хочет записаться на " + DateTimeUtil.format(a.getAppointmentTime()) + ". Подтвердить?");
        notifyClient.sendDoctorApproval(doc);

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
        var now = LocalDateTime.now();
        apptRepo.findByStatusAndDoctorDecisionDeadlineAtBefore(AppointmentStatus.PENDING_DOCTOR, now).forEach(a -> {
            a.setStatus(AppointmentStatus.DECLINED);
            a.setDeclineReason("Врач не ответил в течение 2 часов");
            a.setUpdatedAt(now);
            apptRepo.save(a);

            sendToPatientIfTelegramKnown(a,
                    "Запрос на запись " + DateTimeUtil.format(a.getAppointmentTime()) + " отклонён: врач не ответил вовремя.");
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
        var time = DateTimeFormatUtil.parseBotDateTime(req.getRequestedTime());

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

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setAppointmentTime(time);
        a.setStatus(AppointmentStatus.PENDING_DOCTOR);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.setDoctorDecisionDeadlineAt(LocalDateTime.now().plusMinutes(doctorDecisionTimeoutMinutes));
        a = apptRepo.save(a);

        var doc = new DoctorApprovalNotificationDTO();
        doc.setDoctorChatId(doctorChatId);
        doc.setAppointmentId(a.getId());
        doc.setText("Пациент " + patient.getName() + " хочет записаться на " + DateTimeUtil.format(a.getAppointmentTime()) + ". Подтвердить?");
        notifyClient.sendDoctorApproval(doc);

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
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: id=" + id));

        AppointmentStatus st = a.getStatus();
        if (st == AppointmentStatus.DECLINED || st == AppointmentStatus.CANCELLED || st == AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStateException("Appointment cannot be cancelled in status: " + st);
        }

        a.setCancelReason(reason);
        a.setStatus(AppointmentStatus.CANCELLED);

        Appointment saved = apptRepo.save(a);

        sendToPatientIfTelegramKnown(saved,
                "Запись #" + saved.getId() + " отменена врачом."
                        + (reason == null || reason.isBlank() ? "" : " Причина: " + reason));

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
}
