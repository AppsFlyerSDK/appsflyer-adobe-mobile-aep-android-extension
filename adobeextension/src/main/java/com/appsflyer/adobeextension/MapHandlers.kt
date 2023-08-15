package com.appsflyer.adobeextension

import com.appsflyer.AFInAppEventParameterName

internal object MapHandlers {
    internal fun setKeyPrefixToAppsflyerDot(attributionParams: Map<String, Any?>): Map<String, String?>{
        return attributionParams.toMutableMap().run {
            remove(AppsflyerAdobeConstatns.CALLBACK_TYPE)
            entries.associate { "appsflyer.${it.key}" to it.value.toString() }
        }
    }

    internal fun setKeyPrefixOnAppOpenAttribution(attributionParams: Map<String, String>?): Map<String, String>{
        return attributionParams?.toMutableMap()?.run {
            remove(AppsflyerAdobeConstatns.CALLBACK_TYPE)
            entries.associate { "appsflyer.af_engagement_${it.key}" to it.value }
        } ?: mapOf()
    }

    internal fun setRevenueAndCurrencyKeysNaming(nestedData: Map<String,Any>?): Map<String, Any> {
        val map = nestedData?.toMutableMap() ?:  return mutableMapOf()
        try {
            (map["revenue"] as String?)?.let {
                map.put(AFInAppEventParameterName.REVENUE, it)
            }
            (map["currency"] as String?)?.let{
                map.put(AFInAppEventParameterName.CURRENCY, it)
            }
        } catch (ex: Exception) {
            AppsflyerAdobeExtensionLogger.logErrorAFExtension("Error casting contextdata: $ex")
        }
        return map
    }
}