package it.usuratonkachi.telegranloader.api

import DefaultHandler
import it.tdlight.common.Init
import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.tdlight.tdlib.ClientManager
import it.usuratonkachi.telegranloader.api.handlers.ErrorHandler
import it.usuratonkachi.telegranloader.api.handlers.UpdateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOError
import java.io.IOException


@Configuration
class TelegramApiClientConfigurator(
    private val updateHandler: UpdateHandler,
    private val errorHandler: ErrorHandler
) {

    @Bean
    fun client() : TelegramClient {
        Init.start()
        var client = ClientManager.create()
        client.initialize(updateHandler, errorHandler, errorHandler)
        client.execute(TdApi.SetLogVerbosityLevel(0))
        // disable TDLib log
        if (client.execute(
                TdApi.SetLogStream(
                    TdApi.LogStreamFile(
                        "tdlib.log",
                        1 shl 27,
                        false
                    )
                )
            ) is TdApi.Error
        ) {
            throw IOError(IOException("Write access to the current directory is required"))
        }

        // test Client.execute
        DefaultHandler().onResult(client!!.execute(TdApi.GetTextEntities("@telegram /test_command https://telegram.org telegram.me @gif @test")))

        return client
    }

    class DefaultHandler : ResultHandler {
        override fun onResult(tdApiObj: TdApi.Object) {
            print(tdApiObj.toString())
        }
    }


}
