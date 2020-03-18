package it.usuratonkachi.telegranloader.parser

import com.github.badoualy.telegram.tl.api.TLDocumentAttributeFilename
import com.github.badoualy.telegram.tl.api.TLMessageMediaDocument
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

@Service
class ParserService(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val parserConfiguration: ParserConfiguration
) {

    fun getEpisodeWrapper(media: TLMessageMediaDocument): Path {
        val filename: String = media.document.asDocument.attributes.filterIsInstance<TLDocumentAttributeFilename>().last().fileName
        val caption = media.caption.replace("\n", "")
        return getEpisodeWrapper(filename, caption)
    }

    fun getEpisodeWrapper(mediaName: String, caption: String): Path =
            Optional.ofNullable(
                    parserConfiguration.filename!!.parser!!.entries
                            .filter { entry -> entry.key.toRegex().matches(mediaName) }
                            .map { entry -> getEpisodeWrapper(entry.value, mediaName) }
                            .map { it.toPath(telegramCommonProperties.downloadpath) }
                            .firstOrNull()
            ).orElseGet{
                Optional.ofNullable(
                        parserConfiguration.caption!!.parser!!.entries
                                .filter { entry -> entry.key.toRegex().matches(caption) }
                                .map { entry -> getCaptionWrapper(entry.value, mediaName, caption) }
                                .map { it.toPath(telegramCommonProperties.downloadpath) }
                                .firstOrNull()
                ).orElseGet{ Path.of(telegramCommonProperties.downloadpath, "others", mediaName) }
            }

    private fun getEpisodeWrapper(configurationMapper: ConfigurationMapper, mediaName: String) : EpisodeWrapper {
        val extension = mediaName.split(".").last()
        val cleanMediaName: String = mediaName.replace(".US.", "")
        val filename: String = configurationMapper.calculateFilename(cleanMediaName)
        val season: String = configurationMapper.calculateSeason(cleanMediaName)
        val episode: String = configurationMapper.calculateEpisode(cleanMediaName)
        return EpisodeWrapper(season, episode, filename, extension)
    }

    private fun getCaptionWrapper(configurationMapper: ConfigurationMapper, mediaName: String, caption: String) : EpisodeWrapper {
        val extension = mediaName.split(".").last()
        val cleanMediaName: String = mediaName.replace(".US.", "")
        val filename: String = configurationMapper.calculateFilename(cleanMediaName)
        val season: String = configurationMapper.calculateSeason(caption)
        val episode: String = configurationMapper.calculateEpisode(caption)
        return EpisodeWrapper(season, episode, filename, extension)
    }

}

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
