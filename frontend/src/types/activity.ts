export type TimeMode = 'END_NOW' | 'END_AT_SELECTED_TIME' | 'START_AT_SELECTED_TIME'

export interface PauseConfig {
  offsetSeconds: number
  durationSeconds: number
}

export interface ActivityConfig {
  activityName: string
  averageSpeedKmh: number
  averageHeartRate: number
  recordingIntervalSeconds: number
  seed: number
  timeMode: TimeMode
  selectedTime: string | null
  pauses: PauseConfig[]
}

export const defaultConfig: ActivityConfig = {
  activityName: 'Synthetic Cycling Activity',
  averageSpeedKmh: 25,
  averageHeartRate: 145,
  recordingIntervalSeconds: 1,
  seed: 42,
  timeMode: 'END_NOW',
  selectedTime: null,
  pauses: [],
}

export interface ActivitySummary {
  startTime: string
  endTime: string
  distanceMeters: number
  movingDurationSeconds: number
  elapsedDurationSeconds: number
  totalPauseDurationSeconds: number
  averageSpeedMps: number
  maxSpeedMps: number
  averageHeartRate: number
  maxHeartRate: number
  sampleCount: number
  totalCalories: number
}

export interface ActivitySample {
  timestamp: string
  latitude: number
  longitude: number
  distanceMeters: number
  speedMetersPerSecond: number
  heartRate: number
  isPaused: boolean
}

export interface ActivityPreview {
  summary: ActivitySummary
  samples: ActivitySample[]
}
