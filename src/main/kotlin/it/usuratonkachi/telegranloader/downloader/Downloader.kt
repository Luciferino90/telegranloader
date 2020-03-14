package it.usuratonkachi.telegranloader.downloader

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.utils.getAbsMediaInput
import com.github.badoualy.telegram.tl.api.TLMessage
import com.github.badoualy.telegram.tl.api.TLMessageMediaDocument
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.downloader.jdownloader.JDownloader
import it.usuratonkachi.telegranloader.downloader.telegram.TDownloader
import it.usuratonkachi.telegranloader.downloader.torrent.TorrentDownloader
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

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
        outputPath.parent.toFile().mkdirs()
        download(client, media, outputFile)
        TDownloader.logger().info("Downloaded file @" + outputFile.absolutePath)
    }

    abstract fun download(client: TelegramClient, media: TLMessageMediaDocument, outputFile: File)
}

@Service
class DownloaderSelector(
        private val jDownloader: JDownloader,
        private val tDownloader: TDownloader,
        private val torrentDownloader: TorrentDownloader
) {
    fun downloader(message: TLMessage, client: TelegramClient, media: TLMessageMediaDocument, outputPath: Path) {
        /*
            TODO:
                Message format!
                url -> JDownloader (what about premium?)
                file with torrent mimetype -> torrent
                file with different mimetypes -> Telegram
         */
        when(media) {
            is TLMessageMediaDocument ->  tDownloader.download(message, client, media, outputPath)
        }
    }
}
