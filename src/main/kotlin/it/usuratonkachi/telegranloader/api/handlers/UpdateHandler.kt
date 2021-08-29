package it.usuratonkachi.telegranloader.api.handlers

import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.api.TelegramClientService
import it.usuratonkachi.telegranloader.config.TelegramApiProperties
import it.usuratonkachi.telegranloader.service.TdlibDatabaseCleanerService
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Component
class UpdateHandler(
    private val client: TelegramClient,
    private val telegramApiProperties: TelegramApiProperties,
    private val telegramClientService: TelegramClientService,
    private val tdlibDatabaseCleanerService: TdlibDatabaseCleanerService
) : ResultHandler {

    @Volatile
    private var haveAuthorization = false
    private var newLine = System.getProperty("line.separator")
    private var authorizationState: TdApi.AuthorizationState? = null
    private val authorizationLock: Lock = ReentrantLock()
    private val gotAuthorization = authorizationLock.newCondition()

    override fun onResult(tdApiObj: TdApi.Object) {
        println("${tdApiObj.constructor}: $tdApiObj")
        when (tdApiObj.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> onAuthorizationStateUpdated((tdApiObj as TdApi.UpdateAuthorizationState).authorizationState)
            TdApi.UpdateNewMessage.CONSTRUCTOR -> telegramClientService.onUpdates(tdApiObj as TdApi.UpdateNewMessage)
            else -> {
                println("${tdApiObj.constructor}: $tdApiObj")
            }
        }
    }

    fun checkTdlibDatabase() {
        Path.of(telegramApiProperties.databasePath).toFile().mkdirs()
        tdlibDatabaseCleanerService.cleanDatabase()
    }

    fun onAuthorizationStateUpdated(authorizationState: TdApi.AuthorizationState?) {
        if (authorizationState != null) {
            this.authorizationState = authorizationState
        }
        when (authorizationState!!.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                checkTdlibDatabase()
                val parameters = TdApi.TdlibParameters()
                parameters.databaseDirectory = telegramApiProperties.databasePath
                parameters.useMessageDatabase = true
                parameters.useSecretChats = false
                parameters.apiId = telegramApiProperties.apiId.toInt()
                parameters.apiHash = telegramApiProperties.apiHash
                parameters.systemLanguageCode = telegramApiProperties.languageCode
                parameters.deviceModel = telegramApiProperties.model
                parameters.systemVersion = telegramApiProperties.systemVersion
                parameters.applicationVersion = telegramApiProperties.appVersion
                parameters.enableStorageOptimizer = true
                client.send(TdApi.SetTdlibParameters(parameters), this)
            }
            TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> client.send(
                TdApi.CheckDatabaseEncryptionKey(),
                this
            )
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                client.send(
                    TdApi.SetAuthenticationPhoneNumber(telegramApiProperties.phoneNumber, null),
                    this
                )
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                val code = askAuthenticationCode()
                client.send(TdApi.CheckAuthenticationCode(code), this /*AuthorizationRequestHandler(client, telegramApiProperties)*/)
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                haveAuthorization = true
                authorizationLock.lock()
                try {
                    gotAuthorization.signal()
                } finally {
                    authorizationLock.unlock()
                }
            }
            else -> System.err.println("Unsupported authorization state:$newLine$authorizationState")
        }
    }

    private fun askAuthenticationCode(): String {
        println("Please enter authentication code: ")
        val reader = BufferedReader(InputStreamReader(System.`in`))
        var str = ""
        try {
            str = reader.readLine()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return str
    }

}
