package it.usuratonkachi.telegranloader.api

import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.downloader.DownloaderSelector
import it.usuratonkachi.telegranloader.wrapper.DownloadType
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.util.concurrent.Queues
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration


@Service
class TelegramClientService(
    private val telegramCommonProperties: TelegramCommonProperties,
    private val downloaderSelector: DownloaderSelector
) {

    companion object : Log

    private var messageQueue = Queues.unbounded<DownloadWrapper>().get()
    @SuppressWarnings("unused")
    private var updateFlux : Disposable = Flux.generate { sink: SynchronousSink<DownloadWrapper> ->
        if (messageQueue.size != 0) {
            val messageWrapper: DownloadWrapper = messageQueue.poll()
            sink.next(messageWrapper)
        } else {
            sink.error(RuntimeException())
        }
    }
        .flatMap { downloaderSelector.reactorDownloader(it) }
        .doOnError { ex ->
            if (ex !is java.lang.RuntimeException)
                ex.printStackTrace()
        }
        .onErrorResume { Flux.empty() }
        .repeatWhen { it.delayElements(Duration.ofSeconds(1))}
        .subscribe()

    /*fun onShortMessage(client: TelegramClient, message: TLUpdateShortMessage) {
        if (telegramCommonProperties.owners.contains(message.userId)
            && (message.message.startsWith("http://")
                    || message.message.startsWith("https://")
                    || message.message.startsWith("magnet:")
                    || message.message.startsWith("www.")
                    )
        )
            messageQueue.add(MessageWrapper(messageString = message.message, client = client))
    }*/

    // TODO
    // Episode: (((updates.updates[0] as TLUpdateNewMessage).message as TLMessage).media as TLMessageMediaDocument).caption
    // Series:

    /*fun onUpdates(client: TelegramClient, updates: TLUpdates) {
        Flux.fromStream(updates.updates.stream())
            .filter { it is TLUpdateNewMessage }
            .map { it as TLUpdateNewMessage }
            .map { it.message as TLMessage }
            .filter { telegramCommonProperties.owners.contains(it.fromId) }
            .map { MessageWrapper(message = it, client = client, episode = (it.media as TLMessageMediaDocument).caption) }
            .doOnNext { messageQueue.add(it) }
            .subscribe()
    }*/

    fun onUpdates(updates: TdApi.UpdateNewMessage) {
        Mono.just(updates)
            .filter{ telegramCommonProperties.owners.contains(it.message.chatId.toInt()) }
            .map {
                val message: TdApi.Message = it.message
                val chatId: Long = message.chatId
                val messageId: Long = message.id
                val date: Int = message.forwardInfo?.date ?: message.date
                val content : TdApi.MessageContent = message.content

                if (content is TdApi.MessageVideo) {
                    val video : TdApi.MessageVideo = message.content as TdApi.MessageVideo
                    val caption: String = video.caption.text
                    val filename: String = video.video.fileName
                    val expectedSize : Int = video.video.video.expectedSize
                    DownloadWrapper(
                        chatId,
                        messageId,
                        date,
                        expectedSize,
                        caption,
                        filename,
                        video,
                        "",
                        DownloadType.FILE,
                        null,
                        null
                    )
                } else {
                    // TODO ME
                    DownloadWrapper(
                        chatId,
                        messageId,
                        0,
                        null,
                        null,
                        null,
                        null,
                        "",
                        DownloadType.FILE,
                        null,
                        null
                    )
                }
            }
            .map { messageQueue.add(it) }
            .subscribe()
    }

}
