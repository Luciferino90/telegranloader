package it.usuratonkachi.telegranloader.api.handlers

import it.tdlight.common.ExceptionHandler
import org.springframework.stereotype.Component

@Component
class ErrorHandler : ExceptionHandler {
    override fun onException(e: Throwable) {
        e.printStackTrace()
    }
}
