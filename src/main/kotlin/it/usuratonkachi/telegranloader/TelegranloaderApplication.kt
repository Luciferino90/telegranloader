package it.usuratonkachi.telegranloader

import it.tdlight.common.Init
import it.usuratonkachi.telegranloader.bot.TelegramBotProperties
import it.usuratonkachi.telegranloader.config.TelegramApiProperties
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.parser.ParserConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@SpringBootApplication
@EnableConfigurationProperties(TelegramCommonProperties::class, TelegramApiProperties::class, TelegramBotProperties::class, ParserConfiguration::class)
class TelegranloaderApplication

fun main(args: Array<String>) {
	Init.start()
	runApplication<TelegranloaderApplication>(*args)
}
