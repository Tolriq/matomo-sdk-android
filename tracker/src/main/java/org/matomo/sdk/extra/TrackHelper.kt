package org.matomo.sdk.extra

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import android.os.Bundle
import org.matomo.sdk.Matomo
import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.CustomDimension.Companion.setDimension
import org.matomo.sdk.extra.DownloadTracker.Extra
import org.matomo.sdk.tools.ActivityHelper
import org.matomo.sdk.tools.CurrencyFormatter
import timber.log.Timber
import java.net.URL
import java.util.HashMap

open class TrackHelper private constructor(baseTrackMe: TrackMe? = null) {
    val baseTrackMe: TrackMe = baseTrackMe ?: TrackMe()

    abstract class BaseEvent(mBaseBuilder: TrackHelper?) {
        /**
         * May throw an [IllegalArgumentException] if the TrackMe was build with incorrect arguments.
         */
        abstract fun build(): TrackMe
        fun with(matomoApplication: MatomoApplication) {
            with(matomoApplication.tracker)
        }

        fun with(tracker: Tracker?) {
            val trackMe = build()
            tracker!!.track(trackMe)
        }

        val baseTrackMe = mBaseBuilder?.baseTrackMe ?: TrackMe()

        fun safelyWith(matomoApplication: MatomoApplication): Boolean {
            return safelyWith(matomoApplication.tracker)
        }

        /**
         * [.build] can throw an exception on illegal arguments.
         * This can be used to avoid crashes when using dynamic [TrackMe] arguments.
         *
         * @return false if an error occured, true if the TrackMe has been submitted to be dispatched.
         */
        fun safelyWith(tracker: Tracker?): Boolean {
            try {
                val trackMe = build()
                tracker!!.track(trackMe)
            } catch (e: IllegalArgumentException) {
                Timber.e(e)
                return false
            }
            return true
        }
    }

    /**
     * To track a screenview.
     *
     * @param path Example: "/user/settings/billing"
     * @return an object that allows addition of further details.
     */
    fun screen(path: String?): Screen {
        return Screen(this, path)
    }

    /**
     * Calls [.screen] for an activity.
     * Uses the activity-stack as path and activity title as names.
     *
     * @param activity the activity to track
     */
    fun screen(activity: Activity?): Screen {
        val breadcrumbs = ActivityHelper.getBreadcrumbs(activity)
        return Screen(this, ActivityHelper.breadcrumbsToPath(breadcrumbs)).title(breadcrumbs)
    }

    class Screen internal constructor(baseBuilder: TrackHelper, private val mPath: String?) : BaseEvent(baseBuilder) {
        private val mCustomVariables = CustomVariables()
        private val mCustomDimensions: MutableMap<Int, String?> = HashMap()
        private var mTitle: String? = null
        private var mCampaignName: String? = null
        private var mCampaignKeyword: String? = null

        /**
         * The title of the action being tracked. It is possible to use slashes / to set one or several categories for this action.
         *
         * @param title Example: Help / Feedback will create the Action Feedback in the category Help.
         * @return this object to allow chaining calls
         */
        fun title(title: String?): Screen {
            mTitle = title
            return this
        }

        /**
         * Requires [Custom Dimensions](https://plugins.matomo.org/CustomDimensions) plugin (server-side)
         *
         * @param index          accepts values greater than 0
         * @param dimensionValue is limited to 255 characters, you can pass null to delete a value
         */
        fun dimension(index: Int, dimensionValue: String?): Screen {
            mCustomDimensions[index] = dimensionValue
            return this
        }

        /**
         * Custom Variable valid per screen.
         * Only takes effect when setting prior to tracking the screen view.
         *
         * @see org.matomo.sdk.extra.CustomDimension and {@link .dimension
         */
        @Deprecated("Consider using Custom Dimensions")
        fun variable(index: Int, name: String?, value: String?): Screen {
            mCustomVariables.put(index, name, value)
            return this
        }

        /**
         * The marketing campaign for this visit if the user opens the app for example because of an
         * ad or a newsletter. Used to populate the *Referrers > Campaigns* report.
         *
         * @param name    the name of the campaign
         * @param keyword the keyword of the campaign
         * @return this object to allow chaining calls
         */
        fun campaign(name: String?, keyword: String?): Screen {
            mCampaignName = name
            mCampaignKeyword = keyword
            return this
        }

