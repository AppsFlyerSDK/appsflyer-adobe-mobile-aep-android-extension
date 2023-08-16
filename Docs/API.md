# 📑 APIs

## Properties
### deepLinkDelegate
**Description**
Delegate property of an object, that conforms to DeepLinkDelegate protocol and implements its methods.
| Type |  Name|
|--|--|
| DeepLinkDelegate | deepLinkDelegate |

**Usage example**
```swift
AppsFlyerAdobeExtension.deepLinkDelegate = self
```
### delegate

**Description**
AppsFlyer delegate. See AppsFlyerLibDelegate.
| Type |  Name|
|--|--|
| AppsFlyerLibDelegate | delegate |

**Usage example**
```swift
AppsFlyerAdobeExtension.delegate = self
```

## Native SDK API
All the native SDK API is available using direct access to `AppsFlyerLib.shared()` object. </br>
You can find the complete iOS SDK reference  [here](https://dev.appsflyer.com/hc/docs/ios-sdk-reference)
