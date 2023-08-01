package com.appsflyer.adobeextension

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.MobileCore

class MainActivity : AppCompatActivity() {

    private var button: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        button = findViewById(R.id.mainButton)

        val evtMap: MutableMap<String, String> = HashMap()
        evtMap["currency"] = "ILS"
        evtMap["revenue"] = "200"
        evtMap["freehand"] = "param"


        button!!.setOnClickListener(View.OnClickListener {
            MobileCore.trackAction(
                "testTrackAction",
                evtMap
            )
        })
    }
}