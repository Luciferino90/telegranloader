package it.usuratonkachi.telegranloader.parser

import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ParserService(
        private val telegramCommonProperties: TelegramCommonProperties
) {

    private val rules = mapOf(
            ".*[S][0-9]{2}[E][0-9]{2}.*".toRegex() to { regex: Regex, mediaName: String ->
                {
                    val filenameRegex = "[S][0-9]{2}[E][0-9]{2}".toRegex()
                    val seasonRegex = "[S][0-9]{2}".toRegex()
                    val episodeRegex = "[E][0-9]{2}".toRegex()

                    val filename = filenameRegex.split(mediaName)[0].replace(".", " ").trim()
                    val season = seasonRegex.find(mediaName)!!.groupValues.first().removePrefix(seasonRegex.toString()[1].toString())
                    val episode = episodeRegex.find(mediaName)!!.groupValues.first().removePrefix(episodeRegex.toString()[1].toString())
                    val extension = mediaName.split(".").last()
                    EpisodeWrapper(season, episode, filename, extension).toPath(telegramCommonProperties.downloadpath)
                }
            },
            "^Boku No Hero Academia [0-9]+Th Season - [0-9]{2}.[a-zA-Z0-9]+".toRegex() to { regex: Regex, mediaName: String ->
                {
                    val filenameRegex = "[0-9]".toRegex()
                    val seasonRegex = "[0-9]".toRegex()
                    val episodeRegex = "[0-9]{2}".toRegex()

                    val filename = filenameRegex.split(mediaName)[0].replace(".", " ").trim()
                    val season = seasonRegex.find(mediaName)!!.groupValues.first().removePrefix(seasonRegex.toString()[1].toString())
                    val episode = episodeRegex.find(mediaName)!!.groupValues.first().removePrefix(episodeRegex.toString()[1].toString())
                    val extension = mediaName.split(".").last()
                    EpisodeWrapper(season, episode, filename, extension).toPath(telegramCommonProperties.downloadpath)
                }
            },
            "^[BO]+[0-9]+[_]?[A-Z]+[.][a-zA-Z0-9]+".toRegex() to { regex: Regex, mediaName: String ->
                {
                    val episodeRegex = "[0-9]+".toRegex()
                    val filename = "Boruto"
                    val season = "01"
                    val episode = episodeRegex.find(mediaName)!!.groupValues.first().removePrefix(episodeRegex.toString()[1].toString())
                    val extension = mediaName.split(".").last()
                    EpisodeWrapper("1", episode, "Boruto", extension).toPath(telegramCommonProperties.downloadpath)
                }
            }
    )

    fun getEpisodeWrapper(mediaName: String): Path =
            rules.entries
                    .filter { entry -> entry.key.matches(mediaName) }
                    .map { entry ->
                        {
                            val cleanedName = mediaName.replace(".US.", "")
                            entry.value.invoke(entry.key, cleanedName).invoke()
                        }
                    }
                    .first()
                    .invoke()

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
