package com.appsflyer.adobeextension

import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.MobileCore
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib

class AppsflyerAdobeExtensionConversionListener (val extensionApi: ExtensionApi, val trackAttributionDataGetter: () -> Boolean, val ecidGetter: () -> String?, val contextProvider: ContextProvider): AppsFlyerConversionListener {


    override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>) {
        AppsflyerAdobeExtensionImpl.logAFExtension("called onConversionDataSuccess")
        if (trackAttributionDataGetter()) {
            val isFirstLaunch = conversionData[AppsflyerAdobeConstatns.IS_FIRST_LAUNCH] as Boolean
            if (isFirstLaunch) {
                extensionApi.createSharedState(getSharedEventState(conversionData),null)
                contextProvider.af_application?.let { context ->
                    AppsFlyerLib.getInstance().getAppsFlyerUID(context)?.let { appsflyerUID ->
                        conversionData[AppsflyerAdobeConstatns.APPSFLYER_ID] = appsflyerUID
                    }
                }
                // Send AppsFlyer Attribution data to Adobe Analytics;
                ecidGetter()?.let {
                    conversionData["ecid"] = it
                }
                MobileCore.trackAction(AppsflyerAdobeConstatns.APPSFLYER_ATTRIBUTION_DATA, setKeyPrefix(conversionData))
            } else {
                AppsflyerAdobeExtensionImpl.logAFExtension("Skipping attribution data reporting, not first launch")
            }
        }
        AppsflyerAdobeExtension.afCallbackListener?.onConversionDataSuccess(conversionData.toMap())
    }

    override fun onConversionDataFail(errorMessage: String) {
        AppsflyerAdobeExtensionImpl.logAFExtension("called onConversionDataFail")
        AppsflyerAdobeExtension.afCallbackListener?.onConversionDataFail(errorMessage)
    }

    override fun onAppOpenAttribution(deepLinkData: MutableMap<String, String>) {
        AppsflyerAdobeExtensionImpl.logAFExtension("called onAppOpenAttribution")
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
        AppsflyerAdobeExtensionImpl.logAFExtension("called onAttributionFailure")
        AppsflyerAdobeExtension.afCallbackListener?.onAttributionFailure(errorMessage)
    }

    private fun getSharedEventState(conversionData: Map<String, Any>): MutableMap<String, Any> {
        // copy conversion data
        var sharedEventState = conversionData.toMutableMap()
        contextProvider.af_application?.let{
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

    private fun setKeyPrefix(attributionParams: Map<String, Any?>): Map<String, String?>? {
        return attributionParams.toMutableMap().run {
            remove(AppsflyerAdobeConstatns.CALLBACK_TYPE)
            entries.associate { "appsflyer.${it.key}" to it.value.toString() }
        }
    }

    private fun setKeyPrefixOnAppOpenAttribution(attributionParams: Map<String, String>): Map<String, String>? {
        return attributionParams.toMutableMap().run {
            remove(AppsflyerAdobeConstatns.CALLBACK_TYPE)
            entries.associate { "appsflyer.af_engagement_${it.key}" to it.value }
        }
    }
}