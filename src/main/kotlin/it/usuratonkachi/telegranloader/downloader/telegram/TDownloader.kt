package it.usuratonkachi.telegranloader.downloader.telegram

import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.api.handlers.DownloadHandler
import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.downloader.Downloader
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Service
class TDownloader(
    private var answeringBotService: AnsweringBotService,
    private var client: TelegramClient
)  : Downloader(answeringBotService) {

    companion object : Log

    override fun download(downloadWrapper: DownloadWrapper) {
        download(downloadWrapper, TelegramDownloadHandler(downloadWrapper, answeringBotService))
    }

    fun download(downloadWrapper: DownloadWrapper, downloadHandler: DownloadHandler) {
        client.send(TdApi.DownloadFile(
            downloadWrapper.fileId,
            1,
            0,
            0,
            true
        ), downloadHandler)
        downloadWrapper.countDownLatch.await()
    }

    private class TelegramDownloadHandler(
        private var downloadWrapper: DownloadWrapper,
        private var answeringBotService: AnsweringBotService)
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
                            "Download finished for ${file.local.path}, moving into ${downloadWrapper.outputPath}",
                            true
                        )
                        Files.move(Path.of(file.local.path), downloadWrapper.outputPath!!)
                        answeringBotService.answer(
                            downloadWrapper,
                            "Moving finished for ${file.local.path} into ${downloadWrapper.outputPath}",
                            true
                        )
                        answeringBotService.deleteRequest(downloadWrapper)
                        answeringBotService.answer(
                            downloadWrapper,
                            "Clean up finished for " + downloadWrapper.outputPath,
                            true
                        )
                        downloadWrapper.countDownLatch.countDown()
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
