import React, { useState, useCallback } from 'react'
import { MapPanel } from './components/MapPanel'
import { ConfigPanel } from './components/ConfigPanel'
import { SummaryPanel } from './components/SummaryPanel'
import { PreviewCharts } from './components/PreviewCharts'
import { ActivityActions } from './components/ActivityActions'
import { useRoute } from './hooks/useRoute'
import { useActivityPreview } from './hooks/useActivityPreview'
import { ActivityConfig, defaultConfig } from './types/activity'
import { RouteResult } from './types/route'
import './styles/global.css'
import './styles/app.css'

export default function App() {
  const [config, setConfig] = useState<ActivityConfig>(defaultConfig)
  const [error, setError] = useState<string | null>(null)

  const { route, setRoute, calculateRoute, isLoadingRoute } = useRoute(setError)
  const { preview, setPreview, generatePreview, isLoadingPreview } = useActivityPreview(setError)

  const handleGeneratePreview = useCallback(async () => {
    if (!route) { setError('Please calculate a route first.'); return }
    await generatePreview(route, config)
  }, [route, config, generatePreview])

  const handleDownloadFit = useCallback(async () => {
    if (!route) { setError('Please calculate a route first.'); return }
    setError(null)
    try {
      const response = await fetch('/api/activities/fit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(buildActivityRequest(route, config)),
      })
      if (!response.ok) {
        const err = await response.json() as { message?: string }
        throw new Error(err.message || 'FIT generation failed')
      }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = (config.activityName || 'synthetic-activity').replace(/\s+/g, '_') + '.fit'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e: unknown) {
      const err = e as { message?: string }
      setError(err.message ?? 'Unknown error')
    }
  }, [route, config])

  const handleReset = useCallback(() => {
    setConfig(defaultConfig)
    setRoute(null)
    setPreview(null)
    setError(null)
  }, [setRoute, setPreview])

  return (
    <div className="app">
      <header className="app-header">
        <h1>Synthetic FIT Generator</h1>
        <p>Generate realistic Garmin-compatible activity files for testing</p>
      </header>

      {error && (
        <div className="error-banner">
          <span>{error}</span>
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}

      <div className="app-body">
        <div className="left-panel">
          <MapPanel
            onRouteCalculated={calculateRoute}
            route={route}
            isLoading={isLoadingRoute}
          />
          <ActivityActions
            hasRoute={!!route}
            hasPreview={!!preview}
            isLoadingPreview={isLoadingPreview}
            onPreview={handleGeneratePreview}
            onDownloadFit={handleDownloadFit}
            onReset={handleReset}
          />
        </div>
        <div className="right-panel">
          <ConfigPanel config={config} onChange={setConfig} />
          {preview && <SummaryPanel summary={preview.summary} sport={preview.sport} />}
          {preview && <PreviewCharts samples={preview.samples} sport={preview.sport} />}
        </div>
      </div>
    </div>
  )
}

function buildActivityRequest(route: RouteResult, config: ActivityConfig) {
  return {
    activityName: config.activityName,
    sport: config.sport,
    route: { distanceMeters: route.distanceMeters, points: route.points },
    timeConfiguration: { mode: config.timeMode, selectedTime: config.selectedTime || null },
    averageSpeedKmh: config.sport === 'CYCLING' ? config.averageSpeedKmh : 1, // backend uses pace for running
    averagePaceMinPerKm: config.sport === 'RUNNING' ? config.averagePaceMinPerKm : null,
    averageHeartRate: config.averageHeartRate,
    recordingIntervalSeconds: config.recordingIntervalSeconds,
    seed: config.seed,
    pauses: config.pauses.map(p => ({ offsetSeconds: p.offsetSeconds, durationSeconds: p.durationSeconds })),
    cadenceSpm: config.cadenceSpm,
  }
}
