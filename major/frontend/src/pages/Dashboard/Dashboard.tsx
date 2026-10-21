import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../api/client';
import { RiskBadge } from '../../components/RiskBadge/RiskBadge';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import type { DocumentResponse, Page } from '../../types';
import styles from './Dashboard.module.css';

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function Dashboard() {
  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get<Page<DocumentResponse>>('/documents?page=0&size=10&sort=createdAt')
      .then(p => {
        setDocs(p.content);
        setTotal(p.totalElements);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1>Dashboard</h1>
          <p className={styles.subtitle}>Overview of your document screenings</p>
        </div>
        <Link to="/upload" className="btn btn-primary">
          ↑ New Screening
        </Link>
      </div>

      <div className={styles.stats}>
        <div className="card">
          <div className="label">Total Screened</div>
          <div className={styles.statValue}>
            {loading ? '…' : total}
          </div>
        </div>
        <div className="card">
          <div className="label">System Status</div>
          <div className={styles.statStatus}>Operational</div>
        </div>
      </div>

      <div className="card" style={{ padding: 0 }}>
        <div className={styles.tableHeader}>
          <h3>Recent Screenings</h3>
          <Link to="/history" style={{ fontSize: 13 }}>View all →</Link>
        </div>

        {loading && (
          <p className={styles.stateMsg}>Loading…</p>
        )}
        {error && (
          <p className={styles.stateMsg} style={{ color: 'var(--risk-high)' }}>{error}</p>
        )}
        {!loading && !error && docs.length === 0 && (
          <div className={styles.empty}>
            <p>No screenings yet.</p>
            <Link to="/upload" className="btn btn-primary" style={{ marginTop: 16 }}>
              Upload your first document
            </Link>
          </div>
        )}
        {!loading && docs.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Document</th>
                <th>Type</th>
                <th>Size</th>
                <th>Status</th>
                <th>Risk</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {docs.map(doc => (
                <tr
                  key={doc.id}
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/documents/${doc.id}`)}
                >
                  <td>
                    <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>
                      {doc.filename}
                    </span>
                  </td>
                  <td style={{ color: 'var(--text-secondary)' }}>
                    {doc.documentType}
                  </td>
                  <td className="mono" style={{ color: 'var(--text-muted)' }}>
                    {formatBytes(doc.fileSize)}
                  </td>
                  <td>
                    <StatusBadge status={doc.status} />
                  </td>
                  <td>
                    <RiskBadge level={doc.riskLevel} />
                  </td>
                  <td style={{ color: 'var(--text-muted)' }}>
                    {formatDate(doc.uploadedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
