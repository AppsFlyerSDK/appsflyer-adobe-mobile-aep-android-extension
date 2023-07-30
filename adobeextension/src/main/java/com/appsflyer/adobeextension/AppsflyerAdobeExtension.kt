package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.internal.eventhub.*
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
import com.appsflyer.internal.platform_extension.Plugin
import com.appsflyer.internal.platform_extension.PluginInfo
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


public class AppsflyerAdobeExtension (extensionApi: ExtensionApi) : Extension(extensionApi){
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

    public override fun getName(): String {
        return "com.appsflyer.adobeextension"
    }

    companion object{
        var eventSetting = ""
        lateinit var af_application: Application
        lateinit var af_activity: WeakReference<Activity>
        private var didReceiveConfigurations = false
        private var trackAttributionData = false
        private var afCallbackListener: AppsFlyerExtensionCallbacksListener? = null
        var conversionData: Map<String, Any>? = null
            private set
        var gcd: Map<String, Any>? = null
        public fun registerExtension() {
            val errorCallback: AdobeCallbackWithError<AdobeError> =
                object : AdobeCallbackWithError<AdobeError> {
                    override fun fail(p0: AdobeError?) {
                        Log.e(AFEXTENSION, "error in resgistretion")
                    }

                    override fun call(p0: AdobeError?) {
                        Log.e(AFEXTENSION, "registretion success")
                    }
                }

            MobileCore.registerExtensions(listOf(AppsflyerAdobeExtension::class.java), errorCallback)
                Log.e(
                    AFEXTENSION,
                    "Registered."
                )
            }

        public fun registerCallbacks(){
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
            af_application!!.registerActivityLifecycleCallbacks(callbacks)
        }

        public fun registerAppsFlyerExtensionCallbacks(callbacksListener: AppsFlyerExtensionCallbacksListener){
            if (callbacksListener != null){
                afCallbackListener = callbacksListener
            } else {
                Log.e(AFEXTENSION, "Cannot register callbacks listener with null object")
            }
        }
    }

    init {
        val errorCallback: AdobeCallbackWithError<AdobeError> =
            object : AdobeCallbackWithError<AdobeError> {
                override fun fail(p0: AdobeError?) {
                    Log.e(AFEXTENSION, "Error registering listener")
                }

                override fun call(p0: AdobeError?) {
                    Log.e(AFEXTENSION, "registretion success")
                }
            }
//        api.registerEventListener("com.adobe.eventType.hub", "com.adobe.eventSource.sharedState", AppsFlyerSharedStateListener::class.java)

        api.registerEventListener("com.adobe.eventType.hub", "com.adobe.eventSource.sharedState",this::reciveConfigurationRequestWithSharedState)

        api.registerEventListener("com.adobe.eventType.hub", "com.adobe.eventSource.sharedState",this::reciveConfigurationRequest)

        af_application =
            MobileCore.getApplication()!!
        registerCallbacks()
    }

    private fun reciveConfigurationRequest(e : Event){

        val trackActionEvent =
            eventSetting.equals("all") || eventSetting.equals("actions")
        val trackStateEvent =
            eventSetting.equals("all") || eventSetting.equals("states")

        if (eventSetting.equals("none")) {
            return
        }
        if (e.getType() == "com.adobe.eventtype.generic.track" && e.getSource() == "com.adobe.eventsource.requestcontent") {

            val eventData : Map<String,Any> = e.eventData
            val nestedData = eventData["contextdata"]
            val actionEventName = eventData["action"]
            val stateEventName = eventData["state"]

            val is_action_event = actionEventName != null
            val is_state_event = stateEventName != null


            if (trackActionEvent && is_action_event) {

                // Discard if event is "AppsFlyer Attribution Data" event.
                if (actionEventName == APPSFLYER_ATTRIBUTION_DATA || actionEventName == APPSFLYER_ENGAGMENT_DATA) {
                    Log.d(
                        AFEXTENSION,
                        "Discarding event binding for AppsFlyer Attribution Data event"
                    )
                    return
                }

                if (af_application != null) {
                    AppsFlyerLib.getInstance().logEvent(
                        af_application,
                        actionEventName.toString(),
                        getAppsFlyerEventMap(
                            nestedData
                        )
                    )
                } else {
                    Log.e(
                        AFEXTENSION,
                        "Application is null, please set Application using AppsFlyerAdobeExtension.setApplication(this);"
                    )
                }

            }
        }
    }

    private fun getAppsFlyerEventMap(nestedData: Any?): Map<String?, Any?>? {
        var map: MutableMap<String?, Any?> = HashMap()
        if (nestedData != null) {
            try {
                map = nestedData as MutableMap<String?, Any?>
                val revenue = map["revenue"] as String?
                if (revenue != null) {
                    map[AFInAppEventParameterName.REVENUE] = revenue
                    val currency = map["currency"] as String?
                    if (currency != null) {
                        map[AFInAppEventParameterName.CURRENCY] = currency
                    }
                }
            } catch (ex: Exception) {
                Log.e(AFEXTENSION, "Error casting contextdata: $ex")
            }
        }
        return map
    }

