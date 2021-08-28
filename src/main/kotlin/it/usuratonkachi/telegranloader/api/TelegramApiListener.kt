package it.usuratonkachi.telegranloader.api

/*

class MessageWrapper(var message: TLMessage? = null, var client: TelegramClient, var messageString: String? = null, var episode: String? = null)

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
            .flatMap { if (it.message != null) downloaderSelector.reactorDownloader(it.client, it) else downloaderSelector.reactorDownloader(it.client, it.messageString!!) }
            .doOnError { ex ->
                if (ex !is java.lang.RuntimeException)
                    ex.printStackTrace()
            }
            .onErrorResume { Flux.empty() }
            .repeatWhen { it.delayElements(Duration.ofSeconds(1))}
            .subscribe()

    override fun onShortMessage(client: TelegramClient, message: TLUpdateShortMessage) {
        if (telegramCommonProperties.owners.contains(message.userId)
                && (message.message.startsWith("http://")
                        || message.message.startsWith("https://")
                        || message.message.startsWith("magnet:")
                        || message.message.startsWith("www.")
                        )
        )
            messageQueue.add(MessageWrapper(messageString = message.message, client = client))
    }

    // TODO
    // Episode: (((updates.updates[0] as TLUpdateNewMessage).message as TLMessage).media as TLMessageMediaDocument).caption
    // Series:

    override fun onUpdates(client: TelegramClient, updates: TLUpdates) {
        Flux.fromStream(updates.updates.stream())
                .filter { it is TLUpdateNewMessage }
                .map { it as TLUpdateNewMessage }
                .map { it.message as TLMessage }
                .filter { telegramCommonProperties.owners.contains(it.fromId) }
                .map { MessageWrapper(message = it, client = client, episode = (it.media as TLMessageMediaDocument).caption) }
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
*/