        override fun build(): TrackMe {
            requireNotNull(mPath) { "Screen tracking requires a non-empty path" }
            val trackMe = TrackMe(baseTrackMe)
                    .set(QueryParams.URL_PATH, mPath)
                    .set(QueryParams.ACTION_NAME, mTitle)
                    .set(QueryParams.CAMPAIGN_NAME, mCampaignName)
                    .set(QueryParams.CAMPAIGN_KEYWORD, mCampaignKeyword)
            if (mCustomVariables.size() > 0) {
                trackMe[QueryParams.SCREEN_SCOPE_CUSTOM_VARIABLES] = mCustomVariables.toString()
            }
            for ((key, value) in mCustomDimensions) {
                setDimension(trackMe, key, value)
            }
            return trackMe
        }
    }

    /**
     * Events are a useful way to collect data about a user's interaction with interactive components of your app,
     * like button presses or the use of a particular item in a game.
     *
     * @param category (required) – this String defines the event category.
     * You might define event categories based on the class of user actions,
     * like clicks or gestures or voice commands, or you might define them based upon the
     * features available in your application (play, pause, fast forward, etc.).
     * @param action   (required) this String defines the specific event action within the category specified.
     * In the example, we are basically saying that the category of the event is user clicks,
     * and the action is a button click.
     * @return an object that allows addition of further details.
     */
    fun event(category: String?, action: String?): EventBuilder {
        return EventBuilder(this, category, action)
    }

    class EventBuilder internal constructor(builder: TrackHelper, private val mCategory: String?, private val mAction: String?) : BaseEvent(builder) {
        private var mPath: String? = null
        private var mName: String? = null
        private var mValue: Float? = null

        /**
         * The path under which this event occurred.
         * Example: "/user/settings/billing", if you pass NULL, the last path set by #trackScreenView will be used.
         */
        fun path(path: String?): EventBuilder {
            mPath = path
            return this
        }

        /**
         * Defines a label associated with the event.
         * For example, if you have multiple Button controls on a screen, you might use the label to specify the specific View control identifier that was clicked.
         */
        fun name(name: String?): EventBuilder {
            mName = name
            return this
        }

        /**
         * Defines a numeric value associated with the event.
         * For example, if you were tracking "Buy" button clicks, you might log the number of items being purchased, or their total cost.
         */
        fun value(value: Float?): EventBuilder {
            mValue = value
            return this
        }

        override fun build(): TrackMe {
            val trackMe = TrackMe(baseTrackMe)
                    .set(QueryParams.URL_PATH, mPath)
                    .set(QueryParams.EVENT_CATEGORY, mCategory)
                    .set(QueryParams.EVENT_ACTION, mAction)
                    .set(QueryParams.EVENT_NAME, mName)
            if (mValue != null) trackMe[QueryParams.EVENT_VALUE] = mValue!!
            return trackMe
        }
    }

    /**
     * By default, Goals in Matomo are defined as "matching" parts of the screen path or screen title.
     * In this case a conversion is logged automatically. In some situations, you may want to trigger
     * a conversion manually on other types of actions, for example:
     * when a user submits a form
     * when a user has stayed more than a given amount of time on the page
     * when a user does some interaction in your Android application
     *
     * @param idGoal id of goal as defined in matomo goal settings
     */
    fun goal(idGoal: Int): Goal {
        return Goal(this, idGoal)
    }

    class Goal internal constructor(baseBuilder: TrackHelper, private val mIdGoal: Int) : BaseEvent(baseBuilder) {
        private var mRevenue: Float? = null

        /**
         * Tracking request will trigger a conversion for the goal of the website being tracked with this ID
         *
         * @param revenue a monetary value that was generated as revenue by this goal conversion.
         */
        fun revenue(revenue: Float?): Goal {
            mRevenue = revenue
            return this
        }

        override fun build(): TrackMe {
            require(mIdGoal >= 0) { "Goal id needs to be >=0" }
            val trackMe = TrackMe(baseTrackMe).set(QueryParams.GOAL_ID, mIdGoal)
            if (mRevenue != null) trackMe[QueryParams.REVENUE] = mRevenue!!
            return trackMe
        }
    }

