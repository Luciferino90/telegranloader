package it.usuratonkachi.telegranloader.api

import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Service
class TelegramUserInfoService(
    private var client: TelegramClient
) {

    companion object {
        var userMap: ConcurrentHashMap<Long, UserData> = ConcurrentHashMap()
    }

    fun getChat(chatId: Long): UserData? {
        val semaphore: CountDownLatch = CountDownLatch(1)
        client.send(TdApi.GetChat(chatId), ChatDownloadHandler(client, semaphore))
        try {
            semaphore.await(1, TimeUnit.SECONDS)
        } catch (ignored: Throwable) {}
        return userMap.getOrDefault(chatId, null)
    }

    private class ChatDownloadHandler(
        private var client: TelegramClient,
        private var semaphore: CountDownLatch
    )
        : ResultHandler<TdApi.Chat> {
        override fun onResult(tdApiObj: TdApi.Object) {
            val chat = tdApiObj as TdApi.Chat
            // TODO Cover more options
            when (chat.type.constructor) {
                TdApi.ChatTypePrivate.CONSTRUCTOR -> semaphore.countDown()
                TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> semaphore.countDown()
                TdApi.ChatTypeSupergroup.CONSTRUCTOR -> client.send(TdApi.GetSupergroup((chat.type as TdApi.ChatTypeSupergroup).supergroupId),
                    SuperGroupDownloadHandler(chat.id, semaphore)
                )
                TdApi.ChatTypeSecret.CONSTRUCTOR -> semaphore.countDown()
                else -> semaphore.countDown()
            }
        }
    }

    private class SuperGroupDownloadHandler(
        val chatId : Long,
        private var semaphore: CountDownLatch
    )
        : ResultHandler<TdApi.Supergroup> {
        override fun onResult(tdApiObj: TdApi.Object) {
            userMap.putIfAbsent(chatId, UserData(TdApi.ChatTypeSupergroup(), (tdApiObj as TdApi.Supergroup).id, tdApiObj.username, chatId))
            semaphore.countDown()
        }
    }

}

data class UserData (
    val chatType : TdApi.ChatType,
    val userId : Long,
    val username : String,
    val chatId : Long
)