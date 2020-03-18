package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLUser
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import org.springframework.stereotype.Component
import java.util.*
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
        } catch (e: Exception) {
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

}
