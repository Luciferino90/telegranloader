package it.usuratonkachi.telegranloader.bot

import it.usuratonkachi.telegranloader.config.AnsweringBot
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.parser.ParserRefactorConfiguration
import it.usuratonkachi.telegranloader.service.TdlibDatabaseCleanerService
import jakarta.annotation.PostConstruct
import lombok.extern.slf4j.Slf4j
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.bots.TelegramWebhookBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Slf4j
@Service
@ConditionalOnMissingBean(TelegramWebhookBot::class)
class TelegramBotPolling(
    private var telegramCommonProperties: TelegramCommonProperties,
    private var telegramBotProperties: TelegramBotProperties,
    private var tdlibDatabaseCleanerService: TdlibDatabaseCleanerService,
    private var keyboardService: KeyboardService,
    private var parserRefactorConfiguration: ParserRefactorConfiguration
) : TelegramLongPollingBot(), AnsweringBot {

    companion object: Log

    private val updateHashMap: ConcurrentHashMap<Int, Update> = ConcurrentHashMap()
        @Synchronized get

    @PostConstruct
    private fun init() {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            botsApi.registerBot(this)
        } catch (e: TelegramApiException) {
            throw RuntimeException(e)
        }
    }

    override fun getBotUsername(): String = telegramBotProperties.username

    override fun getBotToken(): String = telegramBotProperties.token

    override fun onUpdateReceived(update: Update?) {
        update?.let {
            it.takeIf {
                (
                        if (update.hasCallbackQuery())
                            Optional.ofNullable(it)
                                .map { update -> update.callbackQuery }
                                .map { callbackQuery -> callbackQuery.from }
                                .map { from -> from.id }
                                .map { userId -> userId.toString() }
                        else
                            Optional.ofNullable(it)
                                .map { update -> update.message }
                                .map { message -> message.from }
                                .map { user -> user.id }
                                .map { userId -> userId.toString() }
                        )
                    .filter { userId -> telegramCommonProperties.owners.contains(userId) }
                    .isPresent
            }
                ?.apply { dispatcherMessage(this) }
        }
    }

    private fun dispatcherMessage(update: Update) = run {
        if (update.hasCallbackQuery())
            botKeyboard(update)
        else if (Optional.ofNullable(update).map { it.message }.map { it.text }.filter { StringUtils.hasText(it) }.filter { it.startsWith("/") }.isPresent)
            botCommand(update)
        else clientCommand(update)
    }

    private fun forwardMessage(chatId: Long, fromChatId: Long, messageId: Int) {
        val sendMessage = ForwardMessage(chatId.toString(), fromChatId.toString(), messageId)
        execute(sendMessage)
    }

    @Synchronized
    private fun addMessage(update: Update){
        updateHashMap[update.message.forwardDate ?: update.message.date] = update
    }

    @Synchronized
    override fun answerMessage(key: Int, response: String, remove: Boolean){
        if (!updateHashMap.containsKey(key)) return
        val update: Update = if (remove) updateHashMap.remove(key)!! else updateHashMap[key]!!
        answerMessage(update, response, remove)
    }

    private fun deleteMessage(chatId: String, messageId: Int){
        val deleteMessage = DeleteMessage(chatId, messageId)
        execute(deleteMessage)
    }

    fun answerMessage(update: Update, response: String, remove: Boolean){
        val sendResponse = SendMessage(update.message.chatId.toString(), response)
        sendResponse.replyToMessageId = update.message.messageId
        execute(sendResponse)
        if (remove) deleteMessage(update.message.chatId.toString(), update.message.messageId)
    }

    fun openKeyboard(update: Update, response: KeyboardResponse?){
        val chatId: String
        val replyToMessageId : Int
        if (update.hasCallbackQuery()) {
            chatId = update.callbackQuery.message.chatId.toString()
            replyToMessageId = update.callbackQuery.message.replyToMessage.messageId
        } else {
            chatId = update.message.chatId.toString()
            replyToMessageId = update.message.messageId
        }
        val sendResponse = SendMessage(chatId, response?.response ?: "")
        sendResponse.replyToMessageId = replyToMessageId
        sendResponse.replyMarkup = response?.keyboard
        execute(sendResponse)
    }

    fun botCommand(update: Update) {
        val command: String = update.message.text
        if (!command.startsWith("/")) return
        when(command.replace("/", "").split(" ")[0]) {
            "dryrun" -> {
                val dryRun = command.replace("/dryrun", "").trim().toBoolean()
                telegramCommonProperties.setDryRun(dryRun)
                answerMessage(update, "DryRun is $dryRun", true)
            }
            "config" -> {
                val extraValues = command.replace("/config", "").split(" ").filter { StringUtils.hasText(it) }
                val response : String = when(extraValues.size) {
                    0 -> parserRefactorConfiguration.getConfiguration()
                    1 -> parserRefactorConfiguration.getConfiguration(extraValues[0].trim())
                    2 -> parserRefactorConfiguration.getConfiguration(extraValues[0].trim(), extraValues[1].trim().toInt())
                    else -> "Input error, please look at /help"
                }
                answerMessage(update, response, false)
            }
            "add_rule" -> {
                val extraValues = command.replace("/add_rule", "").split(" ")
                val seriesName = extraValues[1].trim()
                val input = extraValues.takeLast(extraValues.size - 2).joinToString(" ")
                val response: String = parserRefactorConfiguration.addConfiguration(seriesName, input)
                answerMessage(update, response, false)
            }
            "remove_rule" -> {
                val extraValues = command.replace("/remove_rule", "").split(" ")
                val seriesName = extraValues[1].trim()
                val configNumber = extraValues[2].trim().toInt()
                val response: String = parserRefactorConfiguration.removeConfiguration(seriesName, configNumber)
                answerMessage(update, response, false)
            }
            "set_userid" -> {
                val extraValues = command.replace("/set_userid", "").split(" ")
                val seriesName = extraValues[1].trim()
                val telegramUserId = extraValues[2].trim()
                val response: String = parserRefactorConfiguration.setChatUsername(seriesName, telegramUserId)
                answerMessage(update, response, false)
            }
            "set_type" -> {
                val extraValues = command.replace("/set_type", "").split(" ")
                val seriesName = extraValues[1].trim()
                val type = extraValues[2].trim().trim()
                val response: String = parserRefactorConfiguration.setType(seriesName, type)
                answerMessage(update, response, false)
            }
            "clean" -> tdlibDatabaseCleanerService.cleanDatabase()
            "help" -> {
                val response = keyboardService.helpResponse
                answerMessage(update, response, false)
            }
            else -> {
                val keyboardMarkup = keyboardService.buildKeyBoard()
                openKeyboard(update, keyboardMarkup)
            }
        }
    }

    fun botKeyboard(update: Update) {
        val keyboardMarkup = keyboardService.buildKeyBoard(update.callbackQuery)
        openKeyboard(update, keyboardMarkup)
    }

    fun clientCommand(update: Update) {
        addMessage(update)
        forwardMessage(telegramBotProperties.chatid.toLong(), update.message.chatId, update.message.messageId)
    }

}
