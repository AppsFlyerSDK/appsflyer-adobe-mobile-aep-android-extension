package com.appsflyer.adobeextension

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.appsflyer.adobeextension.AppsflyerAdobeConstatns.AFEXTENSION
import kotlin.reflect.KFunction2

class AcitivityLifeCycleWrapper(af_application: Application?, afActivitySetter: KFunction2<Activity, Bundle?, Unit>) : Application.ActivityLifecycleCallbacks {
    private lateinit var af_activity_setter: KFunction2<Activity, Bundle?, Unit>

    init {
        af_application?.registerActivityLifecycleCallbacks(this) ?: Log.e(AFEXTENSION, "Null application context error - Use MobileCore.setApplication(this) in your app")
        af_activity_setter = afActivitySetter
    }

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        af_activity_setter.invoke(activity,p1)
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