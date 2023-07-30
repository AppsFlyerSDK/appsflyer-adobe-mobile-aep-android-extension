package com.appsflyer.adobeextension

import android.util.Log
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.Event

import com.adobe.marketing.mobile.ExtensionApi

import com.adobe.marketing.mobile.AdobeError

import com.adobe.marketing.mobile.AdobeCallbackWithError

import com.adobe.marketing.mobile.ExtensionEventListener

//class AppsFlyerSharedStateListener : ExtensionEventListener {
//    override fun hear(event: Event) {
//
//        val errorCallback: AdobeCallbackWithError<AdobeError> =
//            object : AdobeCallbackWithError<AdobeError> {
//                override fun fail(adobeError: AdobeError?) {
//                    Log.e(
//                        AppsflyerAdobeConstatns.AFEXTENSION,
//                        "error receiving sharedState event: " + (adobeError?.errorName
//                            ?: "")
//                    )
//                }
//
//                override fun call(p0: AdobeError?) {
//                    Log.e(AppsflyerAdobeConstatns.AFEXTENSION, "received sharedState event")
//                }
//            }
//
//        val eventData : Map<String,Any> = event.eventData
//        val stateOwner : String = eventData.get("stateowner").toString()
//        if (stateOwner.equals("com.adobe.module.configuration")){
//            val configurationSharedState : Map<String,Any> = this.
//        }
//    }
//}

class AppsFlyerSharedStateListener{

}