import { useState, useCallback } from 'react'
import axios from 'axios'
import { ActivityConfig, ActivityPreview } from '../types/activity'
import { RouteResult } from '../types/route'

export function useActivityPreview(setError: (e: string | null) => void) {
  const [preview, setPreview] = useState<ActivityPreview | null>(null)
  const [isLoadingPreview, setIsLoadingPreview] = useState(false)

  const generatePreview = useCallback(async (route: RouteResult, config: ActivityConfig) => {
    setIsLoadingPreview(true)
    setError(null)
    try {
      const response = await axios.post<ActivityPreview>('/api/activities/preview', {
        activityName: config.activityName,
        sport: config.sport,
        route: { distanceMeters: route.distanceMeters, points: route.points },
        timeConfiguration: { mode: config.timeMode, selectedTime: config.selectedTime || null },
        averageSpeedKmh: config.sport === 'CYCLING' ? config.averageSpeedKmh : 1,
        averagePaceMinPerKm: config.sport === 'RUNNING' ? config.averagePaceMinPerKm : null,
        averageHeartRate: config.averageHeartRate,
        recordingIntervalSeconds: config.recordingIntervalSeconds,
        seed: config.seed,
        pauses: config.pauses.map(p => ({ offsetSeconds: p.offsetSeconds, durationSeconds: p.durationSeconds })),
        cadenceSpm: config.cadenceSpm,
      })
      setPreview(response.data)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } }; message?: string }
      const msg = err.response?.data?.message || err.message || 'Failed to generate preview'
      setError(msg)
    } finally {
      setIsLoadingPreview(false)
    }
  }, [setError])

  return { preview, setPreview, generatePreview, isLoadingPreview }
}