    /**
     * Tracks an  [Outlink](http://matomo.org/faq/new-to-matomo/faq_71/)
     *
     * @param url HTTPS, HTTP and FTPare valid
     * @return this Tracker for chaining
     */
    fun outlink(url: URL?): Outlink {
        return Outlink(this, url)
    }

    class Outlink internal constructor(baseBuilder: TrackHelper, private val mURL: URL?) : BaseEvent(baseBuilder) {
        override fun build(): TrackMe {
            require(!(mURL == null || mURL.toExternalForm().isEmpty())) { "Outlink tracking requires a non-empty URL" }
            require(!(mURL.protocol != "http" && mURL.protocol != "https" && mURL.protocol != "ftp")) { "Only http|https|ftp is supported for outlinks" }
            return TrackMe(baseTrackMe)
                    .set(QueryParams.LINK, mURL.toExternalForm())
                    .set(QueryParams.URL_PATH, mURL.toExternalForm())
        }
    }

    /**
     * Tracks an  [site search](http://matomo.org/docs/site-search/)
     *
     * @param keyword Searched query in the app
     * @return this Tracker for chaining
     */
    fun search(keyword: String?): Search {
        return Search(this, keyword)
    }

    class Search internal constructor(baseBuilder: TrackHelper, private val mKeyword: String?) : BaseEvent(baseBuilder) {
        private var mCategory: String? = null
        private var mCount: Int? = null

        /**
         * You can optionally specify a search category with this parameter.
         *
         * @return this object, to chain calls.
         */
        fun category(category: String?): Search {
            mCategory = category
            return this
        }

        /**
         * We recommend to set the search count to the number of search results displayed on the results page.
         * When keywords are tracked with a count of 0, they will appear in the "No Result Search Keyword" report.
         *
         * @return this object, to chain calls.
         */
        fun count(count: Int?): Search {
            mCount = count
            return this
        }

        override fun build(): TrackMe {
            val trackMe = TrackMe(baseTrackMe)
                    .set(QueryParams.SEARCH_KEYWORD, mKeyword)
                    .set(QueryParams.SEARCH_CATEGORY, mCategory)
            if (mCount != null) trackMe[QueryParams.SEARCH_NUMBER_OF_HITS] = mCount!!
            return trackMe
        }
    }

    /**
     * Sends a download event for this app.
     * This only triggers an event once per app version unless you force it.
     *
     *
     * [Download.force]
     *
     *
     * Resulting download url:
     *
     *
     * Case [org.matomo.sdk.extra.DownloadTracker.Extra.ApkChecksum]:<br></br>
     * http://packageName:versionCode/apk-md5-checksum<br></br>
     *
     *
     * Case [org.matomo.sdk.extra.DownloadTracker.Extra.None]:<br></br>
     * http://packageName:versionCode
     *
     *
     *
     * @return this object, to chain calls.
     */
    fun download(downloadTracker: DownloadTracker?): Download {
        return Download(downloadTracker, this)
    }

    fun download(): Download {
        return Download(null, this)
    }

    class Download internal constructor(private var mDownloadTracker: DownloadTracker?, private val mBaseBuilder: TrackHelper) {
        private var mExtra: Extra = Extra.None()
        private var mForced = false
        private var mVersion: String? = null

        /**
         * Sets the identifier type for this download
         *
         * @param identifier [org.matomo.sdk.extra.DownloadTracker.Extra.ApkChecksum] or [org.matomo.sdk.extra.DownloadTracker.Extra.None]
         * @return this object, to chain calls.
         */
        fun identifier(identifier: Extra): Download {
            mExtra = identifier
            return this
        }

        /**
         * Normally a download event is only fired once per app version.
         * If the download has already been tracked for this version, nothing happens.
         * Calling this will force this download to be tracked.
         *
         * @return this object, to chain calls.
         */
        fun force(): Download {
            mForced = true
            return this
        }

        /**
         * To track specific app versions. Useful if the app can change without the apk being updated (e.g. hybrid apps/web apps).
         *
         * @param version by default [android.content.pm.PackageInfo.versionCode] is used.
         * @return this object, to chain calls.
         */
        fun version(version: String?): Download {
            mVersion = version
            return this
        }

