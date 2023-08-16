# Deep linking


Deep Linking vs Deferred Deep Linking:

A deep link is a special URL that routes to a specific spot, whether that’s on a website or in an app. A “mobile deep link” then, is a link that contains all the information needed to take a user directly into an app or a particular location within an app instead of just launching the app’s home page.

If the app is installed on the user's device - the deep link routes them to the correct location in the app. But what if the app isn't installed? This is where Deferred Deep Linking is used.When the app isn't installed, clicking on the link routes the user to the store to download the app. Deferred Deep linking defer or delay the deep linking process until after the app has been downloaded, and ensures that after they install, the user gets to the right location in the app.

- [The 3 Deep Linking Types:](#Deep-Linking)
  - [Deferred Deep Linking (Get Conversion Data) - Legacy APIs](#deferred-deep-linking)
  - [Direct Deeplinking](#handle-deeplinking)
  - [Unified deep linking](#unified-deeplinking)
- [<a id="setup"> Setup](#setup)
  - [Code Setup](#code-setup)
  - [Deeplink Setup](#deeplink-setup)
    - [Universal Links](#universal-links)
    - [URI Scheme](#uri-scheme) 

<!---
- [Deep linking](#deep-linking)
      - [<a id="Deep-Linking"> The 3 Deep Linking Types:](#a-iddeep-linking-the-3-deep-linking-types)
    - [❗️Important!](#️important)
    - [<a id="deferred-deep-linking"> 1. Deferred Deep Linking (Get Conversion Data) - Legacy APIs](#a-iddeferred-deep-linking-1-deferred-deep-linking-get-conversion-data---legacy-apis)
    - [<a id="handle-deeplinking"> 2. Direct Deeplinking](#a-idhandle-deeplinking-2-direct-deeplinking)
    - [<a id="unified-deeplinking"> 3. Unified deep linking](#a-idunified-deeplinking-3-unified-deep-linking)
- [<a id="setup"> Set-up](#a-idsetup-set-up)
    - [<a id="code-setup"> Code Setup](#a-idcode-setup-code-setup)
    - [<a id="deeplink-setup">  Deeplink Setup](#a-iddeeplink-setup--deeplink-setup)
--->
![alt text](https://massets.appsflyer.com/wp-content/uploads/2018/03/21101417/app-installed-Recovered.png "")

#### <a id="Deep-Linking"> The 3 Deep Linking Types:
Since users may or may not have the mobile app installed, there are 2 types of deep linking:

1. Deferred Deep Linking (**Legacy APIs**) - Serving personalized content to new or former users, directly after the installation.  -
2. Direct Deep Linking (**Legacy APIs**)  - Directly serving personalized content to existing users, which already have the mobile app installed.  
3. Unified deep linking - Unified deep linking sends new and existing users to a specific in-app activity as soon as the app is opened.<br>

For more info about <ins>Deferred Deep Linking</ins> please check out the [OneLink™ Deferred deep linking Guide](https://dev.appsflyer.com/hc/docs/android-legacy-apis#deferred-deep-linking). <br>
For more info about <ins>Direct Deep linking</ins> please check out the [OneLink™ Direct Deep Linking Guide](https://dev.appsflyer.com/hc/docs/android-legacy-apis#deep-linking). <br>
For more info about <ins>Unified Deep Linking</ins> please check out the [OneLink™ Direct Deep Linking Guide](https://dev.appsflyer.com/hc/docs/unified-deep-linking-udl). <br>

###  <a id="deferred-deep-linking"> 1. Deferred Deep Linking (Get Conversion Data) - Legacy APIs

Check out the deferred deeplinkg guide from the AppFlyer knowledge base [here](https://support.appsflyer.com/hc/en-us/articles/207032096-Accessing-AppsFlyer-Attribution-Conversion-Data-from-the-SDK-Deferred-Deeplinking-#Introduction).

1. Create and register a delegate to receive the callbacks. Setup guide [here](#code-setup)
2. Implement `onConversionDataSuccess` and `onConversionDataFail` methods from `AppsFlyerLibDelegate` protocol

Code Sample to handle the conversion data:
```swift
// Handle Organic/Non-organic installation
func onConversionDataSuccess(_ data: [AnyHashable: Any]) {

    print("onConversionDataSuccess data:")
    for (key, value) in data {
        print(key, ":", value)
    }

    if let status = data["af_status"] as? String {
        if (status == "Non-organic") {
            if let sourceID = data["media_source"],
                let campaign = data["campaign"] {
                print("This is a Non-Organic install. Media source: \(sourceID)  Campaign: \(campaign)")
            }
        } else {
            print("This is an organic install.")
        }
        if let is_first_launch = data["is_first_launch"] as? Bool,
            is_first_launch {
            print("First Launch")
            if let fruit_name = data["deep_link_value"]
            {
                // The key 'deep_link_value' exists only in OneLink originated installs
                print("deferred deep-linking to \(fruit_name)")
                walkToSceneWithParams(params: data)
            }
            else {
                print("Install from a non-owned media")
            }
        } else {
            print("Not First Launch")
        }
    }
}
func onConversionDataFail(_ error: Error) {
    print("\(error)")
}
```


###  <a id="handle-deeplinking"> 2. Direct Deeplinking
    
When a deeplink is clicked on the device the AppsFlyer SDK will return the resolved link in the [onAppOpenAttribution](https://support.appsflyer.com/hc/en-us/articles/208874366-OneLink-Deep-Linking-Guide#deep-linking-data-the-onappopenattribution-method-) method.

1. Create and register a delegate to receive the callbacks. Setup guide [here](#code-setup)
2. Implement `onAppOpenAttribution` and `onAppOpenAttributionFailure` methods from `AppsFlyerLibDelegate` protocol
   
Code Sample to handle OnAppOpenAttribution:
```swift
  //Handle Deep Link Data
  func onAppOpenAttribution(_ attributionData: [AnyHashable : Any]) {  
    print("onAppOpenAttribution data:")
    for (key, value) in attributionData {
      print(key, ":",value)
    }
    var deepLinkValue: String? = nil
    if let value = attributionData["deep_link_value"] as? String {
      deepLinkValue = value
    } else if let linkParam = attributionData["link"] as? String {
      guard let url = URLComponents(string: linkParam) else {
        print("Could not extract query params from link")
        return
      }
      if let value = url.queryItems?.first(where: { $0.name == "deep_link_value" })?.value {
        deepLinkValue = value
      }
    }
    print("The deep link value is: \(deepLinkValue ?? "Not Found")")
    
  }
  
  func onAppOpenAttributionFailure(_ error: Error) {
    print("onAppOpenAttributionFailure: \(error)")
  }
```

###  <a id="unified-deeplinking"> 3. Unified deep linking

The flow works as follows:

1. User clicks the OneLink short URL.
2. The iOS Universal Links or the deferred deep link, trigger the SDK.
3. The SDK triggers the didResolveDeepLink method, and passes the deep link result object to the user.
4. The `didResolveDeepLink` method uses the deep link result object that includes the `deep_link_value` and other parameters to create the personalized experience for the users, which is the main goal of OneLink.

> Check out the Unified Deep Linking docs for [iOS](https://dev.appsflyer.com/docs/ios-unified-deep-linking).


Implementation:
1. Create and register a delegate to receive the callbacks. Setup guide [here](#code-setup)
2. Implement `didResolveDeepLink` methods from `DeepLinkDelegate` protocol


Example:
```swift
  func didResolveDeepLink(_ result: DeepLinkResult) {
    print("didResolveDeepLink")
    switch result.status {
      case .notFound:
        print("Deep link not found")
        return
      case .failure:
        print("Error: \(result.error)")
        return
      case .found:
        print("Deep link found")
    }
    
    guard let deepLinkObj:DeepLink = result.deepLink else {
      print("Could not extract deep link object")
      return
    }
    if deepLinkObj.isDeferred {
      print("This is a deferred deep link")
    } else {
      print("This is a direct deep link")
    }
    let deepLinkValue = deepLinkObj.deeplinkValue
    print("The deep link value is: \(deepLinkValue ?? "Not Found")")
  }
```


---
    
# <a id="setup"> Setup
This section is about setting up your app with deep links.
###  <a id="code-setup"> Code Setup
Add the following code to your `AppDelegate` class: 
```swift
  func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    ...
    // setup delegate for legacy deep links
    AppsFlyerAdobeExtension.delegate = self
    // setup delegate for  UDL
    AppsFlyerAdobeExtension.deepLinkDelegate = self
    ...
  }

  // For Swift version < 4.2 replace function signature with the commented out code
  // func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([Any]?) -> Void) -> Bool { // this line for Swift < 4.2
  func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
    // Make sure you are using `AppsFlyerAttribution` and not `AppsFlyerLib`
    AppsFlyerAttribution.shared.continueUserActivity(userActivity: userActivity)
    return true
  }
  
  // Open URI-scheme for iOS 9 and above
  func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
    // Make sure you are using `AppsFlyerAttribution` and not `AppsFlyerLib`
    AppsFlyerAttribution.shared.handleOpenUrl(open: url)
    return true
  }
  // Create Legacy Delegate
  extension AppDelegate : AppsFlyerLibDelegate {
  func onAppOpenAttribution(_ attributionData: [AnyHashable : Any]) {
    print("onAppOpenAttribution")
  }
  
  func onAppOpenAttributionFailure(_ error: Error) {
    print("onAppOpenAttributionFailure")
  }
  func onConversionDataFail(_ error: Error) {
    print("onConversionDataFail")
  }
  func onConversionDataSuccess(_ conversionInfo: [AnyHashable : Any]) {
    print("onConversionDataSuccess")
  }
}
// Create UDL Delegate
extension AppDelegate : DeepLinkDelegate {
  func didResolveDeepLink(_ result: DeepLinkResult) {
    print("didResolveDeepLink")
  }
}

```

If you use `SceneDelegate` make sure to add the following code as well in your `SceneDelegate` class:
```swift
 func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
    ...
    if let userActivity = connectionOptions.userActivities.first {
      self.scene(scene, continue: userActivity)
    } else {
      self.scene(scene, openURLContexts: connectionOptions.urlContexts)
    }
    ...
  }
  func scene(_ scene: UIScene, continue userActivity: NSUserActivity){
    // Make sure you are using `AppsFlyerAttribution` and not `AppsFlyerLib`
    AppsFlyerAttribution.shared.continueUserActivity(userActivity: userActivity)
  }
  
  func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>){
    if let url = URLContexts.first?.url {
      // Make sure you are using `AppsFlyerAttribution` and not `AppsFlyerLib`
      AppsFlyerAttribution.shared.handleOpenUrl(open: url)}
  }
```



###  <a id="deeplink-setup">  Deeplink Setup
<a id="universal-links"> **Universal links**</br>
For more on Universal Links check out the guide [here](https://support.appsflyer.com/hc/en-us/articles/208874366-OneLink-Deep-Linking-Guide#setups-universal-links).
    
Essentially, the Universal Links method links between an iOS mobile app and an associate website/domain, such as AppsFlyer’s OneLink domain (xxx.onelink.me). To do so, it is required to:

1. Configure OneLink sub-domain and link to mobile app (by hosting the ‘apple-app-site-association’ file - AppsFlyer takes care of this part in the onelink setup on your dashboard)
2. Configure the mobile app to register approved domains: see [here](https://dev.appsflyer.com/hc/docs/initial-setup-2#configuring-mobile-apps-to-register-approved-domains)
</br>
</br>

<a id="uri-scheme"> **URI scheme**</br>
A URI scheme is a URL that leads users directly to the mobile app.</br>
When an app user enters a URI scheme in a browser address bar box, or clicks on a link based on a URI scheme, the app launches and the user is deep-linked.</br>
Whenever a Universal Link fails to open the app, the URI scheme can be used as a fallback to open the application.</br>
1. Deciding on a URI scheme: see [here](https://dev.appsflyer.com/hc/docs/initial-setup-2#deciding-on-a-uri-scheme)
2. Adding URI scheme: see [here](https://dev.appsflyer.com/hc/docs/initial-setup-2#adding-uri-scheme)