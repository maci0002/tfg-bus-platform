package com.tfg.busplatform.booking.model;

public enum ReservationStatus {
    /** Reserva creada, pendiente de pago (Iteración 3). */
    PENDING_PAYMENT,
    /** Reserva pagada y confirmada (se completará en Iteración 4 con el flujo de pago). */
    CONFIRMED,
    /** Reserva cancelada por el usuario. */
    CANCELLED
}
