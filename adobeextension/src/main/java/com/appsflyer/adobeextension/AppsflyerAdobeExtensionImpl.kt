package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import android.content.Context
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
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.PLUG_IN_VERSION
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.TRACK_ATTR_DATA_CONFIG
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.WAIT_FOR_ECID
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.appsflyer.internal.platform_extension.Plugin
import com.appsflyer.internal.platform_extension.PluginInfo
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object AppsflyerAdobeExtension {

    val EXTENSION: Class<out Extension> = AppsflyerAdobeExtensionImpl::class.java

}

public class AppsflyerAdobeExtensionImpl (extensionApi: ExtensionApi) : Extension(extensionApi){
    private val executorMutex = Any()
    private var sdkStared = false
    private var executor: ExecutorService? = null
        private get() {
            synchronized(executorMutex) {
                if (field == null) {
                    field = Executors.newSingleThreadExecutor()
                }
                return field
            }
        }
    private var ecid: String? = null
    private val TRACK_ALL_EVENTS = "all"
    private val TRACK_ACTION_EVENTS = "actions"
    private val TRACK_STATE_EVENTS = "states"
    private val TRACK_NO_EVENTS = "none"
    private val ACTION = "action"
    private val STATE = "state"

    public override fun getName(): String {
        return "com.appsflyer.adobeextension"
    }

    companion object{
        var eventSetting = ""
        private var af_application: Application? = null
        private var af_activity: WeakReference<Activity>? = null
        private var didReceiveConfigurations = false
        private var trackAttributionData = false
        private var afCallbackListener: AppsFlyerConversionListener? = null
        private var afCallbackDeepLinkListener: DeepLinkListener? = null
        var conversionData: Map<String, Any>? = null
            private set
        var gcd: Map<String, Any>? = null
        fun registerExtension() {
            val errorCallback: AdobeCallbackWithError<AdobeError> =
                object : AdobeCallbackWithError<AdobeError> {
                    override fun fail(p0: AdobeError?) {
                        Log.e(AFEXTENSION, "error in Appsflyer AEP Extension resgistretion")
                    }

                    override fun call(p0: AdobeError?) {
                        Log.e(AFEXTENSION, "registretion of Appsflyer AEP Extension success")
                    }
                }

            MobileCore.registerExtensions(listOf(AppsflyerAdobeExtensionImpl::class.java), errorCallback)
                Log.e(
                    AFEXTENSION,
                    "Appsflyer AEP Extension is Registered."
                )
            }

        fun registerCallbacks(){
            val callbacks : Application.ActivityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks{
                override fun onActivityCreated(activity: Activity, p1: Bundle?) {
                    af_activity = WeakReference(activity)
                }

                override fun onActivityStarted(p0: Activity) {
                }

                override fun onActivityResumed(p0: Activity) {
                }

                override fun onActivityPaused(p0: Activity) {
                }

                override fun onActivityStopped(p0: Activity) {
                }

                override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
                }

                override fun onActivityDestroyed(p0: Activity) {
                }
            }
            if (af_application != null) {
                af_application!!.registerActivityLifecycleCallbacks(callbacks)
            }
            else{
                Log.e(AFEXTENSION, "Null application context error - Use MobileCore.setApplication(this) in your app")
            }
        }

        fun registerAppsFlyerExtensionCallbacks(callbacksListener: AppsFlyerConversionListener){
            if (callbacksListener != null){
                afCallbackListener = callbacksListener
            } else {
                Log.e(AFEXTENSION, "Cannot register callbacks listener with null object")
            }
        }

