package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.AFEXTENSION
import java.lang.ref.WeakReference
import kotlin.reflect.KFunction2

class ContextProvider(internal val af_application: Application?) : Application.ActivityLifecycleCallbacks {
    internal var af_activity: WeakReference<Activity>? = null
    init {
        af_application?.registerActivityLifecycleCallbacks(this) ?: Log.e(AFEXTENSION, "Null application context error - Use MobileCore.setApplication(this) in your app")
    }

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        af_activity = WeakReference(activity)
    }

    override fun onActivityStarted(p0: Activity) {
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }
}