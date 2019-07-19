package fr.volkaert.events.sse.quarkus.subscriber;

import java.util.UUID;

public class Event {
    public String id;
    public String eventCode;
    public String payload;
    public String publicationDate;

    public Event() {
        this.id = UUID.randomUUID().toString();
    }

    public Event(String id, String eventCode, String payload) {
        this.id = id;
        this.eventCode = eventCode;
        this.payload = payload;
    }

    public Event(String eventCode, String payload) {
        this.id = UUID.randomUUID().toString();
        this.eventCode = eventCode;
        this.payload = payload;
    }

    public Event(Event eventToCopy) {
        this.id = eventToCopy.id;
        this.eventCode = eventToCopy.eventCode;
        this.payload = eventToCopy.payload;
    }
}
