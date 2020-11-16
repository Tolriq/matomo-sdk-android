/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk

/**
 * Query parameters supported by the tracking HTTP API.
 * See [Tracking HTTP API](http://developer.matomo.org/api-reference/tracking-api)
 */
enum class QueryParams(private val value: String) {
    //Required parameters
    /**
     * The ID of the website we're tracking a visit/action for.
     *
     *
     * (required)
     */
    SITE_ID("idsite"),

    /**
     * Required for tracking, must be set to one, eg, rec=1.
     *
     *
     * (required)
     */
    RECORD("rec"),

    /**
     * The full URL for the current action.
     *
     *
     * (required)
     */
    URL_PATH("url"),  //Recommended parameters

    /**
     * The title of the action being tracked.
     *
     *
     * It is possible to [use slashes / to set one or several categories for this action.](http://matomo.org/faq/how-to/#faq_62)
     * For example, Help / Feedback will create the Action Feedback in the category Help.
     *
     *
     * (recommended)
     */
    ACTION_NAME("action_name"),

    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.
     *
     *
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Matomo will still track visits, but the unique visitors metric might be less accurate.
     *
     *
     * (recommended)
     */
    VISITOR_ID("_id"),

    /**
     * Meant to hold a random value that is generated before each request.
     *
     *
     * Using it helps avoid the tracking request being cached by the browser or a proxy.
     *
     *
     * (recommended)
     */
    RANDOM_NUMBER("rand"),

    /**
     * The parameter apiv=1 defines the api version to use (currently always set to 1)
     *
     *
     * (recommended)
     */
    API_VERSION("apiv"),  // Optional User info

    /**
     * The full HTTP Referrer URL.
     *
     *
     * This value is used to determine how someone got to your website (ie, through a website, search engine or campaign).
     */
    REFERRER("urlref"),

    /**
     * Visit scope [custom variables](http://matomo.org/docs/custom-variables/).
     *
     *
     * This is a JSON encoded string of the custom variable array.
     * @see org.matomo.sdk.extra.CustomDimension
     */
    VISIT_SCOPE_CUSTOM_VARIABLES("_cvar"),

    /**
     * The current count of visits for this visitor.
     *
     *
     * To set this value correctly, it would be required to store the value for each visitor in your application (using sessions or persisting in a database).
     * Then you would manually increment the counts by one on each new visit or "session", depending on how you choose to define a visit.
     * This value is used to populate the report Visitors &gt; Engagement &gt; Visits by visit number.
     */
    TOTAL_NUMBER_OF_VISITS("_idvc"),

    /**
     * The UNIX timestamp of this visitor's previous visit (seconds since Jan 01 1970. (UTC)).
     *
     *
     * This parameter is used to populate the report Visitors &gt; Engagement &gt; Visits by days since last visit.
     */
    PREVIOUS_VISIT_TIMESTAMP("_viewts"),

    /**
     * The UNIX timestamp of this visitor's first visit (seconds since Jan 01 1970. (UTC)).
     *
     *
     * This could be set to the date where the user first started using your software/app, or when he/she created an account.
     * This parameter is used to populate the Goals &gt; Days to Conversion report.
     */
    FIRST_VISIT_TIMESTAMP("_idts"),

    /**
     * The Campaign name (see [Tracking Campaigns](http://matomo.org/docs/tracking-campaigns/)).
     *
     *
     * Used to populate the Referrers &gt; Campaigns report.
     * Note: this parameter will only be used for the first pageview of a visit.
     */
    CAMPAIGN_NAME("_rcn"),

    /**
     * The Campaign Keyword (see [Tracking Campaigns](http://matomo.org/docs/tracking-campaigns/)).
     *
     *
     * Used to populate the Referrers &gt; Campaigns report (clicking on a campaign loads all keywords for this campaign).
     * Note: this parameter will only be used for the first pageview of a visit.
     */
    CAMPAIGN_KEYWORD("_rck"),

    /**
     * The resolution of the device the visitor is using, eg 1280x1024.
     */
    SCREEN_RESOLUTION("res"),

    /**
     * The current hour (local time).
     */
    HOURS("h"),

    /**
     * The current minute (local time).
     */
    MINUTES("m"),

    /**
     * The current second (local time).
     */
    SECONDS("s"),

    /**
     * An override value for the User-Agent HTTP header field.
     *
     *
     * The user agent is used to detect the operating system and browser used.
     */
    USER_AGENT("ua"),

    /**
     * An override value for the Accept-Language HTTP header field.
     *
     *
     * This value is used to detect the visitor's country if [GeoIP](http://matomo.org/faq/troubleshooting/#faq_65) is not enabled.
     */
    LANGUAGE("lang"),

    /**
     * Defines the User ID for this request.
     *
     *
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can fetch this user ID from your system, and pass it to Matomo.
     * The User ID appears in the visitor log, the Visitor profile, and you can Segment reports for one or several User ID (userId segment).
     * When specified, the User ID will be "enforced". This means that if there is no recent visit with this User ID, a new one will be created.
     * If a visit is found in the last 30 minutes with your specified User ID, then the new action will be recorded to this existing visit.
     */
    USER_ID("uid"),

