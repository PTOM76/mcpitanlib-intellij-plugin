package net.pitan76.mpltemplateideplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object Lang : DynamicBundle("messages.lang") {
    
    @JvmStatic
    fun get(@PropertyKey(resourceBundle = "messages.lang") key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}