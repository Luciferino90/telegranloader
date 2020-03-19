package it.usuratonkachi.telegranloader.config

import org.springframework.boot.context.properties.ConfigurationProperties
import javax.annotation.PostConstruct

@ConfigurationProperties(prefix = "telegram.common")
class TelegramCommonProperties {

    lateinit var downloadpath: String
    lateinit var owners: List<Int>
    lateinit var dryRun: String

    fun isDryRun(): Boolean = dryRun.toBoolean()
    fun setDryRun(dryRun: Boolean) = run { this.dryRun = dryRun.toString() }

}
