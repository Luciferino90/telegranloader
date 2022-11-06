package it.usuratonkachi.telegranloader.parser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider


class RegexDeserializer : JsonDeserializer<Regex>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Regex {
        return Regex(p?.text ?: "")
    }
}

class RegexSerializer : JsonSerializer<Regex?>() {
    override fun serialize(value: Regex?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(value?.pattern ?: "")
    }

}