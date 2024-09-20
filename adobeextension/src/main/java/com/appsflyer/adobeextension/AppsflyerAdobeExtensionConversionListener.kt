package com.appsflyer.adobeextension

import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.MobileCore
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.APPSFLYER_ATTRIBUTION_DATA
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.APPSFLYER_ENGAGMENT_DATA
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logAFExtension

typealias TrackGetter = () -> Boolean
typealias ECIDGetter = () -> String?

const val ADOBE_ACTION_KEY = "action"

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
                ecidGetter()?.let { conversionData["ecid"] = it }
                val attributionData = conversionData.setKeyPrefixToAppsflyerDot()

                sendDataToAdobe(APPSFLYER_ATTRIBUTION_DATA, attributionData)

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

        ecidGetter()?.let { deepLinkData["ecid"] = it }
        val data = deepLinkData.setKeyPrefixOnAppOpenAttribution()

        sendDataToAdobe(APPSFLYER_ENGAGMENT_DATA, data)

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
        return sharedEventState
    }

    private fun sendDataToAdobe(action: String, data: Map<String, String?>) {
        sendAdobeAnalyticsAction(action, data)
        sendAdobeEdgeEvent(action, data)
    }

    private fun sendAdobeAnalyticsAction(action: String, data: Map<String, String?>) {
        MobileCore.trackAction(action, data)
    }

    private fun sendAdobeEdgeEvent(action: String, data: Map<String, String?>) {
        Edge.sendEvent(
            ExperienceEvent.Builder().apply {
                setData(mapOf(ADOBE_ACTION_KEY to action))
                setXdmSchema(data)
            }.build(),
            null
        )
    }
}
