# Track Data with Adobe
This page is about tracking AppsFlyer data with Adobe. (Conversions and Engagement)


## Table of content


- [Data Elements](#data-elements)
- [Attribution Data tracking with Adobe Analytics](#attr-data)
- [Deeplink Data tracking with Adobe Analytics](#deeplink-data)
- [Wait for ECID](#wait-for-ecid)

    
##  <a id="data-elements"> Data Elements
    

The extension supports the following data elements:

> Note that the data elemets are set using shared state in the onConversionDataReceived callback on first launch only.

| Name            | Key               | 
| --------------  | -----------       | 
|  AppsFlyer ID   |   appsflyer_id    | 
|  Attribution Status   |   af_status    | 
|  Campaign   |   campaign    | 
|  Media Source   |   media_source    | 
|  AppsFlyer SDK Version   |   sdk_version    | 
|  Agency   |   agency    | 
|  Campaign ID   |   campaign_id    | 
|  Click Time   |   click_time    | 
|  Install Time   |   install_time    | 
|  Ad ID   |   ad_id    | 
|  Retargeting Conversion Type   |   retargeting_conversion_type    | 
|  Af Keywords    |   af_keywords    | 


For more info on the keys, checkout the AppsFlyer docs [here](https://support.appsflyer.com/hc/en-us/articles/360000726098-Conversion-data-payloads-and-scenarios). <br/>

> In order data elements to work, the 'Send Attribution Data to Adobe Analytics' setting in the AppsFlyer extension page must be enabled.
    
##  <a id="attr-data"> Attribution Data tracking with Adobe Analytics

When the `Send attribution data to Adobe Analytics` setting is enabled, then a Action events is sent from the AppsFlyer extension to adobe analytics. Here is what you need to know about this event:
1. The event name is "AppsFlyer Attribution Data"
2. The event values are a copy of the conversion data
3. The appsflyer_id is added to the event
4. "appsflyer." is appended to all values 
5. The event is sent on first launch only
6. The Event is sent after the `AppsFlyerLibDelegate.onConversionDataSuccess` method is called.

For example here is a sample `organic` "AppsFlyer Attribution Data" event:

```yaml
{
        class: Event,
        name: Analytics Track,
        eventNumber: 13,
        uniqueIdentifier: a8b5a26a-01d2-****-****-feac597ac2eb,
        source: com.adobe.eventsource.requestcontent,
        type: com.adobe.eventtype.generic.track,
        pairId: null,
        responsePairId: d3371461-****-****-9b7d-3bbafb8d3d52,
        timestamp: 1589810779830,
        data: {
            "action" : "AppsFlyer Attribution Data",
            "contextdata" : {
                "appsflyer.appsflyer_id" : "1589810776752-6880051***1087",
                "appsflyer.af_status" : "Organic",
                "appsflyer.sdk_version" : "version: 5.3.0 (build 14)",
                "appsflyer.af_message" : "organic install",
                "appsflyer.install_time" : "2020-05-18 14:06:19.154",
                "appsflyer.media_source" : "organic"
            }
        }
    }
```

Non-organic example:

```yaml
{
        class: Event,
        name: Analytics Track,
        eventNumber: 13,
        uniqueIdentifier: ef1c9625-8eb0-****-****-202c10203ca8,
        source: com.adobe.eventsource.requestcontent,
        type: com.adobe.eventtype.generic.track,
        pairId: null,
        responsePairId: a49fab46-2dd0-****-****-b4a9ba82dd3d,
        timestamp: 1589814096088,
        data: {
            "action" : "AppsFlyer Attribution Data",
            "contextdata" : {
                "appsflyer.adgroup" : null,
                "appsflyer.af_click_lookback" : "7d",
                "appsflyer.sdk_version" : "version: 5.3.0 (build 14)",
                "appsflyer.esp_name" : null,
                "appsflyer.is_universal_link" : null,
                "appsflyer.appsflyer_id" : "1589814094317-3288*****557929",
                "appsflyer.af_cpi" : null,
                "appsflyer.campaign_id" : null,
                "appsflyer.orig_cost" : "0.0",
                "appsflyer.iscache" : "true",
                "appsflyer.adgroup_id" : null,
                "appsflyer.match_type" : "id_matching",
                "appsflyer.advertising_id" : "7ce7ba7d-2b6b-****-****-2158bdf3e2f6",
                "appsflyer.agency" : null,
                "appsflyer.af_status" : "Non-organic",
                "appsflyer.campaign" : "None",
                "appsflyer.install_time" : "2020-05-18 15:01:36.502",
                "appsflyer.media_source" : "af_test",
                "appsflyer.af_siteid" : null,
                "appsflyer.cost_cents_USD" : "0",
                "appsflyer.adset_id" : null,
                "appsflyer.redirect_response_data" : null,
                "appsflyer.is_branded_link" : null,
                "appsflyer.retargeting_conversion_type" : "none",
                "appsflyer.http_referrer" : "https://test.com/test/",
                "appsflyer.engmnt_source" : null,
                "appsflyer.af_sub1" : null,
                "appsflyer.click_time" : "2020-05-18 15:01:29.170",
                "appsflyer.af_sub3" : null,
                "appsflyer.af_sub2" : null,
                "appsflyer.adset" : null,
                "appsflyer.af_sub5" : null,
                "appsflyer.af_sub4" : null
            }
        }
    }
```
    
##  <a id="deeplink-data"> Deeplink Data tracking with Adobe Analytics


When a deeplink is opened, and the `Send attribution data to Adobe Analytics` setting is enabled, then a Action events is sent from the AppsFlyer extension to adobe analytics. Here is what you need to know about this event:
1. The event name is "AppsFlyer Engagement Data"
2. The event values are a copy of the attribution deeplink data
3. The appsflyer_id is added to the event
4. "appsflyer.af_engagement_" is appended to all values 
6. The Event is sent after the `AppsFlyerLibDelegate.onAppOpenAttribution` or `DeepLinkDelegate.didResolveDeepLink` method is called.

For example here is a sample "AppsFlyer Engagement Data" event:

```yaml
{
        class: Event,
        name: Analytics Track,
        eventNumber: 17,
        uniqueIdentifier: 5b51d80b-9bc7-****-****-d579a77ca356,
        source: com.adobe.eventsource.requestcontent,
        type: com.adobe.eventtype.generic.track,
        pairId: null,
        responsePairId: b7c39fd0-0f02-****-****-f8718b12dc88,
        timestamp: 1589811327647,
        data: {
            "action" : "AppsFlyer Engagement Data",
            "contextdata" : {
                "appsflyer.af_engagement_scheme" : "appsflyer",
                "appsflyer.af_engagement_link" : "appsflyer://test",
                "appsflyer.af_engagement_install_time" : "2020-05-18 14:06:14",
                "appsflyer.af_engagement_host" : "test",
                "appsflyer.af_engagement_path" : ""
            }
        }
    }
```

##  <a id="wait-for-ecid"> Wait for ECID

By default the AppsFlyer SDK sets the ExperienceCloudId (ECID) as the CustomerUserID. <br/>
Setting the `Wait for ECID` setting to true, will guarantee that the ECID will be set on the `first launch`. <br/>
Use this setting if you must have ECID attached to the install.<br/>

If this setting is set to false, ECID will be set as soon as it is available.