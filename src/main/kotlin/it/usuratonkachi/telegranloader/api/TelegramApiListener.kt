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
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.util.concurrent.Queues
import java.nio.file.Path
import java.time.Duration


class MessageWrapper(var message: TLMessage, var client: TelegramClient)

@Component
class TelegramApiListener(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val answeringBotService: AnsweringBotService,
        private val downloaderSelector: DownloaderSelector,
        private val parserService: ParserService
) : UpdateCallback {

    companion object : Log

    private var messageQueue = Queues.unbounded<MessageWrapper>().get()
    private var updateFlux : Disposable = Flux.generate { sink: SynchronousSink<MessageWrapper> ->
                if (messageQueue.size != 0) {
                    val messageWrapper: MessageWrapper = messageQueue.poll()
                    sink.next(messageWrapper)
                } else {
                    sink.error(RuntimeException())
                }
            }
            .flatMap { reactorChainer(it.client, it.message) }
            .onErrorResume { Flux.empty() }
            .repeatWhen { it.delayElements(Duration.ofSeconds(1))}
            .subscribe()

    override fun onUpdates(client: TelegramClient, updates: TLUpdates) {
        Flux.fromStream(updates.updates.stream())
                .filter { it is TLUpdateNewMessage }
                .map { it as TLUpdateNewMessage }
                .map { it.message as TLMessage }
                .filter { telegramCommonProperties.owners.contains(it.fromId) }
                .map { MessageWrapper(it, client) }
                .doOnNext { messageQueue.add(it) }
                .subscribe()
    }

    private fun reactorChainer(client: TelegramClient, message: TLMessage): Mono<Pair<TLMessageMediaDocument, Path>> {
        return Mono.just(message)
                // Switch here for download by url / Magnet
                .filter { it.media != null && it.media is TLMessageMediaDocument }
                .map { it.media }
                .map { it as TLMessageMediaDocument to getFilename(it) }
                .flatMap { mediaPathPair ->
                    Mono.just(mediaPathPair)
                            .doOnNext { answeringBotService.answer(message, "Download started for " + it.second, false) }
                            .doOnNext { downloaderSelector.downloader(message, client, it.first, it.second) }
                            .doOnNext { answeringBotService.answer(message, "Download finished for " + it.second, false) }
                            .doOnNext { deleteRequest(client, message) }
                            .doOnNext { answeringBotService.answer(message, "Clean up finished for " + it.second, true) }
                            .doOnError {
                                val errorMsg = "Exception occurred during download for ${mediaPathPair.second} ${it.message}"
                                logger().error(errorMsg, it)
                                answeringBotService.answer(message, errorMsg, true)
                            }
                }
                .doOnError { logger().error("Exception occurred during retrieve of media filename ${it.message}", it) }
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
