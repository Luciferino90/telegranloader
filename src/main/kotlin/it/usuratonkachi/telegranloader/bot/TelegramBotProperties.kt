package it.usuratonkachi.telegranloader.bot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot", ignoreUnknownFields = false)
class TelegramBotProperties {

    lateinit var username: String
    lateinit var token: String
    lateinit var chatid: String

}
