package com.hospitality.rsm.publisher;

import com.hospitality.rsm.event.RoomServiceRequestedEvent;

public interface RoomServiceEventPublisher {

    void publishRoomServiceRequested(RoomServiceRequestedEvent event);
}
