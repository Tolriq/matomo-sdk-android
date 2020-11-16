package org.matomo.sdk.tools

class PropertySource {
    val httpAgent: String?
        get() = getSystemProperty("http.agent")
    val jvmVersion: String?
        get() = getSystemProperty("java.vm.version")

    fun getSystemProperty(key: String): String? {
        return System.getProperty(key)
    }
}
