package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.Identity
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.APPSFLYER_ATTRIBUTION_DATA
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.APPSFLYER_ENGAGMENT_DATA
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.DEV_KEY_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.EVENT_SETTING_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.IS_DEBUG_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.TRACK_ATTR_DATA_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.WAIT_FOR_ECID
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logAFExtension
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logErrorAFExtension
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkResult
import com.appsflyer.internal.platform_extension.Plugin
import com.appsflyer.internal.platform_extension.PluginInfo
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val STATE = "state"
private const val DATA = "data"
private const val XDM_KEY = "xdm"
private const val EVENT_NAME_KEY = "eventName"
private const val ACTION = "action"
private const val TRACK_NO_EVENTS = "none"
private const val TRACK_ALL_EVENTS = "all"
private const val TRACK_STATE_EVENTS = "states"
private const val TRACK_ACTION_EVENTS = "actions"
private const val APP_ID = "com.appsflyer.adobeextension"

class AppsflyerAdobeExtensionImpl(extensionApi: ExtensionApi) : Extension(extensionApi) {

    private var eventSetting = ""
    private var ecid: String? = null

    private var sdkStarted = false
    private var trackAttributionData = false
    private var didReceiveConfigurations = false

    private val appsflyerAdobeExtensionConversionListener: AppsFlyerConversionListener

    private val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val appsFlyerRequestListener by lazy { AppsFlyerAdobeExtensionRequestListener() }

    init {
        ContextProvider.register(MobileCore.getApplication())

        registerAdobeEventListeners()

        AppsflyerAdobeExtension.subscribeForDeepLinkObservers.add {
            subscribeForDeepLink()
        }

        appsflyerAdobeExtensionConversionListener = AppsflyerAdobeExtensionConversionListener(
            api, { trackAttributionData }, { ecid }
        )
    }

    public override fun getName() = APP_ID

    private fun registerAdobeEventListeners() {
        api.registerEventListener(
            EventType.HUB,
            EventSource.SHARED_STATE,
            this::receiveConfigurationRequestWithSharedState
        )

        api.registerEventListener(
            EventType.GENERIC_TRACK,
            EventSource.REQUEST_CONTENT,
            this::sendAdobeAnalyticsEventToAppsflyer
        )

        api.registerEventListener(
            EventType.EDGE,
            EventSource.REQUEST_CONTENT,
            this::sendAdobeEdgeEventToAppsFlyer
        )
    }

    private fun startSDK() {
        if (sdkStarted || AppsflyerAdobeExtension.manual) {
            return
        }
        with(ContextProvider.context) {
            val msg = when (this) {
                is Application -> "start with Activity context"
                is Activity -> "start with Application context"
                else -> "Null application context error - Use MobileCore.setApplication(this) in your app"
            }
            if (this != null) {
                AppsFlyerLib.getInstance().start(this)
                sdkStarted = true
                logAFExtension(msg)
            }
        }
    }

    private fun sendAdobeAnalyticsEventToAppsflyer(e: Event) {
        if (eventSetting == TRACK_NO_EVENTS) {
            return
        }

        if (e.type == EventType.GENERIC_TRACK && e.source == EventSource.REQUEST_CONTENT) {
            val eventData: Map<String, Any> = e.eventData
            val stateEventName = eventData[STATE] as String?
            val actionEventName = eventData[ACTION] as String?
            val nestedData = eventData["contextdata"] as Map<String, Any>?

            if (actionEventName == APPSFLYER_ATTRIBUTION_DATA || actionEventName == APPSFLYER_ENGAGMENT_DATA) {
                logAFExtension("Discarding event binding for AppsFlyer Attribution Data event")
            } else if (ContextProvider.context != null) {
                var eventName = ""
                val trackActionEvent =
                    eventSetting == TRACK_ALL_EVENTS || eventSetting == TRACK_ACTION_EVENTS
                val trackStateEvent =
                    eventSetting == TRACK_ALL_EVENTS || eventSetting == TRACK_STATE_EVENTS
                if (trackActionEvent && actionEventName != null) {
                    eventName = actionEventName
                } else if (trackStateEvent && stateEventName != null) {
                    eventName = stateEventName
                }
                val eventMap = nestedData.setRevenueAndCurrencyKeysNaming()
                AppsFlyerLib.getInstance().logEvent(
                    ContextProvider.context!!, eventName, eventMap, appsFlyerRequestListener
                )
            } else {
                logErrorAFExtension("Didn't send an inApp due to - Null application context error - Use MobileCore.setApplication(this) in your app")
            }
        }
    }

    private fun sendAdobeEdgeEventToAppsFlyer(e: Event) {
        if (eventSetting == TRACK_NO_EVENTS) {
            return
        }

        if (e.type == EventType.EDGE && e.source == EventSource.REQUEST_CONTENT) {
            val eventData: MutableMap<String, Any> = e.eventData.toMutableMap()
            val xdmDataMap: MutableMap<String, Any>? =
                (eventData[XDM_KEY] as? Map<String, Any>)?.toMutableMap()
            val customDataMap: MutableMap<String, Any>? =
                (eventData[DATA] as? Map<String, Any>)?.toMutableMap()

            if (xdmDataMap != null && (xdmDataMap[ADOBE_ACTION_KEY] == APPSFLYER_ATTRIBUTION_DATA
                        || xdmDataMap[ADOBE_ACTION_KEY] == APPSFLYER_ENGAGMENT_DATA)
            ) {
                logAFExtension("Discarding event binding for AppsFlyer Attribution Data event")
            } else if (ContextProvider.context != null) {
                var eventName = e.name
                xdmDataMap?.let { map ->
                    map[EVENT_NAME_KEY]?.let {
                        eventName = it as String
                        xdmDataMap.remove(EVENT_NAME_KEY)
                    }
                    map.replaceRevenueAndCurrencyKeys()
                    eventData.put(XDM_KEY, map)
                }

                customDataMap?.let {
                    it.replaceRevenueAndCurrencyKeys()
                    eventData.put(DATA, it)
                }

                AppsFlyerLib.getInstance().logEvent(
                    ContextProvider.context!!, eventName, eventData, appsFlyerRequestListener
                )
            } else {
                logErrorAFExtension("Didn't send an inApp due to - Null application context error - Use MobileCore.setApplication(this) in your app")
            }
        }
    }

