package org.tiny.whiterun;

import javafx.event.Event;
import javafx.event.EventType;

public class MyEvent extends Event {
    public MyEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