        fun with(tracker: Tracker?) {
            if (mDownloadTracker == null) mDownloadTracker = DownloadTracker(tracker!!)
            if (mVersion != null) mDownloadTracker!!.version = mVersion
            if (mForced) mDownloadTracker!!.trackNewAppDownload(mBaseBuilder.baseTrackMe, mExtra) else mDownloadTracker!!.trackOnce(mBaseBuilder.baseTrackMe, mExtra)
        }
    }

    /**
     * Tracking the impressions
     *
     * @param contentName The name of the content. For instance 'Ad Foo Bar'
     */
    fun impression(contentName: String?): ContentImpression {
        return ContentImpression(this, contentName)
    }

    class ContentImpression internal constructor(baseBuilder: TrackHelper, private val mContentName: String?) : BaseEvent(baseBuilder) {
        private var mContentPiece: String? = null
        private var mContentTarget: String? = null

        /**
         * @param contentPiece The actual content. For instance the path to an image, video, audio, any text
         */
        fun piece(contentPiece: String?): ContentImpression {
            mContentPiece = contentPiece
            return this
        }

        /**
         * @param contentTarget The target of the content. For instance the URL of a landing page.
         */
        fun target(contentTarget: String?): ContentImpression {
            mContentTarget = contentTarget
            return this
        }

        override fun build(): TrackMe {
            require(!(mContentName == null || mContentName.isEmpty())) { "Tracking content impressions requires a non-empty content-name" }
            return TrackMe(baseTrackMe)
                    .set(QueryParams.CONTENT_NAME, mContentName)
                    .set(QueryParams.CONTENT_PIECE, mContentPiece)
                    .set(QueryParams.CONTENT_TARGET, mContentTarget)
        }
    }

    /**
     * Tracking the interactions
     *
     *
     * To map an interaction to an impression make sure to set the same value for contentName and contentPiece as
     * the impression has.
     *
     * @param contentInteraction The name of the interaction with the content. For instance a 'click'
     * @param contentName        The name of the content. For instance 'Ad Foo Bar'
     */
    fun interaction(contentName: String?, contentInteraction: String?): ContentInteraction {
        return ContentInteraction(this, contentName, contentInteraction)
    }

    class ContentInteraction internal constructor(baseBuilder: TrackHelper, private val mContentName: String?, private val mInteraction: String?) : BaseEvent(baseBuilder) {
        private var mContentPiece: String? = null
        private var mContentTarget: String? = null

        /**
         * @param contentPiece The actual content. For instance the path to an image, video, audio, any text
         */
        fun piece(contentPiece: String?): ContentInteraction {
            mContentPiece = contentPiece
            return this
        }

        /**
         * @param contentTarget The target the content leading to when an interaction occurs. For instance the URL of a landing page.
         */
        fun target(contentTarget: String?): ContentInteraction {
            mContentTarget = contentTarget
            return this
        }

        override fun build(): TrackMe {
            require(!(mContentName == null || mContentName.isEmpty())) { "Content name needs to be non-empty" }
            require(!(mInteraction == null || mInteraction.isEmpty())) { "Interaction name needs to be non-empty" }
            return TrackMe(baseTrackMe)
                    .set(QueryParams.CONTENT_NAME, mContentName)
                    .set(QueryParams.CONTENT_PIECE, mContentPiece)
                    .set(QueryParams.CONTENT_TARGET, mContentTarget)
                    .set(QueryParams.CONTENT_INTERACTION, mInteraction)
        }
    }

    /**
     * Tracks a shopping cart. Call this javascript function every time a user is adding, updating
     * or deleting a product from the cart.
     *
     * @param grandTotal total value of items in cart
     */
    fun cartUpdate(grandTotal: Int): CartUpdate {
        return CartUpdate(this, grandTotal)
    }

    class CartUpdate internal constructor(baseBuilder: TrackHelper, private val mGrandTotal: Int) : BaseEvent(baseBuilder) {
        private var mEcommerceItems: EcommerceItems? = null

        /**
         * @param items Items included in the cart
         */
        fun items(items: EcommerceItems?): CartUpdate {
            mEcommerceItems = items
            return this
        }

        override fun build(): TrackMe {
            if (mEcommerceItems == null) mEcommerceItems = EcommerceItems()
            return TrackMe(baseTrackMe)
                    .set(QueryParams.GOAL_ID, 0)
                    .set(QueryParams.REVENUE, CurrencyFormatter.priceString(mGrandTotal))
                    .set(QueryParams.ECOMMERCE_ITEMS, mEcommerceItems!!.toJson())
        }
    }

