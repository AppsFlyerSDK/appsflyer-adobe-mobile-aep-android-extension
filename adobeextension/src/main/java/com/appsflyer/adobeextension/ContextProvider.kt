package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.appsflyer.adobeextension.AppsflyerAdobeConstants.AFEXTENSION
import java.lang.ref.WeakReference

internal object ContextProvider : Application.ActivityLifecycleCallbacks {
    private var afActivity: WeakReference<Activity>? = null
    private var afApplication: Application? = null
    internal val context: Context?
        get() = afActivity?.get() ?: afApplication

    internal fun register(app: Application?) {
        afApplication = app
        afApplication?.registerActivityLifecycleCallbacks(this) ?: Log.e(
            AFEXTENSION,
            "Null application context error - Use MobileCore.setApplication(this) in your app"
        )
    }

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        afActivity = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        afActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}