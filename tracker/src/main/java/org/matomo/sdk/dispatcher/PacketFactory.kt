/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher

import androidx.annotation.VisibleForTesting
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import kotlin.math.ceil

class PacketFactory(private val mApiUrl: String) {
    fun buildPackets(events: List<Event>): List<Packet> {
        if (events.isEmpty()) return emptyList()
        if (events.size == 1) {
            val p = buildPacketForGet(events[0])
            return p?.let { listOf(it) } ?: emptyList()
        }
        val packets = ceil(events.size * 1.0 / PAGE_SIZE).toInt()
        val freshPackets: MutableList<Packet> = ArrayList(packets)
        var i = 0
        while (i < events.size) {
            val batch = events.subList(i, (i + PAGE_SIZE).coerceAtMost(events.size))
            val packet: Packet?
            packet = if (batch.size == 1) buildPacketForGet(batch[0]) else buildPacketForPost(batch)
            if (packet != null) freshPackets.add(packet)
            i += PAGE_SIZE
        }
        return freshPackets
    }

    //{
    //    "requests": ["?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
    //    "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"]
    //}
    private fun buildPacketForPost(events: List<Event>): Packet? {
        if (events.isEmpty()) return null
        try {
            val params = JSONObject()
            val jsonArray = JSONArray()
            for (event in events) jsonArray.put(event.encodedQuery)
            params.put("requests", jsonArray)
            return Packet(mApiUrl, params, events.size)
        } catch (e: JSONException) {
            // Ignore
        }
        return null
    }

    // "http://domain.com/matomo.php?idsite=1&url=http://a.org&action_name=Test bulk log Pageview&rec=1"
    private fun buildPacketForGet(event: Event): Packet? {
        return if (event.encodedQuery.isEmpty()) null else Packet(mApiUrl + event)
    }

    companion object {
        @VisibleForTesting
        const val PAGE_SIZE = 20
    }
}