package com.appsflyer.adobeextension

import com.appsflyer.AFInAppEventParameterName
import org.json.JSONObject

internal fun Map<String, Any?>.setKeyPrefixToAppsflyerDot(): Map<String, String?> {
    return entries.associate { "appsflyer.${it.key}" to it.value.toString() }
}

internal fun Map<String, String>?.setKeyPrefixOnAppOpenAttribution(): Map<String, String> {
    return this?.toMutableMap()?.run {
        entries.associate { "appsflyer.af_engagement_${it.key}" to it.value }
    } ?: mapOf()
}

internal fun Map<String, Any>?.setRevenueAndCurrencyKeysNaming(): Map<String, Any> {
    val map = this?.toMutableMap() ?: return mutableMapOf()
    try {
        (map["revenue"] as String?)?.let {
            map.put(AFInAppEventParameterName.REVENUE, it)
        }
        (map["currency"] as String?)?.let {
            map.put(AFInAppEventParameterName.CURRENCY, it)
        }
    } catch (ex: Exception) {
        AppsflyerAdobeExtensionLogger.logErrorAFExtension("Error casting contextdata: $ex")
    }
    return map
}

internal fun JSONObject.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = optString(key)
        map[key] = value.toString()
    }
    return map
}
