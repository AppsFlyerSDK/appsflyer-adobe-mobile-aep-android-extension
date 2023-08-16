package com.appsflyer.adobeextension

import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.MobileCore
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logAFExtension

typealias TrackGetter = () -> Boolean
typealias ECIDGetter = () -> String?

class AppsflyerAdobeExtensionConversionListener(
    private val extensionApi: ExtensionApi,
    private val trackAttributionDataGetter: TrackGetter,
    private val ecidGetter: ECIDGetter
) : AppsFlyerConversionListener {
    override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>) {
        logAFExtension("called onConversionDataSuccess")
        if (trackAttributionDataGetter()) {
            val isFirstLaunch = conversionData[AppsflyerAdobeConstants.IS_FIRST_LAUNCH] as Boolean
            if (isFirstLaunch) {
                extensionApi.createSharedState(getSharedEventState(conversionData), null)
                ContextProvider.context?.let { context ->
                    AppsFlyerLib.getInstance().getAppsFlyerUID(context)?.let { appsflyerUID ->
                        conversionData[AppsflyerAdobeConstants.APPSFLYER_ID] = appsflyerUID
                    }
                }
                // Send AppsFlyer Attribution data to Adobe Analytics;
                ecidGetter()?.let {
                    conversionData["ecid"] = it
                }
                MobileCore.trackAction(
                    AppsflyerAdobeConstants.APPSFLYER_ATTRIBUTION_DATA,
                    conversionData.setKeyPrefixToAppsflyerDot()
                )
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
        ecidGetter()?.let {
            deepLinkData["ecid"] = it
        }
        MobileCore.trackAction(
            AppsflyerAdobeConstants.APPSFLYER_ENGAGMENT_DATA,
            deepLinkData.setKeyPrefixOnAppOpenAttribution()
        )
        AppsflyerAdobeExtension.afCallbackListener?.onAppOpenAttribution(deepLinkData)
    }

    override fun onAttributionFailure(errorMessage: String) {
        logAFExtension("called onAttributionFailure")
        AppsflyerAdobeExtension.afCallbackListener?.onAttributionFailure(errorMessage)
    }

    private fun getSharedEventState(conversionData: Map<String, Any>): Map<String, Any> {
        // copy conversion data
        val sharedEventState = conversionData.toMutableMap()
        ContextProvider.context?.let {
            AppsFlyerLib.getInstance().getAppsFlyerUID(it)?.let { res ->
                sharedEventState[AppsflyerAdobeConstants.APPSFLYER_ID] = res
            }
        }
        sharedEventState[AppsflyerAdobeConstants.SDK_VERSION] =
            AppsFlyerLib.getInstance().sdkVersion
        if (!conversionData.containsKey(AppsflyerAdobeConstants.MEDIA_SOURCE)) {
            sharedEventState[AppsflyerAdobeConstants.MEDIA_SOURCE] = "organic"
        }
        sharedEventState.remove(AppsflyerAdobeConstants.IS_FIRST_LAUNCH)
        sharedEventState.remove(AppsflyerAdobeConstants.CALLBACK_TYPE)
        return sharedEventState
    }
}