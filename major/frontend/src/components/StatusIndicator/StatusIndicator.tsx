import type { DocumentStatus, AnalysisTaskStatus } from '../../api/types'
import styles from './StatusIndicator.module.css'

interface StatusIndicatorProps {
  status: DocumentStatus | AnalysisTaskStatus
}

export default function StatusIndicator({ status }: StatusIndicatorProps) {
  const cssClass = styles[status.toLowerCase()] ?? styles.default

  return (
    <span
      className={`${styles.badge} ${cssClass}`}
      role="status"
      aria-label={`Status: ${status}`}
    >
      {status}
    </span>
  )
}
