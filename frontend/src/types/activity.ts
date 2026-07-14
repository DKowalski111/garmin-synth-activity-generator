export type TimeMode = 'END_NOW' | 'END_AT_SELECTED_TIME' | 'START_AT_SELECTED_TIME'
export type SportType = 'CYCLING' | 'RUNNING'

export interface PauseConfig {
  offsetSeconds: number
  durationSeconds: number
}

export interface ActivityConfig {
  sport: SportType
  activityName: string
  // cycling: speed in km/h; running: pace in min/km (converted to speed server-side)
  averageSpeedKmh: number
  averagePaceMinPerKm: number
  averageHeartRate: number
  recordingIntervalSeconds: number
  seed: number
  timeMode: TimeMode
  selectedTime: string | null
  pauses: PauseConfig[]
  cadenceSpm: number | null
}

export const defaultCyclingConfig: ActivityConfig = {
  sport: 'CYCLING',
  activityName: 'Synthetic Cycling Activity',
  averageSpeedKmh: 25,
  averagePaceMinPerKm: 0,
  averageHeartRate: 145,
  recordingIntervalSeconds: 1,
  seed: 42,
  timeMode: 'END_NOW',
  selectedTime: null,
  pauses: [],
  cadenceSpm: null,
}

export const defaultRunningConfig: ActivityConfig = {
  sport: 'RUNNING',
  activityName: 'Synthetic Running Activity',
  averageSpeedKmh: 0,
  averagePaceMinPerKm: 6.0,
  averageHeartRate: 155,
  recordingIntervalSeconds: 1,
  seed: 42,
  timeMode: 'END_NOW',
  selectedTime: null,
  pauses: [],
  cadenceSpm: 170,
}

export const defaultConfig = defaultCyclingConfig

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
  cadenceSpm: number | null
}

export interface ActivityPreview {
  sport: SportType
  summary: ActivitySummary
  samples: ActivitySample[]
}
