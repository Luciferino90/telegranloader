package it.usuratonkachi.telegranloader.downloader.telegram

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.utils.getAbsMediaInput
import com.github.badoualy.telegram.tl.api.TLMessageMediaDocument
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.downloader.Downloader
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

@Service
class TDownloader(answeringBotService: AnsweringBotService)  : Downloader(answeringBotService) {

    companion object : Log

    /*
        TODO: Is this working?
     */
    override fun download(client: TelegramClient, media: TLMessageMediaDocument, outputFile: File) {
        val fos = FileOutputStream(outputFile)
        // media.getAbsMediaInput()!!.mimeType switch to torrent WTF
        client.downloadSync(media.getAbsMediaInput()!!.inputFileLocation, media.getAbsMediaInput()!!.size, fos)
    }

}
