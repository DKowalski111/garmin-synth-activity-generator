import React from 'react'
import { ActivitySummary } from '../types/activity'

interface Props {
  summary: ActivitySummary
}

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  return h > 0 ? `${h}h ${m}m ${s}s` : `${m}m ${s}s`
}

function formatSpeed(mps: number): string {
  return (mps * 3.6).toFixed(1) + ' km/h'
}

function formatDist(m: number): string {
  return m >= 1000 ? (m / 1000).toFixed(2) + ' km' : m.toFixed(0) + ' m'
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString()
}

export function SummaryPanel({ summary }: Props) {
  return (
    <div className="summary-panel panel">
      <h2>Activity Summary</h2>
      <dl className="summary-grid">
        <SummaryRow label="Start Time" value={formatTime(summary.startTime)} />
        <SummaryRow label="End Time" value={formatTime(summary.endTime)} />
        <SummaryRow label="Distance" value={formatDist(summary.distanceMeters)} />
        <SummaryRow label="Moving Duration" value={formatDuration(summary.movingDurationSeconds)} />
        <SummaryRow label="Pause Duration" value={formatDuration(summary.totalPauseDurationSeconds)} />
        <SummaryRow label="Elapsed Duration" value={formatDuration(summary.elapsedDurationSeconds)} />
        <SummaryRow label="Avg Speed" value={formatSpeed(summary.averageSpeedMps)} />
        <SummaryRow label="Max Speed" value={formatSpeed(summary.maxSpeedMps)} />
        <SummaryRow label="Avg Heart Rate" value={summary.averageHeartRate + ' BPM'} />
        <SummaryRow label="Max Heart Rate" value={summary.maxHeartRate + ' BPM'} />
        <SummaryRow label="Calories" value={summary.totalCalories + ' kcal'} />
        <SummaryRow label="Total Samples" value={summary.sampleCount.toString()} />
      </dl>
    </div>
  )
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </>
  )
}
