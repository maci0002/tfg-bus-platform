package com.tfg.busplatform.booking.repository;

import com.tfg.busplatform.booking.model.Reservation;
import com.tfg.busplatform.booking.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCode(String code);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Reservas ya hechas para un viaje concreto (línea + fecha + hora de salida). */
    List<Reservation> findByLineIdAndTravelDateAndDepartureTimeAndStatusNot(
            Long lineId,
            LocalDate travelDate,
            LocalTime departureTime,
            ReservationStatus excludedStatus
    );

    /** Comprueba si una plaza está ocupada para un viaje concreto. */
    boolean existsByLineIdAndTravelDateAndDepartureTimeAndSeatCodeAndStatusNot(
            Long lineId,
            LocalDate travelDate,
            LocalTime departureTime,
            String seatCode,
            ReservationStatus excludedStatus
    );
}
