package it.usuratonkachi.telegranloader.parser

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import javax.annotation.PostConstruct

@ConstructorBinding
@ConfigurationProperties(prefix = "series")
data class ParserConfiguration (
        var parser: Map<String, ConfigurationMapper> = mapOf()
){
    @PostConstruct
    fun init(){
        parser.size
        println("")
    }
}

@ConstructorBinding
data class ConfigurationMapper(
        var logicalName: String = "",
        var type: String = "",
        var filename: Element = Element(),
        var season: Element = Element(),
        var episode: Element = Element()
)

@ConstructorBinding
data class Element(
        var regexp: String = "",
        var fixed: String = ""
)
