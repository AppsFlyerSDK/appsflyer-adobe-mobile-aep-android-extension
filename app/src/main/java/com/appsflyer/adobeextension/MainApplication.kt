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
            AppsflyerAdobeExtension.Companion.registerExtension()
            MobileCore.configureWithAppID("cc3f5fb64390/1e339b77b5d2/launch-29205c76e794-development")


            AppsflyerAdobeExtension.Companion.registerAppsFlyerExtensionCallbacks(object :
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