        fun registerAppsFlyerExtensionDeepLinkListener(deepLinkListener: DeepLinkListener){
            if (deepLinkListener != null){
                afCallbackDeepLinkListener = deepLinkListener
            } else {
                Log.e(AFEXTENSION, "Cannot register callbacks listener with null object")
            }
        }
    }

    init {
        api.registerEventListener("com.adobe.eventType.hub", "com.adobe.eventSource.sharedState",this::reciveConfigurationRequestWithSharedState)
        api.registerEventListener("com.adobe.eventType.generic.track", "com.adobe.eventSource.requestContent",this::reciveConfigurationRequest)
        if (MobileCore.getApplication() != null){
            af_application = MobileCore.getApplication()!!
        }
        else{
            Log.e(AFEXTENSION, "Null application context error - Use MobileCore.setApplication(this) in your app")
        }

        registerCallbacks()
    }

    private fun reciveConfigurationRequest(e : Event){

        val trackActionEvent = eventSetting.equals(TRACK_ALL_EVENTS) || eventSetting.equals(TRACK_ACTION_EVENTS)
        val trackStateEvent = eventSetting.equals(TRACK_ALL_EVENTS) || eventSetting.equals(TRACK_STATE_EVENTS)


        if (eventSetting.equals(TRACK_NO_EVENTS)) {
            return
        }
        if (e.getType().equals("com.adobe.eventType.generic.track") && e.getSource().equals("com.adobe.eventSource.requestContent")) {
            val eventData : Map<String,Any> = e.eventData
            val nestedData = eventData["contextdata"]
            val actionEventName = eventData[ACTION]
            val stateEventName = eventData[STATE]
            val is_action_event = actionEventName != null
            val is_state_event = stateEventName != null

            if (af_application!=null){
                if (trackActionEvent && is_action_event) {
                    // Discard if event is "AppsFlyer Attribution Data" event.
                    if (actionEventName != null) {
                        if (actionEventName.equals(APPSFLYER_ATTRIBUTION_DATA) || actionEventName.equals(APPSFLYER_ENGAGMENT_DATA)) {
                            Log.d(
                                AFEXTENSION,
                                "Discarding event binding for AppsFlyer Attribution Data event"
                            )
                            return
                        }
                    }
                    AppsFlyerLib.getInstance().logEvent(
                        af_application!!,
                        actionEventName.toString(),
                        getAppsFlyerEventMap(
                            nestedData
                        ),
                        object : AppsFlyerRequestListener {
                            override fun onSuccess() {
                                Log.d(AFEXTENSION, "Event sent successfully")
                            }
                            override fun onError(errorCode: Int, errorDesc: String) {
                                Log.d(AFEXTENSION, "Event failed to be sent:\n" +
                                        "Error code: " + errorCode + "\n"
                                        + "Error description: " + errorDesc)
                            }
                        }
                    )

                } else if (trackStateEvent && is_state_event) {
                    AppsFlyerLib.getInstance().logEvent(
                        af_application!!,
                        stateEventName.toString(),
                        getAppsFlyerEventMap(
                            nestedData
                        ),
                        object : AppsFlyerRequestListener {
                            override fun onSuccess() {
                                Log.d(AFEXTENSION, "Event sent successfully")
                            }
                            override fun onError(errorCode: Int, errorDesc: String) {
                                Log.d(AFEXTENSION, "Event failed to be sent:\n" +
                                        "Error code: " + errorCode + "\n"
                                        + "Error description: " + errorDesc)
                            }
                        }
                    )
                }
            } else{
                Log.e(AFEXTENSION, "Didn't send an inApp due to - Null application context error - Use MobileCore.setApplication(this) in your app")
            }

        }
    }

    private fun reciveConfigurationRequestWithSharedState(e : Event){
        val eventData : Map<String,Any> = e.eventData
        val stateOwner : String = eventData.get("stateowner").toString()
        if (stateOwner.equals("com.adobe.module.configuration")){
            // This is a line needs to be chcked. See the change from the Deprectation.
            val sharedStateResult : SharedStateResult? = super.getApi().getSharedState("com.adobe.module.configuration", e, false,SharedStateResolution.ANY)
            val configurationSharedState : Map<String,Object> = sharedStateResult?.value as Map<String, Object>
            try{
                if(configurationSharedState != null){
                    if (!configurationSharedState.isEmpty() && configurationSharedState.get(DEV_KEY_CONFIG) != null) {
                        val appsFlyerDevKey = configurationSharedState[DEV_KEY_CONFIG].toString()
                        var inAppEventSetting: String = ""
                        var isDebug = false
                        var shouldTrackAttr = false
                        var waitForECID = false

                        if (configurationSharedState[IS_DEBUG_CONFIG] != null) {
                            isDebug = configurationSharedState[IS_DEBUG_CONFIG] as Boolean
                        }

                        if (configurationSharedState[TRACK_ATTR_DATA_CONFIG] != null) {
                            shouldTrackAttr =
                                configurationSharedState[TRACK_ATTR_DATA_CONFIG] as Boolean
                        }

                        if (configurationSharedState[WAIT_FOR_ECID] != null) {
                            waitForECID = configurationSharedState[WAIT_FOR_ECID] as Boolean
                        }

                        if (configurationSharedState[EVENT_SETTING_CONFIG] != null) {
                            inAppEventSetting =
                                configurationSharedState[EVENT_SETTING_CONFIG].toString()
                        }

                        this.handleConfigurationEvent(
                            appsFlyerDevKey,
                            isDebug,
                            shouldTrackAttr,
                            inAppEventSetting,
                            waitForECID
                        )
                    } else {
                        Log.e(
                            AFEXTENSION,
                            "Cannot initialize AppsFlyer tracking without a valid DevKey"
                        )
                    }
                } else {
                    Log.e(
                        AFEXTENSION,
                        "Cannot initialize Appsflyer tracking: null configuration json"
                    )
                }
            } catch (npx : NullPointerException) {
                Log.e(AFEXTENSION, "Exception while casting devKey to String: $npx")
            }
        }
    }

    private fun getAppsFlyerEventMap(nestedData: Any?): Map<String?, Any?>? {
        var map: MutableMap<String?, Any?> = HashMap()
        if (nestedData != null) {
            try {
                map = (nestedData as MutableMap<String?, Any?>).toMutableMap()
                val revenue = map["revenue"] as String?
                if (revenue != null) {
                    map.put(AFInAppEventParameterName.REVENUE , revenue)
                    val currency = map["currency"] as String?
                    if (currency != null) {
                        map.put(AFInAppEventParameterName.CURRENCY, currency)
                    }
                }
            } catch (ex: Exception) {
                Log.e(AFEXTENSION, "Error casting contextdata: $ex")
            }
        }
        return map
    }

    private fun handleDeepLink(deepLinkResult: DeepLinkResult) {
        afCallbackDeepLinkListener?.onDeepLinking(deepLinkResult)
        when (deepLinkResult.status) {
            DeepLinkResult.Status.FOUND -> {
                Log.d(AFEXTENSION, "Deep link found")
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                Log.d(AFEXTENSION, "Deep link not found")
                return
            }
            else -> {
                // dlStatus == DeepLinkResult.Status.ERROR
                val dlError = deepLinkResult.error
                Log.d(AFEXTENSION, "There was an error getting Deep Link data: $dlError")
                return
            }
        }
        var deepLinkObj: DeepLink = deepLinkResult.deepLink
        try {
            Log.d(AFEXTENSION, "The DeepLink data is: $deepLinkObj")
        } catch (e: Exception) {
            Log.d(AFEXTENSION, "DeepLink data came back null")
            return
        }

        // An example for using is_deferred
        if (deepLinkObj.isDeferred == true) {
            Log.d(AFEXTENSION, "This is a deferred deep link")
        } else {
            Log.d(AFEXTENSION, "This is a direct deep link")
        }

        try {
            val fruitName = deepLinkObj.deepLinkValue
            Log.d(AFEXTENSION, "The DeepLink will route to: $fruitName")
        } catch (e: Exception) {
            Log.d(AFEXTENSION, "There's been an error: $e")
            return
        }
    }

    fun handleConfigurationEvent(
        appsFlyerDevKey: String,
        appsFlyerIsDebug: Boolean,
        trackAttrData: Boolean,
        inAppEventSetting: String,
        waitForECID: Boolean
    ) {
        synchronized(executorMutex) {
            if (executor == null) {
                executor = Executors.newSingleThreadExecutor()
            }
            executor!!.execute(Runnable {
                if (af_application != null && !didReceiveConfigurations) {
                    val additionalParams: MutableMap<String, String> = HashMap()
                    additionalParams["build_number"] = PLUG_IN_VERSION
                    val pluginInfo = PluginInfo(
                        Plugin.ADOBE_MOBILE,
                        PLUG_IN_VERSION, additionalParams
                    )
                    AppsFlyerLib.getInstance().setPluginInfo(pluginInfo)
                    // Set Adobe ID as the AppsFlyer customerUserId as early as possible.
                    AppsFlyerLib.getInstance().setDebugLog(appsFlyerIsDebug)
                    AppsFlyerLib.getInstance().subscribeForDeepLink(this::handleDeepLink)
                    AppsFlyerLib.getInstance().init(
                        appsFlyerDevKey,
                        getConversionListener(),
                        af_application!!.applicationContext
                    )
                    // wait for Adobe ID to be sent with the install event
                    // wait for Adobe ID to be sent with the install event
                    if (waitForECID) {
                        afLogger("waiting for Experience Cloud Id")
                        AppsFlyerLib.getInstance().waitForCustomerUserId(true)
                    }

                    Identity.getExperienceCloudId( AdobeCallback {
                        if (it != null) {
                            ecid = it
                        }
                        val id = ecid ?: ""
                        if (waitForECID && sdkStared) {
                            val context: Context = if (af_activity != null && af_activity!!.get() != null ) {
                                af_activity!!.get()!!
                            } else {
                                af_application!!.getApplicationContext()
                            }
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
                    Log.e(AFEXTENSION, "Null application context error - Use MobileCore.setApplication(this) in your app")
                }
            })
        }
    }

    private fun afLogger(msg: String) {
        Log.d("AppsFlyer_adobe_ext", msg)
    }

    private fun startSDK() {
        if (sdkStared) {
            return
        }
        if (af_activity != null && af_activity!!.get() != null) {
            afLogger("start with Activity context")
            AppsFlyerLib.getInstance().start(af_activity!!.get()!!)
            sdkStared = true
        } else if (af_application != null) {
            afLogger("start with Application context")
            AppsFlyerLib.getInstance()
                .start(af_application!!.getApplicationContext())
            sdkStared = true
        } else {
            afLogger("Null application context error - Use MobileCore.setApplication(this) in your app")
        }
    }


    private fun getConversionListener(): AppsFlyerConversionListener {
        return object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>) {
                afLogger("called onConversionDataSuccess")
                conversionData[CALLBACK_TYPE] = "onConversionDataReceived"
                if (trackAttributionData) {
                    val isFirstLaunch = conversionData[IS_FIRST_LAUNCH] as Boolean
                    if (isFirstLaunch) {
                    // deprected - need to check for replacement.
                    // api.setSharedEventState(getSaredEventState(conversionData), null, null)

                        if (af_application != null &&
                            AppsFlyerLib.getInstance()
                                .getAppsFlyerUID(af_application!!.getApplicationContext()) != null) {
                            conversionData[APPSFLYER_ID] = AppsFlyerLib.getInstance()
                                .getAppsFlyerUID(af_application!!.getApplicationContext()) !!
                        }
                        // Send AppsFlyer Attribution data to Adobe Analytics;
                        if (ecid != null) {
                            conversionData["ecid"] = ecid!!
                        }
                        MobileCore.trackAction(
                            APPSFLYER_ATTRIBUTION_DATA,
                            setKeyPrefix(conversionData)
                        )
                    } else {
                        Log.d(AFEXTENSION, "Skipping attribution data reporting, not first launch")
                    }
                }
                afCallbackListener?.onConversionDataSuccess(convertConversionData(
                    conversionData.toMap()
                ))
//                afCallbackListener?.onCallbackReceived(
//                    convertConversionData(
//                        conversionData.toMap()
//                    )
//                )
                gcd = conversionData
            }

            override fun onConversionDataFail(errorMessage: String) {
                afLogger("called onConversionDataFail")
                afCallbackListener?.onConversionDataFail(errorMessage)
//                afCallbackListener?.onCallbackError(
//                    errorMessage
//                )
            }

            override fun onAppOpenAttribution(deepLinkData: MutableMap<String, String>) {
                afLogger("called onAppOpenAttribution")
                deepLinkData[CALLBACK_TYPE] = "onAppOpenAttribution"
                if (ecid != null) {
                    deepLinkData["ecid"] = ecid!!
                }
                MobileCore.trackAction(
                    APPSFLYER_ENGAGMENT_DATA,
                    setKeyPrefixOnAppOpenAttribution(deepLinkData)
                )
//                afCallbackListener?.onCallbackReceived(
//                    deepLinkData
//                )
                afCallbackListener?.onAppOpenAttribution(deepLinkData)
            }

            override fun onAttributionFailure(errorMessage: String) {
                afLogger("called onAttributionFailure")
//                afCallbackListener?.onCallbackError(
//                    errorMessage
//                )
                afCallbackListener?.onAttributionFailure(errorMessage)
            }
        }
    }

    private fun setKeyPrefix(attributionParams: Map<String, Any?>): Map<String, String?>? {
        val newConversionMap: MutableMap<String, String?> = HashMap()
        attributionParams.forEach{
            val value = it.value
            val key = it.key
            if (key != CALLBACK_TYPE) {
                val newKey = "appsflyer.$key"
                if (value != null) {
                    newConversionMap[newKey] = value.toString()
                } else {
                    newConversionMap[newKey] = null
                }
            }
        }
        return newConversionMap
    }

    private fun setKeyPrefixOnAppOpenAttribution(attributionParams: Map<String, String>): Map<String, String>? {
        val newConversionMap: MutableMap<String, String> = HashMap()
        attributionParams.forEach{
            val value = it.value
            val key = it.key
            if (key != CALLBACK_TYPE) {
                val newKey = "appsflyer.af_engagement_$key"
                newConversionMap[newKey] = value
            }
        }
        return newConversionMap
    }

//    private fun getSaredEventState(conversionData: Map<String, Any?>): Map<String, Any?>? {
//        // copy conversion data
//        val sharedEventState: MutableMap<String, Any?> = HashMap(conversionData)
//        sharedEventState[APPSFLYER_ID] = AppsFlyerLib.getInstance()
//            .getAppsFlyerUID(af_application)
//        sharedEventState[SDK_VERSION] = AppsFlyerLib.getInstance().sdkVersion
//        if (!conversionData.containsKey(MEDIA_SOURCE)) {
//            sharedEventState[MEDIA_SOURCE] = "organic"
//        }
//        sharedEventState.remove(IS_FIRST_LAUNCH)
//        sharedEventState.remove(CALLBACK_TYPE)
//        return sharedEventState
//    }

    private fun convertConversionData(map: Map<String, Any>): Map<String, String?> {
        val newMap: MutableMap<String, String?> = HashMap()
        map.forEach{
            val value = it.value
            val key = it.key
            if (value != null) {
                newMap[key] = value.toString()
            } else {
                newMap[key] = null
            }
        }
        return newMap
    }
}

