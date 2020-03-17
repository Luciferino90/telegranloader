package it.usuratonkachi.telegranloader.downloader

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.utils.getAbsMediaInput
import com.github.badoualy.telegram.tl.api.TLDocumentAttributeFilename
import com.github.badoualy.telegram.tl.api.TLMessage
import com.github.badoualy.telegram.tl.api.TLMessageMediaDocument
import com.github.badoualy.telegram.tl.core.TLIntVector
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.api.TelegramApiListener
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.downloader.jdownloader.JDownloader
import it.usuratonkachi.telegranloader.downloader.telegram.TDownloader
import it.usuratonkachi.telegranloader.downloader.torrent.TorrentDownloader
import it.usuratonkachi.telegranloader.parser.ParserService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Path

class Download(var client: TelegramClient, var outputFile: File, var url: String? = null, var media: TLMessageMediaDocument? = null, var message: TLMessage? = null)

abstract class Downloader(private val answeringBotService: AnsweringBotService) {

    companion object : Log

    fun download(message: TLMessage, client: TelegramClient, media: TLMessageMediaDocument, outputPath: Path) {
        val outputFile = outputPath.toFile()
        TDownloader.logger().info("Trying to download file @" + outputFile.absolutePath)
        if (outputFile.exists()) {
            if (outputFile.length() != media.getAbsMediaInput()!!.size.toLong()) {
                val msg = "Deleted uncompleted download file @" + outputFile.absolutePath
                TDownloader.logger().info(msg)
                answeringBotService.answer(message, msg, false)
                outputFile.delete()
            } else {
                val msg = "Completed file already exists @" + outputFile.absolutePath
                TDownloader.logger().info(msg)
                answeringBotService.answer(message, msg, false)
                return
            }
        }
        download(Download(client = client, media = media, outputFile = outputFile))
        TDownloader.logger().info("Downloaded file @" + outputFile.absolutePath)
    }

    fun download(message: TLMessage, client: TelegramClient, url: String, outputPath: Path) {
        val outputFile = outputPath.toFile()
        TDownloader.logger().info("Trying to download file @" + outputFile.absolutePath)
        if (outputFile.exists()) {
            val msg = "Deleted old download file @" + outputFile.absolutePath
            TDownloader.logger().info(msg)
            answeringBotService.answer(message, msg, false)
            outputFile.delete()
        }
        download(Download(client = client, url = url, outputFile = outputFile))
        TDownloader.logger().info("Downloaded file @" + outputFile.absolutePath)
    }

    abstract fun download(download: Download)
}

@Service
class DownloaderSelector(
        private val jDownloader: JDownloader,
        private val tDownloader: TDownloader,
        private val torrentDownloader: TorrentDownloader,
        private val answeringBotService: AnsweringBotService,
        private val parserService: ParserService
) {

    companion object: Log

    fun reactorDownloader(client: TelegramClient, url: String): Mono<Void> {
        //  Has a Media?
        //      Media has mime torrent  -> Torrent Downloader
        //      Media has others media  -> Telegram Downloader
        return Mono.just(url)
                .map { downloader(TLMessage(), client,  url, Path.of("")) }
                .doOnError { TelegramApiListener.logger().error("Exception occurred during retrieve of media url $url", it) }
                .then()
    }

    fun reactorDownloader(client: TelegramClient, message: TLMessage): Mono<Pair<TLMessageMediaDocument, Path>> {
        //  Has a Media?
        //      Media has mime torrent  -> Torrent Downloader
        //      Media has others media  -> Telegram Downloader
        return Mono.just(message)
                .filter { it.media != null && it.media is TLMessageMediaDocument }
                .map { it.media }
                .map { it as TLMessageMediaDocument to getFilename(it) }
                .flatMap { mediaPathPair ->
                    Mono.just(mediaPathPair)
                            .doOnNext { answeringBotService.answer(message, "Download started for " + it.second, false) }
                            .doOnNext { downloader(message, client, it.first, it.second) }
                            .doOnNext { answeringBotService.answer(message, "Download finished for " + it.second, false) }
                            .doOnNext { deleteRequest(client, message) }
                            .doOnNext { answeringBotService.answer(message, "Clean up finished for " + it.second, true) }
                            .doOnError {
                                val errorMsg = "Exception occurred during download for ${mediaPathPair.second} ${it.message}"
                                TelegramApiListener.logger().error(errorMsg, it)
                                answeringBotService.answer(message, errorMsg, true)
                            }
                }
                .doOnError { TelegramApiListener.logger().error("Exception occurred during retrieve of media filename ${it.message}", it) }
    }

    // Torrent and Media file
    private fun downloader(message: TLMessage, client: TelegramClient, media: TLMessageMediaDocument, outputPath: Path) {
        when(media.getAbsMediaInput()!!.mimeType) {
            "application/x-bittorrent" ->  torrentDownloader.download(Download(client = client, outputFile = outputPath.toFile(), message = message, media = media))
            else ->  tDownloader.download(Download(client = client, outputFile = outputPath.toFile(), message = message, media = media))
        }
    }

    // Magnet and Url file
    // magnet example: magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fbig-buck-bunny.torrent
    private fun downloader(message: TLMessage, client: TelegramClient, url: String, outputPath: Path) {
        if (url.startsWith("magnet:")) jDownloader.download(Download(message = message, client = client, url = url, outputFile = outputPath.toFile()))
        else jDownloader.download(Download(message = message, client = client, url = url, outputFile = outputPath.toFile()))
    }

    private fun getFilename(media: TLMessageMediaDocument): Path =
            parserService.getEpisodeWrapper(media)

    private fun deleteRequest(client: TelegramClient, message: TLMessage) {
        val vector = TLIntVector()
        vector.add(message.id)
        client.messagesDeleteMessages(true, vector)
        TelegramApiListener.logger().debug("Delete Message Done")
    }

}
