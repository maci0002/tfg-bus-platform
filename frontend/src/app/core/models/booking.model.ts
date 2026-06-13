export type ReservationStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'CANCELLED';

export interface Seat {
  code: string;       // "5B"
  row: number;        // 5
  column: string;     // "B"
  occupied: boolean;
}

export interface SeatMap {
  lineId: number;
  lineCode: string;
  lineName: string;
  travelDate: string;        // "YYYY-MM-DD"
  departureTime: string;     // "HH:mm"
  arrivalTime: string;       // "HH:mm"
  durationMinutes: number;
  originStop: string;
  destinationStop: string;
  price: number;
  rows: number;
  columns: string[];
  seats: Seat[];
}

export interface CreateReservationRequest {
  lineId: number;
  originStopCode: string;
  destinationStopCode: string;
  travelDate: string;        // "YYYY-MM-DD"
  departureTime: string;     // "HH:mm"
  seatCode: string;
}

export interface Reservation {
  id: number;
  code: string;
  lineId: number;
  lineCode: string;
  lineName: string;
  lineColor: string;
  originStop: string;
  destinationStop: string;
  travelDate: string;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
  seatCode: string;
  status: ReservationStatus;
  price: number;
  paidAt?: string | null;
  cardLast4?: string | null;
  createdAt: string;
}

/** Datos del pago simulado de una reserva. */
export interface PaymentRequest {
  cardHolder: string;
  cardNumber: string;
  expiryMonth: number;
  expiryYear: number;
  cvv: string;
}

/** Resultado de la verificación pública de un ticket (contenido del QR). */
export interface TicketVerification {
  valid: boolean;
  reason?: string;
  code?: string;
  status?: string;
  lineCode?: string;
  lineName?: string;
  originStop?: string;
  destinationStop?: string;
  travelDate?: string;
  departureTime?: string;
  seatCode?: string;
}

/** Datos del trayecto seleccionado en el planificador, almacenados en BookingFlowService. */
export interface SelectedTrip {
  lineId: number;
  lineCode: string;
  lineName: string;
  lineColor: string;
  originStopName: string;
  originStopCode: string;
  destinationStopName: string;
  destinationStopCode: string;
  travelDate: string;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
}
