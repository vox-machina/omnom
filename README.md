# Omnom - the eater of content

## About

![Alt Text](https://media.giphy.com/media/jgUG5cnss7T9K/giphy.gif)

N.B. Originally Omnom was an early experiment in consuming APIs - now stored over at [old-api-consumer-omnom](https://github.com/rossajmcd/old-api-consumer-omnom).

Omnom is a configurable server side 'funnel' for capturing content you care about.

Currently it handles content types:
- Github issues from repositories you would like notifications from
- GPS from iOS app 'Overland'
- Steps count from any device you can call out to an API from

And offers the following functionality additionally:
- Alerts to devices of your choosing with Alerty integration

## Getting started

Copy config.template.edn to config.edn and set the variables to your needs.

then run

```bash
 clj -X app.core/-main
```

## Roadmap

See the Github Repository [roadmap](https://github.com/rossajmcd/omnom/issues?q=is%3Aopen+is%3Aissue+label%3Aenhancement).