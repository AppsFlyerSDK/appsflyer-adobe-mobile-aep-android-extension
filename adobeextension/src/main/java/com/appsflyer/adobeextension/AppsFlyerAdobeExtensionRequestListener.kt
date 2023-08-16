package com.appsflyer.adobeextension

import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logAFExtension
import com.appsflyer.adobeextension.AppsflyerAdobeExtensionLogger.logErrorAFExtension
import com.appsflyer.attribution.AppsFlyerRequestListener

class AppsFlyerAdobeExtensionRequestListener : AppsFlyerRequestListener {
    override fun onSuccess() {
        logAFExtension("Event sent successfully")
    }

    override fun onError(errorCode: Int, errorDesc: String) {
        logErrorAFExtension(
            "Event failed to be sent:\n" +
                    "Error code: " + errorCode + "\n"
                    + "Error description: " + errorDesc
        )
    }
}