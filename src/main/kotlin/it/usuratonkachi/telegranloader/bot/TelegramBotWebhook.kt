package it.usuratonkachi.telegranloader.bot

import it.usuratonkachi.telegranloader.config.AnsweringBot
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import lombok.extern.slf4j.Slf4j
import org.telegram.telegrambots.bots.TelegramWebhookBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import javax.annotation.PostConstruct

@Slf4j
//@Component
//@ConditionalOnMissingBean(TelegramWebhookBot::class)
class TelegramBotWebhook(
        private val telegramBotProperties: TelegramBotProperties,
        private val telegramCommonProperties: TelegramCommonProperties
) : TelegramWebhookBot(), AnsweringBot {

    companion object : Log

    @PostConstruct
    private fun init() {
        logger().info("Bot started in " + this.javaClass.simpleName + " mode")
        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(this)
        } catch (e: TelegramApiException) {
            throw RuntimeException(e)
        }
    }

    override fun getBotPath(): String = telegramCommonProperties.downloadpath

    override fun getBotUsername(): String = telegramBotProperties.username

    override fun getBotToken(): String = telegramBotProperties.token

    override fun onWebhookUpdateReceived(update: Update?): BotApiMethod<*>? {
        if (update!!.hasMessage() && update.message.hasText()) {
            val sendMessage = SendMessage()
            sendMessage.chatId = update.message.chatId.toString()
            sendMessage.text = "Well, all information looks like noise until you break the code."
            return sendMessage
        }
        return null
    }

    @Synchronized
    override fun answerMessage(key: Int, response: String, remove: Boolean): Nothing = run { throw RuntimeException("Not yet implemented on " + this.javaClass.simpleName) }

}
