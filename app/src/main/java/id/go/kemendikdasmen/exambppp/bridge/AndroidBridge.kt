package id.go.kemendikdasmen.exambppp.bridge

import android.webkit.JavascriptInterface
import java.util.regex.Pattern

/**
 * Reverse-Engineered from id.go.kemendikdasmen.exambppp.bridge.AndroidBridgeImpl
 * Official JavaScript Interface for ExamBPPP
 */
class AndroidBridge(
    private val callback: BridgeListener
) {
    companion object {
        const val VERSION = "9.0.0"
        const val USER_AGENT_TOKEN = " JSB8123BKDSB9239BJBJDS89JNSDKK17"
        private val ROUTE_PATTERN = Pattern.compile("^/[a-zA-Z0-9/_-]*$")
    }

    interface BridgeListener {
        fun onExamStarted()
        fun onExamFinished()
        fun onRouteChanged(route: String)
        fun onRequestNativeExit()
    }

    @JavascriptInterface
    fun notifyExamStarted() {
        callback.onExamStarted()
    }

    @JavascriptInterface
    fun notifyExamFinished() {
        callback.onExamFinished()
    }

    @JavascriptInterface
    fun notifyRouteChanged(path: String) {
        if (ROUTE_PATTERN.matcher(path).matches()) {
            callback.onRouteChanged(path)
        }
    }

    @JavascriptInterface
    fun requestNativeExit() {
        callback.onRequestNativeExit()
    }

    @JavascriptInterface
    override fun toString(): String {
        return VERSION
    }
}
