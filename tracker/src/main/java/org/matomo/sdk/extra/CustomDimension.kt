package org.matomo.sdk.extra

import org.matomo.sdk.TrackMe

/**
 * Allows you to track Custom Dimensions.
 * In order to use this functionality install and configure
 * https://plugins.matomo.org/CustomDimensions plugin.
 */
class CustomDimension(val id: Int, val value: String) {

    companion object {
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
                return false
            }
            if (newDimensionValue != null && newDimensionValue.length > 255) {
                newDimensionValue = newDimensionValue.substring(0, 255)
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