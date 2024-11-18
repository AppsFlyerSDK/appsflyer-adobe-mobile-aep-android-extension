package com.appsflyer.adobeextension

import android.app.Application
import android.util.Log
import com.adobe.marketing.mobile.*
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.DEBUG)
        try {
            // test
            MobileCore.configureWithAppID("<ADOBE_APP_ID>")

            val extensions = listOf(Analytics.EXTENSION, Identity.EXTENSION, AppsflyerAdobeExtension.EXTENSION )
            MobileCore.registerExtensions(extensions) {
                Log.d(AppsflyerAdobeConstants.AFEXTENSION, "AEP Mobile SDK is initialized")
            }

            AppsflyerAdobeExtension.registerConversionListener(
                object : AppsFlyerConversionListener {
                    override fun onConversionDataSuccess(p0: MutableMap<String, Any>?) {
                        Log.d("AppsFlyerCallbacks", p0.toString())
                    }

                    override fun onConversionDataFail(p0: String?) {
                        Log.d("AppsFlyerCallbacks", p0?: " error onConversionDataFail")
                    }

                    override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
                        Log.d("AppsFlyerCallbacks", p0.toString())
                    }

                    override fun onAttributionFailure(p0: String?) {
                        Log.d("AppsFlyerCallbacks", p0?:" error onAttributionFailure")
                    }
                }
            )

            AppsflyerAdobeExtension.subscribeForDeepLink(object :
                DeepLinkListener {
                override fun onDeepLinking(p0: DeepLinkResult) {
                    Log.d("AppsFlyerDeepLink", p0.toString())
                }
            })

        } catch (ex: Exception) {
            Log.d("AdobeException: ", ex.toString())
        }
    }
}