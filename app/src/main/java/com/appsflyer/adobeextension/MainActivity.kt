package com.appsflyer.adobeextension

import android.os.Bundle
import android.widget.Button
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.MobileCore
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.ExperienceEvent

private const val TEST_EVENT = "testTrackAction"

class MainActivity : AppCompatActivity() {

    private lateinit var trackAction: Button
    private lateinit var sendEdgeEvent: Button
    private lateinit var unregisterButton: Button

    private val evtMap = mapOf(
        "currency" to "ILS",
        "revenue" to "200",
        "freehand" to "param"
    )

    private val xdmData = mapOf(
        "eventType" to "SampleXDMEvent",
        "sample" to "data"
    )

    private val experienceEvent = ExperienceEvent.Builder().apply {
        setXdmSchema(xdmData)
    }.build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trackAction = findViewById(R.id.track_action)
        sendEdgeEvent = findViewById(R.id.send_edge_event)
        unregisterButton = findViewById(R.id.unregister_button)

        handleButtonListeners()
    }

    private fun handleButtonListeners() {
        trackAction.setOnClickListener {
            MobileCore.trackAction(TEST_EVENT, evtMap)
        }

        sendEdgeEvent.setOnClickListener {
            Edge.sendEvent(experienceEvent, null)
        }

        unregisterButton.setOnClickListener {
            AppsflyerAdobeExtension.unregisterConversionListener()
        }
    }
}
