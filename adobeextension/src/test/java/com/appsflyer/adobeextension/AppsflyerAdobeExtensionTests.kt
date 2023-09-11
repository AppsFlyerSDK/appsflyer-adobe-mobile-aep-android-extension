package com.appsflyer.adobeextension

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.deeplink.DeepLinkListener
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AppsflyerAdobeExtensionTests {

    private lateinit var mockedAppsFlyerConversionListener : AppsFlyerConversionListener

    @Before
    fun setup(){
        mockedAppsFlyerConversionListener = mockk()
    }

    @Test
    fun testAppsflyerAdobeExtension_registerAndUnregisterConversionListener_happyFlow() {
        assertNull(AppsflyerAdobeExtension.afCallbackListener)
        AppsflyerAdobeExtension.registerConversionListener(mockedAppsFlyerConversionListener)
        assertEquals(AppsflyerAdobeExtension.afCallbackListener, mockedAppsFlyerConversionListener)
        AppsflyerAdobeExtension.unregisterConversionListener()
        assertNull(AppsflyerAdobeExtension.afCallbackListener)
    }

    @Test
    fun testAppsflyerAdobeExtension_subscribeForDeepLink_happyFlow() {
        val functionMock : () -> Unit = mockk(relaxed = true)
        every { functionMock.invoke() } returns Unit
        AppsflyerAdobeExtension.subscribeForDeepLinkObservers.add { functionMock() }
        val mockedAppsFlyerDeepLinkListener : DeepLinkListener = mockk(relaxed = true)
        AppsflyerAdobeExtension.subscribeForDeepLink(mockedAppsFlyerDeepLinkListener)
        verify (exactly = 1) { functionMock() }
        assertEquals(AppsflyerAdobeExtension.afCallbackDeepLinkListener,mockedAppsFlyerDeepLinkListener)
    }
}