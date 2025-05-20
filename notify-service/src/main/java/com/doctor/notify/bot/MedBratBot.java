package com.doctor.notify.bot;

import com.doctor.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Бот «Медбрат» для общения с врачом и пациентами через команды:
 * - /schedule — получить расписание на сегодня
 * - /cancel {id} — отменить запись с указанным ID
 */
@Component
@RequiredArgsConstructor
public class MedBratBot extends TelegramLongPollingBot {
    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.username}")
    private String username;

    @Value("${telegram.doctor.chat-id}")
    private String doctorChatId;

    private final NotificationService notificationService;

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public String getDoctorChatId() {
        return doctorChatId;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        String chatId = update.getMessage().getChatId().toString();
        SendMessage response = new SendMessage();
        response.setChatId(chatId);

        if ("/schedule".equalsIgnoreCase(text)) {
            response.setText(notificationService.getTodaySchedule());
        } else if (text.toLowerCase().startsWith("/cancel")) {
            try {
                Long id = Long.parseLong(text.split("\\s+")[1]);
                response.setText(notificationService.cancelAppointment(id));
            } catch (Exception e) {
                response.setText("Использование: /cancel <id>");
            }
        } else {
            response.setText("Команды:\n/schedule — расписание на сегодня\n/cancel <id> — отменить запись");
        }

        try {
            execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
