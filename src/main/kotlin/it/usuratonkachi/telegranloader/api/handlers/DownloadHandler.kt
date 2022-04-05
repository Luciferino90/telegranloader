package it.usuratonkachi.telegranloader.api.handlers

import it.tdlight.jni.TdApi
import it.tdlight.common.ResultHandler


interface DownloadHandler : ResultHandler<TdApi.File> {

}
