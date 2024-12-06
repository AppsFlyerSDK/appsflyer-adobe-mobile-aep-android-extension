package com.appsflyer.adobeextension

import android.os.Bundle
import android.widget.Button
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.MobileCore
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.ExperienceEvent

private const val TEST_STATE_EVENT = "testTrackState"
private const val TEST_ACTION_EVENT = "testTrackAction"

class MainActivity : AppCompatActivity() {

    private lateinit var trackState: Button
    private lateinit var trackAction: Button
    private lateinit var sendEdgeEvent: Button
    private lateinit var unregisterButton: Button

    private val evtMap = mapOf(
        "currency" to "ILS",
        "revenue" to "200",
        "freehand" to "param"
    )

    private val data = mapOf(
        "customKey1" to "customVal1",
        "currency" to "ILS",
        "revenue" to "200",
    )

    private val xdmData = mapOf(
        "eventName" to "Appsflyer Edge Event",
        "eventType" to "SampleXDMEvent",
        "eventKey1" to "eventVal1",
        "currency" to "ILS",
        "revenue" to "200",
    )

    private val experienceEvent = ExperienceEvent.Builder().apply {
        setData(data)
        setXdmSchema(xdmData)
    }.build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trackState = findViewById(R.id.track_state)
        trackAction = findViewById(R.id.track_action)
        sendEdgeEvent = findViewById(R.id.send_edge_event)
        unregisterButton = findViewById(R.id.unregister_button)

        setButtonListeners()
    }

    private fun setButtonListeners() {
        trackState.setOnClickListener {
            MobileCore.trackState(TEST_STATE_EVENT, evtMap)
        }

        trackAction.setOnClickListener {
            MobileCore.trackAction(TEST_ACTION_EVENT, evtMap)
        }

        sendEdgeEvent.setOnClickListener {
            Edge.sendEvent(experienceEvent, null)
        }

        unregisterButton.setOnClickListener {
            AppsflyerAdobeExtension.unregisterConversionListener()
        }
    }
}
