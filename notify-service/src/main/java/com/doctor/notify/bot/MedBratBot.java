package com.doctor.notify.bot;

import com.doctor.notify.bot.flow.BotFlowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class MedBratBot extends TelegramLongPollingBot {

    private final BotFlowService flow;
    private final String botUsername;

    public MedBratBot(
            BotFlowService flow,
            @Value("${telegram-bot.token}") String botToken,
            @Value("${telegram-bot.username}") String botUsername
    ) {
        super(botToken);
        this.flow = flow;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null) {
            return;
        }
        flow.handle(update);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
