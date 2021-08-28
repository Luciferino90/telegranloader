package it.usuratonkachi.telegranloader.downloader.torrent

/*

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
*/
