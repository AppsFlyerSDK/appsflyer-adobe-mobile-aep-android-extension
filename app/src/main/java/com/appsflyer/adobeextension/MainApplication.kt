package com.appsflyer.adobeextension

import android.app.Application
import android.util.Log
import com.adobe.marketing.mobile.*

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.DEBUG)
        try {
            MobileCore.configureWithAppID("cc3f5fb64390/dc8e2c465f6b/launch-c0c30f37db8f-development")

            val extensions = listOf(Analytics.EXTENSION, Identity.EXTENSION, AppsflyerAdobeExtension.EXTENSION )
            MobileCore.registerExtensions(extensions) {
                Log.d(AppsflyerAdobeConstatns.AFEXTENSION, "AEP Mobile SDK is initialized")
            }

            AppsflyerAdobeExtensionImpl.Companion.registerAppsFlyerExtensionCallbacks(object :
                AppsFlyerExtensionCallbacksListener {
                override fun onCallbackReceived(callback: Map<String, String?>) {
                    Log.d("AppsFlyerCallbacks", callback.toString())
                }

                override fun onCallbackError(errorMessage: String) {}
            })

        } catch (ex: Exception) {
            Log.d("AdobeException: ", ex.toString())
        }
    }
}