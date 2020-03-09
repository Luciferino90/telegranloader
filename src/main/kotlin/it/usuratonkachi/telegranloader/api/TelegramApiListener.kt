package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.UpdateCallback
import com.github.badoualy.telegram.api.utils.getAbsMediaInput
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.core.TLIntVector
import it.usuratonkachi.telegranloader.config.AnsweringBot
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.parser.ParserService
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.nio.file.Path

@Component
class TelegramApiListener(
        private val telegramCommonProperties: TelegramCommonProperties,
        private val answeringBot: AnsweringBot,
        private val parserService: ParserService
) : UpdateCallback {

    override fun onUpdates(client: TelegramClient, updates: TLUpdates) {
        updates.updates
                .asSequence()
                .filter { it is TLUpdateNewMessage }
                .map { it as TLUpdateNewMessage }
                .map{ it.message as TLMessage }
                .filter { telegramCommonProperties.owners.contains(it.fromId) }
                .filter { it.media != null }
                .map { it to getFilename(it.media as TLMessageMediaDocument) }
                .onEach { answer(it.first, "Download started for " + it.second, false) }
                .onEach { download(client, it.first.media as TLMessageMediaDocument, it.second) }
                .onEach { answer(it.first, "Download finished for " + it.second, false) }
                .onEach { deleteRequest(client, it.first) }
                .onEach { answer(it.first, "Clean up finished for " + it.second, true) }
                .toList()
    }

    private fun getFilename(media: TLMessageMediaDocument): Path =
            parserService.getEpisodeWrapper(media.document.asDocument.attributes.filterIsInstance<TLDocumentAttributeFilename>().last().fileName)

    fun download(client: TelegramClient, media: TLMessageMediaDocument, outputPath: Path) {
        val outputFile = outputPath.toFile()
        if (outputFile.exists()) {
            if (outputFile.length() != media.getAbsMediaInput()!!.size.toLong()) {
                println("Deleted uncompleted download file @" + outputFile.absolutePath)
                outputFile.delete()
            } else {
                println("Completed file already exists @" + outputFile.absolutePath)
                return
            }
        }
        outputPath.parent.toFile().mkdirs()
        val fos = FileOutputStream(outputFile)
        client.downloadSync(media.getAbsMediaInput()!!.inputFileLocation, media.getAbsMediaInput()!!.size, fos)
    }

    private fun answer(message: TLMessage, response: String, remove: Boolean) {
        answeringBot.answerMessage(message.fwdFrom.date, response, remove)
    }

    private fun deleteRequest(client: TelegramClient, message: TLMessage) {
        val vector = TLIntVector()
        vector.add(message.id)
        client.messagesDeleteMessages(true, vector)
        println("Delete Message Done")
    }

    override fun onUpdatesCombined(client: TelegramClient, updates: TLUpdatesCombined) {
        return
    }

    override fun onShortChatMessage(client: TelegramClient, message: TLUpdateShortChatMessage) {
        return
    }

    override fun onShortMessage(client: TelegramClient, message: TLUpdateShortMessage) {
        if (telegramCommonProperties.owners.contains(message.userId))
            println("onShortMessage")
        return
    }

    override fun onShortSentMessage(client: TelegramClient, message: TLUpdateShortSentMessage) {
        return
    }

    override fun onUpdateShort(client: TelegramClient, update: TLUpdateShort) {
        return
    }

    override fun onUpdateTooLong(client: TelegramClient) {
        return
    }

}
