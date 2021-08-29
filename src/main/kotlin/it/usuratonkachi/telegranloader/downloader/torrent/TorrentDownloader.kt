package it.usuratonkachi.telegranloader.downloader.torrent

import com.turn.ttorrent.client.Client
import com.turn.ttorrent.client.SharedTorrent
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Downloader
import it.usuratonkachi.telegranloader.downloader.telegram.TDownloader
import it.usuratonkachi.telegranloader.wrapper.DownloadWrapper
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.util.stream.Collectors

@Service
class TorrentDownloader(
    answeringBotService: AnsweringBotService,
    var tDownloader: TDownloader
) : Downloader(answeringBotService) {

    override fun download(downloadWrapper: DownloadWrapper) {
        tDownloader.download(downloadWrapper)
        val outputFile: File = downloadWrapper.outputPath!!.toFile()
        val outputFolder = File(outputFile.absolutePath.split(".")
            .stream()
            .limit(outputFile.absolutePath.split(".").size - 1L).collect(Collectors.joining("."))
        )
        outputFolder.mkdirs()
        val client = Client(InetAddress.getLocalHost(), SharedTorrent.fromFile(outputFile, outputFolder))
        client.download()
        client.waitForCompletion()
        outputFile.delete()
    }

}
