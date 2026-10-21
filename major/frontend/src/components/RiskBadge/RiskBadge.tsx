import styles from './RiskBadge.module.css';

interface Props {
  level: string | null;
}

export function RiskBadge({ level }: Props) {
  if (!level) return <span className={`${styles.badge} ${styles.pending}`}>—</span>;
  const cls =
    level === 'HIGH' ? styles.high
    : level === 'MEDIUM' ? styles.medium
    : styles.low;
  return <span className={`${styles.badge} ${cls}`}>{level}</span>;
}
