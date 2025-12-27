package com.doctor.notify.bot;

import com.doctor.notify.bot.flow.BotFlowService;
import com.doctor.notify.secrets.VaultSecretsService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class MedBratBot extends TelegramLongPollingBot {

    private final BotFlowService flow;

    @Getter
    private final String username;

    private final String token;

    @Getter
    private final Long doctorChatId;

    public MedBratBot(
            BotFlowService flow,
            VaultSecretsService vaultSecrets,
            @Value("${telegram-bot.username:}") String usernameEnv,
            @Value("${telegram-bot.token:}") String tokenEnv,
            @Value("${telegram.doctor.chat-id}") Long doctorChatId
    ) {
        this.flow = flow;
        this.username = vaultSecrets.resolve("telegram-bot.username", usernameEnv);
        this.token = vaultSecrets.resolve("telegram-bot.token", tokenEnv);
        this.doctorChatId = doctorChatId;
        if (this.username == null || this.username.isBlank()) throw new IllegalStateException("Telegram bot username is empty");
        if (this.token == null || this.token.isBlank()) throw new IllegalStateException("Telegram bot token is empty");
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        flow.handle(update);
    }
}