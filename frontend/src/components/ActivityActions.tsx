import React from 'react'

interface Props {
  hasRoute: boolean
  hasPreview: boolean
  isLoadingPreview: boolean
  onPreview: () => void
  onDownloadFit: () => void
  onReset: () => void
}

export function ActivityActions({ hasRoute, hasPreview, isLoadingPreview, onPreview, onDownloadFit, onReset }: Props) {
  // hasPreview is reserved for future use (e.g. enabling additional actions)
  void hasPreview
  return (
    <div className="actions-panel panel">
      <button
        className="btn-primary btn-large"
        onClick={onPreview}
        disabled={!hasRoute || isLoadingPreview}
      >
        {isLoadingPreview ? 'Generating preview…' : 'Preview Activity'}
      </button>
      <button
        className="btn-success btn-large"
        onClick={onDownloadFit}
        disabled={!hasRoute}
      >
        Download .FIT File
      </button>
      <button className="btn-secondary" onClick={onReset}>Reset</button>
    </div>
  )
}
