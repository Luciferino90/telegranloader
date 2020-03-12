package it.usuratonkachi.telegranloader.downloader.jdownloader

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLMessageMediaDocument
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Downloader
import org.goots.jdownloader.JDownloader
import org.springframework.stereotype.Service
import java.io.File

@Service
class JDownloader(answeringBotService: AnsweringBotService) : Downloader(answeringBotService) {

    override fun download(client: TelegramClient, media: TLMessageMediaDocument, outputFile: File) {
        /*
            TODO: Is this even working?
         */
        val url: String = ""
        JDownloader(url).target(outputFile.toPath().toString()).execute()
    }

}
