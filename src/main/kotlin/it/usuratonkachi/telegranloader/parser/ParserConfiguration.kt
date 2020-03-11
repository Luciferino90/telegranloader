package it.usuratonkachi.telegranloader.parser

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "series")
class ParserConfiguration (
        var parser: Map<String, ConfigurationMapper>? = null
)


class ConfigurationMapper(
        var logicalName: String? = null,
        var type: String? = null,
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
        var fixed: String? = null
) {
    fun calculateFilename(mediaName: String) : String {
        return fixed ?: regexp!!.toRegex().split(mediaName)[0].replace(".", " ").trim()
    }

    fun calculateSeason(mediaName: String) : String {
        return fixed ?: regexp!!.toRegex().find(mediaName)!!.groupValues.first().removePrefix(regexp!!.toRegex().toString()[1].toString())
    }

    fun calculateEpisode(mediaName: String) : String {
        return fixed ?: regexp!!.toRegex().find(mediaName)!!.groupValues.first().removePrefix(regexp!!.toRegex().toString()[1].toString())
    }
}
