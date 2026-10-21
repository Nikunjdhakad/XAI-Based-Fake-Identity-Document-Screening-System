import styles from './LoadingSpinner.module.css'

interface LoadingSpinnerProps {
  message?: string
}

export default function LoadingSpinner({ message = 'Loading…' }: LoadingSpinnerProps) {
  return (
    <div className={styles.container} role="status" aria-live="polite">
      <div className={styles.spinner} aria-hidden="true" />
      <span className={styles.message}>{message}</span>
    </div>
  )
}
