package com.doctor.notify.bot.ui;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public class KeyboardFactory {

    public static InlineKeyboardMarkup doctorApproval(long appointmentId) {
        InlineKeyboardButton ok = new InlineKeyboardButton("✅ Подтвердить");
        ok.setCallbackData("APPT_CONFIRM:" + appointmentId);

        InlineKeyboardButton no = new InlineKeyboardButton("❌ Отклонить");
        no.setCallbackData("APPT_DECLINE:" + appointmentId);

        InlineKeyboardButton rs = new InlineKeyboardButton("🕒 Предложить другое время");
        rs.setCallbackData("APPT_RESCHEDULE:" + appointmentId);

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
                List.of(ok),
                List.of(no),
                List.of(rs)
        ));
        return kb;
    }
}