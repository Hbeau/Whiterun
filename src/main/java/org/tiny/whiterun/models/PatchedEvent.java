package org.tiny.whiterun.models;

import javafx.event.Event;
import javafx.event.EventType;

public class PatchedEvent extends Event {
    public PatchedEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
