package it.usuratonkachi.telegranloader.telegranloader.bot;

import it.usuratonkachi.telegranloader.telegranloader.service.BotEntryPointHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(TelegramWebhookBot.class)
public class TelegranLoaderPolling extends TelegramLongPollingBot {

    @Value("${telegram.ownertelegramid}")
    private Integer ownerId;

    private final TelegranLoaderProperties telegranLoaderProperties;
    private final BotEntryPointHandler botEntryPointHandler;

    @Override
    public void onUpdateReceived(Update update) {
        if (ownerId.equals(update.getMessage().getFrom().getId()) && update.getMessage().hasDocument()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(update.getMessage().getChatId().toString());
            sendMessage.setText("Well, all information looks like noise until you break the code.");
            botEntryPointHandler.downloadFlow(update.getMessage().getDocument().getFileId())
                    .doOnNext(response -> {
                        try {
                            sendMessage.setText(response.getMessage());
                            execute(sendMessage);
                        } catch (Exception ex) {
                            throw new RuntimeException("foo");
                        }
                    })
                    .subscribe();
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);
    }

    @Override
    public String getBotUsername() {
        return telegranLoaderProperties.getUsername();
    }

    @Override
    public String getBotToken() {
        return telegranLoaderProperties.getToken();
    }

    @PostConstruct
    private void init(){
        log.info("Bot started in " + this.getClass().getSimpleName() +" mode");
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
