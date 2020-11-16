package org.matomo.sdk.extra

import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.CustomDimension.Companion.getDimension
import org.matomo.sdk.extra.CustomDimension.Companion.setDimension
import java.util.ArrayList

/**
 * A helper class for custom dimensions. Acts like a queue for dimensions to be send.
 * On each tracking call it will insert as many saved dimensions as it is possible without overwriting existing information.
 */
class DimensionQueue(tracker: Tracker) {
    private val mOneTimeDimensions: MutableList<CustomDimension> = ArrayList()

    /**
     * The added id-value-pair will be injected into the next tracked event,
     * if that events slot for this ID is still empty.
     */
    fun add(id: Int, value: String?) {
        mOneTimeDimensions.add(CustomDimension(id, value!!))
    }

    private fun onTrack(trackMe: TrackMe): TrackMe {
        val it = mOneTimeDimensions.iterator()
        while (it.hasNext()) {
            val dim = it.next()
            val existing = getDimension(trackMe, dim.id)
            if (existing == null) {
                setDimension(trackMe, dim)
                it.remove()
            }
        }
        return trackMe
    }

    init {
        val callback = Tracker.Callback { trackMe: TrackMe -> onTrack(trackMe) }
        tracker.addTrackingCallback(callback)
    }
}