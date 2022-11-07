package it.usuratonkachi.telegranloader.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import java.io.Serializable
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.pathString


@Configuration
@ConfigurationProperties(prefix = "parser-configuration")
data class ParserRefactorConfiguration (
    var filename: String = "",
    var titles: ConcurrentHashMap<String, SeriesWrapper> = ConcurrentHashMap(),
    val jsonMapper : ObjectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT),
    val yamlMapper : ObjectMapper = ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
) {
    init {
        val serializerModuleConf = SimpleModule()
        serializerModuleConf.addSerializer(Regex::class.java, RegexSerializer())
        serializerModuleConf.addDeserializer(Regex::class.java, RegexDeserializer())
        jsonMapper.registerModule(serializerModuleConf)
        yamlMapper.registerModule(serializerModuleConf)
        yamlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    fun getConfiguration() : String {
        return jsonMapper.writeValueAsString(titles)
    }

    fun getConfiguration(series : String) : String {
        return jsonMapper.writeValueAsString(titles.getOrDefault(series, SeriesWrapper()))
    }

    fun setType(series: String, type: String) : String {
        var hasChanges = false
        if (titles.containsKey(series)) {
            titles.compute(series) { key: String, oldVal: SeriesWrapper? ->
                val returnValue: SeriesWrapper =
                    if (!CollectionUtils.isEmpty(oldVal?.rules)) oldVal!! else SeriesWrapper()
                returnValue.type = type
                hasChanges = true
                returnValue
            }
            if (hasChanges) swapFile()
        }
        return getConfiguration(series)
    }

    fun setChatUsername(series: String, chatUsername: String) : String {
        var hasChanges = false
        if (titles.containsKey(series)) {
            titles.compute(series) { key: String, oldVal: SeriesWrapper? ->
                val returnValue: SeriesWrapper =
                    if (!CollectionUtils.isEmpty(oldVal?.rules)) oldVal!! else SeriesWrapper()
                returnValue.chatUsername = chatUsername
                hasChanges = true
                returnValue
            }
            if (hasChanges) swapFile()
        }
        return getConfiguration(series)
    }

    fun removeConfiguration(series : String, number : Int) : String {
        var hasChanges = false
        if (titles.containsKey(series) && titles[series]?.rules?.size!! > number) {
            val response : RulesMapper = titles[series]!!.rules[number]
            titles[series]?.rules?.removeAt(number)
            if (CollectionUtils.isEmpty(titles[series]?.rules)) {
                titles.remove(series)
                hasChanges = true
            }
            if (hasChanges) swapFile()
            return "Removed: " + jsonMapper.writeValueAsString(response)
        }
        return "Configuration not found"
    }

    fun addConfiguration(series : String, input: String) : String {
        var hasChanges = false
        val newRegexMapper : RulesMapper = jsonMapper.readValue(input, RulesMapper::class.java)
        titles.compute(series) { key: String, oldVal: SeriesWrapper? ->
            val returnValue : SeriesWrapper = if (!CollectionUtils.isEmpty(oldVal?.rules)) oldVal!! else SeriesWrapper()
            returnValue.rules.add(newRegexMapper)
            hasChanges = true
            returnValue
        }
        if (hasChanges) swapFile()
        return getConfiguration(series)
    }

    private fun swapFile() {
        val originalFilePath : Path = if (filename.startsWith("classpath:"))
            ClassPathResource(filename.replace("classpath:", "")).file.toPath()
        else
            Path.of(filename)
        if (!originalFilePath.exists())
            return
        val swapFilePath : Path = Path.of("$originalFilePath.swp")
        swapFilePath.deleteIfExists()

        originalFilePath.copyTo(swapFilePath, true)
        originalFilePath.deleteIfExists()
        yamlMapper.writeValue(originalFilePath.toFile(), ParserConfigurationOutput(filename, titles))
        swapFilePath.deleteIfExists()
    }

}

class SeriesWrapper(
    var title: String = "",
    var type: String = "",
    var chatUsername: String = "",
    var rules: ArrayList<RulesMapper> = ArrayList()
)

class RulesMapper(
    var regex: Regex = Regex(""),
    var filename: ElementRefactor = ElementRefactor(),
    var season: ElementRefactor = ElementRefactor(),
    var episode: ElementRefactor = ElementRefactor()
) {
    fun calcolateFileName(rootPath: String, seriesWrapper: SeriesWrapper, fullName: String) : Path {
        val extension = fullName.split(".").last()
        val seasonNumber = (season.regex.toRegex().find(fullName)?.groupValues?.firstOrNull() ?: "___").replace(season.replace, "")
        val episodeNumber = String.format("%1$2s", (episode.regex.toRegex().find(fullName)?.groupValues?.firstOrNull() ?: "___").replace(episode.replace, "")).replace(' ', '0')
        var path = Path.of(rootPath, seriesWrapper.type, seriesWrapper.title, "Season%s".format(seasonNumber), "%s %sx%s.%s".format(seriesWrapper.title, seasonNumber, episodeNumber, extension))
        val originalChoosenPath = path
        while (path.exists()) {
            path = Path.of(originalChoosenPath.pathString.replace(extension, String.format("%s.%s", Date().time, extension)))
        }
        return path
    }

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

class ElementRefactor(
    var regex: String = "",
    var fixed: String = "",
    var replace: String = ""
) {
    fun calculateFilename(mediaName: String) : String {
        return if (StringUtils.hasText(fixed)) fixed else regex.toRegex().split(mediaName)[0].replace(".", " ").replace("_", " ").trim()
    }

    fun calculateSeason(mediaName: String) : String {
        return if (StringUtils.hasText(fixed)) fixed else regex.toRegex().find(mediaName)!!.groupValues.first().replace(replace ?: "", "")
    }

    fun calculateEpisode(mediaName: String) : String {
        return if (StringUtils.hasText(fixed)) fixed else regex.toRegex().find(mediaName)!!.groupValues.first().replace(replace ?: "", "")
    }
}

data class ParserConfigurationOutput (
    val filename: String,
    val titles: ConcurrentHashMap<String, SeriesWrapper>
) : Serializable