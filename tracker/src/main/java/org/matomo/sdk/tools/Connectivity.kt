package org.matomo.sdk.tools

import android.content.Context
import android.net.ConnectivityManager

class Connectivity(context: Context) {
    private val mConnectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected: Boolean
        get() {
            val network = mConnectivityManager.activeNetworkInfo
            return network != null && network.isConnected
        }

    enum class Type {
        NONE, MOBILE, WIFI
    }

    val type: Type
        get() {
            val network = mConnectivityManager.activeNetworkInfo ?: return Type.NONE
            return if (network.type == ConnectivityManager.TYPE_WIFI) Type.WIFI else Type.MOBILE
        }
}