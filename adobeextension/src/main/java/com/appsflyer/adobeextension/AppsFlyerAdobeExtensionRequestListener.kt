package com.appsflyer.adobeextension

import com.appsflyer.attribution.AppsFlyerRequestListener

class AppsFlyerAdobeExtensionRequestListener : AppsFlyerRequestListener {
    override fun onSuccess() {
        AppsflyerAdobeExtensionImpl.logAFExtension("Event sent successfully")
    }
    override fun onError(errorCode: Int, errorDesc: String) {
        AppsflyerAdobeExtensionImpl.logErrorAFExtension(
            "Event failed to be sent:\n" +
                    "Error code: " + errorCode + "\n"
                    + "Error description: " + errorDesc
        )
    }
}