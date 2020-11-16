package org.matomo.sdk.dispatcher

enum class DispatchMode(private val key: String) {
    /**
     * Dispatch always (default)
     */
    ALWAYS("always"),

    /**
     * Dispatch only on WIFI
     */
    WIFI_ONLY("wifi_only"),

    /**
     * The dispatcher will assume being offline. This is not persisted and will revert on app restart.
     * Ensures no information is lost when tracking exceptions. See #247
     */
    EXCEPTION("exception");

    override fun toString(): String {
        return key
    }

    companion object {
        fun fromString(raw: String?): DispatchMode? {
            return values().firstOrNull { it.key == raw }
        }
    }
}