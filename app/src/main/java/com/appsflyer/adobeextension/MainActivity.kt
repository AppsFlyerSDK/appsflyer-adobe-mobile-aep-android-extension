package com.appsflyer.adobeextension

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.MobileCore

class MainActivity : AppCompatActivity() {

    private var mainButton: Button? = null
    private var unregisterButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        mainButton = findViewById(R.id.mainButton)
        unregisterButton = findViewById(R.id.unregisterButton)

        val evtMap: MutableMap<String, String> = HashMap()
        evtMap["currency"] = "ILS"
        evtMap["revenue"] = "200"
        evtMap["freehand"] = "param"


        mainButton!!.setOnClickListener(View.OnClickListener {
            MobileCore.trackAction(
                "testTrackAction",
                evtMap
            )
        })

        unregisterButton!!.setOnClickListener(View.OnClickListener {
            AppsflyerAdobeExtension.unregisterConversionListener()
        })
    }
}