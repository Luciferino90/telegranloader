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
        val fallback: Path
        /*
        fallback = if (false){ //( caption.isNotEmpty() && "#Ep[0-9]{1,3} #S[0-9].*".toRegex().matches(caption.replace("\n", "")) ) {
            val series = filename.split(".").stream().limit(filename.split(".").size - 1L).collect(Collectors.joining(".")).replace("_", " ")
            val extension = filename.split(".").last()
            val episode = "#Ep[0-9]{1,3}".toRegex().find(caption)!!.groups[0]!!.value.replace("#Ep", "")
            val season = "#S[0-9]".toRegex().find(caption)!!.groups[0]!!.value.replace("#S", "")
            EpisodeWrapper(season, episode, series, extension).toPath(telegramCommonProperties.downloadpath)
        } else {
            Path.of(telegramCommonProperties.downloadpath, "others", filename)
        }*/
        return if (caption.isEmpty()) getEpisodeWrapper(filename) else getEpisodeWrapper(filename, caption)
    }

    fun getEpisodeWrapper(mediaName: String): Path =
            Optional.ofNullable(
                    parserConfiguration.parser!!.entries
                            .filter { entry -> entry.key.toRegex().matches(mediaName) }
                            .map { entry -> getEpisodeWrapper(entry.value, mediaName) }
                            .map { it.toPath(telegramCommonProperties.downloadpath) }
                            .firstOrNull()
            ).orElseGet{ Path.of(telegramCommonProperties.downloadpath, "others", mediaName) }

    fun getEpisodeWrapper(mediaName: String, caption: String): Path =
            Optional.ofNullable(
                    parserConfiguration.parser!!.entries
                            .filter { entry -> entry.key.toRegex().matches(mediaName) }
                            .map { entry -> getEpisodeWrapper(entry.value, mediaName) }
                            .map { it.toPath(telegramCommonProperties.downloadpath) }
                            .firstOrNull()
            ).orElseGet{
                Optional.ofNullable(
                        parserConfiguration.parser!!.entries
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
