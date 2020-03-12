package it.usuratonkachi.telegranloader.downloader.torrent

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLMessageMediaDocument
import com.turn.ttorrent.client.Client
import com.turn.ttorrent.client.SharedTorrent
import it.usuratonkachi.telegranloader.api.AnsweringBotService
import it.usuratonkachi.telegranloader.downloader.Downloader
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress

@Service
class TorrentDownloader(answeringBotService: AnsweringBotService) : Downloader(answeringBotService) {

    override fun download(client: TelegramClient, media: TLMessageMediaDocument, outputFile: File) {
        /*
            TODO:
               Download torrent file
               Start downloader
               Delete torrent

               Is this working?
         */
        val client = Client(InetAddress.getLocalHost(), SharedTorrent.fromFile(File(""), outputFile))
        client.download()
        client.waitForCompletion()
    }

}
