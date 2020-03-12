package it.usuratonkachi.telegranloader.api

import com.github.badoualy.telegram.tl.api.TLMessage
import it.usuratonkachi.telegranloader.config.AnsweringBot
import org.springframework.stereotype.Service

@Service
class AnsweringBotService(private val answeringBot: AnsweringBot){

    fun answer(message: TLMessage, response: String, remove: Boolean) {
        answeringBot.answerMessage(message.fwdFrom.date, response, remove)
    }

}
