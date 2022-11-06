package it.usuratonkachi.telegranloader.parser

import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import reactor.util.function.Tuples
import java.nio.file.Path


@Service
class ParserService(
    private val telegramCommonProperties: TelegramCommonProperties,
    private val parserRefactorConfiguration: ParserRefactorConfiguration
) {

    fun getEpisodeWrapper(downloadWrapper: DownloadWrapper): Path {
        return getEpisodeWrapper(downloadWrapper.filename!!, downloadWrapper.caption!!)
    }

    fun getEpisodeWrapper(mediaName: String, caption: String): Path {
        val fullName = caption + " " + mediaName.replace("\n", " ").replace("\r", " ").replace("\t", " ")
        return parserRefactorConfiguration.titles.entries.stream()
            .flatMap { it.value.stream().map { d -> Tuples.of(it.key, d) }.filter { r -> r.t2.regex.containsMatchIn(fullName) } }
            .findFirst()
            .map { it.t2.calcolateFileName(telegramCommonProperties.downloadpath, fullName, it.t1) }
            .orElseGet { Path.of(telegramCommonProperties.downloadpath, "others", mediaName) }
    }

}
