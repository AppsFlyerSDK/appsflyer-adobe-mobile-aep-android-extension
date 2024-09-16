package com.appsflyer.adobeextension

import android.app.Application
import android.util.Log
import com.adobe.marketing.mobile.Analytics
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.consent.Consent
import com.appsflyer.AppsFlyerConversionListener

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.DEBUG)

        try {
            MobileCore.configureWithAppID("cc3f5fb64390/dc8e2c465f6b/launch-c0c30f37db8f-development")

            registerAbodeExtensions()

            registerAppsflyerConversionListener()

            subscribeForAppsFlyerDeepLink()

        } catch (ex: Exception) {
            Log.d("AdobeException: ", ex.toString())
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
                    Log.d("AppsFlyerCallbacks", p0.toString())
                }

                override fun onConversionDataFail(p0: String?) {
                    Log.d("AppsFlyerCallbacks", p0 ?: " error onConversionDataFail")
                }

                override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
                    Log.d("AppsFlyerCallbacks", p0.toString())
                }

                override fun onAttributionFailure(p0: String?) {
                    Log.d("AppsFlyerCallbacks", p0 ?: " error onAttributionFailure")
                }
            }
        )
    }

    private fun subscribeForAppsFlyerDeepLink() {
        AppsflyerAdobeExtension.subscribeForDeepLink { deeplinkResult ->
            Log.d("AppsFlyerDeepLink", deeplinkResult.toString())
        }
    }
}
