package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.UpdateCallback
import com.github.badoualy.telegram.tl.api.*
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.downloader.DownloaderSelector
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import reactor.util.concurrent.Queues
import java.time.Duration


class MessageWrapper(var message: TLMessage? = null, var client: TelegramClient, var messageString: String? = null)

@Component
class TelegramApiListener(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val downloaderSelector: DownloaderSelector
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
            .flatMap { if (it.message != null) downloaderSelector.reactorDownloader(it.client, it.message!!) else downloaderSelector.reactorDownloader(it.client, it.messageString!!) }
            .onErrorResume { Flux.empty() }
            .repeatWhen { it.delayElements(Duration.ofSeconds(1))}
            .subscribe()

    override fun onShortMessage(client: TelegramClient, message: TLUpdateShortMessage) {
        if (telegramCommonProperties.owners.contains(message.userId)
                || message.message.startsWith("http://")
                || message.message.startsWith("https://")
                || message.message.startsWith("magnet:")
                || message.message.startsWith("www.")
        )
            messageQueue.add(MessageWrapper(messageString = message.message, client = client))
    }

    override fun onUpdates(client: TelegramClient, updates: TLUpdates) {
        Flux.fromStream(updates.updates.stream())
                .filter { it is TLUpdateNewMessage }
                .map { it as TLUpdateNewMessage }
                .map { it.message as TLMessage }
                .filter { telegramCommonProperties.owners.contains(it.fromId) }
                .map { MessageWrapper(message = it, client = client) }
                .doOnNext { messageQueue.add(it) }
                .subscribe()
    }

    override fun onUpdatesCombined(client: TelegramClient, updates: TLUpdatesCombined) {
        return
    }

    override fun onShortChatMessage(client: TelegramClient, message: TLUpdateShortChatMessage) {
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
