package it.usuratonkachi.telegranloader.parser

import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.api.UserData
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
        return getEpisodeWrapper(downloadWrapper.filename!!, downloadWrapper.caption!!, downloadWrapper.forwardedUserData)
    }

    fun getEpisodeWrapper(mediaName: String, caption: String, userData: UserData?): Path {
        val fullName = caption + " " + mediaName.replace("\n", " ").replace("\r", " ").replace("\t", " ")
        return retrieveSeriesIndex(fullName, mediaName, userData) ?: retrieveSeriesFullScan(fullName, mediaName, userData)
    }

    fun retrieveSeriesIndex(fullName: String, mediaName: String, userData: UserData?) : Path? {
        return parserRefactorConfiguration.titles.entries.stream()
            .filter{ userData != null && userData.username == it.value.chatUsername }
            .map {
                it.value.title = it.key
                it.value
            }
            .flatMap { it.rules.stream().map { d -> Tuples.of(it, d) }.filter { r -> r.t2.regex.containsMatchIn(fullName) } }
            .findFirst()
            .map { it.t2.calcolateFileName(telegramCommonProperties.downloadpath, it.t1, fullName) }
            .orElseGet { null }
    }

    fun retrieveSeriesFullScan(fullName: String, mediaName: String, userData: UserData?) : Path {
        return parserRefactorConfiguration.titles.entries.stream()
            .flatMap { it.value.rules.stream()
                .filter { r -> r.regex.containsMatchIn(fullName) }
                .map { r ->
                    it.value.title = it.key
                    Tuples.of(it.value, r)
                }
            }
            .findFirst()
            .map { it.t2.calcolateFileName(telegramCommonProperties.downloadpath, it.t1, fullName) }
            .orElseGet { Path.of(telegramCommonProperties.downloadpath, "others", mediaName) }
    }

}
