package fr.volkaert.events.sse.quarkus.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// See https://www.baeldung.com/java-ee-jax-rs-sse

@Path("/events")
@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
public class BrokerController {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerController.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    Sse sse;
    OutboundSseEvent.Builder eventBuilder;
    //SseEventSink sseEventSink = null;
    //SseBroadcaster sseBroadcaster = null;

    // Usage: instantFormatter.format(Instant.now())
    private DateTimeFormatter instantFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

    // Key is an event code, value is a SseBroadcaster
    Map<String, SseBroadcaster> broadcastersMap = new ConcurrentHashMap<>();

    Object lock = new Object();

    @Context
    public void setSse (Sse sse) {
        this.sse = sse;
        this.eventBuilder = sse.newEventBuilder();
        //this.sseBroadcaster = sse.newBroadcaster();
    }

    @POST
    @Path("/{eventCode}/subscriptions")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    // Example using curl: curl -X POST http://localhost:8080/events/eventA/subscriptions
    public void subscribe(@Context SseEventSink sseEventSink, @PathParam("eventCode") String eventCode) {
        LOG.info("Subscription received for event {}", eventCode);
        SseBroadcaster broadcaster = broadcastersMap.computeIfAbsent(eventCode, newBroadcaster -> sse.newBroadcaster());
        broadcaster.register(sseEventSink);
    }

    @GET // For tests only (typically to ease subscription from the navigation bar of a browser)
    @Path("/{eventCode}/subscriptions")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    // Example (from the navigation bar of a browser): http://localhost:8080/events/eventA/subscriptions
    public void subscribeUsingGETForTestsOnly(@Context SseEventSink sseEventSink, @PathParam("eventCode") String eventCode) {
        subscribe(sseEventSink, eventCode);
    }


    @POST
    @Path("/publications")
    // Example using curl: curl -d '{"eventCode":"eventA", "payload":"helloA"}' -H "Content-Type: application/json" -X POST http://localhost:8080/events/publications
    public Event publish(Event eventToPublish) {
        if (eventToPublish.id == null) {    // do not overwrite existing id explicitly set by the publisher
            eventToPublish.id = UUID.randomUUID().toString();
        }
        eventToPublish.publicationDate = instantFormatter.format(Instant.now());
        LOG.info("Publication for event {} published: {}", eventToPublish.eventCode, eventToPublish.payload);
        SseBroadcaster broadcaster = broadcastersMap.get(eventToPublish.eventCode);
        if (broadcaster != null) {
            LOG.info("SseBroadcaster found for event {}", eventToPublish.eventCode);
            String eventToPublishAsJSONString = GSON.toJson(eventToPublish);
            OutboundSseEvent sseEvent = this.eventBuilder
                    .name("message")
                    .id(eventToPublish.id)
                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                    .data(eventToPublishAsJSONString)
                    .reconnectDelay(3000)
                    .comment("Event type is " + eventToPublish.eventCode)
                    .build();
            broadcaster.broadcast(sseEvent);
        }
        else {
            LOG.info("SseBroadcaster NOT found for event {}", eventToPublish.eventCode);
        }
        Event publishedEvent = new Event(eventToPublish);
        publishedEvent.payload = null;  // remove the payload from the response to preserve bandwidth
        return publishedEvent;
    }

    @GET // For tests only (typically to ease publication from the navigation bar of a browser)
    @Path("/{eventCode}/publications")
    // Example (from the navigation bar of a browser): http://localhost:8080/events/eventA/publications?payload=helloA
    public Event publishUsingGETForTestsOnly(@PathParam("eventCode") String eventCode, @QueryParam("payload") String payload) {
        Event eventToPublish = new Event(eventCode, payload);
        Event publishedEvent = publish(eventToPublish);
        return publishedEvent;
    }
}
