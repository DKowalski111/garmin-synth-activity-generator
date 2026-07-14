import React, { useMemo } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import { ActivitySample } from '../types/activity'

interface Props {
  samples: ActivitySample[]
}

export function PreviewCharts({ samples }: Props) {
  // Subsample for performance — show at most 500 points
  const data = useMemo(() => {
    if (samples.length <= 500) return samples
    const step = Math.ceil(samples.length / 500)
    return samples.filter((_, i) => i % step === 0)
  }, [samples])

  const elapsed = useMemo(() =>
    data.map((s, i) => ({
      t: i,
      speed: parseFloat((s.speedMetersPerSecond * 3.6).toFixed(2)),
      hr: s.heartRate,
      dist: parseFloat((s.distanceMeters / 1000).toFixed(3)),
    })),
    [data]
  )

  return (
    <div className="preview-charts panel">
      <h2>Activity Preview</h2>

      <h3>Speed (km/h)</h3>
      <ResponsiveContainer width="100%" height={180}>
        <LineChart data={elapsed} margin={{ left: 0, right: 10, top: 5, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
          <XAxis dataKey="t" hide />
          <YAxis domain={[0, 'auto']} width={40} unit=" km/h" tick={{ fontSize: 11 }} />
          <Tooltip formatter={(v) => [`${v} km/h`, 'Speed']} />
          <Line type="monotone" dataKey="speed" stroke="#e67e22" dot={false} strokeWidth={1.5} />
        </LineChart>
      </ResponsiveContainer>

      <h3>Heart Rate (BPM)</h3>
      <ResponsiveContainer width="100%" height={180}>
        <LineChart data={elapsed} margin={{ left: 0, right: 10, top: 5, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
          <XAxis dataKey="t" hide />
          <YAxis domain={[40, 220]} width={40} unit=" bpm" tick={{ fontSize: 11 }} />
          <Tooltip formatter={(v) => [`${v} bpm`, 'Heart Rate']} />
          <Line type="monotone" dataKey="hr" stroke="#e74c3c" dot={false} strokeWidth={1.5} />
        </LineChart>
      </ResponsiveContainer>

      <h3>Distance (km)</h3>
      <ResponsiveContainer width="100%" height={180}>
        <LineChart data={elapsed} margin={{ left: 0, right: 10, top: 5, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
          <XAxis dataKey="t" hide />
          <YAxis width={50} unit=" km" tick={{ fontSize: 11 }} />
          <Tooltip formatter={(v) => [`${v} km`, 'Distance']} />
          <Line type="monotone" dataKey="dist" stroke="#3498db" dot={false} strokeWidth={1.5} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
