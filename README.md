# ifttt-shim

Welcome! ifttt-shim is an open source template for quickly making an api shim between ifttt and any other api. Use ifttt-shim to integrate with channels that don't exist or by augmenting existing channels to do exactly what you want.

## Features
* Preconfigured to host on Heroku.
* Json4s for the best json dsl on the jvm.
* Designed to contain mutiple channel integrations.
* Scala Spray for powerful http server and routing.
* Akka actors for simple abstraction.

## Getting Started

### IFTTT

You will need an [ifttt developer account](https://developer.ifttt.com). Read the documentation carefully and then read my primer on how to extend iftt-shim with your own custom channel.

### Running the app
The goal of ifttt-shim is to be a generic shim solution for any channel you might like to integrate with. It is designed so that you can easily add multiple channels to one instance of the app, making its hosting very cost effective.  

ifttt-shim requires two environment variables to be set for each channel the app integrates with.

```
{CHANNEL_NAME}_CHANNEL_KEY={channel-key-from-ifttt}

{CHANNEL_NAME}_ACCESS_TOKEN={test-account-o-auth-token}
```

Each channel that you set up in your IFTTT developer account will get a unique channel key and require you to have a test account set up, which will require a valid OAuth token for the specific channel api you are shimming for. Add these variables into your IDE, bash, and/or Heroku's config tool.

Run tests with:

```
> sbt test
```

Use revolver start and stop the app

```
> sbt
sbt> re-start
...
sbt> re-stop
```

### Hosting on Heroku
ifttt-shim is configured to run on Heroku. The native packager sbt plugin and procfile are already present. Simply use `heroku config:set` to tell heroku the channel keys and access tokens for your channels and git push the app to the heroku remote.

## Road Map
* Add dynamic options to the new\_personal\_record trigger. e.g. Filter by rank or activity type.
* Make use of `X-Request-ID` header value in logging and error messages.
* Generalize the architecture more so that it can accommodate multiple api shims.
* Diagram architecture and include it in this document.

## Integration ideas
* Connect Google fit

