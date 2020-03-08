package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.utils.id
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import com.github.badoualy.telegram.tl.api.messages.TLAbsDialogs
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import org.springframework.stereotype.Component
import java.io.IOError
import java.util.*
import java.util.function.Consumer
import javax.annotation.PostConstruct


@Component
class TelegramApiClientConfigurator(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val telegramApiProperties: TelegramApiProperties,
        private val telegramApiListener: TelegramApiListener
) {

    @PostConstruct
    fun configureClient() {
        val telegramApp: TelegramApp = TelegramApp(telegramApiProperties.apiId.toInt(),
                telegramApiProperties.apiHash, telegramApiProperties.model, telegramApiProperties.systemVersion,
                telegramApiProperties.appVersion, telegramApiProperties.languageCode)

        val client: TelegramClient = Kotlogram.getDefaultClient(telegramApp, ApiStorage(), updateCallback = telegramApiListener)

        try {
            client.accountGetAuthorizations()
        } catch (e: IOError) {
            ifCodeIsRequired(client)
        }

    }

    fun ifCodeIsRequired(client: TelegramClient) {
        val sentCode: TLSentCode = client.authSendCode(false, telegramApiProperties.phoneNumber, true)
        print("Authentication Code: ")
        val code: String = Scanner(System.`in`).nextLine()

        val authorization: TLAuthorization = client.authSignIn(telegramApiProperties.phoneNumber, sentCode.phoneCodeHash, code)
        val self: TLUser = authorization.user.asUser
        print("You are signed as ${self.firstName} ${self.lastName} @ ${self.username}")
    }

    fun readMessages(client: TelegramClient) {
        val tlAbsDialogs: TLAbsDialogs = client.messagesGetDialogs(false, 0, 0, TLInputPeerEmpty(), 30)

        val inputPeer = getInputPeer(tlAbsDialogs)
        val tlAbsMessages = client.messagesGetHistory(inputPeer, 0, 0, 0, 10, 0, 0)

        val mes = tlAbsMessages.messages.map { it as TLMessage }.first()

        val messageMap = HashMap<Int, TLAbsMessage>()
        tlAbsDialogs.messages.forEach(Consumer { message: TLAbsMessage -> messageMap[message.id] = message })

        tlAbsDialogs.dialogs.forEach(Consumer { dialog: TLDialog ->
            //print(nameMap[getId(dialog.peer)].toString() + ": ")
            val topMessage = messageMap[dialog.topMessage]
            if (topMessage is TLMessage) { // The message could also be a file, a photo, a gif, ...
                println(topMessage.message)
            } else if (topMessage is TLMessageService) {
                val action = topMessage.action
                // action defined the type of message (user joined group, ...)
                println("Service message")
            }
        })
    }

    fun getInputPeer(tlAbsDialogs: TLAbsDialogs): TLAbsInputPeer? {
        val tlAbsPeer: TLAbsPeer = tlAbsDialogs.dialogs.filter { telegramCommonProperties.owners.contains((it.peer as TLPeerUser).userId) }.first().peer
        val peerId: Int = tlAbsPeer.id!!
        val peer = if (tlAbsPeer is TLPeerUser) tlAbsDialogs.users.stream().filter { user: TLAbsUser -> user.id == peerId }.findFirst().get() else tlAbsDialogs.chats.stream().filter { chat: TLAbsChat -> chat.id == peerId }.findFirst().get()
        if (peer is TLChannel) return TLInputPeerChannel(peer.id, peer.accessHash)
        if (peer is TLChat) return TLInputPeerChat(peer.id)
        return if (peer is TLUser) TLInputPeerUser(peer.id, peer.accessHash) else TLInputPeerEmpty()
    }

}
