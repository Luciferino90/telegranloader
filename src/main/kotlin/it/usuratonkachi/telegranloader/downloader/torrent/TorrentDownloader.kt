package it.usuratonkachi.telegranloader.downloader.torrent

import com.turn.ttorrent.client.Client
import com.turn.ttorrent.client.SharedTorrent
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Download
import it.usuratonkachi.telegranloader.downloader.Downloader
import it.usuratonkachi.telegranloader.downloader.telegram.TDownloader
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths
import java.util.stream.Collectors

@Service
class TorrentDownloader(
        var answeringBotService: AnsweringBotService,
        var tDownloader: TDownloader
) : Downloader(answeringBotService) {

    override fun download(download: Download) {
        tDownloader.download(download)
        val outputFolder = File(download.outputFile.toString().split(".").stream().limit(download.outputFile.toString().split(".").size - 1L).collect(Collectors.joining(".")))
        outputFolder.mkdirs()
        val client = Client(InetAddress.getLocalHost(), SharedTorrent.fromFile(download.outputFile, outputFolder))
        client.download()
        client.waitForCompletion()
        download.outputFile.delete()
    }

}
