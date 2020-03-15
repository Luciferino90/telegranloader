package it.usuratonkachi.telegranloader.downloader.torrent

import com.turn.ttorrent.client.Client
import com.turn.ttorrent.client.SharedTorrent
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Download
import it.usuratonkachi.telegranloader.downloader.Downloader
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress

@Service
class TorrentDownloader(answeringBotService: AnsweringBotService) : Downloader(answeringBotService) {

    override fun download(download: Download) {
        val client = Client(InetAddress.getLocalHost(), SharedTorrent.fromFile(File(""), download.outputFile))
        client.download()
        client.waitForCompletion()
    }

}
