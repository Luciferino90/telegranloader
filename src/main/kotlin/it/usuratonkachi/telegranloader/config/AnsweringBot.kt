package it.usuratonkachi.telegranloader.config

interface AnsweringBot {
    fun answerMessage(key: Int, response: String, remove: Boolean)
}
