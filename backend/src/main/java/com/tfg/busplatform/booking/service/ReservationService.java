package com.tfg.busplatform.booking.service;

import com.tfg.busplatform.booking.dto.CreateReservationRequest;
import com.tfg.busplatform.booking.dto.PaymentRequest;
import com.tfg.busplatform.booking.dto.ReservationDto;
import com.tfg.busplatform.booking.dto.SeatDto;
import com.tfg.busplatform.booking.dto.SeatMapDto;
import com.tfg.busplatform.booking.dto.TicketVerificationDto;
import com.tfg.busplatform.booking.model.Reservation;
import com.tfg.busplatform.booking.model.ReservationStatus;
import com.tfg.busplatform.booking.model.SeatLayout;
import com.tfg.busplatform.booking.repository.ReservationRepository;
import com.tfg.busplatform.model.User;
import com.tfg.busplatform.repository.UserRepository;
import com.tfg.busplatform.transport.model.Line;
import com.tfg.busplatform.transport.model.LineStop;
import com.tfg.busplatform.transport.model.Stop;
import com.tfg.busplatform.transport.repository.LineRepository;
import com.tfg.busplatform.transport.repository.StopRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationRepository    reservationRepository;
    private final LineRepository           lineRepository;
    private final StopRepository           stopRepository;
    private final UserRepository           userRepository;
    private final PricingService           pricingService;
    private final SimulatedPaymentGateway  paymentGateway;
    private final QrCodeService            qrCodeService;

    // ── Mapa de asientos para un viaje concreto ──────────────────────────────

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(Long lineId, String originCode, String destinationCode,
                                 LocalDate travelDate, LocalTime departureTime) {

        Line line = lineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Línea no encontrada: " + lineId));
        Stop origin      = stopRepository.findByCode(originCode)
                .orElseThrow(() -> new EntityNotFoundException("Parada origen no encontrada: " + originCode));
        Stop destination = stopRepository.findByCode(destinationCode)
                .orElseThrow(() -> new EntityNotFoundException("Parada destino no encontrada: " + destinationCode));

        TripContext ctx = computeTripContext(line, origin, destination, departureTime);

        // Asientos ocupados para este viaje (línea + fecha + hora exacta)
        Set<String> occupied = new HashSet<>();
        reservationRepository
                .findByLineIdAndTravelDateAndDepartureTimeAndStatusNot(
                        lineId, travelDate, ctx.actualDepartureFromOrigin, ReservationStatus.CANCELLED)
                .forEach(r -> occupied.add(r.getSeatCode()));

        List<SeatDto> seats = SeatLayout.allSeatCodes().stream()
                .map(code -> {
                    int rowEnd = code.length() - 1;
                    return SeatDto.builder()
                            .code(code)
                            .row(Integer.parseInt(code.substring(0, rowEnd)))
                            .column(String.valueOf(code.charAt(rowEnd)))
                            .occupied(occupied.contains(code))
                            .build();
                })
                .toList();

        return SeatMapDto.builder()
                .lineId(line.getId())
                .lineCode(line.getCode())
                .lineName(line.getName())
                .travelDate(travelDate)
                .departureTime(ctx.actualDepartureFromOrigin.format(HHMM))
                .arrivalTime(ctx.arrivalAtDestination.format(HHMM))
                .durationMinutes(ctx.durationMinutes)
                .originStop(origin.getName())
                .destinationStop(destination.getName())
                .price(pricingService.calculatePrice(ctx.durationMinutes))
                .rows(SeatLayout.ROWS)
                .columns(List.of("A", "B", "C", "D"))
                .seats(seats)
                .build();
    }

    // ── Creación de reserva ──────────────────────────────────────────────────

    public ReservationDto create(CreateReservationRequest req, String userEmail) {

        if (!SeatLayout.isValidSeatCode(req.getSeatCode())) {
            throw new IllegalArgumentException("Código de asiento no válido: " + req.getSeatCode());
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Line line = lineRepository.findById(req.getLineId())
                .orElseThrow(() -> new EntityNotFoundException("Línea no encontrada"));
        Stop origin      = stopRepository.findByCode(req.getOriginStopCode())
                .orElseThrow(() -> new EntityNotFoundException("Origen no encontrado"));
        Stop destination = stopRepository.findByCode(req.getDestinationStopCode())
                .orElseThrow(() -> new EntityNotFoundException("Destino no encontrado"));

        TripContext ctx = computeTripContext(line, origin, destination, req.getDepartureTime());

        // ¿La plaza ya está reservada?
        boolean taken = reservationRepository
                .existsByLineIdAndTravelDateAndDepartureTimeAndSeatCodeAndStatusNot(
                        line.getId(), req.getTravelDate(),
                        ctx.actualDepartureFromOrigin, req.getSeatCode(),
                        ReservationStatus.CANCELLED);

        if (taken) {
            throw new IllegalStateException("El asiento " + req.getSeatCode()
                    + " ya está reservado para este viaje. Elige otro.");
        }

        Reservation reservation = Reservation.builder()
                .code("TMP")  // se sobrescribe tras guardar para incluir el id
                .user(user)
                .line(line)
                .originStop(origin)
                .destinationStop(destination)
                .travelDate(req.getTravelDate())
                .departureTime(ctx.actualDepartureFromOrigin)
                .arrivalTime(ctx.arrivalAtDestination)
                .seatCode(req.getSeatCode())
                .status(ReservationStatus.PENDING_PAYMENT)
                .price(pricingService.calculatePrice(ctx.durationMinutes))
                .build();

        Reservation saved = reservationRepository.save(reservation);
        saved.setCode(buildReservationCode(saved.getId(), saved.getTravelDate().getYear()));
        return toDto(saved);
    }

    // ── Listado y detalle ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReservationDto> getMyReservations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ReservationDto getById(Long id, String userEmail) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        ensureOwnership(r, userEmail);
        return toDto(r);
    }

    // ── Cancelación ──────────────────────────────────────────────────────────

    public ReservationDto cancel(Long id, String userEmail) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        ensureOwnership(r, userEmail);

        if (r.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("La reserva ya estaba cancelada");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        return toDto(r);
    }

    // ── Pago simulado ────────────────────────────────────────────────────────

    /**
     * Procesa el pago simulado de una reserva pendiente. Si la pasarela autoriza
     * el cargo, la reserva pasa a CONFIRMED, se registran el momento del pago y los
     * últimos 4 dígitos de la tarjeta, y se genera el token del ticket (QR).
     */
    public ReservationDto pay(Long id, PaymentRequest req, String userEmail) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        ensureOwnership(r, userEmail);

        if (r.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("No se puede pagar una reserva cancelada");
        }
        if (r.getStatus() == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("La reserva ya estaba pagada");
        }

        SimulatedPaymentGateway.PaymentResult result = paymentGateway.authorize(req);
        if (!result.approved()) {
            throw new IllegalStateException(result.declineReason());
        }

        r.setStatus(ReservationStatus.CONFIRMED);
        r.setPaidAt(LocalDateTime.now());
        r.setCardLast4(result.cardLast4());
        r.setQrToken(UUID.randomUUID().toString());
        return toDto(r);
    }

    // ── Ticket electrónico (QR) ──────────────────────────────────────────────

    /** Devuelve el PNG del QR del ticket; solo para reservas confirmadas y del propio dueño. */
    @Transactional(readOnly = true)
    public byte[] getQrPng(Long id, String userEmail) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        ensureOwnership(r, userEmail);
        if (r.getStatus() != ReservationStatus.CONFIRMED || r.getQrToken() == null) {
            throw new IllegalStateException("El ticket aún no está disponible: la reserva no está pagada");
        }
        return qrCodeService.pngFor(r);
    }

    /** Verificación pública de un ticket a partir del código y el token del QR. */
    @Transactional(readOnly = true)
    public TicketVerificationDto verify(String code, String token) {
        Reservation r = reservationRepository.findByCode(code).orElse(null);
        if (r == null) {
            return TicketVerificationDto.builder().valid(false).reason("Ticket no encontrado").build();
        }
        if (r.getStatus() != ReservationStatus.CONFIRMED || r.getQrToken() == null
                || !r.getQrToken().equals(token)) {
            String reason = r.getStatus() == ReservationStatus.CANCELLED
                    ? "Ticket cancelado"
                    : (r.getStatus() != ReservationStatus.CONFIRMED ? "Ticket no pagado" : "Token no válido");
            return TicketVerificationDto.builder()
                    .valid(false).reason(reason).code(r.getCode()).status(r.getStatus().name())
                    .build();
        }
        return TicketVerificationDto.builder()
                .valid(true)
                .code(r.getCode())
                .status(r.getStatus().name())
                .lineCode(r.getLine().getCode())
                .lineName(r.getLine().getName())
                .originStop(r.getOriginStop().getName())
                .destinationStop(r.getDestinationStop().getName())
                .travelDate(r.getTravelDate())
                .departureTime(r.getDepartureTime().format(HHMM))
                .seatCode(r.getSeatCode())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void ensureOwnership(Reservation r, String userEmail) {
        if (!r.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("No tienes acceso a esta reserva");
        }
    }

    /**
     * Para una línea y un par origen/destino, calcula la hora real de salida desde el origen,
     * la hora real de llegada al destino y la duración del trayecto.
     *
     * El parámetro `requestedDeparture` puede ser:
     *   a) la hora base de la línea (HH:mm desde el origen de la ruta), o
     *   b) la hora real desde la parada origen (ya pre-calculada por el planner).
     * Detectamos cuál es comparando con las horas de salida configuradas en la línea.
     */
    private TripContext computeTripContext(Line line, Stop origin, Stop destination, LocalTime requestedDeparture) {
        LineStop originLs = line.getStops().stream()
                .filter(ls -> ls.getStop().getId().equals(origin.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La parada " + origin.getName() + " no pertenece a la línea " + line.getCode()));
        LineStop destLs = line.getStops().stream()
                .filter(ls -> ls.getStop().getId().equals(destination.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La parada " + destination.getName() + " no pertenece a la línea " + line.getCode()));
        if (originLs.getSequence() >= destLs.getSequence()) {
            throw new IllegalArgumentException("El origen debe ir antes que el destino en el recorrido");
        }

        // Determinar la salida base de la línea coherente con la hora pedida desde origen
        LocalTime baseDeparture = line.getDepartureTimes().stream()
                .filter(t -> t.plusMinutes(originLs.getMinutesFromStart()).equals(requestedDeparture))
                .findFirst()
                .orElseGet(() -> {
                    // Si no coincide, asumimos que `requestedDeparture` es la salida base
                    if (line.getDepartureTimes().contains(requestedDeparture)) return requestedDeparture;
                    throw new IllegalArgumentException(
                            "La hora de salida solicitada no coincide con ninguna salida programada de la línea " + line.getCode());
                });

        LocalTime actualDepartureFromOrigin = baseDeparture.plusMinutes(originLs.getMinutesFromStart());
        LocalTime arrivalAtDestination      = baseDeparture.plusMinutes(destLs.getMinutesFromStart());
        int duration = destLs.getMinutesFromStart() - originLs.getMinutesFromStart();

        return new TripContext(actualDepartureFromOrigin, arrivalAtDestination, duration);
    }

    private String buildReservationCode(Long id, int year) {
        return String.format("BK-%d-%05d", year, id);
    }

    private ReservationDto toDto(Reservation r) {
        int durationMinutes = (int) java.time.Duration.between(r.getDepartureTime(), r.getArrivalTime()).toMinutes();
        return ReservationDto.builder()
                .id(r.getId())
                .code(r.getCode())
                .lineId(r.getLine().getId())
                .lineCode(r.getLine().getCode())
                .lineName(r.getLine().getName())
                .lineColor(r.getLine().getColor())
                .originStop(r.getOriginStop().getName())
                .destinationStop(r.getDestinationStop().getName())
                .travelDate(r.getTravelDate())
                .departureTime(r.getDepartureTime().format(HHMM))
                .arrivalTime(r.getArrivalTime().format(HHMM))
                .durationMinutes(durationMinutes)
                .seatCode(r.getSeatCode())
                .status(r.getStatus().name())
                .price(r.getPrice())
                .paidAt(r.getPaidAt())
                .cardLast4(r.getCardLast4())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private record TripContext(LocalTime actualDepartureFromOrigin,
                               LocalTime arrivalAtDestination,
                               int durationMinutes) {}
}
