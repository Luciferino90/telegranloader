package it.usuratonkachi.telegranloader.downloader.torrent

import com.turn.ttorrent.client.Client
import com.turn.ttorrent.client.SharedTorrent
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.api.handlers.DownloadHandler
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.downloader.Downloader
import it.usuratonkachi.telegranloader.downloader.telegram.TDownloader
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.util.stream.Collectors

@Service
class TorrentDownloader(
    private var answeringBotService: AnsweringBotService,
    private var tDownloader: TDownloader
) : Downloader(answeringBotService) {

    override fun download(downloadWrapper: DownloadWrapper) {
        tDownloader.download(downloadWrapper, TorrentDownloadHandler(downloadWrapper, answeringBotService))
    }

    private class TorrentDownloadHandler(private var downloadWrapper: DownloadWrapper, private var answeringBotService: AnsweringBotService)
        : DownloadHandler
    {
        companion object : Log
        override fun onResult(tdApiObj: TdApi.Object) {
            when(tdApiObj.constructor) {
                TdApi.File.CONSTRUCTOR -> run {
                    try {
                        val file: TdApi.File = tdApiObj as TdApi.File
                        answeringBotService.answer(
                            downloadWrapper,
                            "Download finished for ${file.local.path}, downloading torrent into ${downloadWrapper.outputPath}",
                            true
                        )

                        val outputFile: File = downloadWrapper.outputPath!!.toFile()
                        val outputFolder = File(outputFile.absolutePath.split(".")
                            .stream()
                            .limit(outputFile.absolutePath.split(".").size - 1L).collect(Collectors.joining("."))
                        )

                        outputFolder.parentFile.mkdirs()
                        val sharedTorrent : SharedTorrent = SharedTorrent.fromFile(File(file.local.path), outputFolder.parentFile)
                        val client = Client(InetAddress.getLocalHost(), sharedTorrent)
                        client.download()
                        client.waitForCompletion()

                        outputFile.delete()
                        answeringBotService.answer(
                            downloadWrapper,
                            "Download finished for ${file.local.path} into ${downloadWrapper.outputPath}",
                            true
                        )

                        answeringBotService.deleteRequest(downloadWrapper)
                        answeringBotService.answer(
                            downloadWrapper,
                            "Clean up finished for " + downloadWrapper.outputPath,
                            true
                        )
                    } catch (ex: Exception) {
                        val errorMsg = "Exception occurred during download for ${downloadWrapper.outputPath} ${ex.message}"
                        logger().error(errorMsg, ex)
                        answeringBotService.answer(downloadWrapper, errorMsg, true)
                    }
                }
            }
        }
    }

}
