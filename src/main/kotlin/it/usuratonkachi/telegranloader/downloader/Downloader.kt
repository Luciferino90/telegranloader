@file:Suppress("unused")

package it.usuratonkachi.telegranloader.downloader

import it.tdlight.common.TelegramClient
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.api.TelegramClientService
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.downloader.jdownloader.JDownloader
import it.usuratonkachi.telegranloader.downloader.telegram.TDownloader
import it.usuratonkachi.telegranloader.downloader.torrent.TorrentDownloader
import it.usuratonkachi.telegranloader.parser.ParserService
import it.usuratonkachi.telegranloader.wrapper.DownloadType
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.file.Path

abstract class Downloader(private val answeringBotService: AnsweringBotService) {

    companion object : Log

    fun dispatchDownload(downloadWrapper: DownloadWrapper) {
        val outputFile = downloadWrapper.outputPath!!.toFile()
        TDownloader.logger().info("Trying to download file @" + outputFile.absolutePath)

        if (outputFile.exists() && (outputFile.length() != (downloadWrapper.expectedSize?.toLong() ?: (outputFile.length() + 1000000)))) {
            val msg = "Deleted uncompleted download file @" + outputFile.absolutePath
            TDownloader.logger().info(msg)
            answeringBotService.answer(downloadWrapper, msg, false)
            outputFile.delete()
        } else if (outputFile.exists()) {
            val msg = "Completed file already exists @" + outputFile.absolutePath
            TDownloader.logger().info(msg)
            answeringBotService.answer(downloadWrapper, msg, false)
            return
        }
        download(downloadWrapper)
        TDownloader.logger().info("Downloaded file @" + outputFile.absolutePath)
    }

    abstract fun download(downloadWrapper: DownloadWrapper)
}

@Service
class DownloaderSelector(
    private val jDownloader: JDownloader,
    private val tDownloader: TDownloader,
    private val torrentDownloader: TorrentDownloader,
    private val answeringBotService: AnsweringBotService,
    private val parserService: ParserService,
    private val telegramCommonProperties: TelegramCommonProperties,
    private val client: TelegramClient
) {

    companion object: Log

    fun reactorDownloader(downloadWrapper: DownloadWrapper): Mono<Void> {
        return when(downloadWrapper.downloadType) {
            //  Has a Media?
            //      Media has mime torrent  -> Torrent Downloader
            //      Media has others media  -> Telegram Downloader
            DownloadType.FILE -> Mono.just(downloadWrapper)
                .doOnNext{ it.mediaContent!! }
                .doOnNext{ it.outputPath = getFilename(it) }
                .flatMap { dispatcher(it) }
                .doOnError { TelegramClientService.logger().error("Exception occurred during retrieve of media filename ${it.message}", it) }
                .then()
            //  Has a Media?
            //      Media has mime torrent  -> Torrent Downloader
            //      Media has others media  -> Telegram Downloader
            DownloadType.URL -> Mono.just(downloadWrapper)
                .doOnNext { it.message }
                .doOnNext{ it.outputPath = getFilename(it) }
                .map { downloader(it) }
                .doOnError { TelegramClientService.logger().error("Exception occurred during retrieve of media url ${it.message}", it) }
                .then()
        }
    }

    private fun getFilename(downloadWrapper: DownloadWrapper): Path =
        parserService.getEpisodeWrapper(downloadWrapper)

    private fun dispatcher(downloadWrapper: DownloadWrapper): Mono<DownloadWrapper> {
        return if (!telegramCommonProperties.isDryRun()) download(downloadWrapper) else generatePathPrediction(downloadWrapper)
    }

    private fun download(downloadWrapper: DownloadWrapper): Mono<DownloadWrapper> {
        return Mono.just(downloadWrapper)
            .doOnNext { answeringBotService.answer(it, "Download started for " + it.outputPath, false) }
            .doOnNext { downloader(it) }
    }

    private fun generatePathPrediction(downloadWrapper: DownloadWrapper): Mono<DownloadWrapper> {
        return Mono.just(downloadWrapper)
            .doOnNext { generatePath(it) }
            .doOnNext { answeringBotService.deleteRequest(downloadWrapper) }
            .doOnError {
                val errorMsg = "Exception occurred during download for ${downloadWrapper.outputPath} ${it.message}"
                TelegramClientService.logger().error(errorMsg, it)
                answeringBotService.answer(downloadWrapper, errorMsg, true)
            }
    }

    private fun generatePath(downloadWrapper: DownloadWrapper) {
        val log = """
            filename: ${downloadWrapper.filename}
            caption: ${downloadWrapper.caption}
            calculated: ${downloadWrapper.outputPath}
            
            """.trimIndent()
        logger().info(log)
        answeringBotService.answer(downloadWrapper, log, true)
    }

    // Torrent and Media file
    private fun downloader(downloadWrapper: DownloadWrapper) {
        when (downloadWrapper.downloadType) {
            DownloadType.URL ->
                if (downloadWrapper.message.startsWith("magnet:"))
                    torrentDownloader.dispatchDownload(downloadWrapper)
                else
                    jDownloader.dispatchDownload(downloadWrapper)
            DownloadType.FILE -> when (downloadWrapper.mimeType) {
                "application/x-bittorrent" -> torrentDownloader.dispatchDownload(downloadWrapper)
                else -> tDownloader.dispatchDownload(downloadWrapper)
            }
        }
    }

}
