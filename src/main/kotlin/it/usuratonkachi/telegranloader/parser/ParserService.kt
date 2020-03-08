package it.usuratonkachi.telegranloader.parser

import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ParserService(
        private val telegramCommonProperties: TelegramCommonProperties
) {

    private val seasonAndEpisodeLiteralRegex = "[S][0-9]{2}[E][0-9]{2}".toRegex()

    fun getEpisodeWrapper(mediaName: String) : Path {
        val seasonEpisode = seasonAndEpisodeLiteralRegex.find(mediaName)!!.groupValues.first().replace("S", "").split("E")
        val extension = mediaName.split(".").last()
        val filename = seasonAndEpisodeLiteralRegex.split(mediaName)[0].replace(".", " ").trim()
        return EpisodeWrapper(seasonEpisode[0].toInt(), seasonEpisode[1].toInt(), filename, extension).toPath(telegramCommonProperties.downloadpath)
    }

}

class EpisodeWrapper(
        private val season: Int,
        private val episode: Int,
        private val series: String,
        private val extension: String
) {
    override fun toString(): String {
        return "${this.series} ${this.season}x${this.episode}.${this.extension}"
    }

    fun toPath(rootPath: String): Path = Paths.get(rootPath, this.series, "Season ${this.season}", toString())
}
