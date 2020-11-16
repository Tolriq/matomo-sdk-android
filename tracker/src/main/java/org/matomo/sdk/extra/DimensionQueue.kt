package org.matomo.sdk.extra

import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.CustomDimension.Companion.getDimension
import org.matomo.sdk.extra.CustomDimension.Companion.setDimension
import timber.log.Timber
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
            if (existing != null) {
                Timber.tag(TAG).d("Setting dimension %s to slot %d would overwrite %s, skipping!", dim.value, dim.id, existing)
            } else {
                setDimension(trackMe, dim)
                it.remove()
            }
        }
        return trackMe
    }

    companion object {
        private val TAG = Matomo.tag(DimensionQueue::class.java)
    }

    init {
        val callback = Tracker.Callback { trackMe: TrackMe -> onTrack(trackMe) }
        tracker.addTrackingCallback(callback)
    }
}