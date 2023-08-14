package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.adobe.marketing.mobile.*
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.AFEXTENSION
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.APPSFLYER_ATTRIBUTION_DATA
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.APPSFLYER_ENGAGMENT_DATA
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.APPSFLYER_ID
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.CALLBACK_TYPE
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.DEV_KEY_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.EVENT_SETTING_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.IS_DEBUG_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.IS_FIRST_LAUNCH
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.MEDIA_SOURCE
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.PLUG_IN_VERSION
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.SDK_VERSION
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.TRACK_ATTR_DATA_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.WAIT_FOR_ECID
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionImpl.Companion.afCallbackDeepLinkListener
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionImpl.Companion.afCallbackListener
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionImpl.Companion.subscribeForDeepLink
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.appsflyer.internal.platform_extension.Plugin
import com.appsflyer.internal.platform_extension.PluginInfo
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object AppsflyerAdobeExtension {
    val EXTENSION: Class<out Extension> = AppsflyerAdobeExtensionImpl::class.java

    fun registerConversionListener(callbacksListener: AppsFlyerConversionListener){
        afCallbackListener = callbacksListener
    }

    fun unregisterConversionListener(){
        AppsflyerAdobeExtensionImpl.unregisterConversionListener()
    }

    fun subscribeForDeepLink(deepLinkListener: DeepLinkListener){
        afCallbackDeepLinkListener = deepLinkListener
        subscribeForDeepLink()
    }
}

public class AppsflyerAdobeExtensionImpl (extensionApi: ExtensionApi) : Extension(extensionApi){
    private var sdkStarted = false
    private val executor : ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private var ecid: String? = null
    private val appsFlyerRequestListener by lazy {
        object : AppsFlyerRequestListener {
            override fun onSuccess() {
                logAFExtension("Event sent successfully")
            }
            override fun onError(errorCode: Int, errorDesc: String) {
                logErrorAFExtension("Event failed to be sent:\n" +
                        "Error code: " + errorCode + "\n"
                        + "Error description: " + errorDesc)
            }
        }
    }
    private val appsflyerAdobeExtensionConversionListener : AppsFlyerConversionListener by lazy {
        object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>) {
                logAFExtension("called onConversionDataSuccess")
                conversionData[CALLBACK_TYPE] = "onConversionDataReceived"
                if (trackAttributionData) {
                    val isFirstLaunch = conversionData[IS_FIRST_LAUNCH] as Boolean
                    if (isFirstLaunch) {
                        api.createSharedState(getSharedEventState(conversionData),null)
                        af_application?.let { context ->
                            AppsFlyerLib.getInstance().getAppsFlyerUID(context)?.let { appsflyerUID ->
                                conversionData[APPSFLYER_ID] = appsflyerUID
                            }
                        }
                        // Send AppsFlyer Attribution data to Adobe Analytics;
                        ecid?.let {
                            conversionData["ecid"] = it
                        }
                        MobileCore.trackAction( APPSFLYER_ATTRIBUTION_DATA, setKeyPrefix(conversionData))
                    } else {
                        logAFExtension("Skipping attribution data reporting, not first launch")
                    }
                }
                afCallbackListener?.onConversionDataSuccess(
                    convertConversionData(
                        conversionData.toMap()
                    )
                )
                gcd = conversionData
            }

            override fun onConversionDataFail(errorMessage: String) {
                logAFExtension("called onConversionDataFail")
                afCallbackListener?.onConversionDataFail(errorMessage)
            }

            override fun onAppOpenAttribution(deepLinkData: MutableMap<String, String>) {
                logAFExtension("called onAppOpenAttribution")
                deepLinkData[CALLBACK_TYPE] = "onAppOpenAttribution"
                ecid?.let{
                    deepLinkData["ecid"] = it
                }
                MobileCore.trackAction(
                    APPSFLYER_ENGAGMENT_DATA,
                    setKeyPrefixOnAppOpenAttribution(deepLinkData)
                )
                afCallbackListener?.onAppOpenAttribution(deepLinkData)
            }

