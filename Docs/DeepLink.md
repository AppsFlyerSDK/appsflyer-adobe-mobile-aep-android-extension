# Deep linking
    
![alt text](https://massets.appsflyer.com/wp-content/uploads/2018/03/21101417/app-installed-Recovered.png "")


#### The 2 Deep Linking Types:
Since users may or may not have the mobile app installed, there are 2 types of deep linking:

1. Deferred Deep Linking - Serving personalized content to new or former users, directly after the installation. 
2. Direct Deep Linking - Directly serving personalized content to existing users, which already have the mobile app installed.

For more info please check out the [OneLink™ Deep Linking Guide](https://support.appsflyer.com/hc/en-us/articles/208874366-OneLink-Deep-Linking-Guide#Intro).

Also, Check out the deferred deeplinkg guide from the AppFlyer knowledge base [here](https://support.appsflyer.com/hc/en-us/articles/207032096-Accessing-AppsFlyer-Attribution-Conversion-Data-from-the-SDK-Deferred-Deeplinking-#Introduction).

Code Sample to handle deeplinking data (beneath MobileCore.registerExtensions(extensions) call):

```kotlin
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
...
AppsflyerAdobeExtension.subscribeForDeepLink(object :
    DeepLinkListener {
    override fun onDeepLinking(p0: DeepLinkResult) {
        Log.d("AppsFlyerDeepLink", p0.toString())
    }
})
```
This option above is the preffered way to implement deep linking.

Another option for implementing deep linking is by setting up a ConversionListener (see [here](AdvancedAPI.md) how). This opiton is the legecy way.
You can read [here](https://dev.appsflyer.com/hc/docs/dl_android_gcd_legacy) more about the difference between both implementations.


###  <a id="android-deeplink"> Android Deeplink Setup
    
    
    
#### <a id="uri-scheme"> URI Scheme
In your app’s manifest add the following intent-filter to your relevant activity:
```xml 
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="your unique scheme" />
</intent-filter>
```

#### <a id="app-links"> App Links
For more on App Links check out the guide [here](https://support.appsflyer.com/hc/en-us/articles/115005314223-Deep-Linking-Users-with-Android-App-Links#what-are-android-app-links).

