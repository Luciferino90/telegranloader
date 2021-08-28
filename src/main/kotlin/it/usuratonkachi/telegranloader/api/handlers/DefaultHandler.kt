import it.tdlight.common.ResultHandler
import it.tdlight.jni.TdApi
import org.springframework.stereotype.Component

@Component
class DefaultHandler : ResultHandler {
    override fun onResult(tdApiObj: TdApi.Object) {
        print(tdApiObj.toString())
    }
}
