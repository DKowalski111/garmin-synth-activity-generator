import React, { useCallback } from 'react'
import { ActivityConfig, TimeMode, PauseConfig } from '../types/activity'

interface Props {
  config: ActivityConfig
  onChange: (c: ActivityConfig) => void
}

export function ConfigPanel({ config, onChange }: Props) {
  const update = useCallback(
    <K extends keyof ActivityConfig>(key: K, value: ActivityConfig[K]) =>
      onChange({ ...config, [key]: value }),
    [config, onChange]
  )

  const addPause = () =>
    update('pauses', [...config.pauses, { offsetSeconds: 300, durationSeconds: 120 }])

  const removePause = (i: number) =>
    update('pauses', config.pauses.filter((_, idx) => idx !== i))

  const updatePause = (i: number, field: keyof PauseConfig, val: number) => {
    const pauses = [...config.pauses]
    pauses[i] = { ...pauses[i], [field]: val }
    update('pauses', pauses)
  }

  return (
    <div className="config-panel panel">
      <h2>Activity Configuration</h2>

      <label>Activity Name
        <input type="text" value={config.activityName}
          onChange={e => update('activityName', e.target.value)} />
      </label>

      <label>Average Speed (km/h)
        <input type="number" min={1} max={100} step={0.5}
          value={config.averageSpeedKmh}
          onChange={e => update('averageSpeedKmh', parseFloat(e.target.value))} />
      </label>

      <label>Average Heart Rate (BPM)
        <input type="number" min={40} max={220}
          value={config.averageHeartRate}
          onChange={e => update('averageHeartRate', parseInt(e.target.value))} />
      </label>

      <label>Recording Interval (seconds)
        <input type="number" min={1} max={60}
          value={config.recordingIntervalSeconds}
          onChange={e => update('recordingIntervalSeconds', parseInt(e.target.value))} />
      </label>

      <label>Random Seed
        <input type="number"
          value={config.seed}
          onChange={e => update('seed', parseInt(e.target.value))} />
      </label>

      <label>Timing Mode
        <select value={config.timeMode}
          onChange={e => update('timeMode', e.target.value as TimeMode)}>
          <option value="END_NOW">End Now (activity ends at generation time)</option>
          <option value="END_AT_SELECTED_TIME">End at Selected Time</option>
          <option value="START_AT_SELECTED_TIME">Start at Selected Time</option>
        </select>
      </label>

      {(config.timeMode === 'END_AT_SELECTED_TIME' || config.timeMode === 'START_AT_SELECTED_TIME') && (
        <label>
          {config.timeMode === 'END_AT_SELECTED_TIME' ? 'End Date/Time' : 'Start Date/Time'}
          <input type="datetime-local"
            value={config.selectedTime
              ? new Date(config.selectedTime).toLocaleString('sv').slice(0, 16).replace(' ', 'T')
              : ''}
            onChange={e => {
              const v = e.target.value
              if (!v) { update('selectedTime', null); return }
              // datetime-local value is local time — convert to UTC ISO string
              update('selectedTime', new Date(v).toISOString())
            }} />
        </label>
      )}

      <div className="pauses-section">
        <div className="pauses-header">
          <h3>Pauses</h3>
          <button onClick={addPause} className="btn-secondary btn-small">+ Add Pause</button>
        </div>
        {config.pauses.map((pause, i) => (
          <div key={i} className="pause-row">
            <label>Offset (s)
              <input type="number" min={0} value={pause.offsetSeconds}
                onChange={e => updatePause(i, 'offsetSeconds', parseInt(e.target.value))} />
            </label>
            <label>Duration (s)
              <input type="number" min={1} value={pause.durationSeconds}
                onChange={e => updatePause(i, 'durationSeconds', parseInt(e.target.value))} />
            </label>
            <button onClick={() => removePause(i)} className="btn-danger btn-small">Remove</button>
          </div>
        ))}
        {config.pauses.length === 0 && <p className="empty-hint">No pauses configured.</p>}
      </div>
    </div>
  )
}