    @VisibleForTesting
    internal fun receiveConfigurationRequestWithSharedState(e: Event) {
        val eventData: Map<String, Any> = e.eventData
        val stateOwner: String = eventData["stateowner"].toString()
        if (stateOwner == "com.adobe.module.configuration") {
            val sharedStateResult: SharedStateResult? = api.getSharedState(
                "com.adobe.module.configuration",
                e,
                false,
                SharedStateResolution.ANY
            )
            val configurationSharedState: Map<String, Any>? = sharedStateResult?.value
            try {
                if (!configurationSharedState.isNullOrEmpty()) {
                    configurationSharedState[DEV_KEY_CONFIG]?.let { devKey ->
                        val appsFlyerDevKey = devKey.toString()
                        val isDebug =
                            (configurationSharedState[IS_DEBUG_CONFIG] as Boolean?) ?: false
                        val shouldTrackAttr =
                            (configurationSharedState[TRACK_ATTR_DATA_CONFIG] as Boolean?) ?: false
                        val waitForECID =
                            (configurationSharedState[WAIT_FOR_ECID] as Boolean?) ?: false
                        val inAppEventSetting =
                            (configurationSharedState[EVENT_SETTING_CONFIG] as String?) ?: ""

                        this.handleConfigurationEvent(
                            appsFlyerDevKey,
                            isDebug,
                            shouldTrackAttr,
                            inAppEventSetting,
                            waitForECID
                        )
                    } ?: logErrorAFExtension("Cannot initialize Appsflyer tracking: null devkey")
                } else {
                    logErrorAFExtension("Cannot initialize Appsflyer tracking: null configuration json")
                }
            } catch (npx: NullPointerException) {
                logErrorAFExtension("Exception while casting devKey to String: $npx")
            }
        }
    }

    private fun handleConfigurationEvent(
        appsFlyerDevKey: String,
        appsFlyerIsDebug: Boolean,
        trackAttrData: Boolean,
        inAppEventSetting: String,
        waitForECID: Boolean
    ) {
        background {
            handleConfigurationEventRunnable(
                appsFlyerDevKey,
                appsFlyerIsDebug,
                trackAttrData,
                inAppEventSetting,
                waitForECID
            )
        }
    }

    @VisibleForTesting
    internal fun handleConfigurationEventRunnable(
        appsFlyerDevKey: String,
        appsFlyerIsDebug: Boolean,
        trackAttrData: Boolean,
        inAppEventSetting: String,
        waitForECID: Boolean
    ) {
        if (ContextProvider.context != null && !didReceiveConfigurations) {
            val pluginInfo = PluginInfo(
                Plugin.ADOBE_MOBILE,
                BuildConfig.VERSION_NAME,
                mapOf("Adobe version" to "3.x")
            )
            AppsFlyerLib.getInstance().apply {
                setPluginInfo(pluginInfo)
                setDebugLog(appsFlyerIsDebug)
                init(
                    appsFlyerDevKey,
                    appsflyerAdobeExtensionConversionListener,
                    ContextProvider.context!!.applicationContext
                )

                if (AppsflyerAdobeExtension.afCallbackDeepLinkListener != null) {
                    subscribeForDeepLink()
                }

                if (waitForECID) {
                    logAFExtension("waiting for Experience Cloud Id")
                    waitForCustomerUserId(true)
                }

                Identity.getExperienceCloudId {
                    ecid = it
                    val id = ecid.orEmpty()
                    if (waitForECID && sdkStarted) {
                        val context = ContextProvider.context!!
                        setCustomerIdAndLogSession(id, context)
                    } else {
                        setCustomerUserId(id)
                    }
                }

                trackAttributionData = trackAttrData
                eventSetting = inAppEventSetting
                didReceiveConfigurations = true

                startSDK()
            }
        } else if (ContextProvider.context == null) {
            logErrorAFExtension("Null application context error - Use MobileCore.setApplication(this) in your app")
        }
    }

    private fun subscribeForDeepLink() {
        AppsFlyerLib.getInstance().subscribeForDeepLink(this::handleDeepLink)
    }

    private fun handleDeepLink(deepLinkResult: DeepLinkResult) {
        AppsflyerAdobeExtension.afCallbackDeepLinkListener?.onDeepLinking(deepLinkResult)
        val deepLinkObj: DeepLink? = deepLinkResult.deepLink
        try {
            logAFExtension("The DeepLink data is: $deepLinkObj")
            MobileCore.trackAction(
                APPSFLYER_ENGAGMENT_DATA,
                deepLinkObj?.clickEvent?.toMap().setKeyPrefixOnAppOpenAttribution()
            )

            Edge.sendEvent(
                ExperienceEvent.Builder().apply {
                    setData(mapOf(ADOBE_ACTION_KEY to APPSFLYER_ENGAGMENT_DATA))
                    setXdmSchema(
                        deepLinkObj?.clickEvent?.toMap().setKeyPrefixOnAppOpenAttribution()
                    )
                }.build(),
                null
            )
        } catch (e: Exception) {
            logErrorAFExtension("DeepLink data came back null")
            return
        }
    }

    private fun background(r: Runnable) {
        executor.execute(r)
    }
}

