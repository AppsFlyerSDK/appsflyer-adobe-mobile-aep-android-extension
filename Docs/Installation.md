## <a id="add-sdk-to-project"> 📲 Adding the SDK to your project

Add the following to your app's `build.gradle (Module: app)` file:

```groovy
repositories {
    mavenCentral()
}

dependencies {
...
implementation '<******>'
implementation 'com.android.installreferrer:installreferrer:1.1'
}
```

> Add the installreferrer library to improve attribution accuracy, protects from install fraud and more.
