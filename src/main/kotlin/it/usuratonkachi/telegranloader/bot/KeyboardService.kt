package it.usuratonkachi.telegranloader.bot

import it.usuratonkachi.telegranloader.config.Log
import it.usuratonkachi.telegranloader.config.TelegramCommonProperties
import it.usuratonkachi.telegranloader.parser.ParserRefactorConfiguration
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
@Service
class KeyboardService(
    private var telegramCommonProperties: TelegramCommonProperties,
    private var parserRefactorConfiguration: ParserRefactorConfiguration
) {

    companion object: Log

    val helpResponse = """
                    /dryrun true|false: On true bot will return a preview of the parser filename, with false it will try to download the content.
                    /config [title] [counter]: Return parsing config file.
                    /add_rule title rule: Add rule for a title, for rule format look at config command.
                    /remove_rule title counter: Remove rule using counter. If not sure use /config file, changes are not revertible.
                    /set_userid title telegram_user_id: Configure a series by a specific userId.
                    /set_type title type: Configure type for series, used for output path.
                    /clean: Cleans telegram metadata.
                    /help: Print this help menu.
                """.trimIndent()

    private fun buildKeyBoard(subcommands : List<Keyboard>) : InlineKeyboardMarkup {
        val atomicCounter = AtomicInteger(0)
        val command : List<List<InlineKeyboardButton>> = subcommands
            .groupBy { atomicCounter.getAndIncrement() / 3 }
            .values
            .map {
                it.map { feature -> InlineKeyboardButton.builder().text(feature.text).callbackData(feature.callbackData).build() }
            }
        return InlineKeyboardMarkup.builder()
            .keyboard(command)
            .build()
    }

    fun buildKeyBoard() : KeyboardResponse {
        return KeyboardResponse("Start",
            buildKeyBoard(listOf(
                Keyboard("dryrun", "dryrun"),
                Keyboard("config", "config"),
                Keyboard("add_rule", "add_rule"),
                Keyboard("remove_rule", "remove_rule"),
                Keyboard("set_userid", "set_userid"),
                Keyboard("set_type", "set_type"),
                Keyboard("clean", "clean"),
                Keyboard("help", "help")
            ))
        )
    }

    fun buildKeyBoard(command: CallbackQuery) : KeyboardResponse {
        return when (command.data) {
            "start" -> buildKeyBoard()
            "dryrun" -> {
                KeyboardResponse("Changing dryrun value, actually is: ${telegramCommonProperties.dryRun}",
                    buildKeyBoard(listOf(
                        Keyboard("True", "dryrun_true"),
                        Keyboard("False", "dryrun_false"),
                        Keyboard("Back", "start")
                    ))
                )
            }
            "dryrun_true" -> {
                telegramCommonProperties.setDryRun(true)
                KeyboardResponse("Changed dryrun to ${telegramCommonProperties.dryRun}",
                    buildKeyBoard(listOf(
                        Keyboard("Back", "dryrun")
                    ))
                )
            }
            "dryrun_false" -> {
                telegramCommonProperties.setDryRun(false)
                KeyboardResponse("Changed dryrun to ${telegramCommonProperties.dryRun}",
                    buildKeyBoard(listOf(
                        Keyboard("Back", "dryrun")
                    ))
                )
            }
            "config" -> {
                KeyboardResponse(parserRefactorConfiguration.getConfiguration(),
                    buildKeyBoard(
                        parserRefactorConfiguration.titles.keys.map { Keyboard(it, "config_$it") }
                    ).also { Keyboard("Back", "start") }
                )
            }
            "add_rule" -> {
                KeyboardResponse(parserRefactorConfiguration.getConfiguration(),
                    buildKeyBoard(
                        parserRefactorConfiguration.titles.keys.map { Keyboard(it, "add_rule_$it") }
                    ).also { Keyboard("Back", "start") }
                )
            }
            "remove_rule" -> {
                KeyboardResponse(parserRefactorConfiguration.getConfiguration(),
                    buildKeyBoard(
                        parserRefactorConfiguration.titles.keys.map { Keyboard(it, "remove_rule_$it") }
                    ).also { Keyboard("Back", "start") }
                )
            }
            "set_userid" -> {
                KeyboardResponse(parserRefactorConfiguration.getConfiguration(),
                    buildKeyBoard(
                        parserRefactorConfiguration.titles.keys.map { Keyboard(it, "set_userid_$it") }
                    ).also { Keyboard("Back", "start") }
                )
            }
            "set_type" -> {
                KeyboardResponse(parserRefactorConfiguration.getConfiguration(),
                    buildKeyBoard(
                        parserRefactorConfiguration.titles.keys.map { Keyboard(it, "set_type_$it") }
                    ).also { Keyboard("Back", "start") }
                )
            }
            "clean" -> {
                KeyboardResponse("Clean process finished",
                    buildKeyBoard(
                        listOf(Keyboard("Back", "start"))
                    )
                )
            }
            "help" -> {
                KeyboardResponse(helpResponse,
                    buildKeyBoard(
                        listOf(Keyboard("Back", "start"))
                    )
                )
            }
            else -> {
                if (command.data.startsWith("config_")) {
                    val title = command.data.replace("config_", "").split("_")[0]
                    KeyboardResponse(parserRefactorConfiguration.getConfiguration(title),
                        buildKeyBoard(
                            listOf(Keyboard("Back", "config"))
                        )
                    )
                } else if (command.data.startsWith("add_rule_")
                    && command.data.replace("add_rule_", "").split("_").size == 1) {
                    val title = command.data.replace("config_", "").split("_")[0]
                    // TODO Requires input box!
                } else if (command.data.startsWith("add_rule_")
                    && command.data.replace("add_rule_", "").split("_").size > 1) {
                    val title = command.data.replace("add_rule_", "").split("_")[0]
                    // TODO Requires read input box
                } else if (command.data.startsWith("remove_rule_")
                    && command.data.replace("remove_rule_", "").split("_").size == 1) {
                    val title = command.data.replace("remove_rule_", "").split("_")[0]
                    // TODO Show rules list as button numbered
                } else if (command.data.startsWith("remove_rule_")
                    && command.data.replace("remove_rule_", "").split("_").size > 1) {
                    val title = command.data.replace("remove_rule_", "").split("_")[0]
                    val number = command.data.replace("remove_rule_", "").split("_")[1].toInt()
                    KeyboardResponse(parserRefactorConfiguration.removeConfiguration(title, number),
                        buildKeyBoard(
                            listOf(Keyboard("Back", "config"))
                        )
                    )
                } else if (command.data.startsWith("set_userid_")
                    && command.data.replace("set_userid_", "").split("_").size == 1) {
                    val title = command.data.replace("set_userid_", "").split("_")[0]
                    // TODO Requires input box!
                } else if (command.data.startsWith("set_userid_")
                    && command.data.replace("set_userid_", "").split("_").size > 1) {
                    val title = command.data.replace("set_userid_", "").split("_")[0]
                    val telegramUserId = command.data.replace("set_userid_", "").split("_")[1]
                    // TODO Requires read input box
                } else if (command.data.startsWith("set_type_")
                    && command.data.replace("set_type_", "").split("_").size > 1) {
                    val title = command.data.replace("set_type_", "").split("_")[0]
                    // TODO Requires input box!
                } else if (command.data.startsWith("set_type_")
                    && command.data.replace("set_type_", "").split("_").size > 1) {
                    val title = command.data.replace("set_type_", "").split("_")[0]
                    // TODO Requires read input box
                }
                KeyboardResponse("", InlineKeyboardMarkup())
            }
        }
    }

}



data class Keyboard (
    val text: String,
    val callbackData: String
)

data class KeyboardResponse (
    val response: String,
    val keyboard: InlineKeyboardMarkup
)