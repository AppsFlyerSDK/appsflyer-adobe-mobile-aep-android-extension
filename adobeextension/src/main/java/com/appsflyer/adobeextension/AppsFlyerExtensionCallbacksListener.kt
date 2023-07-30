package com.appsflyer.adobeextension

interface AppsFlyerExtensionCallbacksListener {
    fun onCallbackReceived (callBack : Map<String,String?>)
    fun onCallbackError (errorMessage : String)
}