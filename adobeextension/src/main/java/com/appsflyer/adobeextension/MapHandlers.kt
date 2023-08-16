package com.appsflyer.adobeextension

import com.appsflyer.AFInAppEventParameterName

internal object MapHandlers {
    internal fun Map<String, Any?>.setKeyPrefixToAppsflyerDot(): Map<String, String?> {
        return this.toMutableMap().run {
            remove(AppsflyerAdobeConstants.CALLBACK_TYPE)
            entries.associate { "appsflyer.${it.key}" to it.value.toString() }
        }
    }

    internal fun Map<String, String>?.setKeyPrefixOnAppOpenAttribution(): Map<String, String> {
        return this?.toMutableMap()?.run {
            remove(AppsflyerAdobeConstants.CALLBACK_TYPE)
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
}