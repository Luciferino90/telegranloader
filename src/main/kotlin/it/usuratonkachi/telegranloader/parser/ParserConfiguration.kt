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
)

class Element(
        var regexp: String? = null,
        var fixed: String? = null
)

// TODO FIX
fun getEpisodeWrapper(configurationMapper: ConfigurationMapper, mediaName: String) : ParserService.EpisodeWrapper {
    var cleanMediaName: String = mediaName.replace(".US.", "")

    var filename: String
    var season: String
    var episode: String

    val extension = mediaName.split(".").last()

    if (!configurationMapper.filename!!.fixed.isNullOrEmpty()) {
        filename = configurationMapper.filename!!.fixed!!
    } else {
        filename = configurationMapper.filename!!.regexp!!.toRegex().split(cleanMediaName)[0].replace(".", " ").trim()
    }
    if (!configurationMapper.season!!.fixed.isNullOrEmpty()) {
        season = configurationMapper.season!!.fixed!!
    } else {
        season = configurationMapper.season!!.regexp!!.toRegex().find(cleanMediaName)!!.groupValues.first().removePrefix(configurationMapper.season!!.regexp!!.toRegex().toString()[1].toString())
    }
    if (!configurationMapper.episode!!.fixed.isNullOrEmpty()) {
        episode = configurationMapper.episode!!.fixed!!
    } else {
        episode = configurationMapper.episode!!.regexp!!.toRegex().find(mediaName)!!.groupValues.first().removePrefix(configurationMapper.episode!!.regexp!!.toRegex().toString()[1].toString())
    }
    return ParserService.EpisodeWrapper(season, episode, filename, extension)
}
