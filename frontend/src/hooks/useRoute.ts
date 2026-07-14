import { useState, useCallback } from 'react'
import axios from 'axios'
import { RouteResult } from '../types/route'

interface MapSelection {
  start: { latitude: number; longitude: number } | null
  end: { latitude: number; longitude: number } | null
  waypoints: { latitude: number; longitude: number }[]
}

export function useRoute(setError: (e: string | null) => void) {
  const [route, setRoute] = useState<RouteResult | null>(null)
  const [isLoadingRoute, setIsLoadingRoute] = useState(false)

  const calculateRoute = useCallback(async (selection: MapSelection) => {
    if (!selection.start || !selection.end) {
      setError('Please select both a start and end point on the map.')
      return
    }
    setIsLoadingRoute(true)
    setError(null)
    try {
      const response = await axios.post<RouteResult>('/api/routes', {
        start: selection.start,
        end: selection.end,
        waypoints: selection.waypoints,
      })
      setRoute(response.data)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } }; message?: string }
      const msg = err.response?.data?.message || err.message || 'Failed to calculate route'
      setError(msg)
    } finally {
      setIsLoadingRoute(false)
    }
  }, [setError])

  return { route, setRoute, calculateRoute, isLoadingRoute }
}
