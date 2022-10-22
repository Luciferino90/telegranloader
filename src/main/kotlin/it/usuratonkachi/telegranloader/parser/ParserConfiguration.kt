package it.usuratonkachi.telegranloader.parser

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

@Service
@ConfigurationProperties(prefix = "series")
class ParserConfiguration (
        var filename: ParserMapConfiguration? = null,
        var caption: ParserMapConfiguration? = null
)


class ParserMapConfiguration (
        var parser: Map<String, ConfigurationMapper>? = null
)

class ConfigurationMapper(
    var filename: Element? = null,
    var season: Element? = null,
    var episode: Element? = null
) {
    fun calculateFilename(mediaName: String) : String {
        return filename!!.calculateFilename(mediaName)
    }

    fun calculateSeason(mediaName: String) : String {
        return season!!.calculateSeason(mediaName)
    }

    fun calculateEpisode(mediaName: String) : String {
        return episode!!.calculateEpisode(mediaName)
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
