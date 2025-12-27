package com.doctor.notify.bot;

import com.doctor.notify.bot.ui.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class TelegramSendService {

    private final MedBratBot bot;

    public void sendText(Long chatId, String text) {
        SendMessage m = new SendMessage();
        m.setChatId(String.valueOf(chatId));
        m.setText(text);
        try {
            bot.execute(m);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendText(Long chatId, String text, InlineKeyboardMarkup kb) {
        SendMessage m = new SendMessage();
        m.setChatId(String.valueOf(chatId));
        m.setText(text);
        m.setReplyMarkup(kb);
        try {
            bot.execute(m);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void answerCallback(String callbackId) {
        AnswerCallbackQuery a = new AnswerCallbackQuery();
        a.setCallbackQueryId(callbackId);
        try {
            bot.execute(a);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendDoctorApproval(Long doctorChatId, Long appointmentId, String text) {
        sendText(doctorChatId, text, KeyboardFactory.doctorApproval(appointmentId));
    }
}