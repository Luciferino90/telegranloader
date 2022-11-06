package it.usuratonkachi.telegranloader.parser

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "series")
data class ParserConfiguration (
    var filename: ParserMapConfiguration = ParserMapConfiguration(),
    var caption: ParserMapConfiguration = ParserMapConfiguration()
)

class ParserMapConfiguration (
    var parser: Map<String, ConfigurationMapper> = HashMap()
)

class ConfigurationMapper(
    var filename: Element = Element(),
    var season: Element = Element(),
    var episode: Element = Element()
) {
    fun calculateFilename(mediaName: String) : String {
        return filename.calculateFilename(mediaName)
    }

    fun calculateSeason(mediaName: String) : String {
        return season.calculateSeason(mediaName)
    }

    fun calculateEpisode(mediaName: String) : String {
        return episode.calculateEpisode(mediaName)
    }
}

class Element(
    var regexp: String? = null,
    var fixed: String? = null,
    var replace: String? = null
) {
    fun calculateFilename(mediaName: String) : String {
        return fixed ?: regexp!!.toRegex().split(mediaName)[0].replace(".", " ").replace("_", " ").trim()
    }

    fun calculateSeason(mediaName: String) : String {
        return fixed ?: regexp!!.toRegex().find(mediaName)!!.groupValues.first().replace(replace ?: "", "")
    }

    fun calculateEpisode(mediaName: String) : String {
        return fixed ?: regexp!!.toRegex().find(mediaName)!!.groupValues.first().replace(replace ?: "", "")
    }
}
