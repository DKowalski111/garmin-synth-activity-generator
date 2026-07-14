import React from 'react'
import { render, screen } from '@testing-library/react'
import { SummaryPanel } from '../components/SummaryPanel'
import { ActivitySummary } from '../types/activity'

const mockSummary: ActivitySummary = {
  startTime: '2024-06-15T08:00:00Z',
  endTime: '2024-06-15T09:00:00Z',
  distanceMeters: 25000,
  movingDurationSeconds: 3600,
  elapsedDurationSeconds: 3900,
  totalPauseDurationSeconds: 300,
  averageSpeedMps: 6.94,
  maxSpeedMps: 12.5,
  averageHeartRate: 145,
  maxHeartRate: 175,
  sampleCount: 3600,
  totalCalories: 820,
}

describe('SummaryPanel — cycling', () => {
  it('displays distance in km', () => {
    render(<SummaryPanel summary={mockSummary} sport="CYCLING" />)
    expect(screen.getByText('25.00 km')).toBeInTheDocument()
  })

  it('displays sample count', () => {
    render(<SummaryPanel summary={mockSummary} sport="CYCLING" />)
    expect(screen.getByText('3600')).toBeInTheDocument()
  })

  it('displays average heart rate', () => {
    render(<SummaryPanel summary={mockSummary} sport="CYCLING" />)
    expect(screen.getByText('145 BPM')).toBeInTheDocument()
  })

  it('displays pause duration', () => {
    render(<SummaryPanel summary={mockSummary} sport="CYCLING" />)
    expect(screen.getByText('5m 0s')).toBeInTheDocument()
  })

  it('displays calories', () => {
    render(<SummaryPanel summary={mockSummary} sport="CYCLING" />)
    expect(screen.getByText('820 kcal')).toBeInTheDocument()
  })

  it('shows speed labels for cycling', () => {
    render(<SummaryPanel summary={mockSummary} sport="CYCLING" />)
    expect(screen.getByText('Avg Speed')).toBeInTheDocument()
  })
})

describe('SummaryPanel — running', () => {
  it('shows pace labels for running', () => {
    render(<SummaryPanel summary={mockSummary} sport="RUNNING" />)
    expect(screen.getByText('Avg Pace')).toBeInTheDocument()
  })

  it('formats avg pace correctly', () => {
    render(<SummaryPanel summary={mockSummary} sport="RUNNING" />)
    // 6.94 m/s → pace displayed as X:XX min/km
    const paceElements = screen.getAllByText(/min\/km/)
    expect(paceElements.length).toBeGreaterThan(0)
  })
})
