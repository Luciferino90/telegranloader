package it.usuratonkachi.telegranloader.parser

import lombok.RequiredArgsConstructor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.Date
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Configuration
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "parser")
class NewParserConfiguration (
    val regexMap: Map<String, NewConfigurationMapper>
)

@ConstructorBinding
class NewConfigurationMapper(
    val type: String,
    val title: String,
    val season: RegexConfig,
    val episode: RegexConfig
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

@ConstructorBinding
class RegexConfig(
    val regex: String,
    val replace: String
)