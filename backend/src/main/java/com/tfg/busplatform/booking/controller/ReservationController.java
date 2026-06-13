package com.tfg.busplatform.booking.controller;

import com.tfg.busplatform.booking.dto.CreateReservationRequest;
import com.tfg.busplatform.booking.dto.PaymentRequest;
import com.tfg.busplatform.booking.dto.ReservationDto;
import com.tfg.busplatform.booking.dto.SeatMapDto;
import com.tfg.busplatform.booking.dto.TicketVerificationDto;
import com.tfg.busplatform.booking.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** Mapa de asientos para un viaje concreto (público para previsualizar antes de logar). */
    @GetMapping("/seats")
    public ResponseEntity<SeatMapDto> getSeatMap(
            @RequestParam Long lineId,
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime time
    ) {
        return ResponseEntity.ok(reservationService.getSeatMap(lineId, origin, destination, date, time));
    }

    /** Crear nueva reserva (requiere autenticación). */
    @PostMapping
    public ResponseEntity<ReservationDto> create(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.create(request, userDetails.getUsername()));
    }

    /** Listado de mis reservas. */
    @GetMapping("/my")
    public ResponseEntity<List<ReservationDto>> getMy(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.getMyReservations(userDetails.getUsername()));
    }

    /** Detalle de una reserva (solo el dueño). */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.getById(id, userDetails.getUsername()));
    }

    /** Cancelar reserva (solo el dueño). */
    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationDto> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.cancel(id, userDetails.getUsername()));
    }

    /** Pago simulado de una reserva pendiente (solo el dueño). */
    @PostMapping("/{id}/pay")
    public ResponseEntity<ReservationDto> pay(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.pay(id, request, userDetails.getUsername()));
    }

    /** Imagen PNG del código QR del ticket (solo reservas confirmadas del dueño). */
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        byte[] png = reservationService.getQrPng(id, userDetails.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(png);
    }

    /** Verificación pública del ticket a partir del contenido del QR (código + token). */
    @GetMapping("/verify")
    public ResponseEntity<TicketVerificationDto> verify(
            @RequestParam String code,
            @RequestParam String token
    ) {
        return ResponseEntity.ok(reservationService.verify(code, token));
    }
}
