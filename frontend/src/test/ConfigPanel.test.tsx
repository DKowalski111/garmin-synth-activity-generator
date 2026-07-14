import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import { ConfigPanel } from '../components/ConfigPanel'
import { defaultConfig, defaultRunningConfig } from '../types/activity'

describe('ConfigPanel', () => {
  it('renders sport selector buttons', () => {
    const onChange = vi.fn()
    render(<ConfigPanel config={defaultConfig} onChange={onChange} />)
    expect(screen.getByText(/cycling/i)).toBeInTheDocument()
    expect(screen.getByText(/running/i)).toBeInTheDocument()
  })

  it('renders cycling fields by default', () => {
    const onChange = vi.fn()
    render(<ConfigPanel config={defaultConfig} onChange={onChange} />)
    expect(screen.getByText(/activity name/i)).toBeInTheDocument()
    expect(screen.getByText(/average speed/i)).toBeInTheDocument()
    expect(screen.getByText(/average heart rate/i)).toBeInTheDocument()
    expect(screen.getByText(/recording interval/i)).toBeInTheDocument()
    expect(screen.getByText(/timing mode/i)).toBeInTheDocument()
  })

  it('shows pace and cadence fields for running', () => {
    const onChange = vi.fn()
    render(<ConfigPanel config={defaultRunningConfig} onChange={onChange} />)
    expect(screen.getByText(/average pace/i)).toBeInTheDocument()
    expect(screen.getByText(/steps per minute/i)).toBeInTheDocument()
  })

  it('calls onChange when speed is updated', () => {
    const onChange = vi.fn()
    render(<ConfigPanel config={defaultConfig} onChange={onChange} />)
    const speedInput = screen.getAllByRole('spinbutton')[0]
    fireEvent.change(speedInput, { target: { value: '30' } })
    expect(onChange).toHaveBeenCalled()
  })

  it('shows datetime input when END_AT_SELECTED_TIME is chosen', () => {
    const config = { ...defaultConfig, timeMode: 'END_AT_SELECTED_TIME' as const }
    const onChange = vi.fn()
    render(<ConfigPanel config={config} onChange={onChange} />)
    expect(screen.getByText(/end date\/time/i)).toBeInTheDocument()
  })

  it('adds a pause when Add Pause is clicked', () => {
    const onChange = vi.fn()
    render(<ConfigPanel config={defaultConfig} onChange={onChange} />)
    fireEvent.click(screen.getByText(/add pause/i))
    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        pauses: expect.arrayContaining([
          expect.objectContaining({ offsetSeconds: 300, durationSeconds: 120 }),
        ]),
      })
    )
  })
})
