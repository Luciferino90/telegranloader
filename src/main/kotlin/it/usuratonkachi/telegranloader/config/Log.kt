package it.usuratonkachi.telegranloader.config

import org.slf4j.LoggerFactory

interface Log {
    fun logger() = LoggerFactory.getLogger(this.javaClass)
}
