package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.*
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

class AppsflyerAdobeExtensionImpl(extensionApi: ExtensionApi) : Extension(extensionApi) {
    private var sdkStarted = false
    private val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private var ecid: String? = null
    private var trackAttributionData = false
    private val appsFlyerRequestListener by lazy {
        AppsFlyerAdobeExtensionRequestListener()
    }
    private val appsflyerAdobeExtensionConversionListener: AppsFlyerConversionListener
    private var eventSetting = ""
    private var didReceiveConfigurations = false

    companion object {
        private const val APP_ID = "com.appsflyer.adobeextension"
        private const val TRACK_ALL_EVENTS = "all"
        private const val TRACK_ACTION_EVENTS = "actions"
        private const val TRACK_STATE_EVENTS = "states"
        private const val TRACK_NO_EVENTS = "none"
        private const val ACTION = "action"
        private const val STATE = "state"
    }

    init {
        api.registerEventListener(
            EventType.HUB,
            EventSource.SHARED_STATE,
            this::receiveConfigurationRequestWithSharedState
        )
        api.registerEventListener(
            EventType.GENERIC_TRACK,
            EventSource.REQUEST_CONTENT,
            this::inAppEventsHandler
        )
        ContextProvider.register(MobileCore.getApplication())
        AppsflyerAdobeExtension.subscribeForDeepLinkObservers.add {
            subscribeForDeepLink()
        }
        appsflyerAdobeExtensionConversionListener =
            AppsflyerAdobeExtensionConversionListener(api, { trackAttributionData }, { ecid })
    }

    public override fun getName(): String {
        return APP_ID
    }

//    private fun manualModeStatus() : Boolean{
//        return !AppsflyerAdobeExtension.manualMode || AppsflyerAdobeExtension.manualModeOverrider
//    }

    private fun startSDK() {
        if (sdkStarted) {
            return
        }
        with(ContextProvider.context) {
            val msg = when (this) {
                is Application -> "start with Activity context"
                is Activity -> "start with Application context"
                else -> "Null application context error - Use MobileCore.setApplication(this) in your app"
            }
            if (this != null) {
                if (!AppsflyerAdobeExtension.manualMode){
                    AppsFlyerLib.getInstance().start(this)
                    sdkStarted = true
                    logAFExtension(msg)
                }
            }

        }
    }

    private fun inAppEventsHandler(e: Event) {
        val trackActionEvent =
            eventSetting == TRACK_ALL_EVENTS || eventSetting == TRACK_ACTION_EVENTS
        val trackStateEvent = eventSetting == TRACK_ALL_EVENTS || eventSetting == TRACK_STATE_EVENTS

        if (eventSetting == TRACK_NO_EVENTS) {
            return
        }
        if (e.type == EventType.GENERIC_TRACK && e.source == EventSource.REQUEST_CONTENT) {
            val eventData: Map<String, Any> = e.eventData
            val nestedData = eventData["contextdata"] as Map<String, Any>?
            val actionEventName = eventData[ACTION] as String?
            val stateEventName = eventData[STATE] as String?

            ContextProvider.context?.let { context ->
                var eventName = ""
                if (trackActionEvent && actionEventName != null) {
                    if (actionEventName == APPSFLYER_ATTRIBUTION_DATA || actionEventName == APPSFLYER_ENGAGMENT_DATA) {
                        logAFExtension("Discarding event binding for AppsFlyer Attribution Data event")
                        return
                    }
                    eventName = actionEventName

                } else if (trackStateEvent && stateEventName != null) {
                    eventName = stateEventName
                }
                val eventMap = nestedData.setRevenueAndCurrencyKeysNaming()
                AppsFlyerLib.getInstance()
                    .logEvent(context, eventName, eventMap, appsFlyerRequestListener)
            }
                ?: logErrorAFExtension("Didn't send an inApp due to - Null application context error - Use MobileCore.setApplication(this) in your app")
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
                mapOf("Adobe version" to "2.x")
            )
            AppsFlyerLib.getInstance().apply {
                setPluginInfo(pluginInfo)
                setDebugLog(appsFlyerIsDebug)
                init(
                    appsFlyerDevKey,
                    appsflyerAdobeExtensionConversionListener,
                    ContextProvider.context!!.applicationContext
                )
                if (AppsflyerAdobeExtension.afCallbackDeepLinkListener != null){
                    subscribeForDeepLink()
                }
                if (waitForECID) {
                    logAFExtension("waiting for Experience Cloud Id")
                    waitForCustomerUserId(true)
                }

                Identity.getExperienceCloudId(AdobeCallback {
                    ecid = it
                    val id = ecid.orEmpty()
                    if (waitForECID && sdkStarted) {
                        val context = ContextProvider.context!!
                        setCustomerIdAndLogSession(id, context)
                    } else {
                        setCustomerUserId(id)
                    }
                })
                startSDK()

                trackAttributionData = trackAttrData
                eventSetting = inAppEventSetting
                didReceiveConfigurations = true
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
        } catch (e: Exception) {
            logErrorAFExtension("DeepLink data came back null")
            return
        }
    }

    private fun background(r: Runnable) {
        executor.execute(r)
    }
}

