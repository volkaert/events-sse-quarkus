# Introduction

The purpose of this demo is to show how to use SSE (Server Sent Events) protocol to publish and subscribe events 
(useful in an Event Driven Architecture - EDA - based system). 

SSE is HTTP based, contrary to WebSockets which is TCP based (lower level).
Also, SSE is one way only (from server to client), contrary to WebSockets which is 2-ways.

This demo uses Quarkus (www.quarkus.io) (and not SpringBoot). No special reason for that; it's just because I wanted to play with Quarkus !


# In a nutshell

Build and start the broker:
```
cd broker
../mvnw clean package
java -jar target/events-sse-quarkus-broker-1.0-SNAPSHOT-runner.jar
```

In a terminal, start a first subscription for events of category `eventA`:
```
curl -X POST http://localhost:8080/events/eventA/subscriptions
```

In another terminal, start a second subscription for events of category `eventA`:
```
curl -X POST http://localhost:8080/events/eventA/subscriptions
```

In another terminal, publish an event of category `eventA`:
```
curl -d '{"eventCode":"eventA", "payload":"helloA"}' -H "Content-Type: application/json" -X POST http://localhost:8080/events/publications
```
>Pay attention to the fact that the URL for a subscription contains the `eventCode` but 
>the URL for a publication does NOT contain the `eventCode`
>because the `eventCode` for a publication is contained in the JSON request body !

# Broker 

## Build and start

>The broker runs on the port `8080`

### Build and start in dev mode (with hot reload)
```
cd broker
../mvnw compile quarkus:dev
```

### Build and start as a jar
```
cd broker
../mvnw clean package
java -jar target/events-sse-quarkus-broker-1.0-SNAPSHOT-runner.jar
```

### Test

In a terminal, start a subscription for events of category `eventA`:
```
curl -X POST http://localhost:8080/events/eventA/subscriptions
```

In another terminal, publish an event of category `eventA`:
```
curl -d '{"eventCode":"eventA", "payload":"helloA"}' -H "Content-Type: application/json" -X POST http://localhost:8080/events/publications
``` 

 
# Subscriber

## Build and start

>The Subscriber runs on the port `8082`

### Build and start in dev mode (with hot reload)
```
cd subscriber
../mvnw compile quarkus:dev
```

### Build and start as a jar
```
cd subscriber
../mvnw clean package
java -jar target/events-sse-quarkus-subscriber-1.0-SNAPSHOT-runner.jar
```
### Test

In a terminal, start a subscription for events of category `eventA`:
```
curl -X POST http://localhost:8082/events/eventA/subscriptions
```
>Pay attention to use the port `8082` for the subscription (`8082` is for the subscriber, `8080` is for the broker)

In another terminal, publish an event of category `eventA`:
```
curl -d '{"eventCode":"eventA", "payload":"helloA"}' -H "Content-Type: application/json" -X POST http://localhost:8080/events/publications
``` 
>Pay attention to use the port `8080` for the publication (publications are done directly by the broker)

To unsubscribe from events of category `eventA`:
```
curl -X DELETE http://localhost:8082/events/eventA/subscriptions
```