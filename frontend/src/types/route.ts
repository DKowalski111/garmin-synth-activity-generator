export interface LatLng {
  latitude: number
  longitude: number
}

export interface RouteResult {
  distanceMeters: number
  encodedPolyline: string
  points: LatLng[]
}

export interface MapWaypoint {
  position: LatLng
  type: 'start' | 'end' | 'waypoint'
}
