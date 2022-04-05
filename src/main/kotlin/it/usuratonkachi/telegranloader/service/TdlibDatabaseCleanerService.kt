package it.usuratonkachi.telegranloader.service

import it.usuratonkachi.telegranloader.config.TelegramApiProperties
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class TdlibDatabaseCleanerService(private var telegramApiProperties: TelegramApiProperties) {

    fun cleanDatabase() {
        Path.of(telegramApiProperties.databasePath).toFile()
            .listFiles { folder -> folder.isDirectory }
            ?.forEach { folder -> folder
                .listFiles{ file -> file.isFile }
                ?.forEach { it.delete() }
            }
    }

}
