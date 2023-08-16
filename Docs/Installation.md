
# 📲 Adding the SDK to your project
The extension available via Swift Package Manager (SPM) and Cocoapods. To install the extension, please use one of the following options.

## <a id="SPM">  Swift Package Manager (SPM)
 **Step 1: Navigate to Add Package Dependency**  
In Xcode, go to **File** > **Add Packages** 

**Step 2: Add `AppsFlyerAdobeAEPExtension` GitHub repository**  
Enter the `AppsFlyerAdobeAEPExtension` GitHub repository:  
`https://github.com/AppsFlyerSDK/appsflyer-adobe-mobile-ios-swift-extension.git`

**Step 3: Select SDK version**

**Step 4: Add `AppsFlyerAdobeAEPExtension` to desired Target**

## <a id="Cocoapods">  Cocoapods
**Step 1: Download CocoaPods**  
[Download and install](https://guides.cocoapods.org/using/getting-started.html#installation)  the latest version of CocoaPods.

**Step 2: Add dependencies**  
Add the  [latest version of  `AppsFlyerAdobeAEPExtension`](https://github.com/AppsFlyerSDK/AppsFlyerAdobeAEPExtension/releases/latest)  to your project's Podfile:

```
	pod 'AppsFlyerAdobeAEPExtension', '<PLUGIN_VERSION>'
```

**Step 3: Install dependencies**  
In your terminal, navigate to your project's root folder and run  `pod install`.

**Step 4: Open Xcode workspace**  
In Xcode, use the  `.xcworkspace`  file to open the project from this point forward, instead of the  `.xcodeproj`  file.
