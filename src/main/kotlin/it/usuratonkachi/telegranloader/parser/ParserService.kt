package it.usuratonkachi.telegranloader.parser

import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ParserService(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val parserConfiguration: ParserConfiguration
) {

    fun getEpisodeWrapper(mediaName: String): Path =
            parserConfiguration.parser!!.entries
                    .filter { entry -> entry.key.toRegex().matches(mediaName) }
                    .map { entry -> getEpisodeWrapper(entry.value, mediaName) }
                    .map { it.toPath(telegramCommonProperties.downloadpath) }
                    .first()

    class EpisodeWrapper(
            private val season: String,
            private val episode: String,
            private val series: String,
            private val extension: String
    ) {
        override fun toString(): String {
            return "${this.series} ${this.season.toInt()}x${this.episode}.${this.extension}"
        }

        fun toPath(rootPath: String): Path = Paths.get(rootPath, this.series, "Season ${this.season}", toString())
    }

}
