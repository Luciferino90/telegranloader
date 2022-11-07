package it.usuratonkachi.telegranloader.api

import ch.qos.logback.core.util.ContentTypeUtil
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.downloader.DownloaderSelector
import it.usuratonkachi.telegranloader.wrapper.DownloadType
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.core.scheduler.Schedulers
import reactor.util.concurrent.Queues
import java.time.Duration


@Service
class TelegramClientService(
    private val telegramCommonProperties: TelegramCommonProperties,
    private val downloaderSelector: DownloaderSelector,
    private val telegramUserInfoService: TelegramUserInfoService
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
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe()

    fun onUpdates(updates: TdApi.UpdateNewMessage) {
        Mono.just(updates)
            .subscribeOn(Schedulers.boundedElastic())
            .filter{ telegramCommonProperties.owners.contains(it.message.chatId.toString()) }
            .map {
                val message: TdApi.Message = it.message
                val chatId: Long = message.chatId
                val messageId: Long = message.id
                val date: Int = message.forwardInfo?.date ?: message.date
                val content : TdApi.MessageContent = message.content
                val userData : UserData? = if (message.forwardInfo.origin is TdApi.MessageForwardOriginChannel)
                    telegramUserInfoService.getChat((message.forwardInfo.origin as TdApi.MessageForwardOriginChannel).chatId)
                else
                    null
                when (content) {
                    is TdApi.MessageVideo -> {
                        val video : TdApi.MessageVideo = message.content as TdApi.MessageVideo
                        val caption: String = video.caption.text
                        val extension: String = ContentTypeUtil.getSubType("video/mp4")
                        val filename: String = if ( StringUtils.hasText(video.video.fileName) ) video.video.fileName else caption + "." + extension
                        val expectedSize : Int = video.video.video.expectedSize.toInt()
                        DownloadWrapper(
                            chatId,
                            messageId,
                            date,
                            expectedSize,
                            caption,
                            filename,
                            video.video.video.id,
                            video,
                            "",
                            DownloadType.FILE,
                            null,
                            null,
                            userData
                        )
                    }
                    is TdApi.MessageDocument -> {
                        DownloadWrapper(
                            chatId,
                            messageId,
                            date,
                            content.document.document.size.toInt(),
                            content.document.fileName,
                            content.document.fileName,
                            content.document.document.id,
                            content,
                            content.document.fileName,
                            DownloadType.FILE,
                            null,
                            content.document.mimeType,
                            userData
                        )
                    }
                    is TdApi.MessageText -> {
                        val text: String = content.text.text
                        val fallbackText: String = text.replace(":", "_").replace("?", "-")
                        DownloadWrapper(
                            chatId,
                            messageId,
                            date,
                            null,
                            content.webPage?.document?.fileName ?: fallbackText,
                            content.webPage?.document?.fileName ?: fallbackText,
                            -1,
                            null,
                            content.webPage?.url ?: text,
                            DownloadType.URL,
                            null,
                            content.webPage?.document?.mimeType,
                            userData
                        )
                    }
                    else -> {
                        DownloadWrapper(
                            chatId,
                            messageId,
                            date,
                            null,
                            "",
                            "",
                            -1,
                            null,
                            "",
                            DownloadType.URL,
                            null,
                            "",
                            userData
                        )
                    }
                }
            }
            .map { messageQueue.add(it) }
            .subscribe()
    }

}