    /**
     * If set to 1, will force a new visit to be created for this action.
     */
    SESSION_START("new_visit"),  // Optional Action info (measure Page view, Outlink, Download, Site search)

    /**
     * Page scope [custom variables](http://matomo.org/docs/custom-variables/).
     * This is a JSON encoded string of the custom variable array.
     * @see org.matomo.sdk.extra.CustomDimension
     */
    SCREEN_SCOPE_CUSTOM_VARIABLES("cvar"),

    /**
     * An external URL the user has opened.
     *
     *
     * Used for tracking outlink clicks. We recommend to also set the url parameter to this same value.
     */
    LINK("link"),

    /**
     * URL of a file the user has downloaded.
     *
     *
     * Used for tracking downloads. We recommend to also set the url parameter to this same value.
     */
    DOWNLOAD("download"),

    /**
     * The Site Search keyword.
     *
     *
     * When specified, the request will not be tracked as a normal pageview but will instead be tracked as a [Site Search](http://matomo.org/docs/site-search/) request.
     */
    SEARCH_KEYWORD("search"),

    /**
     * When [.SEARCH_KEYWORD] is specified, you can optionally specify a search category with this parameter.
     */
    SEARCH_CATEGORY("search_cat"),

    /**
     * When [.SEARCH_KEYWORD] is specified, we also recommend to set this to the number of search results.
     */
    SEARCH_NUMBER_OF_HITS("search_count"),

    /**
     * If specified, the tracking request will trigger a conversion for the goal of the website being tracked with this ID.
     */
    GOAL_ID("idgoal"),

    /**
     * A monetary value that was generated as revenue by this goal conversion.
     *
     *
     * Only used if [.GOAL_ID] is specified in the request.
     */
    REVENUE("revenue"),

    /**
     * Override for the datetime of the request (normally the current time is used).
     *
     *
     * This can be used to record visits and page views in the past.
     * The expected format is: 2011-04-05 00:11:42 (remember to URL encode the value!).
     * The datetime must be sent in UTC timezone.
     * Events can only be backdated for a maximum time of 24h.
     * Note: if you record data in the past, you will need to [force Matomo to re-process reports for the past dates.](http://matomo.org/faq/how-to/#faq_59)
     */
    DATETIME_OF_REQUEST("cdt"),

    /**
     * The name of the content. For instance 'Ad Foo Bar'
     *
     * @see [Content Tracking](http://matomo.org/docs/content-tracking/)
     */
    CONTENT_NAME("c_n"),

    /**
     * The actual content piece. For instance the path to an image, video, audio, any text
     *
     * @see [Content Tracking](http://matomo.org/docs/content-tracking/)
     */
    CONTENT_PIECE("c_p"),

    /**
     * The target of the content. For instance the URL of a landing page
     *
     * @see [Content Tracking](http://matomo.org/docs/content-tracking/)
     */
    CONTENT_TARGET("c_t"),

    /**
     * The name of the interaction with the content. For instance a 'click'
     *
     * @see [Content Tracking](http://matomo.org/docs/content-tracking/)
     */
    CONTENT_INTERACTION("c_i"),

    /**
     * The event category. Must not be empty. (eg. Videos, Music, Games...)
     *
     * @see [Event Tracking](http://matomo.org/docs/event-tracking/)
     */
    EVENT_CATEGORY("e_c"),

    /**
     * The event action. Must not be empty. (eg. Play, Pause, Duration, Add Playlist, Downloaded, Clicked...)
     *
     * @see [Event Tracking](http://matomo.org/docs/event-tracking/)
     */
    EVENT_ACTION("e_a"),

    /**
     * The event name. (eg. a Movie name, or Song name, or File name...)
     *
     * @see [Event Tracking](http://matomo.org/docs/event-tracking/)
     */
    EVENT_NAME("e_n"),

    /**
     * The event value. Must be a float or integer value (numeric), not a string.
     *
     * @see [Event Tracking](http://matomo.org/docs/event-tracking/)
     */
    EVENT_VALUE("e_v"),  // Ecommerce parameters

    /**
     * Items in your cart or order for ecommerce tracking
     */
    ECOMMERCE_ITEMS("ec_items"),

    /**
     * The amount of tax paid for the order
     */
    TAX("ec_tx"),

    /**
     * The unique identifier for the order
     */
    ORDER_ID("ec_id"),

    /**
     * The amount of shipping paid on the order
     */
    SHIPPING("ec_sh"),

    /**
     * The amount of the discount on the order
     */
    DISCOUNT("ec_dt"),

    /**
     * The sub total amount of the order
     */
    SUBTOTAL("ec_st"),  // Other parameters

    /**
     * If set to 0 (send_image=0) Matomo will respond with a HTTP 204 response code instead of a GIF image.
     *
     *
     * This improves performance and can fix errors if images are not allowed to be obtained directly (eg Chrome Apps). Available since Matomo 2.10.0
     */
    SEND_IMAGE("send_image");

    override fun toString(): String {
        return value
    }
}