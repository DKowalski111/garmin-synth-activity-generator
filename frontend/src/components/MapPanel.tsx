import React, { useEffect, useRef, useState, useCallback } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { RouteResult, LatLng } from '../types/route'

// Fix Leaflet default marker icon paths broken by Vite bundling
import markerIconUrl from 'leaflet/dist/images/marker-icon.png'
import markerIcon2xUrl from 'leaflet/dist/images/marker-icon-2x.png'
import markerShadowUrl from 'leaflet/dist/images/marker-shadow.png'
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconUrl: markerIconUrl,
  iconRetinaUrl: markerIcon2xUrl,
  shadowUrl: markerShadowUrl,
})

interface Props {
  onRouteCalculated: (selection: {
    start: LatLng | null
    end: LatLng | null
    waypoints: LatLng[]
  }) => void
  route: RouteResult | null
  isLoading: boolean
}

type ClickMode = 'start' | 'end' | 'waypoint'

const startIcon = L.divIcon({
  className: '',
  html: '<div style="background:#2ecc71;width:20px;height:20px;border-radius:50%;border:3px solid white;box-shadow:0 1px 4px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:bold;color:white">S</div>',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
})
const endIcon = L.divIcon({
  className: '',
  html: '<div style="background:#e74c3c;width:20px;height:20px;border-radius:50%;border:3px solid white;box-shadow:0 1px 4px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:bold;color:white">E</div>',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
})
const waypointIcon = (n: number) => L.divIcon({
  className: '',
  html: `<div style="background:#3498db;width:18px;height:18px;border-radius:50%;border:2px solid white;box-shadow:0 1px 4px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:bold;color:white">${n}</div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
})

export function MapPanel({ onRouteCalculated, route, isLoading }: Props) {
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<L.Map | null>(null)
  const markersRef = useRef<L.Marker[]>([])
  const polylineRef = useRef<L.Polyline | null>(null)
  const clickModeRef = useRef<ClickMode>('start')

  const [start, setStart] = useState<LatLng | null>(null)
  const [end, setEnd] = useState<LatLng | null>(null)
  const [waypoints, setWaypoints] = useState<LatLng[]>([])
  const [clickMode, setClickMode] = useState<ClickMode>('start')

  // keep ref in sync so map click handler never captures stale state
  useEffect(() => { clickModeRef.current = clickMode }, [clickMode])

  // Initialise map once
  useEffect(() => {
    if (!mapRef.current || mapInstanceRef.current) return
    const map = L.map(mapRef.current).setView([50.294, 18.665], 13)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map)

    map.on('click', (e: L.LeafletMouseEvent) => {
      const coord: LatLng = { latitude: e.latlng.lat, longitude: e.latlng.lng }
      const mode = clickModeRef.current
      if (mode === 'start') {
        setStart(coord)
        setClickMode('end')
      } else if (mode === 'end') {
        setEnd(coord)
        setClickMode('waypoint')
      } else {
        setWaypoints(prev => [...prev, coord])
      }
    })

    mapInstanceRef.current = map
  }, [])

  // Redraw markers whenever pins change
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!map) return
    markersRef.current.forEach(m => m.remove())
    markersRef.current = []
    if (start) markersRef.current.push(L.marker([start.latitude, start.longitude], { icon: startIcon }).addTo(map))
    waypoints.forEach((wp, i) => markersRef.current.push(L.marker([wp.latitude, wp.longitude], { icon: waypointIcon(i + 1) }).addTo(map)))
    if (end) markersRef.current.push(L.marker([end.latitude, end.longitude], { icon: endIcon }).addTo(map))
  }, [start, end, waypoints])

  // Draw route polyline
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!map) return
    if (polylineRef.current) { polylineRef.current.remove(); polylineRef.current = null }
    if (!route || route.points.length < 2) return
    const latlngs = route.points.map(p => [p.latitude, p.longitude] as [number, number])
    polylineRef.current = L.polyline(latlngs, { color: '#e67e22', weight: 4, opacity: 0.9 }).addTo(map)
    map.fitBounds(polylineRef.current.getBounds(), { padding: [40, 40] })
  }, [route])

  const handleClear = useCallback(() => {
    setStart(null)
    setEnd(null)
    setWaypoints([])
    setClickMode('start')
    if (polylineRef.current) { polylineRef.current.remove(); polylineRef.current = null }
  }, [])

  const handleCalculate = useCallback(() => {
    onRouteCalculated({ start, end, waypoints })
  }, [start, end, waypoints, onRouteCalculated])

  return (
    <div className="map-panel">
      <div className="map-toolbar">
        <span className="mode-indicator">
          Click to set: <strong>{clickMode === 'start' ? 'Start' : clickMode === 'end' ? 'End' : 'Waypoint'}</strong>
        </span>
        <div className="map-status">
          {start && <span className="badge green">Start set</span>}
          {end && <span className="badge red">End set</span>}
          {waypoints.length > 0 && <span className="badge blue">{waypoints.length} waypoint(s)</span>}
        </div>
        <button onClick={handleCalculate} disabled={!start || !end || isLoading} className="btn-primary">
          {isLoading ? 'Calculating…' : 'Calculate Route'}
        </button>
        <button onClick={handleClear} className="btn-secondary">Clear</button>
      </div>
      <div ref={mapRef} className="map-container" />
      {route && (
        <div className="route-info">
          Route: {(route.distanceMeters / 1000).toFixed(1)} km · {route.points.length} points
        </div>
      )}
    </div>
  )
}
