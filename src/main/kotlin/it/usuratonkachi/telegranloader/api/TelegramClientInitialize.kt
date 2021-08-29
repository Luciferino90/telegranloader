package it.usuratonkachi.telegranloader.api

import it.tdlight.common.TelegramClient
import it.tdlight.tdlib.ClientManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class TelegramClientInitialize {

    @Bean
    fun client() : TelegramClient = ClientManager.create()

}
