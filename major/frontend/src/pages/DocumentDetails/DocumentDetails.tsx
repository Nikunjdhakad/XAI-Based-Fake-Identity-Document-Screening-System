import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, ApiException } from '../../api/client';
import { RiskBadge } from '../../components/RiskBadge/RiskBadge';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import type { DocumentResponse } from '../../types';
import styles from './DocumentDetails.module.css';

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatBytes(b: number) {
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)} KB`;
  return `${(b / 1024 / 1024).toFixed(1)} MB`;
}

export function DocumentDetails() {
  const { id } = useParams<{ id: string }>();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    api
      .get<DocumentResponse>(`/documents/${id}`)
      .then(setDoc)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  async function handleDelete() {
    if (!id) return;
    if (!window.confirm('Delete this document? This action cannot be undone.')) return;
    setDeleting(true);
    try {
      await api.delete(`/documents/${id}`);
      navigate('/history');
    } catch (e) {
      setError(e instanceof ApiException ? e.message : 'Delete failed');
      setDeleting(false);
    }
  }

  if (loading) return <div className={styles.state}>Loading…</div>;
  if (error) return <div className={styles.state} style={{ color: 'var(--risk-high)' }}>{error}</div>;
  if (!doc) return null;

  const analysisMsg =
    doc.status === 'UPLOADED'
      ? 'This document has not been analysed yet. Start a screening to inspect it.'
      : doc.status === 'QUEUED' || doc.status === 'PROCESSING'
      ? 'Analysis is in progress. Check the analysis progress page for updates.'
      : doc.status === 'COMPLETED'
      ? 'Analysis complete. View the screening results.'
      : 'Analysis failed. You can start a new analysis.';

  return (
    <div className={styles.page}>
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to="/history">History</Link>
        <span className={styles.sep}>/</span>
        <span>{doc.filename}</span>
      </nav>

      <div className={styles.pageHeader}>
        <div className={styles.titleBlock}>
          <h1 className={styles.docName}>{doc.filename}</h1>
          <div className={styles.meta}>
            <StatusBadge status={doc.status} />
            <span className="mono" style={{ color: 'var(--text-muted)', fontSize: 12 }}>
              {formatBytes(doc.fileSize)}
            </span>
            <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>{doc.mimeType}</span>
          </div>
        </div>
        <div className={styles.headerActions}>
          <Link to={`/documents/${id}/analysis`} className="btn btn-primary">
            {doc.status === 'COMPLETED' ? 'View Results' : 'Start Analysis →'}
          </Link>
          <button
            className="btn btn-danger"
            onClick={handleDelete}
            disabled={deleting}
          >
            {deleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>

      <div className={styles.grid}>
        <div className="card">
          <h3 style={{ marginBottom: 20 }}>Document Information</h3>
          <dl className={styles.dl}>
            <dt>Document type</dt>
            <dd>{doc.documentType}</dd>

            <dt>Status</dt>
            <dd><StatusBadge status={doc.status} /></dd>

            <dt>Risk level</dt>
            <dd><RiskBadge level={doc.riskLevel} /></dd>

            <dt>Uploaded</dt>
            <dd>{formatDate(doc.uploadedAt)}</dd>

            <dt>File size</dt>
            <dd className="mono">{formatBytes(doc.fileSize)}</dd>

            <dt>MIME type</dt>
            <dd className="mono">{doc.mimeType}</dd>

            {doc.description && (
              <>
                <dt>Description</dt>
                <dd>{doc.description}</dd>
              </>
            )}
          </dl>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Analysis</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.7 }}>
            {analysisMsg}
          </p>
          <div className={styles.analysisActions}>
            <Link to={`/documents/${id}/analysis`} className="btn btn-primary">
              {doc.status === 'COMPLETED' ? 'View Results' : 'Start / View Analysis'}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
