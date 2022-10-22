package it.usuratonkachi.telegranloader.api

import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.config.AnsweringBot
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service


@Service
class AnsweringBotService(private val AnsweringBot: AnsweringBot, private val client: TelegramClient){

    fun answer(downloadWrapper: DownloadWrapper, response: String, remove: Boolean) {
        AnsweringBot.answerMessage(downloadWrapper.date, response, remove)
    }

    fun deleteRequest(downloadWrapper: DownloadWrapper) {
        client.send(TdApi.DeleteMessages(downloadWrapper.chatId, longArrayOf(downloadWrapper.messageId), true)) {}
        TelegramClientService.logger().debug("Delete Message Done")
    }

}
