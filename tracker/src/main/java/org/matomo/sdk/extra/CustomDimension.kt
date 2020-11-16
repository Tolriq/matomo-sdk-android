package org.matomo.sdk.extra

import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackMe
import timber.log.Timber

/**
 * Allows you to track Custom Dimensions.
 * In order to use this functionality install and configure
 * https://plugins.matomo.org/CustomDimensions plugin.
 */
class CustomDimension(val id: Int, val value: String) {

    companion object {
        private val TAG = Matomo.tag(CustomDimension::class.java)

        /**
         * This method sets a tracking API parameter dimension%dimensionId%=%dimensionValue%.
         * Eg dimension1=foo or dimension2=bar.
         * So the tracking API parameter starts with dimension followed by the set dimensionId.
         *
         *
         * Requires [Custom Dimensions](https://plugins.matomo.org/CustomDimensions) plugin (server-side)
         *
         * @param trackMe        into which the data should be inserted
         * @param dimensionId    accepts values greater than 0
         * @param dimensionValue is limited to 255 characters, you can pass null to delete a value
         * @return true if the value was valid
         */
        @JvmStatic
        fun setDimension(trackMe: TrackMe, dimensionId: Int, dimensionValue: String?): Boolean {
            var newDimensionValue = dimensionValue
            if (dimensionId < 1) {
                Timber.tag(TAG).e("dimensionId should be great than 0 (arg: %d)", dimensionId)
                return false
            }
            if (newDimensionValue != null && newDimensionValue.length > 255) {
                newDimensionValue = newDimensionValue.substring(0, 255)
                Timber.tag(TAG).w("dimensionValue was truncated to 255 chars.")
            }
            if (newDimensionValue != null && newDimensionValue.isEmpty()) {
                newDimensionValue = null
            }
            trackMe[formatDimensionId(dimensionId)] = newDimensionValue
            return true
        }

        @JvmStatic
        fun setDimension(trackMe: TrackMe, dimension: CustomDimension): Boolean {
            return setDimension(trackMe, dimension.id, dimension.value)
        }

        @JvmStatic
        fun getDimension(trackMe: TrackMe, dimensionId: Int): String? {
            return trackMe[formatDimensionId(dimensionId)]
        }

        private fun formatDimensionId(id: Int): String {
            return "dimension$id"
        }
    }
}