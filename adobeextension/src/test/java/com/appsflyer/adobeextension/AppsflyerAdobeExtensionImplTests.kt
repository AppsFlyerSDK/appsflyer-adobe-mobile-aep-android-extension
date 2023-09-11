package com.appsflyer.adobeextension

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adobe.marketing.mobile.*
import com.appsflyer.AppsFlyerLib
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field

@RunWith(AndroidJUnit4::class)
class AppsflyerAdobeExtensionImplTests {
    // Define your class instance
    private lateinit var mockAppsflyerAdobeExtensionImpl: AppsflyerAdobeExtensionImpl
    private lateinit var mockAppsFlyerLib: AppsFlyerLib

    @Before
    fun setupBeforeEvery() {
        // Class instantiation
        mockAppsflyerAdobeExtensionImpl = spyk(AppsflyerAdobeExtensionImpl(mockApi))
        mockkStatic(AppsFlyerLib::class)
        mockAppsFlyerLib = spyk()
        every { AppsFlyerLib.getInstance() } returns mockAppsFlyerLib
    }

    companion object {
        @io.mockk.impl.annotations.MockK
        private lateinit var mockApi: ExtensionApi

        @io.mockk.impl.annotations.MockK
        private lateinit var mockApplicationContext: Application

        @BeforeClass
        @JvmStatic
        fun setup() {
            // Important to call before setting up mock objects
            MockKAnnotations.init(this)
            mockkObject(AppsflyerAdobeExtension)
            // Mock the static MobileCore class
            mockkStatic(MobileCore::class)

            // When getApplication is called, return our mock application context
            every { MobileCore.getApplication() } returns mockApplicationContext

            every { mockApplicationContext.registerActivityLifecycleCallbacks(any()) } just runs

            every { mockApi.registerEventListener(any(), any(), any()) } just runs

            mockkObject(ContextProvider)
            every { ContextProvider.context } returns mockApplicationContext
            every { mockApplicationContext.applicationContext } returns mockApplicationContext
        }
    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_init_happyFlow() {
        verify {
            mockApi.registerEventListener(EventType.HUB, EventSource.SHARED_STATE, any())
            mockApi.registerEventListener(
                EventType.GENERIC_TRACK,
                EventSource.REQUEST_CONTENT,
                any()
            )
            ContextProvider.register(mockApplicationContext)
            AppsflyerAdobeExtensionConversionListener(mockApi, { false }, { null })
        }
    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_startSDK_happyFlow() {
        val sdkStartedField: Field =
            AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("sdkStarted")
        sdkStartedField.isAccessible = true
        var sdkStartedNewValue = sdkStartedField.getBoolean(mockAppsflyerAdobeExtensionImpl)
        assertEquals(false, sdkStartedNewValue)
        val startSDKMethod = AppsflyerAdobeExtensionImpl::class.java.getDeclaredMethod("startSDK")
        startSDKMethod.isAccessible = true
        every { mockAppsFlyerLib.start(any()) } answers { Unit }
        startSDKMethod.invoke(mockAppsflyerAdobeExtensionImpl)
        verify(exactly = 1) {
            mockAppsFlyerLib.start(any())
        }
        sdkStartedNewValue = sdkStartedField.getBoolean(mockAppsflyerAdobeExtensionImpl)
        assertEquals(true, sdkStartedNewValue)
    }

//    @Test
//    fun testAppsflyerAdobeExtensionImpl_handleConfigurationEvent_happyflow(){
//        every { mockAppsFlyerLib.setPluginInfo(any()) } just Runs
//        every { mockAppsFlyerLib.setDebugLog(any()) } just Runs
//        val mockApplication: Application = mockk()
//        every { mockApplicationContext.applicationContext } returns mockApplicationContext
//        every { mockAppsFlyerLib.init(any(),any(),any()) } returns mockAppsFlyerLib
//        every { mockAppsFlyerLib.start(mockApplicationContext) } just Runs
//        val handleConfigurationEventMethod = AppsflyerAdobeExtensionImpl::class.java.getDeclaredMethod(
//            "handleConfigurationEvent",
//            String::class.java,   // the type of appsFlyerDevKey
//            Boolean::class.java,  // the type of appsFlyerIsDebug
//            Boolean::class.java,  // the type of trackAttrData
//            String::class.java,   // the type of inAppEventSetting
//            Boolean::class.java   // the type of waitForECID
//        )
//        handleConfigurationEventMethod.isAccessible = true
//        val inAppEventSettingParam = "settings"
//        handleConfigurationEventMethod.invoke(mockAppsflyerAdobeExtensionImpl,"DevKey", true, true, inAppEventSettingParam, false)
//
//        val executorField: Field = AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("executor")
//        executorField.isAccessible = true
//        var executorValue : ExecutorService = executorField.get(mockAppsflyerAdobeExtensionImpl) as ExecutorService
//        executorValue.execute {
//            val appsflyerAdobeExtensionConversionListenerField: Field = AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("appsflyerAdobeExtensionConversionListener")
//            appsflyerAdobeExtensionConversionListenerField.isAccessible = true
//            var appsflyerAdobeExtensionConversionListenerValue : AppsFlyerConversionListener = appsflyerAdobeExtensionConversionListenerField.get(mockAppsflyerAdobeExtensionImpl) as AppsFlyerConversionListener
//
//            val trackAttributionDataField: Field = AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("trackAttributionData")
//            trackAttributionDataField.isAccessible = true
//            var trackAttributionDataValue = trackAttributionDataField.getBoolean(mockAppsflyerAdobeExtensionImpl)
//
//            val eventSettingField: Field = AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("eventSetting")
//            eventSettingField.isAccessible = true
//            var eventSettingValue: String = eventSettingField.get(mockAppsflyerAdobeExtensionImpl) as String
//
//            verify(exactly = 1) {
////            mockAppsFlyerLib.setPluginInfo(PluginInfo(Plugin.ADOBE_MOBILE, any(),any()))
//                mockAppsFlyerLib.init("DevKey", appsflyerAdobeExtensionConversionListenerValue ,mockApplicationContext)
//            }
//
//            assertEquals(inAppEventSettingParam, eventSettingValue)
//            assertEquals(true, trackAttributionDataValue)
//        }
//    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_inAppEventsHandler_action_happyflow() {
        every { mockAppsFlyerLib.logEvent(any(), any(), any(), any()) } just Runs
        val inAppEventsHandlerMethod = AppsflyerAdobeExtensionImpl::class.java.getDeclaredMethod(
            "inAppEventsHandler",
            Event::class.java
        )
        inAppEventsHandlerMethod.isAccessible = true
        val eventSettingField: Field =
            AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("eventSetting")
        eventSettingField.isAccessible = true
        eventSettingField.set(mockAppsflyerAdobeExtensionImpl, "actions")
        val GENERIC_TRACK = "com.adobe.eventType.generic.track"
        val REQUEST_CONTENT = "com.adobe.eventSource.requestContent"
        val eventName = "testingEventName"
        val nestedData = mapOf("1" to "2")
        val eventData = mapOf("contextdata" to nestedData, "action" to eventName)
        val event: Event = Event.Builder("testingEvent", GENERIC_TRACK, REQUEST_CONTENT)
            .setEventData(eventData)
            .build()

        inAppEventsHandlerMethod.invoke(mockAppsflyerAdobeExtensionImpl, event)
        verify { mockAppsFlyerLib.logEvent(mockApplicationContext, eventName, nestedData, any()) }
    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_inAppEventsHandler_withState_happyflow() {
        every { mockAppsFlyerLib.logEvent(any(), any(), any(), any()) } just Runs
        val inAppEventsHandlerMethod = AppsflyerAdobeExtensionImpl::class.java.getDeclaredMethod(
            "inAppEventsHandler",
            Event::class.java
        )
        inAppEventsHandlerMethod.isAccessible = true
        val eventSettingField: Field =
            AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("eventSetting")
        eventSettingField.isAccessible = true
        eventSettingField.set(mockAppsflyerAdobeExtensionImpl, "states")
        val GENERIC_TRACK = "com.adobe.eventType.generic.track"
        val REQUEST_CONTENT = "com.adobe.eventSource.requestContent"
        val eventName = "testingEventName"
        val nestedData = mapOf("1" to "2")
        val eventData = mapOf("contextdata" to nestedData, "state" to eventName)
        val event: Event = Event.Builder("testingEvent", GENERIC_TRACK, REQUEST_CONTENT)
            .setEventData(eventData)
            .build()

        inAppEventsHandlerMethod.invoke(mockAppsflyerAdobeExtensionImpl, event)
        verify { mockAppsFlyerLib.logEvent(mockApplicationContext, eventName, nestedData, any()) }
    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_receiveConfigurationRequestWithSharedState_happyflow() {
        val GENERIC_TRACK = "com.adobe.eventType.generic.track"
        val REQUEST_CONTENT = "com.adobe.eventSource.requestContent"
        val eventName = "testingEventName"
        val stateOwner = "com.adobe.module.configuration"
        val eventData = mapOf("stateowner" to stateOwner, "state" to eventName)
        val event: Event = Event.Builder("testingEvent", GENERIC_TRACK, REQUEST_CONTENT)
            .setEventData(eventData)
            .build()
        val sharedStateResult: SharedStateResult = mockk()
        every {
            mockAppsflyerAdobeExtensionImpl.api.getSharedState(
                stateOwner,
                event,
                false,
                SharedStateResolution.ANY
            )
        } returns sharedStateResult
        val devKey = "DevKeyTest"
        val eventSettingsConfig = "settingsTest"
        val map = mapOf(
            AppsflyerAdobeConstants.DEV_KEY_CONFIG to devKey,
            AppsflyerAdobeConstants.IS_DEBUG_CONFIG to true,
            AppsflyerAdobeConstants.TRACK_ATTR_DATA_CONFIG to true,
            AppsflyerAdobeConstants.WAIT_FOR_ECID to true,
            AppsflyerAdobeConstants.EVENT_SETTING_CONFIG to eventSettingsConfig
        )

        every { sharedStateResult.value } returns map
        mockAppsflyerAdobeExtensionImpl.receiveConfigurationRequestWithSharedState(event)
        verify {
            mockAppsflyerAdobeExtensionImpl.handleConfigurationEventRunnable(
                devKey,
                true,
                true,
                eventSettingsConfig,
                true
            )
        }
    }
}