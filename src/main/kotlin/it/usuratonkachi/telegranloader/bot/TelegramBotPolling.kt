package it.usuratonkachi.telegranloader.bot

import it.usuratonkachi.telegranloader.config.AnsweringBot
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import lombok.extern.slf4j.Slf4j
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.bots.TelegramWebhookBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Slf4j
@Component
@ConditionalOnMissingBean(TelegramWebhookBot::class)
class TelegramBotPolling(
        private var telegramCommonProperties: TelegramCommonProperties,
        private var telegramBotProperties: TelegramBotProperties
) : TelegramLongPollingBot(), AnsweringBot {

    companion object: Log

    private val updateHashMap: ConcurrentHashMap<Int, Update> = ConcurrentHashMap()
        @Synchronized get

    @PostConstruct
    private fun init() {
        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(this)
        } catch (e: TelegramApiException) {
            throw RuntimeException(e)
        }
    }

    override fun getBotUsername(): String = telegramBotProperties.username

    override fun getBotToken(): String = telegramBotProperties.token

    override fun onUpdateReceived(update: Update?) {
        update.takeIf { telegramCommonProperties.owners.contains(it!!.message.from.id) }
                ?.apply {
                    addMessage(this)
                    forwardMessage(telegramBotProperties.chatid, this.message.chatId, this.message.messageId)
                }
    }

    private fun forwardMessage(chatId: String, fromChatId: Long, messageId: Int) {
        val sendMessage: ForwardMessage = ForwardMessage(chatId, fromChatId, messageId)
        execute(sendMessage)
    }

    @Synchronized
    private fun addMessage(update: Update){
        updateHashMap[update.message.forwardDate] = update
    }

    @Synchronized
    override fun answerMessage(key: Int, response: String, remove: Boolean){
        if (!updateHashMap.containsKey(key)) return
        val update: Update = if (remove) updateHashMap.remove(key)!! else updateHashMap[key]!!
        val sendResponse = SendMessage(update.message.chatId, response)
        sendResponse.replyToMessageId = update.message.messageId
        execute(sendResponse)
        if (remove) deleteMessage(update.message.chatId, update.message.messageId)
    }

    private fun deleteMessage(chatId: Long, messageId: Int){
        val deleteMessage = DeleteMessage(chatId, messageId)
        execute(deleteMessage)
    }

}
