package org.matomo.sdk.tools

object Objects {
    @JvmStatic
    fun equals(a: Any?, b: Any?): Boolean {
        return a === b || a != null && a == b
    }
}