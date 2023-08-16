# 🚀 Basic integration of the Extension

Register the AppsFlyer extension from your `AppDelegate` class, alongside the Adobe SDK initialisation code:
```swift
  func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    // Override point for customization after application launch.
    let appState = application.applicationState
    MobileCore.registerExtensions([Lifecycle.self, Identity.self, Signal.self,Analytics.self,  AEPMobileServices.self, AppsFlyerAdobeExtension.self]) {
    }
    MobileCore.configureWith(appId: "REPLACE_WITH_YOUR_ADOBE_KEY")
    if appState != .background {
      // only start lifecycle if the application is not in the background
      MobileCore.lifecycleStart(additionalContextData: nil)
    }
    return true
  }
```

In Addition to adding the init code, the settings inside the launch dashboard must be set.

<img src="../gitresources/LaunchAFInitNew.png" width="550" >

| Setting  | Description   |
| -------- | ------------- |
| AppsFlyer iOS App ID      | Your App Store [application ID](https://support.appsflyer.com/hc/en-us/articles/207377436-Adding-a-new-app#available-in-the-app-store-google-play-store-windows-phone-store)  |
| AppsFlyer Dev Key   | Your application [devKey](https://support.appsflyer.com/hc/en-us/articles/211719806-Global-app-settings-#sdk-dev-key) provided by AppsFlyer (required)  |
| Bind in-app events for    | Bind adobe event to appsflyer in-app events. For more info see the doc [here](/Docs/InAppEvents.md). |
| Send attribution data    | Send conversion data from the AppsFlyer SDK to adobe. This is required for data elements. |
| Debug Mode    | Debug mode - set to `true` for testing only.  |
| Wait for ECID   | Once enabled, the SDK Initialization will be delayed until the Experience Cloud ID is set.  |

> Note: For Send attribution data, use this feature if you are only working with ad networks that allow sharing user level data with 3rd party tools.