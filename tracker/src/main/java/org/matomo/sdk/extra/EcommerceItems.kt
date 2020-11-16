/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.extra

import org.json.JSONArray
import org.matomo.sdk.tools.CurrencyFormatter
import java.util.HashMap

class EcommerceItems {
    private val mItems: MutableMap<String, JSONArray> = HashMap()

    /**
     * Adds a product into the ecommerce order. Must be called for each product in the order.
     * If the same sku is used twice, the first item is overwritten.
     */
    fun addItem(item: Item) {
        mItems[item.sku] = item.toJson()
    }

    class Item
    /**
     * If the same sku is used twice, the first item is overwritten.
     *
     * @param sku Unique identifier for the product
     */(val sku: String) {
        var category: String? = null
            private set
        var price: Int? = null
            private set
        var quantity: Int? = null
            private set
        var name: String? = null
            private set

        /**
         * @param name Product name
         */
        fun name(name: String?): Item {
            this.name = name
            return this
        }

        /**
         * @param category Product category
         */
        fun category(category: String?): Item {
            this.category = category
            return this
        }

        /**
         * @param price Price of the product in cents
         */
        fun price(price: Int): Item {
            this.price = price
            return this
        }

        /**
         * @param quantity Quantity
         */
        fun quantity(quantity: Int): Item {
            this.quantity = quantity
            return this
        }

        fun toJson(): JSONArray {
            val item = JSONArray()
            item.put(sku)
            if (name != null) item.put(name)
            if (this.category != null) item.put(this.category)
            if (price != null) item.put(CurrencyFormatter.priceString(price))
            if (quantity != null) item.put(quantity.toString())
            return item
        }
    }

    /**
     * Remove a product from an ecommerce order.
     *
     * @param sku unique identifier for the product
     */
    fun remove(sku: String) {
        mItems.remove(sku)
    }

    fun remove(item: Item) {
        mItems.remove(item.sku)
    }

    /**
     * Clears all items from the ecommerce order
     */
    fun clear() {
        mItems.clear()
    }

    fun toJson(): String {
        val jsonItems = JSONArray()
        for (item in mItems.values) {
            jsonItems.put(item)
        }
        return jsonItems.toString()
    }
}