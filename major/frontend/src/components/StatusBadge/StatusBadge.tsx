import styles from './StatusBadge.module.css';

type Status = 'UPLOADED' | 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIALLY_COMPLETED';

interface Props {
  status: Status;
}

const MAP: Record<Status, { label: string; cls: string }> = {
  UPLOADED: { label: 'Uploaded', cls: 'uploaded' },
  QUEUED: { label: 'Queued', cls: 'queued' },
  PROCESSING: { label: 'Processing', cls: 'processing' },
  COMPLETED: { label: 'Completed', cls: 'completed' },
  FAILED: { label: 'Failed', cls: 'failed' },
  PARTIALLY_COMPLETED: { label: 'Partial', cls: 'partial' },
};

export function StatusBadge({ status }: Props) {
  const { label, cls } = MAP[status] ?? { label: status, cls: 'uploaded' };
  return (
    <span className={`${styles.badge} ${styles[cls as keyof typeof styles]}`}>
      {label}
    </span>
  );
}
