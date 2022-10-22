package it.usuratonkachi.telegranloader.api.handlers

import it.tdlight.client.TelegramError
import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.SetTdlibParameters
import it.usuratonkachi.telegranloader.api.TelegramClientService
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramApiProperties
import it.usuratonkachi.telegranloader.service.TdlibDatabaseCleanerService
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Service
class OkHandler (
    private val client: TelegramClient,
    private val telegramApiProperties: TelegramApiProperties,
    private val telegramClientService: TelegramClientService,
    private val tdlibDatabaseCleanerService: TdlibDatabaseCleanerService
)  : ResultHandler<TdApi.Ok> {

    companion object : Log

    @Volatile
    private var haveAuthorization = false
    private var newLine = System.getProperty("line.separator")
    private var authorizationState: TdApi.AuthorizationState? = null
    private val authorizationLock: Lock = ReentrantLock()
    private val gotAuthorization = authorizationLock.newCondition()

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

                val parameters = SetTdlibParameters()
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
                client.send(parameters, { ok: TdApi.Object ->
                    if (ok.constructor == TdApi.Error.CONSTRUCTOR) {
                        throw TelegramError(ok as TdApi.Error)
                    }
                }, { it.printStackTrace() })
            }
            //TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> client.send(TdApi.CheckDatabaseEncryptionKey()) { println("") }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> { client.send(TdApi.SetAuthenticationPhoneNumber(telegramApiProperties.phoneNumber, null)) { println("") } }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                val code = askAuthenticationCode()
                client.send(TdApi.CheckAuthenticationCode(code)) { println("") }
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

    override fun onResult(tdApiObject: TdApi.Object?) {
        if (logger().isDebugEnabled)
            logger().debug("${tdApiObject?.constructor}: $tdApiObject")
        when (tdApiObject?.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> onAuthorizationStateUpdated((tdApiObject as TdApi.UpdateAuthorizationState).authorizationState)
            TdApi.UpdateNewMessage.CONSTRUCTOR -> telegramClientService.onUpdates(tdApiObject as TdApi.UpdateNewMessage)
            else -> {
                if (UpdateHandler.logger().isDebugEnabled)
                    UpdateHandler.logger().debug("${tdApiObject?.constructor}: $tdApiObject")
            }
        }
    }

}