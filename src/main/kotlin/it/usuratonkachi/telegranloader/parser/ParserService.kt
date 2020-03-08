package it.usuratonkachi.telegranloader.parser

import org.springframework.stereotype.Service

@Service
class ParserService {

    private val seasonAndEpisodeLiteralRegex = "[S][0-9]{2}[E][0-9]{2}".toRegex()

    fun getEpisodeWrapper(mediaName: String) : EpisodeWrapper {
        val seasonEpisode = seasonAndEpisodeLiteralRegex.find(mediaName)!!.groupValues.first().replace("S", "").split("E")
        val extension = mediaName.split(".").last()
        val filename = seasonAndEpisodeLiteralRegex.split(mediaName)[0].replace(".", " ").trim()
        return EpisodeWrapper(seasonEpisode[0].toInt(), seasonEpisode[1].toInt(), filename, extension)
    }

}

class EpisodeWrapper(private val season: Int, private val episode: Int, private val series: String, private val extension: String) {
    override fun toString(): String {
        return "${this.series} ${this.season}x${this.episode}.${this.extension}"
    }
}
