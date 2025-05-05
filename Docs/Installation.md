## <a id="add-sdk-to-project"> 📲 Adding the SDK to your project

Add the following to your app's `build.gradle (Module: app)` file:

```groovy
repositories {
    mavenCentral()
}

dependencies {
...
implementation platform('com.adobe.marketing.mobile:sdk-bom:3.+')

implementation 'com.adobe.marketing.mobile:core'
implementation 'com.adobe.marketing.mobile:identity'
implementation 'com.adobe.marketing.mobile:analytics'

implementation 'com.appsflyer:appsflyer-adobe-aep-sdk-extension:6.+'
implementation 'com.android.installreferrer:installreferrer:2.1'

// Only for Adobe Edge Network events support:   
implementation 'com.adobe.marketing.mobile:edge'
implementation 'com.adobe.marketing.mobile:edgeidentity'
implementation 'com.adobe.marketing.mobile:edgeconsent'

}
```

> The Appsflyer plugin is compatible with Adobe versions > 3.x .  

> Add the installreferrer library to improve attribution accuracy, protects from install fraud and more.
 
Starting from **6.14.0** Huawei Referrer integration was updated. [Learn more](https://dev.appsflyer.com/hc/docs/install-android-sdk#huawei-install-referrer).
