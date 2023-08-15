package com.appsflyer.adobeextension

import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.MobileCore
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logAFExtension
import com.appsflyer.adobeextension.MapHandlers.setKeyPrefixOnAppOpenAttribution
import com.appsflyer.adobeextension.MapHandlers.setKeyPrefixToAppsflyerDot

typealias TrackGetter =  () -> Boolean
typealias ECIDGetter =  () -> String?

class AppsflyerAdobeExtensionConversionListener (private val extensionApi: ExtensionApi, private val trackAttributionDataGetter: TrackGetter, private val ecidGetter: ECIDGetter): AppsFlyerConversionListener {
    override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>) {
        logAFExtension("called onConversionDataSuccess")
        if (trackAttributionDataGetter()) {
            val isFirstLaunch = conversionData[AppsflyerAdobeConstatns.IS_FIRST_LAUNCH] as Boolean
            if (isFirstLaunch) {
                extensionApi.createSharedState(getSharedEventState(conversionData),null)
                ContextProvider.afApplication?.let { context ->
                    AppsFlyerLib.getInstance().getAppsFlyerUID(context)?.let { appsflyerUID ->
                        conversionData[AppsflyerAdobeConstatns.APPSFLYER_ID] = appsflyerUID
                    }
                }
                // Send AppsFlyer Attribution data to Adobe Analytics;
                ecidGetter()?.let {
                    conversionData["ecid"] = it
                }
                MobileCore.trackAction(AppsflyerAdobeConstatns.APPSFLYER_ATTRIBUTION_DATA, setKeyPrefixToAppsflyerDot(conversionData))
            } else {
                logAFExtension("Skipping attribution data reporting, not first launch")
            }
        }
        AppsflyerAdobeExtension.afCallbackListener?.onConversionDataSuccess(conversionData.toMap())
    }

    override fun onConversionDataFail(errorMessage: String) {
        logAFExtension("called onConversionDataFail")
        AppsflyerAdobeExtension.afCallbackListener?.onConversionDataFail(errorMessage)
    }

    override fun onAppOpenAttribution(deepLinkData: MutableMap<String, String>) {
        logAFExtension("called onAppOpenAttribution")
        ecidGetter()?.let{
            deepLinkData["ecid"] = it
        }
        MobileCore.trackAction(
            AppsflyerAdobeConstatns.APPSFLYER_ENGAGMENT_DATA,
            setKeyPrefixOnAppOpenAttribution(deepLinkData)
        )
        AppsflyerAdobeExtension.afCallbackListener?.onAppOpenAttribution(deepLinkData)
    }

    override fun onAttributionFailure(errorMessage: String) {
        logAFExtension("called onAttributionFailure")
        AppsflyerAdobeExtension.afCallbackListener?.onAttributionFailure(errorMessage)
    }

    private fun getSharedEventState(conversionData: Map<String, Any>): MutableMap<String, Any> {
        // copy conversion data
        val sharedEventState = conversionData.toMutableMap()
        ContextProvider.afApplication?.let {
            AppsFlyerLib.getInstance().getAppsFlyerUID(it)?.let{ res ->
                sharedEventState[AppsflyerAdobeConstatns.APPSFLYER_ID] = res
            }
        }
        sharedEventState[AppsflyerAdobeConstatns.SDK_VERSION] = AppsFlyerLib.getInstance().sdkVersion
        if (!conversionData.containsKey(AppsflyerAdobeConstatns.MEDIA_SOURCE)) {
            sharedEventState[AppsflyerAdobeConstatns.MEDIA_SOURCE] = "organic"
        }
        sharedEventState.remove(AppsflyerAdobeConstatns.IS_FIRST_LAUNCH)
        sharedEventState.remove(AppsflyerAdobeConstatns.CALLBACK_TYPE)
        return sharedEventState
    }
}