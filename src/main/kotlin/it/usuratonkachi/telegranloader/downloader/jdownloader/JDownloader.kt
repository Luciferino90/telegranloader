package it.usuratonkachi.telegranloader.downloader.jdownloader

import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Download
import it.usuratonkachi.telegranloader.downloader.Downloader
import org.goots.jdownloader.JDownloader
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.util.*

@Service
class JDownloader(answeringBotService: AnsweringBotService) : Downloader(answeringBotService) {

    override fun download(download: Download) {
        download.outputFile = Paths.get("/tmp", UUID.randomUUID().toString()).toFile()
        JDownloader(download.url).target(download.outputFile.toPath().toString()).execute()
        println("")
    }

}
