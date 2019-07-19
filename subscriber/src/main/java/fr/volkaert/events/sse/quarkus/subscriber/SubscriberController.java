package fr.volkaert.events.sse.quarkus.subscriber;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Path("/events")
@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
public class SubscriberController {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberController.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @ConfigProperty(name = "sse.address")
    String sseAddress;

    // Key is eventCode, value is SseEventSource
    Map<String, SseEventSource> eventSourceMap = new ConcurrentHashMap<>();

    @POST
    @Path("/{eventCode}/subscriptions")
    // Example using curl: curl -X POST http://localhost:8082/events/eventA/subscriptions
    public void subscribe(@PathParam("eventCode") String eventCode) {
        LOG.info("Subscribe for event  {}", eventCode);
        String eventSourceKey = eventCode;
        SseEventSource eventSource = eventSourceMap.get(eventSourceKey);
        if (eventSource == null) {
            Client client = ClientBuilder.newClient();
            String url = sseAddress + "/" + eventCode + "/subscriptions";
            WebTarget target = client.target(url);
            eventSource = SseEventSource.target(target).reconnectingEvery(5, TimeUnit.SECONDS).build();
            eventSource.register(onEvent, onError, onComplete);
            eventSource.open();
            eventSourceMap.put(eventSourceKey, eventSource);
        }
    }

    @GET // For tests only (typically to ease subscription from the navigation bar of a browser)
    @Path("/{eventCode}/subscriptions")
    // Example (from the navigation bar of a browser): http://localhost:8082/events/eventA/subscriptions
    public void subscribeUsingGETForTestsOnly(@PathParam("eventCode") String eventCode) {
        subscribe(eventCode);
    }

    @DELETE
    @Path("/{eventCode}/subscriptions")
    // Example using curl: curl -X DELETE http://localhost:8082/events/eventA/subscriptions
    public void unsubscribe(@PathParam("eventCode") String eventCode) {
        LOG.info("Unsubscribe for event  {}", eventCode);
        String eventSourceKey = eventCode;
        SseEventSource eventSource = eventSourceMap.get(eventSourceKey);
        if (eventSource != null) {
            eventSourceMap.remove(eventSourceKey);
            eventSource.close();
        }
    }

    // A new event is received
    private static Consumer<InboundSseEvent> onEvent = (inboundSseEvent) -> {
        String data = inboundSseEvent.readData();
        LOG.info("Event received: {}", data);
        Event event = GSON.fromJson(data, Event.class);
        LOG.info("Event received: {}", event);
    };

    //Error
    private static Consumer<Throwable> onError = (throwable) -> {
        LOG.error("Error received: {}", throwable.getMessage(), throwable);
        throwable.printStackTrace();
    };

    //Connection close and there is nothing to receive
    private static Runnable onComplete = () -> {
        LOG.info("onComplete");
    };
}


