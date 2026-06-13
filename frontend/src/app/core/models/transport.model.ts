export interface Stop {
  id: number;
  code: string;
  name: string;
  municipality?: string;
  latitude: number;
  longitude: number;
}

export interface LineSummary {
  id: number;
  code: string;
  name: string;
  color: string;
  originName: string;
  destinationName: string;
  totalStops: number;
  durationMinutes: number;
}

export interface LineStopInfo {
  stopId: number;
  code: string;
  name: string;
  latitude: number;
  longitude: number;
  sequence: number;
  minutesFromStart: number;
}

export interface LineDetail {
  id: number;
  code: string;
  name: string;
  color: string;
  originName: string;
  destinationName: string;
  durationMinutes: number;
  departureTimes: string[];
  stops: LineStopInfo[];
}

export interface TripSearchResult {
  lineId: number;
  lineCode: string;
  lineName: string;
  lineColor: string;
  originStop: string;
  destinationStop: string;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
  stops: LineStopInfo[];
  /** [lat, lng] para pintar polyline en el mapa */
  coordinates: [number, number][];
}

export interface TripSearchParams {
  origin: string;
  destination: string;
  date?: string;
  time?: string;
}
