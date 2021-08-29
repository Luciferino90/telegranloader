package it.usuratonkachi.telegranloader.api

import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.api.handlers.ErrorHandler
import it.usuratonkachi.telegranloader.api.handlers.UpdateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOError
import java.io.IOException


@Configuration
class TelegramApiClientConfigurator(
    private val client: TelegramClient,
    private val updateHandler: UpdateHandler,
    private val errorHandler: ErrorHandler
) {

    @Bean
    fun clientConfiguration() : TelegramClient {
        // val updateHandler = UpdateHandler(client)
        client.initialize(updateHandler, errorHandler, errorHandler)
        client.execute(TdApi.SetLogVerbosityLevel(0))
        // disable TDLib log
        if (client.execute(
                TdApi.SetLogStream(
                    TdApi.LogStreamFile(
                        "tdlib.log",
                        1000,
                        false
                    )
                )
            ) is TdApi.Error
        ) {
            throw IOError(IOException("Write access to the current directory is required"))
        }
        return client
    }

}
