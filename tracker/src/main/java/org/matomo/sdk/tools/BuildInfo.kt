package org.matomo.sdk.tools

import android.os.Build

class BuildInfo {
    val release: String?
        get() = Build.VERSION.RELEASE
    val model: String?
        get() = Build.MODEL
    val buildId: String?
        get() = Build.ID
}
