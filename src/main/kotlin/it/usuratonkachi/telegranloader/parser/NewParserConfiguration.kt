package it.usuratonkachi.telegranloader.parser

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Configuration
@ConfigurationProperties(prefix = "parser")
class NewParserConfiguration (
    var regexMap: Map<String, NewConfigurationMapper> = HashMap()
)

// @ConstructorBinding
data class NewConfigurationMapper(
    var type: String = "",
    var title: String = "",
    var season: RegexConfig = RegexConfig(),
    var episode: RegexConfig = RegexConfig()
) {
    fun calcolateFileName(rootPath: String, fullName: String) : Path {
        val extension = fullName.split(".").last()
        val seasonNumber = (season.regex.toRegex().find(fullName)?.groupValues?.firstOrNull() ?: "___").replace(season.replace, "")
        val episodeNumber = String.format("%1$2s", (episode.regex.toRegex().find(fullName)?.groupValues?.firstOrNull() ?: "___").replace(episode.replace, "")).replace(' ', '0')
        var path = Path.of(rootPath, type, title, "Season%s".format(seasonNumber), "%s %sx%s.%s".format(title, seasonNumber, episodeNumber, extension))
        val originalChoosenPath = path
        while (path.exists()) {
            path = Path.of(originalChoosenPath.pathString.replace(extension, String.format("%s.%s", Date().time, extension)))
        }
        return path
    }
}

// @ConstructorBinding
class RegexConfig(
    var regex: String = "",
    var replace: String = ""
)