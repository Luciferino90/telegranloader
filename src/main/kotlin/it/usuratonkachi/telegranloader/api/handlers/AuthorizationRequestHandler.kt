package it.usuratonkachi.telegranloader.api.handlers

import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.tdlight.tdlib.ClientManager
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Component
class AuthorizationRequestHandler(
    private var client: TelegramClient,
) : ResultHandler {

    private var newLine = System.getProperty("line.separator")
    private var authorizationState: TdApi.AuthorizationState? = null
    private val authorizationLock: Lock = ReentrantLock()
    private val gotAuthorization = authorizationLock.newCondition()
    @Volatile
    private var haveAuthorization = false
    @Volatile
    private var needQuit = false
    @Volatile
    private var canQuit = false

    @Volatile
    private var currentPrompt: String? = null
    private fun printo(str: String?) {
        if (currentPrompt != null) {
            println("")
        }
        println(str)
        if (currentPrompt != null) {
            print(currentPrompt)
        }
    }

    override fun onResult(`object`: TdApi.Object) {
        when (`object`.constructor) {
            TdApi.Error.CONSTRUCTOR -> {
                System.err.println("Receive an error:" + newLine + `object`)
                onAuthorizationStateUpdated(null) // repeat last action
            }
            TdApi.Ok.CONSTRUCTOR -> {
            }
            else -> System.err.println("Receive wrong response from TDLib:" + newLine + `object`)
        }
    }

    fun onAuthorizationStateUpdated(authorizationState: TdApi.AuthorizationState?) {
        if (authorizationState != null) {
            this.authorizationState = authorizationState
        }
        when (authorizationState!!.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                val parameters = TdApi.TdlibParameters()
                parameters.databaseDirectory = "tdlib"
                parameters.useMessageDatabase = true
                parameters.useSecretChats = true
                parameters.apiId = 94575
                parameters.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2"
                parameters.systemLanguageCode = "en"
                parameters.deviceModel = "Desktop"
                parameters.applicationVersion = "1.0"
                parameters.enableStorageOptimizer = true
                client.send(TdApi.SetTdlibParameters(parameters), AuthorizationRequestHandler(client))
            }
            TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> client.send(
                TdApi.CheckDatabaseEncryptionKey(),
                AuthorizationRequestHandler(client)
            )
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                val phoneNumber = promptString("Please enter phone number: ")
                client.send(
                    TdApi.SetAuthenticationPhoneNumber(phoneNumber, null),
                    AuthorizationRequestHandler(client)
                )
            }
            TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> {
                val link = (authorizationState as TdApi.AuthorizationStateWaitOtherDeviceConfirmation?)!!.link
                println("Please confirm this login link on another device: $link")
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                val code = promptString("Please enter authentication code: ")
                client.send(TdApi.CheckAuthenticationCode(code), AuthorizationRequestHandler(client))
            }
            TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR -> {
                val firstName = promptString("Please enter your first name: ")
                val lastName = promptString("Please enter your last name: ")
                client.send(TdApi.RegisterUser(firstName, lastName), AuthorizationRequestHandler(client))
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                val password = promptString("Please enter password: ")
                client.send(
                    TdApi.CheckAuthenticationPassword(password),
                    AuthorizationRequestHandler(client)
                )
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
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                haveAuthorization = false
                printo("Logging out")
            }
            TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                haveAuthorization = false
                printo("Closing")
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                printo("Closed")
                if (!needQuit) {
                    client = ClientManager.create() // recreate client after previous has closed
                    client.initialize(UpdateHandler(this), ErrorHandler(), ErrorHandler())
                } else {
                    canQuit = true
                }
            }
            else -> System.err.println("Unsupported authorization state:" + newLine + authorizationState)
        }
    }

    private fun promptString(prompt: String): String {
        printo(prompt)
        currentPrompt = prompt
        val reader = BufferedReader(InputStreamReader(System.`in`))
        var str = ""
        try {
            str = reader.readLine()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        currentPrompt = null
        return str
    }

}
