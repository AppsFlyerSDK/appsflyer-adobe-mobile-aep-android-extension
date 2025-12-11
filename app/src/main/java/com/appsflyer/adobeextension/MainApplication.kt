package com.appsflyer.adobeextension

import android.app.Application
import android.util.Log
import com.adobe.marketing.mobile.Analytics
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.consent.Consent
import com.appsflyer.AppsFlyerConversionListener

const val APP_ID = "APP_ID"
const val ADOBE_EXCEPTION_TAG = "AdobeException"
const val APPSFLYER_CALLBACK_TAG = "AppsFlyerCallback"
const val APPSFLYER_DEEPLINK_TAG = "AppsFlyerDeepLink"

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.DEBUG)

        try {
            MobileCore.configureWithAppID(APP_ID)

            registerAbodeExtensions()

            registerAppsflyerConversionListener()

            subscribeForAppsFlyerDeepLink()

        } catch (ex: Exception) {
            Log.d(ADOBE_EXCEPTION_TAG, ex.toString())
        }
    }

    private fun registerAbodeExtensions() {
        val extensions = listOf(
            Analytics.EXTENSION,
            Edge.EXTENSION,
            Consent.EXTENSION,
            com.adobe.marketing.mobile.Identity.EXTENSION,
            com.adobe.marketing.mobile.edge.identity.Identity.EXTENSION,
            AppsflyerAdobeExtension.EXTENSION
        )
        MobileCore.registerExtensions(extensions) {
            Log.d(AppsflyerAdobeConstants.AFEXTENSION, "AEP Mobile SDK is initialized")
        }
    }

    private fun registerAppsflyerConversionListener() {
        AppsflyerAdobeExtension.registerConversionListener(
            object : AppsFlyerConversionListener {

                override fun onConversionDataSuccess(p0: MutableMap<String, Any>?) {
                    Log.d(APPSFLYER_CALLBACK_TAG, p0.toString())
                }

                override fun onConversionDataFail(p0: String?) {
                    Log.d(APPSFLYER_CALLBACK_TAG, p0 ?: " error onConversionDataFail")
                }

                override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
                    Log.d(APPSFLYER_CALLBACK_TAG, p0.toString())
                }

                override fun onAttributionFailure(p0: String?) {
                    Log.d(APPSFLYER_CALLBACK_TAG, p0 ?: " error onAttributionFailure")
                }
            }
        )
    }

    private fun subscribeForAppsFlyerDeepLink() {
        AppsflyerAdobeExtension.subscribeForDeepLink { deeplinkResult ->
            Log.d(APPSFLYER_DEEPLINK_TAG, deeplinkResult.toString())
        }
    }
}
