package it.usuratonkachi.telegranloader.wrapper

import it.tdlight.jni.TdApi
import java.nio.file.Path

class DownloadWrapper(
    val chatId: Long,
    val messageId: Long,
    val date: Int,
    val expectedSize: Int?,
    val caption: String?,
    val filename: String?,
    val mediaContent: TdApi.MessageContent?,
    val message: String?,
    val downloadType: DownloadType,
    var outputPath: Path?,
    var mimeType: String?
)

enum class DownloadType {
    FILE, URL;
}
