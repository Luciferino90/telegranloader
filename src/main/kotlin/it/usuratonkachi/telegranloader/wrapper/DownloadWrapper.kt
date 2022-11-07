package it.usuratonkachi.telegranloader.wrapper

import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.api.UserData
import java.nio.file.Path
import java.util.concurrent.CountDownLatch

class DownloadWrapper(
    val chatId: Long,
    val messageId: Long,
    val date: Int,
    val expectedSize: Int?,
    val caption: String?,
    val filename: String?,
    val fileId: Int,
    val mediaContent: TdApi.MessageContent?,
    val message: String,
    val downloadType: DownloadType,
    var outputPath: Path?,
    var mimeType: String?,
    val forwardedUserData: UserData?,
    val countDownLatch: CountDownLatch = CountDownLatch(1)
)

enum class DownloadType {
    FILE, URL;
}
