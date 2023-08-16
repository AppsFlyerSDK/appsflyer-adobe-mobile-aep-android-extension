package com.appsflyer.adobeextension

import com.adobe.marketing.mobile.Extension
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.deeplink.DeepLinkListener

typealias DeepLinkObserver = () -> Unit

object AppsflyerAdobeExtension {
    val EXTENSION: Class<out Extension> = AppsflyerAdobeExtensionImpl::class.java
    internal var afCallbackListener: AppsFlyerConversionListener? = null
    internal var afCallbackDeepLinkListener: DeepLinkListener? = null
    internal val subscribeForDeepLinkObservers = mutableListOf<DeepLinkObserver>()

    fun registerConversionListener(callbacksListener: AppsFlyerConversionListener) {
        afCallbackListener = callbacksListener
    }

    fun unregisterConversionListener() {
        afCallbackListener = null
    }

    fun subscribeForDeepLink(deepLinkListener: DeepLinkListener) {
        afCallbackDeepLinkListener = deepLinkListener
        subscribeForDeepLinkObservers.forEach {
            it.invoke()
        }
    }
}