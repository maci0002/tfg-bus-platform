package com.tfg.busplatform.booking.service;

import com.tfg.busplatform.booking.dto.PaymentRequest;
import com.tfg.busplatform.booking.dto.ReservationDto;
import com.tfg.busplatform.booking.dto.TicketVerificationDto;
import com.tfg.busplatform.booking.model.Reservation;
import com.tfg.busplatform.booking.model.ReservationStatus;
import com.tfg.busplatform.booking.repository.ReservationRepository;
import com.tfg.busplatform.model.User;
import com.tfg.busplatform.notification.EmailService;
import com.tfg.busplatform.repository.UserRepository;
import com.tfg.busplatform.transport.model.Line;
import com.tfg.busplatform.transport.model.Stop;
import com.tfg.busplatform.transport.repository.LineRepository;
import com.tfg.busplatform.transport.repository.StopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica de negocio de las reservas con repositorios y servicios
 * simulados (Mockito): máquina de estados de pago y cancelación, comprobación de
 * propiedad de la reserva y verificación del ticket electrónico.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final String OWNER = "alice@example.com";

    @Mock private ReservationRepository reservationRepository;
    @Mock private LineRepository lineRepository;
    @Mock private StopRepository stopRepository;
    @Mock private UserRepository userRepository;
    @Mock private PricingService pricingService;
    @Mock private SimulatedPaymentGateway paymentGateway;
    @Mock private QrCodeService qrCodeService;
    @Mock private EmailService emailService;

    @InjectMocks private ReservationService reservationService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Reservation reservation(ReservationStatus status, String ownerEmail, String qrToken) {
        User user = User.builder().email(ownerEmail).build();
        Line line = Line.builder().code("L05").name("Jaén - Martos").color("#7B1FA2").build();
        Stop origin = Stop.builder().code("JAEN").name("Jaén").build();
        Stop destination = Stop.builder().code("MAR").name("Martos").build();
        return Reservation.builder()
                .id(1L)
                .code("BK-2026-00001")
                .user(user)
                .line(line)
                .originStop(origin)
                .destinationStop(destination)
                .travelDate(LocalDate.of(2026, 7, 1))
                .departureTime(LocalTime.of(7, 0))
                .arrivalTime(LocalTime.of(7, 30))
                .seatCode("5B")
                .status(status)
                .price(new BigDecimal("6.60"))
                .qrToken(qrToken)
                .build();
    }

    private PaymentRequest validPaymentRequest() {
        return new PaymentRequest("Titular de Prueba", "4242424242424242", 12, 2099, "123");
    }

    // ── Verificación del ticket ──────────────────────────────────────────────

    @Test
    @DisplayName("verify: un ticket confirmado con el token correcto es válido")
    void verifyValidTicket() {
        when(reservationRepository.findByCode("BK-2026-00001"))
                .thenReturn(Optional.of(reservation(ReservationStatus.CONFIRMED, OWNER, "tok-123")));

        TicketVerificationDto dto = reservationService.verify("BK-2026-00001", "tok-123");

        assertThat(dto.isValid()).isTrue();
        assertThat(dto.getCode()).isEqualTo("BK-2026-00001");
        assertThat(dto.getLineCode()).isEqualTo("L05");
        assertThat(dto.getOriginStop()).isEqualTo("Jaén");
        assertThat(dto.getDestinationStop()).isEqualTo("Martos");
        assertThat(dto.getSeatCode()).isEqualTo("5B");
    }

    @Test
    @DisplayName("verify: un código inexistente devuelve no válido")
    void verifyTicketNotFound() {
        when(reservationRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        TicketVerificationDto dto = reservationService.verify("NOPE", "x");

        assertThat(dto.isValid()).isFalse();
        assertThat(dto.getReason()).isEqualTo("Ticket no encontrado");
    }

    @Test
    @DisplayName("verify: un ticket cancelado no es válido")
    void verifyCancelledTicket() {
        when(reservationRepository.findByCode("BK-2026-00001"))
                .thenReturn(Optional.of(reservation(ReservationStatus.CANCELLED, OWNER, "tok-123")));

        TicketVerificationDto dto = reservationService.verify("BK-2026-00001", "tok-123");

        assertThat(dto.isValid()).isFalse();
        assertThat(dto.getReason()).isEqualTo("Ticket cancelado");
    }

    @Test
    @DisplayName("verify: un token que no coincide no es válido")
    void verifyWrongToken() {
        when(reservationRepository.findByCode("BK-2026-00001"))
                .thenReturn(Optional.of(reservation(ReservationStatus.CONFIRMED, OWNER, "tok-123")));

        TicketVerificationDto dto = reservationService.verify("BK-2026-00001", "otro-token");

        assertThat(dto.isValid()).isFalse();
        assertThat(dto.getReason()).isEqualTo("Token no válido");
    }

    @Test
    @DisplayName("verify: un ticket pendiente de pago no es válido")
    void verifyPendingTicket() {
        when(reservationRepository.findByCode("BK-2026-00001"))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, OWNER, null)));

        TicketVerificationDto dto = reservationService.verify("BK-2026-00001", "x");

        assertThat(dto.isValid()).isFalse();
        assertThat(dto.getReason()).isEqualTo("Ticket no pagado");
    }

    // ── Cancelación ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: cancela una reserva del propio usuario")
    void cancelOwnReservation() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, OWNER, null)));

        ReservationDto dto = reservationService.cancel(1L, OWNER);

        assertThat(dto.getStatus()).isEqualTo(ReservationStatus.CANCELLED.name());
    }

    @Test
    @DisplayName("cancel: no se puede cancelar una reserva ya cancelada")
    void cancelAlreadyCancelled() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation(ReservationStatus.CANCELLED, OWNER, null)));

        assertThatThrownBy(() -> reservationService.cancel(1L, OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya estaba cancelada");
    }

    @Test
    @DisplayName("cancel: un usuario no puede cancelar la reserva de otro")
    void cancelNotOwner() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, OWNER, null)));

        assertThatThrownBy(() -> reservationService.cancel(1L, "bob@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── Pago simulado ────────────────────────────────────────────────────────

    @Test
    @DisplayName("pay: un pago aprobado confirma la reserva y envía el correo")
    void payApproved() {
        Reservation r = reservation(ReservationStatus.PENDING_PAYMENT, OWNER, null);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
        when(paymentGateway.authorize(any()))
                .thenReturn(new SimulatedPaymentGateway.PaymentResult(true, null, "4242"));
        when(qrCodeService.pngFor(any())).thenReturn(new byte[]{1, 2, 3});

        ReservationDto dto = reservationService.pay(1L, validPaymentRequest(), OWNER);

        assertThat(dto.getStatus()).isEqualTo(ReservationStatus.CONFIRMED.name());
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(r.getPaidAt()).isNotNull();
        assertThat(r.getCardLast4()).isEqualTo("4242");
        assertThat(r.getQrToken()).isNotNull();
        verify(emailService, times(1)).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("pay: un pago rechazado deja la reserva pendiente y no envía correo")
    void payDeclined() {
        Reservation r = reservation(ReservationStatus.PENDING_PAYMENT, OWNER, null);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
        when(paymentGateway.authorize(any()))
                .thenReturn(new SimulatedPaymentGateway.PaymentResult(false, "Pago rechazado", null));

        assertThatThrownBy(() -> reservationService.pay(1L, validPaymentRequest(), OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pago rechazado");

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("pay: no se puede volver a pagar una reserva ya confirmada")
    void payAlreadyConfirmed() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation(ReservationStatus.CONFIRMED, OWNER, "tok-123")));

        assertThatThrownBy(() -> reservationService.pay(1L, validPaymentRequest(), OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya estaba pagada");

        verify(paymentGateway, never()).authorize(any());
    }

    // ── Ticket (QR) ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getQrPng: el ticket no está disponible si la reserva no está pagada")
    void qrUnavailableWhenNotPaid() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, OWNER, null)));

        assertThatThrownBy(() -> reservationService.getQrPng(1L, OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está pagada");
    }
}
