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
    var titles: ConcurrentHashMap<String, ArrayList<RegexMapper>> = ConcurrentHashMap(),
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
        return jsonMapper.writeValueAsString(titles.getOrDefault(series, ArrayList()))
    }

    fun removeConfiguration(series : String, number : Int) : String {
        if (titles.containsKey(series) && titles[series]?.size!! > number) {
            val response : RegexMapper = titles[series]!![number]
            titles[series]?.removeAt(number)
            if (CollectionUtils.isEmpty(titles[series]))
                titles.remove(series)
            swapFile()
            return "Removed: " + jsonMapper.writeValueAsString(response)
        }
        return "Configuration not found"
    }

    fun addConfiguration(series : String, input: String) : String {
        val newRegexMapper : RegexMapper = jsonMapper.readValue(input, RegexMapper::class.java)
        titles.compute(series) { key: String, oldVal: ArrayList<RegexMapper>? ->
            val returnValue : ArrayList<RegexMapper> = if (!CollectionUtils.isEmpty(oldVal)) oldVal!! else ArrayList()
            returnValue.add(newRegexMapper)
            returnValue
        }
        swapFile()
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
        println()
    }

}

class RegexMapper(
    var regex: Regex = Regex(""),
    var type: String = "",
    var filename: ElementRefactor = ElementRefactor(),
    var season: ElementRefactor = ElementRefactor(),
    var episode: ElementRefactor = ElementRefactor()
) {
    fun calcolateFileName(rootPath: String, fullName: String, title: String) : Path {
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
    val titles: ConcurrentHashMap<String, ArrayList<RegexMapper>>
) : Serializable