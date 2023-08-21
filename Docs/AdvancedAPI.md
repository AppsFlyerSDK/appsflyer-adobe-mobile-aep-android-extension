# API

<img src="https://massets.appsflyer.com/wp-content/uploads/2018/06/20092440/static-ziv_1TP.png"  width="400" >

- [registerConversionListener](#registerConversionListener)
- [unregisterConversionListener](#unregisterConversionListener)
- [subscribeForDeepLink](#subscribeForDeepLink)


---


 ##### <a id="registerConversionListener"> **`void registerConversionListener(callbacksListener: AppsFlyerConversionListener)`**
 

| parameter          | type                        | description  |
| -----------        |-----------------------------|--------------|
| `callbacksListener` | `AppsFlyerConversionListener` | AppsFlyer conversion interface|

*Example:*

```kotlin

AppsflyerAdobeExtension.registerConversionListener(
    object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(p0: MutableMap<String, Any>?) {
            Log.d("AppsFlyerCallbacks", p0.toString())
        }

        override fun onConversionDataFail(p0: String?) {
            Log.d("AppsFlyerCallbacks", p0?: " error onConversionDataFail")
        }

        override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
            Log.d("AppsFlyerCallbacks", p0.toString())
        }

        override fun onAttributionFailure(p0: String?) {
            Log.d("AppsFlyerCallbacks", p0?:" error onAttributionFailure")
        }
    }
)

```

---


 ##### <a id="unregisterConversionListener"> **`void unregisterConversionListener()`**


*Example:*

```kotlin

AppsflyerAdobeExtension.unregisterConversionListener()

```


---


 ##### <a id="subscribeForDeepLink"> **`void subscribeForDeepLink(deepLinkListener: DeepLinkListener)`**

 | parameter          | type                        | description  |
| -----------        |-----------------------------|--------------|
| `deepLinkListener` | `DeepLinkListener` | AppsFlyer Deeplink interface|


*Example:*

```kotlin

AppsflyerAdobeExtension.subscribeForDeepLink(
object : DeepLinkListener {
    override fun onDeepLinking(p0: DeepLinkResult) {
        Log.d("AppsFlyerDeepLink", p0.toString())
    }
})

```
