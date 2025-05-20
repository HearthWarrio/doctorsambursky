package com.doctor.notify.service;

import com.doctor.notify.bot.MedBratBot;
import com.doctor.notify.entity.Appointment;
import com.doctor.notify.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final AppointmentRepository appointmentRepository;
    private final MedBratBot medbratBot;

    /**
     * Отправляет доктору уведомление о новой записи.
     */
    public void notifyDoctorOfNewAppointment(Appointment ap) {
        String text = String.format(
                "🆕 Новая запись #%d:\nПациент: %s\nВремя: %s\nСтатус: %s",
                ap.getId(), ap.getPatientName(), ap.getAppointmentTime(), ap.getStatus()
        );
        SendMessage msg = new SendMessage();
        msg.setChatId(medbratBot.getDoctorChatId());
        msg.setText(text);
        try {
            medbratBot.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Возвращает расписание на сегодня в виде текста.
     */
    public String getTodaySchedule() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = start.plusDays(1);
        List<Appointment> list = appointmentRepository.findByAppointmentTimeBetween(start, end);
        if (list.isEmpty()) {
            return "На сегодня нет записей.";
        }
        StringBuilder sb = new StringBuilder("📋 Расписание на сегодня:\n");
        for (Appointment a : list) {
            sb.append(String.format(
                    "#%d: %s — %s (пациент: %s)\n",
                    a.getId(), a.getStatus(), a.getAppointmentTime(), a.getPatientName()
            ));
        }
        return sb.toString();
    }

    /**
     * Отменяет запись по заданному ID.
     */
    public String cancelAppointment(Long id) {
        return appointmentRepository.findById(id)
                .map(a -> {
                    a.setStatus("CANCELLED");
                    appointmentRepository.save(a);
                    return "✅ Запись #" + id + " отменена.";
                })
                .orElse("❌ Запись #" + id + " не найдена.");
    }
}
