package it.usuratonkachi.telegranloader.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "telegram.api", ignoreUnknownFields = false)
class TelegramApiProperties {

    lateinit var apiId: Integer
    lateinit var apiHash: String
    lateinit var appVersion: String
    lateinit var model: String
    lateinit var systemVersion: String
    lateinit var languageCode: String
    lateinit var phoneNumber: String

}
