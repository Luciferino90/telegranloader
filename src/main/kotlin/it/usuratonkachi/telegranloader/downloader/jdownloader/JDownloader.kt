package it.usuratonkachi.telegranloader.downloader.jdownloader

import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Downloader
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.goots.jdownloader.JDownloader
import org.springframework.stereotype.Service


@Service
class JDownloader(answeringBotService: AnsweringBotService) : Downloader(answeringBotService) {

    override fun download(downloadWrapper: DownloadWrapper) {
        JDownloader(downloadWrapper.message).target(downloadWrapper.outputPath.toString()).execute()
    }

}
