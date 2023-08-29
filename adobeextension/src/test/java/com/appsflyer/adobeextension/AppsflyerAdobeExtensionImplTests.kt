package com.appsflyer.adobeextension

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.MobileCore
import com.appsflyer.AppsFlyerLib
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field


@RunWith(AndroidJUnit4::class)
class AppsflyerAdobeExtensionImplTests {
    @io.mockk.impl.annotations.MockK
    private lateinit var mockApi: ExtensionApi

    @io.mockk.impl.annotations.MockK
    private lateinit var mockApplicationContext: Application

    // Define your class instance
    private lateinit var mockAppsflyerAdobeExtensionImpl: AppsflyerAdobeExtensionImpl

    @io.mockk.impl.annotations.MockK
    private lateinit var mockAppsFlyerLib: AppsFlyerLib

//    private lateinit var mockContextProvider: ContextProvider

    @Before
    fun setup() {
        // Important to call before setting up mock objects
        MockKAnnotations.init(this)
        mockkObject(AppsflyerAdobeExtension)
        // Mock the static MobileCore class
        mockkStatic(MobileCore::class)

        // When getApplication is called, return our mock application context
        every { MobileCore.getApplication() } returns mockApplicationContext

        mockkStatic(AppsFlyerLib::class)
        every { AppsFlyerLib.getInstance() } returns mockAppsFlyerLib

        every { mockApplicationContext.registerActivityLifecycleCallbacks(any()) } just runs
//        every { AppsflyerAdobeExtension.subscribeForDeepLinkObservers } returns mutableListOf()
        every { mockApi.registerEventListener(any(), any(), any()) } just runs

        mockkObject(ContextProvider)
        every { ContextProvider.context } returns mockApplicationContext

        // Class instantiation
        mockAppsflyerAdobeExtensionImpl = AppsflyerAdobeExtensionImpl(mockApi)
    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_init_happyFlow() {
        verify {
            mockApi.registerEventListener(EventType.HUB, EventSource.SHARED_STATE, any())
            mockApi.registerEventListener(EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT, any())
            ContextProvider.register(mockApplicationContext)
            AppsflyerAdobeExtensionConversionListener(mockApi, { false }, { null })
        }
        assertEquals(1,AppsflyerAdobeExtension.subscribeForDeepLinkObservers.count())
    }

    @Test
    fun testAppsflyerAdobeExtensionImpl_startSDK_happyFlow() {
        val sdkStartedField: Field = AppsflyerAdobeExtensionImpl::class.java.getDeclaredField("sdkStarted")
        sdkStartedField.isAccessible = true
        var sdkStartedNewValue = sdkStartedField.getBoolean(mockAppsflyerAdobeExtensionImpl)
        assertEquals(false,sdkStartedNewValue)
        val startSDKMethod = AppsflyerAdobeExtensionImpl::class.java.getDeclaredMethod("startSDK")
        startSDKMethod.isAccessible = true
        every { mockAppsFlyerLib.start(any()) } answers { Unit }
        startSDKMethod.invoke(mockAppsflyerAdobeExtensionImpl)
        verify(exactly = 1) {
            mockAppsFlyerLib.start(any())
        }
        sdkStartedNewValue = sdkStartedField.getBoolean(mockAppsflyerAdobeExtensionImpl)
        assertEquals(true,sdkStartedNewValue)
    }
}