    private fun reciveConfigurationRequestWithSharedState(e : Event){
        val eventData : Map<String,Any> = e.eventData
        val stateOwner : String = eventData.get("stateowner").toString()
        if (stateOwner.equals("com.adobe.module.configuration")){
            val sharedStateResult : SharedStateResult? = super.getApi().getSharedState("com.adobe.module.configuration", e, false,SharedStateResolution.ANY)
            val configurationSharedState : Map<String,Object> = sharedStateResult?.value as Map<String, Object>
            try{
                if(configurationSharedState!=null){
                    if (!configurationSharedState.isEmpty() && (configurationSharedState.get(DEV_KEY_CONFIG) != null)) {
                        val appsFlyerDevKey = configurationSharedState[DEV_KEY_CONFIG].toString()
                        var inAppEventSetting: String? = null
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

                        if (inAppEventSetting != null) {
                            this.handleConfigurationEvent(
                                appsFlyerDevKey,
                                isDebug,
                                shouldTrackAttr,
                                inAppEventSetting,
                                waitForECID
                            )
                        }
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

    fun handleConfigurationEvent(
        appsFlyerDevKey: String?,
        appsFlyerIsDebug: Boolean,
        trackAttrData: Boolean,
        inAppEventSetting: String, waitForECID: Boolean
    ) {
        synchronized(executorMutex) {
            if (executor == null) {
                executor = Executors.newSingleThreadExecutor()
            }
            executor?.execute(Runnable {
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
                    AppsFlyerLib.getInstance().init(
                        appsFlyerDevKey!!,
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
                            val context: Context = if (af_activity.get() != null) af_activity.get()!! else af_application.getApplicationContext();
                            if (context != null) {
                                AppsFlyerLib.getInstance().setCustomerIdAndLogSession(id, context)
                            }
                        } else {
                            AppsFlyerLib.getInstance().setCustomerUserId(id)
                        }
                    })
                    startSDK()

                    trackAttributionData = trackAttrData
                    eventSetting = inAppEventSetting
                    didReceiveConfigurations = true

                } else if (af_application == null) {
                    Log.e(AFEXTENSION, "Null application context error")
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
        if (af_activity.get() != null) {
            afLogger("start with Activity context")
            AppsFlyerLib.getInstance().start(af_activity.get()!!)
            sdkStared = true
        } else if (af_application != null) {
            afLogger("start with Application context")
            AppsFlyerLib.getInstance()
                .start(af_application.getApplicationContext())
            sdkStared = true
        } else {
            afLogger("no context to start the SDK")
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
//                        api.setSharedEventState(getSaredEventState(conversionData), null, null)
                        // add appsflyer_id to send to MobileCore
                        if (af_application != null) {
                            conversionData[APPSFLYER_ID] = AppsFlyerLib.getInstance()
                                .getAppsFlyerUID(af_application.getApplicationContext())!!
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
                afCallbackListener?.onCallbackReceived(
                    convertConversionData(
                        conversionData
                    )
                )
                gcd = conversionData
            }

            override fun onConversionDataFail(errorMessage: String) {
                afLogger("called onConversionDataFail")
                afCallbackListener?.onCallbackError(
                    errorMessage
                )
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
                afCallbackListener?.onCallbackReceived(
                    deepLinkData
                )
            }

            override fun onAttributionFailure(errorMessage: String) {
                afLogger("called onAttributionFailure")
                afCallbackListener?.onCallbackError(
                    errorMessage
                )
            }
        }
    }

    private fun setKeyPrefix(attributionParams: Map<String, Any?>): Map<String, String?>? {
        val newConversionMap: MutableMap<String, String?> = HashMap()
        attributionParams.forEach{
            val value = it.value
            val key = it.key
            if (key != CALLBACK_TYPE) {
                if (value != null) {
                    val newKey = "appsflyer.$key"
                    newConversionMap[newKey] = value.toString()
                } else {
                    val newKey = "appsflyer.$key"
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

    private fun getSaredEventState(conversionData: Map<String, Any?>): Map<String, Any?>? {
        // copy conversion data
        val sharedEventState: MutableMap<String, Any?> = HashMap(conversionData)
        sharedEventState[APPSFLYER_ID] = AppsFlyerLib.getInstance()
            .getAppsFlyerUID(af_application)
        sharedEventState[SDK_VERSION] = AppsFlyerLib.getInstance().sdkVersion
        if (!conversionData.containsKey(MEDIA_SOURCE)) {
            sharedEventState[MEDIA_SOURCE] = "organic"
        }
        sharedEventState.remove(IS_FIRST_LAUNCH)
        sharedEventState.remove(CALLBACK_TYPE)
        return sharedEventState
    }

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

