package com.appsflyer.adobeextension

import android.util.Log

object AppsflyerAdobeExtensionLogger {
    internal fun logAFExtension(message: String) {
        Log.d(AppsflyerAdobeConstants.AFEXTENSION, message)
    }

    internal fun logErrorAFExtension(errorMessage: String) {
        Log.e(AppsflyerAdobeConstants.AFEXTENSION, errorMessage)
    }
}