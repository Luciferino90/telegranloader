package it.usuratonkachi.telegranloader.downloader.telegram

import com.github.badoualy.telegram.api.utils.getAbsMediaInput
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.downloader.Download
import it.usuratonkachi.telegranloader.downloader.Downloader
import org.springframework.stereotype.Service
import java.io.FileOutputStream

@Service
class TDownloader(answeringBotService: AnsweringBotService)  : Downloader(answeringBotService) {

    companion object : Log

    override fun download(download: Download) {
        download.outputFile.parentFile.mkdirs()
        val fos = FileOutputStream(download.outputFile)
        download.client.downloadSync(download.media!!.getAbsMediaInput()!!.inputFileLocation, download.media!!.getAbsMediaInput()!!.size, fos)
    }

}
