package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.tl.api.TLMessage
import it.usuratonkachi.telegranloader.config.AnsweringBot
import org.springframework.stereotype.Service

@Service
class AnsweringBotService(private val AnsweringBot: AnsweringBot){

    fun answer(message: TLMessage, response: String, remove: Boolean) {
        AnsweringBot.answerMessage(message.fwdFrom.date ?: message.date, response, remove)
    }

}
