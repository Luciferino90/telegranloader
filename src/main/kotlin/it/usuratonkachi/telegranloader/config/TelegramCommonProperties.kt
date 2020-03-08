package it.usuratonkachi.telegranloader.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.common")
class TelegramCommonProperties {

    lateinit var downloadpath: String
    lateinit var owners: List<Int>

}