    /**
     * Tracks an Ecommerce order, including any ecommerce item previously added to the order.  All
     * monetary values should be passed as an integer number of cents (or the smallest integer unit
     * for your currency)
     *
     * @param orderId    (required) A unique string identifying the order
     * @param grandTotal (required) total amount of the order, in cents
     */
    fun order(orderId: String?, grandTotal: Int): Order {
        return Order(this, orderId, grandTotal)
    }

    class Order internal constructor(baseBuilder: TrackHelper, private val mOrderId: String?, private val mGrandTotal: Int) : BaseEvent(baseBuilder) {
        private var mEcommerceItems: EcommerceItems? = null
        private var mDiscount: Int? = null
        private var mShipping: Int? = null
        private var mTax: Int? = null
        private var mSubTotal: Int? = null

        /**
         * @param subTotal the subTotal for the order, in cents
         */
        fun subTotal(subTotal: Int?): Order {
            mSubTotal = subTotal
            return this
        }

        /**
         * @param tax the tax for the order, in cents
         */
        fun tax(tax: Int?): Order {
            mTax = tax
            return this
        }

        /**
         * @param shipping the shipping for the order, in cents
         */
        fun shipping(shipping: Int?): Order {
            mShipping = shipping
            return this
        }

        /**
         * @param discount the discount for the order, in cents
         */
        fun discount(discount: Int?): Order {
            mDiscount = discount
            return this
        }

        /**
         * @param items the items included in the order
         */
        fun items(items: EcommerceItems?): Order {
            mEcommerceItems = items
            return this
        }

        override fun build(): TrackMe {
            if (mEcommerceItems == null) mEcommerceItems = EcommerceItems()
            return TrackMe(baseTrackMe)
                    .set(QueryParams.GOAL_ID, 0)
                    .set(QueryParams.ORDER_ID, mOrderId)
                    .set(QueryParams.REVENUE, CurrencyFormatter.priceString(mGrandTotal))
                    .set(QueryParams.ECOMMERCE_ITEMS, mEcommerceItems!!.toJson())
                    .set(QueryParams.SUBTOTAL, CurrencyFormatter.priceString(mSubTotal))
                    .set(QueryParams.TAX, CurrencyFormatter.priceString(mTax))
                    .set(QueryParams.SHIPPING, CurrencyFormatter.priceString(mShipping))
                    .set(QueryParams.DISCOUNT, CurrencyFormatter.priceString(mDiscount))
        }
    }

    /**
     * Caught exceptions are errors in your app for which you've defined exception handling code,
     * such as the occasional timeout of a network connection during a request for data.
     *
     *
     * This is just a different way to define an event.
     * Keep in mind Matomo is not a crash tracker, use this sparingly.
     *
     *
     * For this to be useful you should ensure that proguard does not remove all classnames and line numbers.
     * Also note that if this is used across different app versions and obfuscation is used, the same exception might be mapped to different obfuscated names by proguard.
     * This would mean the same exception (event) is tracked as different events by Matomo.
     *
     * @param throwable exception instance
     */
    fun exception(throwable: Throwable): Exception {
        return Exception(this, throwable)
    }

    class Exception internal constructor(baseBuilder: TrackHelper, private val mThrowable: Throwable) : BaseEvent(baseBuilder) {
        private var mDescription: String? = null
        private var mIsFatal = false

        /**
         * @param description exception message
         */
        fun description(description: String?): Exception {
            mDescription = description
            return this
        }

        /**
         * @param isFatal true if it's fatal exception
         */
        fun fatal(isFatal: Boolean): Exception {
            mIsFatal = isFatal
            return this
        }

        override fun build(): TrackMe {
            val className: String
            className = try {
                val trace = mThrowable.stackTrace[0]
                trace.className + "/" + trace.methodName + ":" + trace.lineNumber
            } catch (e: java.lang.Exception) {
                Timber.tag(TAG).w(e, "Couldn't get stack info")
                mThrowable.javaClass.name
            }
            val actionName = "exception/" + (if (mIsFatal) "fatal/" else "") + "$className/" + mDescription
            return TrackMe(baseTrackMe)
                    .set(QueryParams.ACTION_NAME, actionName)
                    .set(QueryParams.EVENT_CATEGORY, "Exception")
                    .set(QueryParams.EVENT_ACTION, className)
                    .set(QueryParams.EVENT_NAME, mDescription)
                    .set(QueryParams.EVENT_VALUE, if (mIsFatal) 1 else 0)
        }
    }

