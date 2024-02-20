## <a id="dma_support"> Send consent for DMA compliance 
For a general introduction to DMA consent data, see [here](https://dev.appsflyer.com/hc/docs/send-consent-for-dma-compliance).<be> 
The SDK offers two alternative methods for gathering consent data:<br> 
- **Through a Consent Management Platform (CMP)**: If the app uses a CMP that complies with the [Transparency and Consent Framework (TCF) v2.2 protocol](https://iabeurope.eu/tcf-supporting-resources/), the SDK can automatically retrieve the consent details.<br> 
<br>OR<br><br> 
- **Through a dedicated SDK API**: Developers can pass Google's required consent data directly to the SDK using a specific API designed for this purpose. 
### Use CMP to collect consent data 
A CMP compatible with TCF v2.2 collects DMA consent data and stores it in <code>SharedPreferences</code>. To enable the SDK to access this data and include it with every event, follow these steps:<br> 
<ol> 
  <li> Call <code>AppsFlyerLib.getInstance().enableTCFDataCollection(true)</code> to instruct the SDK to collect the TCF data from the device. 
  <li> Set the the adapter to be manual : <code>AppsflyerAdobeExtension.manual = true</code>. <br> This will allow us to delay the Conversion call in order to provide the SDK with the user consent. 
  <li> Initialize Adobe. 
  <li> In the <code>Activity</code> class, use the CMP to decide if you need the consent dialog in the current session.
  <li> If needed, show the consent dialog, using the CMP, to capture the user consent decision. Otherwise, go to step 6. 
  <li> Get confirmation from the CMP that the user has made their consent decision, and the data is available in <code>SharedPreferences</code>.
  <li> Call <code>AppsFlyerLib.getInstance().start(this)</code>
</ol> 
 
 #### Application class
``` kotlin
override fun onCreate() {
    super.onCreate()
    AppsFlyerLib.getInstance().enableTCFDataCollection(true)
    AppsflyerAdobeExtension.manual = true
    MobileCore.setApplication(this)
    MobileCore.setLogLevel(LoggingMode.DEBUG)
    try {
        MobileCore.configureWithAppID("DEV_KEY")
        val extensions = listOf(Analytics.EXTENSION, Identity.EXTENSION, AppsflyerAdobeExtension.EXTENSION )
        MobileCore.registerExtensions(extensions) {
            Log.d(AppsflyerAdobeConstants.AFEXTENSION, "AEP Mobile SDK is initialized")
        }

        AppsflyerAdobeExtension.registerConversionListener(
            ...
        )
    } catch (ex: Exception) {
        Log.d("AdobeException: ", ex.toString())
    }
}
```

 
### Manually collect consent data 
If your app does not use a CMP compatible with TCF v2.2, use the SDK API detailed below to provide the consent data directly to the SDK. 
<ol> 
  <li> Initialize <code>AppsflyerAdobeExtension</code> using manual mode and also <code>MobileCore</code>. This will allow us to delay the Conversion call in order to provide the SDK with the user consent. 
  <li> In the <code>Activity</code> class, determine whether the GDPR applies or not to the user.<br> 
  - If GDPR applies to the user, perform the following:  
      <ol> 
        <li> Given that GDPR is applicable to the user, determine whether the consent data is already stored for this session. 
            <ol> 
              <li> If there is no consent data stored, show the consent dialog to capture the user consent decision. 
              <li> If there is consent data stored continue to the next step. 
            </ol> 
        <li> To transfer the consent data to the SDK create an object called <code>AppsFlyerConsent</code> using the <code>forGDPRUser()</code> method with the following parameters:<br> 
          - <code>hasConsentForDataUsage</code> - Indicates whether the user has consented to use their data for advertising purposes.<br>
          - <code>hasConsentForAdsPersonalization</code> - Indicates whether the user has consented to use their data for personalized advertising purposes.
        <li> Call <code>AppsFlyerLib.getInstance().setConsentData()</code> with the <code>AppsFlyerConsent</code> object.    
        <li> Call <code>AppsFlyerLib.getInstance().start(this)</code>. 
      </ol><br> 
    - If GDPR doesn’t apply to the user perform the following: 
      <ol> 
        <li> Create an <code>AppsFlyerConsent</code> object using the <code>forNonGDPRUser()</code> method. This method doesn’t accept any parameters.
        <li> Call <code>AppsFlyerLib.getInstance().setConsentData()</code> with the <code>AppsFlyerConsent</code> object.  
        <li> Call <code>AppsFlyerLib.getInstance().start(this)</code>. 
      </ol> 
</ol> 
