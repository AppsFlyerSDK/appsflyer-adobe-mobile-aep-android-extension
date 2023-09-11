## <a id="add-sdk-to-project"> 📲 Adding the SDK to your project

Add the following to your app's `build.gradle (Module: app)` file:

```groovy
repositories {
    mavenCentral()
}

dependencies {
...
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:lifecycle:2.+'
implementation 'com.adobe.marketing.mobile:identity:2.+'
implementation 'com.adobe.marketing.mobile:signal:2.+'

implementation 'com.appsflyer.appsflyer-adobe-aep-sdk-extension:6.+'
implementation 'com.android.installreferrer:installreferrer:2.1'
}
```

> The Appsflyer plugin is compatibe with Adobe versions > 2.x .  

> Add the installreferrer library to improve attribution accuracy, protects from install fraud and more.
