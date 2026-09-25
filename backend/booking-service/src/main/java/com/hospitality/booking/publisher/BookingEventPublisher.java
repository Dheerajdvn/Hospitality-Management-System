package com.hospitality.booking.publisher;

import com.hospitality.booking.event.BookingCancelledEvent;
import com.hospitality.booking.event.BookingConfirmedEvent;
import com.hospitality.booking.event.BookingCreatedEvent;
import com.hospitality.booking.event.BookingExpiredEvent;

public interface BookingEventPublisher {

    void publishBookingCreated(BookingCreatedEvent event);

    void publishBookingConfirmed(BookingConfirmedEvent event);

    void publishBookingCancelled(BookingCancelledEvent event);

    void publishBookingExpired(BookingExpiredEvent event);
}

