package org.matomo.sdk.dispatcher

import org.matomo.sdk.Tracker

fun interface DispatcherFactory {
    fun build(tracker: Tracker?): Dispatcher?
}
