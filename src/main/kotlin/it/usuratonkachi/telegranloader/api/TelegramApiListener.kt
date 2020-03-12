package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.UpdateCallback
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.core.TLIntVector
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.downloader.DownloaderSelector
import it.usuratonkachi.telegranloader.parser.ParserService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path

@Component
class TelegramApiListener(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val answeringBotService: AnsweringBotService,
        private val downloaderSelector: DownloaderSelector,
        private val parserService: ParserService
) : UpdateCallback {

    companion object : Log

    override fun onUpdates(client: TelegramClient, updates: TLUpdates) {
        reactorChainer(client, updates)
                .subscribe()
    }

    fun reactorChainer(client: TelegramClient, updates: TLUpdates): Flux<Pair<TLMessageMediaDocument, Path>> {
        return Flux.fromStream(updates.updates.stream())
                .filter { it is TLUpdateNewMessage }
                .map { it as TLUpdateNewMessage }
                .map { it.message as TLMessage }
                .flatMap { tlMessage ->
                    Mono.just(tlMessage)
                            .filter { telegramCommonProperties.owners.contains(it.fromId) }
                            .filter { it.media != null && it.media is TLMessageMediaDocument }
                            .map { it.media }
                            .map { it as TLMessageMediaDocument to getFilename(it) }
                            .flatMap {
                                mediaPathPair -> Mono.just(mediaPathPair)
                                    .doOnNext { answeringBotService.answer(tlMessage, "Download started for " + it.second, false) }
                                    .doOnNext { downloaderSelector.downloader(tlMessage, client, it.first, it.second) }
                                    .doOnNext { answeringBotService.answer(tlMessage, "Download finished for " + it.second, false) }
                                    .doOnNext { deleteRequest(client, tlMessage) }
                                    .doOnNext { answeringBotService.answer(tlMessage, "Clean up finished for " + it.second, true) }
                                    .doOnError {
                                        val errorMsg = "Exception occurred during download for ${mediaPathPair.second} ${it.message}"
                                        logger().error(errorMsg, it)
                                        answeringBotService.answer(tlMessage, errorMsg, true)
                                    }
                            }
                            .doOnError { logger().error("Exception occurred during retrieve of media filename ${it.message}", it) }
                }
                .doOnError { logger().error("Exception occurred during on message received ${it.message}", it) }

    }

    private fun getFilename(media: TLMessageMediaDocument): Path =
            parserService.getEpisodeWrapper(media.document.asDocument.attributes.filterIsInstance<TLDocumentAttributeFilename>().last().fileName)

    private fun deleteRequest(client: TelegramClient, message: TLMessage) {
        val vector = TLIntVector()
        vector.add(message.id)
        client.messagesDeleteMessages(true, vector)
        logger().debug("Delete Message Done")
    }

    override fun onUpdatesCombined(client: TelegramClient, updates: TLUpdatesCombined) {
        return
    }

    override fun onShortChatMessage(client: TelegramClient, message: TLUpdateShortChatMessage) {
        return
    }

    override fun onShortMessage(client: TelegramClient, message: TLUpdateShortMessage) {
        if (telegramCommonProperties.owners.contains(message.userId))
            println("onShortMessage")
        return
    }

    override fun onShortSentMessage(client: TelegramClient, message: TLUpdateShortSentMessage) {
        return
    }

    override fun onUpdateShort(client: TelegramClient, update: TLUpdateShort) {
        return
    }

    override fun onUpdateTooLong(client: TelegramClient) {
        return
    }

}