    /**
     * This will create an exception handler that wraps any existing exception handler.
     * Exceptions will be caught, tracked, dispatched and then rethrown to the previous exception handler.
     *
     *
     * Be wary of relying on this for complete crash tracking..
     * Think about how to deal with older app versions still throwing already fixed exceptions.
     *
     *
     * See discussion here: https://github.com/matomo-org/matomo-sdk-android/issues/28
     */
    fun uncaughtExceptions(): UncaughtExceptions {
        return UncaughtExceptions(this)
    }

    class UncaughtExceptions internal constructor(private val mBaseBuilder: TrackHelper) {
        /**
         * @param tracker the tracker that should receive the exception events.
         * @return returns the new (but already active) exception handler.
         */
        fun with(tracker: Tracker?): Thread.UncaughtExceptionHandler {
            if (Thread.getDefaultUncaughtExceptionHandler() is MatomoExceptionHandler) {
                throw RuntimeException("Trying to wrap an existing MatomoExceptionHandler.")
            }
            val handler: Thread.UncaughtExceptionHandler = MatomoExceptionHandler(tracker!!, mBaseBuilder.baseTrackMe)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            return handler
        }
    }

    /**
     * This method will bind a tracker to your application,
     * causing it to automatically track Activities with [.screen] within your app.
     *
     * @param app your app
     * @return the registered callback, you need this if you wanted to unregister the callback again
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun screens(app: Application): AppTracking {
        return AppTracking(this, app)
    }

    class AppTracking(private val mBaseBuilder: TrackHelper, private val mApplication: Application) {
        /**
         * @param tracker the tracker to use
         * @return the registered callback, you need this if you wanted to unregister the callback again
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        fun with(tracker: Tracker): ActivityLifecycleCallbacks {
            val callback: ActivityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {}
                override fun onActivityStarted(activity: Activity?) {}
                override fun onActivityResumed(activity: Activity?) {
                    mBaseBuilder.screen(activity).with(tracker)
                }

                override fun onActivityPaused(activity: Activity?) {}
                override fun onActivityStopped(activity: Activity?) {
                    if (activity != null && activity.isTaskRoot) {
                        tracker.dispatch()
                    }
                }

                override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {}
                override fun onActivityDestroyed(activity: Activity?) {}
            }
            mApplication.registerActivityLifecycleCallbacks(callback)
            return callback
        }
    }

    open fun dimension(id: Int, value: String?): Dimension {
        return Dimension(baseTrackMe).dimension(id, value)
    }

    class Dimension internal constructor(base: TrackMe?) : TrackHelper(base) {
        override fun dimension(id: Int, value: String?): Dimension {
            setDimension(baseTrackMe, id, value)
            return this
        }
    }

    /**
     * To track visit scoped custom variables.
     *
     */
    open fun visitVariables(id: Int, name: String?, value: String?): VisitVariables {
        val customVariables = CustomVariables()
        customVariables.put(id, name, value)
        return visitVariables(customVariables)
    }

    /**
     * To track visit scoped custom variables.
     *
     */
    fun visitVariables(customVariables: CustomVariables?): VisitVariables {
        return VisitVariables(this, customVariables)
    }

    class VisitVariables(baseBuilder: TrackHelper, customVariables: CustomVariables?) : TrackHelper(baseBuilder.baseTrackMe) {

        override fun visitVariables(id: Int, name: String?, value: String?): VisitVariables {
            val customVariables = CustomVariables(baseTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES])
            customVariables.put(id, name, value)
            baseTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES] = customVariables.toString()
            return this
        }

        init {
            val mergedVariables = CustomVariables(baseTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES])
            mergedVariables.putAll(customVariables!!)
            baseTrackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES] = mergedVariables.toString()
        }
    }

    companion object {
        private val TAG = Matomo.tag(TrackHelper::class.java)

        @JvmStatic
        fun track(): TrackHelper {
            return TrackHelper()
        }

        @JvmStatic
        fun track(base: TrackMe?): TrackHelper {
            return TrackHelper(base)
        }
    }
}
