# IFTTT-Shim

Welcome! This lovely collection of code does a great job of shimming interactions between the glorious product connector IFTTT and any other api channel of your choosing. This document is a quick overview of how and why it works.

**TLDR:** I built a personal general solution to integrating channels with IFTTT. There is only one trigger configured now but the hard work is essentially done so I thought I would show it. I plan to keep working on it in the coming weeks.

## Hello IFTTT Team
I built IFTTT-Shim to satisfy your interview coding assignment. Originally I intended to build a quick and somewhat dirty solution, but when I decided to use Strava as my channel I realized that I would need something better designed.

You see Strava --like a lot of api's out in the interwebs-- is pretty, restful and completely inefficient for anything other than 2 resources. It provides almost zero filtering by parameters and most of its interesting resources must be requested singly by their id's. This means that any useful integration will require lots of api calls to extract and transform the desired data.

Which unfortunately also means the solution must have some pretty good concurrency. My quick and dirty tool is Python/Flask, but my new shiny tool --honed at the Recurse Center-- is Scala/Spray.

This is why it has been some two weeks since I received the assignment and I am only now showing it to you with its one, lonely, albeit complicated trigger.

## Features
IFTTT-Shim is a webapp written in Scala and the Spray framework, built with sbt, and hosted on Heroku. The current implementation satisfies the IFTTT testing suite and integrates the Strava API.

### A little bit about Strava
Strava has apps that tracks exercising activities via gps. On top of that it adds a lot of value. One of the interesting features is that they have separated the streets and paths into segments. Strava names and maintains leader boards for these segments. The global leader boards are extremely competitive but Strava also maintains individual leader boards for every segment a user has completed.

Thus when you go on your daily jog you, run through familiar segments. As you get faster and stronger Strava recognizes the top 3 best performances and alerts you when you have achieved new personal records.

### NewPersonalRecord
The new\_personal\_record trigger fires when a Strava athlete beats one of his personal records. In order to know about these achievements, the shim must call the athlete's recent activities, then individually query those activities extract the summary of the segment efforts and achievements then transform them into the trigger response data.

This gets out of hand in a hurry. Since there is no way to know which activities have segment effort achievements we have to essentially get them all and check. Because IFTTT polls at 15 minute increments I can limit the potential new activities to 20, this should give enough context and still catch every new activity for even the most zealous athletes.

This is about as API intensive at it gets for this channel and requires ~ 21 api calls. IFTTT-Shim handles this request on the free tier of Heroku on an average of 2.5 seconds. I have tested it at 46 api calls with similar response times.

## Architecture
IFTTT-Shim makes use of the Spray routing DSL and uses the per request actor model to handle requests. This allows very fine control over error handling by preferring a tell don't ask policy with actor messages.

--Diagram Coming soon--

## Road Map
* Add more Triggers.
* Add dynamic options to the new_personal_record trigger. e.g. Filter by rank or activity type.
* Make use of `X-Request-ID` header value in logging and error messages.
* Generalize the architecture more so that it can accommodate multiple api shims.
* Diagram architecture and include it in this document.

## Getting Started
The goal of IFTTT-Shim is to be a generic shim solution for any channel you might like to integrate with. It is designed so that you can easily add multiple channels to one app, making it's hosting very cost effective.  

IFTTT-Shim requires two environment variables to be set for each channel the app integrates with.

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

### Heroku
IFTTT-Shim is configured to run on Heroku. The native packager sbt plugin and procfile are already present. Simply use `heroku config:set` to tell heroku the channel key's and access token's for your channels and git push the app to the heroku remote.

## Issues
The biggest issue is that supporting api intensive triggers like new\_personal\_record exhausts the rate limit pretty quickly, Continuous polling only allows 30,000 api calls per day, about 300 every 15 minutes which can only support ~15 users.

Either Strava needs to make their api more query efficient or one would need to negotiate a more generous api rate limit. As a personal shim for myself and couple of friends this would be acceptable. 