            override fun onAttributionFailure(errorMessage: String) {
                logAFExtension("called onAttributionFailure")
                afCallbackListener?.onAttributionFailure(errorMessage)
            }
        }
    }

    companion object{
        private const val APP_ID = "com.appsflyer.adobeextension"
        private const val TRACK_ALL_EVENTS = "all"
        private const val TRACK_ACTION_EVENTS = "actions"
        private const val TRACK_STATE_EVENTS = "states"
        private const val TRACK_NO_EVENTS = "none"
        private const val ACTION = "action"
        private const val STATE = "state"

        internal var afCallbackListener: AppsFlyerConversionListener? = null
        internal var afCallbackDeepLinkListener: DeepLinkListener? = null
        var eventSetting = ""
        private var af_application: Application? = null
        private var af_activity: WeakReference<Activity>? = null
        private var didReceiveConfigurations = false
        private var trackAttributionData = false
        var gcd: Map<String, Any>? = null

        private fun handleDeepLink(deepLinkResult: DeepLinkResult) {
            afCallbackDeepLinkListener?.onDeepLinking(deepLinkResult)
            when (deepLinkResult.status) {
                DeepLinkResult.Status.FOUND -> {
                    logAFExtension("Deep link found")
                }
                DeepLinkResult.Status.NOT_FOUND -> {
                    logErrorAFExtension("Deep link not found")
                    return
                }
                else -> {
                    val dlError = deepLinkResult.error
                    logErrorAFExtension("There was an error getting Deep Link data: $dlError")
                    return
                }
            }
            var deepLinkObj: DeepLink = deepLinkResult.deepLink
            try {
                logAFExtension("The DeepLink data is: $deepLinkObj")
                MobileCore.trackAction(APPSFLYER_ENGAGMENT_DATA, jsonObjectToMap(deepLinkObj.clickEvent))
            } catch (e: Exception) {
                logErrorAFExtension("DeepLink data came back null")
                return
            }

            if (deepLinkObj.isDeferred == true) {
                logAFExtension("This is a deferred deep link")
            } else {
                logAFExtension("This is a direct deep link")
            }
        }

        internal fun subscribeForDeepLink(){
            AppsFlyerLib.getInstance().subscribeForDeepLink(this::handleDeepLink)
        }

        internal fun unregisterConversionListener(){
            AppsFlyerLib.getInstance().unregisterConversionListener()
        }

        private fun logAFExtension(message: String) {
            Log.d(AFEXTENSION, message)
        }

        private fun logErrorAFExtension(errorMessage :String) {
            Log.e(AFEXTENSION, errorMessage)
        }

        private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, String> {
            val map = mutableMapOf<String, String>()

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject[key]
                map[key] = value.toString()
            }

            return map
        }
    }

    init {
        api.registerEventListener(EventType.HUB, EventSource.SHARED_STATE, this::reciveConfigurationRequestWithSharedState)
        api.registerEventListener(EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT, this::reciveConfigurationRequest)
        MobileCore.getApplication()?.let {
            af_application = it
        } ?: logErrorAFExtension("Null application context error - Use MobileCore.setApplication(this) in your app")

        AcitivityLifeCycleWrapper(af_application,this::onActivityCreated)
    }

    private fun onActivityCreated(activity: Activity, p1: Bundle?) {
        af_activity = WeakReference(activity)
    }

    public override fun getName(): String {
        return APP_ID
    }

    private fun reciveConfigurationRequest(e : Event){
        val trackActionEvent = eventSetting == TRACK_ALL_EVENTS || eventSetting == TRACK_ACTION_EVENTS
        val trackStateEvent = eventSetting == TRACK_ALL_EVENTS || eventSetting == TRACK_STATE_EVENTS

        if (eventSetting == TRACK_NO_EVENTS) {
            return
        }
        if (e.getType() == EventType.GENERIC_TRACK && e.getSource() == EventSource.REQUEST_CONTENT) {
            val eventData : Map<String,Any> = e.eventData
            val nestedData = eventData["contextdata"] as Map<String, Any>?
            val actionEventName = eventData[ACTION] as String?
            val stateEventName = eventData[STATE] as String?

            af_application?.let{ app ->
                var eventName = ""
                if (trackActionEvent && actionEventName!= null) {
                    if (actionEventName == APPSFLYER_ATTRIBUTION_DATA || actionEventName == APPSFLYER_ENGAGMENT_DATA) {
                        logAFExtension("Discarding event binding for AppsFlyer Attribution Data event")
                        return
                    }
                    eventName = actionEventName

                } else if (trackStateEvent && stateEventName != null) {
                    eventName = stateEventName
                }
                val eventMap = getAppsFlyerEventMap(nestedData)
                AppsFlyerLib.getInstance().logEvent(app, eventName, eventMap, appsFlyerRequestListener)
            } ?: logErrorAFExtension("Didn't send an inApp due to - Null application context error - Use MobileCore.setApplication(this) in your app")
        }
    }

    private fun reciveConfigurationRequestWithSharedState(e : Event){
        val eventData : Map<String,Any> = e.eventData
        val stateOwner : String = eventData.get("stateowner").toString()
        if (stateOwner.equals("com.adobe.module.configuration")){
            val sharedStateResult : SharedStateResult? = api.getSharedState("com.adobe.module.configuration", e, false,SharedStateResolution.ANY)
            val configurationSharedState : Map<String,Any>? = sharedStateResult?.value as Map<String, Any>
            try{
                if(!configurationSharedState.isNullOrEmpty()){
                    configurationSharedState[DEV_KEY_CONFIG]?.let { devKey ->
                        val appsFlyerDevKey = devKey.toString()
                        val isDebug = (configurationSharedState[IS_DEBUG_CONFIG] as Boolean?) ?: false
                        val shouldTrackAttr = (configurationSharedState[TRACK_ATTR_DATA_CONFIG] as Boolean?) ?: false
                        val waitForECID = (configurationSharedState[WAIT_FOR_ECID] as Boolean?) ?: false
                        val inAppEventSetting = (configurationSharedState[EVENT_SETTING_CONFIG] as String?) ?: ""

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
            } catch (npx : NullPointerException) {
                logErrorAFExtension("Exception while casting devKey to String: $npx")
            }
        }
    }

    private fun getAppsFlyerEventMap(nestedData: Map<String,Any>?): Map<String, Any> {
        var map = nestedData?.toMutableMap() ?:  return mutableMapOf()
        try {
            (map["revenue"] as String?)?.let {
                map.put(AFInAppEventParameterName.REVENUE, it)
            }
            (map["currency"] as String?)?.let{
                map.put(AFInAppEventParameterName.CURRENCY, it)
            }
        } catch (ex: Exception) {
            logErrorAFExtension("Error casting contextdata: $ex")
        }
        return map
    }

    fun handleConfigurationEvent(
        appsFlyerDevKey: String,
        appsFlyerIsDebug: Boolean,
        trackAttrData: Boolean,
        inAppEventSetting: String,
        waitForECID: Boolean
    ) {
        executor.execute(Runnable {
            if (af_application != null && !didReceiveConfigurations) {
                val additionalParams: MutableMap<String, String> = HashMap()
                val pluginInfo = PluginInfo(
                    Plugin.ADOBE_MOBILE,
                    PLUG_IN_VERSION, additionalParams
                )
                AppsFlyerLib.getInstance().setPluginInfo(pluginInfo)
                AppsFlyerLib.getInstance().setDebugLog(appsFlyerIsDebug)
                AppsFlyerLib.getInstance().init(
                    appsFlyerDevKey,
                    appsflyerAdobeExtensionConversionListener,
                    af_application!!.applicationContext
                )
                if (waitForECID) {
                    logAFExtension("waiting for Experience Cloud Id")
                    AppsFlyerLib.getInstance().waitForCustomerUserId(true)
                }

                Identity.getExperienceCloudId( AdobeCallback {
                    ecid = it
                    val id = ecid.orEmpty()
                    if (waitForECID && sdkStarted) {
                        val context = af_activity?.get() ?: af_application!!
                        AppsFlyerLib.getInstance().setCustomerIdAndLogSession(id, context)
                    } else {
                        AppsFlyerLib.getInstance().setCustomerUserId(id)
                    }
                })
                startSDK()

                trackAttributionData = trackAttrData
                eventSetting = inAppEventSetting
                didReceiveConfigurations = true

            } else if (af_application == null) {
                logErrorAFExtension("Null application context error - Use MobileCore.setApplication(this) in your app")
            }
        })
    }

    private fun startSDK() {
        if (sdkStarted) {
            return
        }
        val (message,context) = if (af_activity?.get() != null) {
                                    Pair ("start with Activity context", af_activity!!.get()!!)
                                } else if (af_application != null) {
                                    Pair("start with Application context", af_application!!)
                                } else {
                                    Pair("Null application context error - Use MobileCore.setApplication(this) in your app",null)
                                }
        if (context != null){
            AppsFlyerLib.getInstance().start(context)
            sdkStarted = true
        }
        logAFExtension(message)
    }

    private fun setKeyPrefix(attributionParams: Map<String, Any?>): Map<String, String?>? {
        return attributionParams.toMutableMap().run {
            remove(CALLBACK_TYPE)
            entries.associate { "appsflyer.${it.key}" to it.value.toString() }
        }
    }

    private fun setKeyPrefixOnAppOpenAttribution(attributionParams: Map<String, String>): Map<String, String>? {
        return attributionParams.toMutableMap().run {
            remove(CALLBACK_TYPE)
            entries.associate { "appsflyer.af_engagement_${it.key}" to it.value }
        }
    }

    private fun getSharedEventState(conversionData: Map<String, Any>): MutableMap<String, Any> {
        // copy conversion data
        var sharedEventState = conversionData.toMutableMap()
        af_application?.let{
            AppsFlyerLib.getInstance().getAppsFlyerUID(it)?.let{ res ->
                sharedEventState[APPSFLYER_ID] = res
            }
        }
        sharedEventState[SDK_VERSION] = AppsFlyerLib.getInstance().sdkVersion
        if (!conversionData.containsKey(MEDIA_SOURCE)) {
            sharedEventState[MEDIA_SOURCE] = "organic"
        }
        sharedEventState.remove(IS_FIRST_LAUNCH)
        sharedEventState.remove(CALLBACK_TYPE)
        return sharedEventState
    }

    private fun convertConversionData(map: Map<String, Any>): Map<String, String?> {
        return map.entries.associate { it.key to it.value.toString() }
    }
}

