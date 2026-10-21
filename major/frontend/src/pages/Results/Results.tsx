import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api/client';
import { RiskBadge } from '../../components/RiskBadge/RiskBadge';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import type { AnalysisTaskResponse, DocumentResponse } from '../../types';
import styles from './Results.module.css';

const MODULES = [
  { key: 'ocr',       label: 'OCR',                  phase: 4, desc: 'Text extraction and field recognition' },
  { key: 'forensics', label: 'Image Forensics',       phase: 7, desc: 'Visual manipulation indicators' },
  { key: 'metadata',  label: 'Metadata Analysis',     phase: 5, desc: 'File metadata and anomalies' },
  { key: 'identity',  label: 'Identity Consistency',  phase: 6, desc: 'Internal field validation' },
  { key: 'face',      label: 'Face Verification',     phase: 8, desc: 'Document face vs reference' },
  { key: 'ai',        label: 'AI Screening',          phase: 9, desc: 'Deep learning manipulation detection' },
];

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export function Results() {
  const { id } = useParams<{ id: string }>();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [task, setTask] = useState<AnalysisTaskResponse | null>(null);
  const [openModules, setOpenModules] = useState<Record<string, boolean>>({});
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;
    Promise.all([
      api.get<DocumentResponse>(`/documents/${id}`),
      api.get<AnalysisTaskResponse>(`/documents/${id}/analysis`).catch(() => null),
    ])
      .then(([d, t]) => { setDoc(d); setTask(t); })
      .catch(e => setError(e.message));
  }, [id]);

  function toggleModule(key: string) {
    setOpenModules(prev => ({ ...prev, [key]: !prev[key] }));
  }

  if (error) return <div className={styles.state} style={{ color: 'var(--risk-high)' }}>{error}</div>;
  if (!doc) return <div className={styles.state}>Loading…</div>;

  return (
    <div className={styles.page}>
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to={`/documents/${id}`}>Document</Link>
        <span className={styles.sep}>/</span>
        <span>Results</span>
      </nav>

      {/* ── Risk header ── */}
      <div className={styles.riskHeader}>
        <div className={styles.riskLeft}>
          <span className="label">Screening Result</span>
          <RiskBadge level={doc.riskLevel} />
          <StatusBadge status={doc.status} />
        </div>
        <div className={styles.riskRight}>
          <span className="label">Risk Score</span>
          <span className={styles.scoreValue}>—</span>
          <span className={styles.scoreNote}>Available after analysis</span>
        </div>
      </div>

      {/* ── Document info + evidence summary ── */}
      <div className={styles.grid2}>
        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Document</h3>
          <div className={styles.docName}>{doc.filename}</div>
          <div className={styles.docMeta}>{doc.documentType} · {doc.mimeType}</div>
          {doc.uploadedAt && (
            <div className={styles.docMeta}>Uploaded {formatDate(doc.uploadedAt)}</div>
          )}
          {task && (
            <div style={{ marginTop: 16, fontSize: 12, color: 'var(--text-muted)' }}>
              Job ID: <span className="mono">{task.jobId.slice(0, 8)}…</span>
              <br />
              Requested: {formatDate(task.requestedAt)}
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Evidence Summary</h3>
          <div className={styles.evidenceTable}>
            {MODULES.map(m => (
              <div key={m.key} className={styles.evidenceRow}>
                <span className={styles.moduleName}>{m.label}</span>
                <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  {task ? 'Pending' : '—'}
                </span>
              </div>
            ))}
          </div>
          <p className={styles.evidenceNote}>
            Module results will appear once AI analysis is connected (Phases 4–9).
          </p>
        </div>
      </div>

      {/* ── Suspicious regions ── */}
      <div className="card">
        <h3 style={{ marginBottom: 8 }}>Suspicious Regions</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.7 }}>
          Region overlay visualisation will be available after image forensics and AI
          screening modules are implemented (Phases 7–9).
        </p>
      </div>

      {/* ── Risk explanation ── */}
      <div className="card">
        <h3 style={{ marginBottom: 12 }}>Risk Explanation</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.8 }}>
          Screening results and supporting evidence will be displayed here after analysis
          is complete. Each finding will include the source module, evidence type, and
          confidence level.
        </p>
        <p style={{ color: 'var(--text-muted)', fontSize: 12, marginTop: 12, fontStyle: 'italic', lineHeight: 1.7 }}>
          Note: VeriLens provides risk assessments — not legal determinations. Results
          indicate "potential manipulation" and "elevated risk", not definitive proof of
          authenticity or forgery.
        </p>
      </div>

      {/* ── Detailed analysis accordions ── */}
      <div className="card" style={{ padding: 0 }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
          <h3>Detailed Analysis</h3>
        </div>
        {MODULES.map(m => (
          <div key={m.key} className={styles.accordion}>
            <button
              className={styles.accordionBtn}
              onClick={() => toggleModule(m.key)}
              aria-expanded={openModules[m.key] ?? false}
            >
              <span>{m.label}</span>
              <span className={styles.accordionDesc}>{m.desc}</span>
              <span className={styles.chevron}>{openModules[m.key] ? '▲' : '▼'}</span>
            </button>
            {openModules[m.key] && (
              <div className={styles.accordionBody}>
                <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.7 }}>
                  {m.label} module results will be available after Phase {m.phase} is
                  implemented.
                </p>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* ── Actions ── */}
      <div className={styles.actions}>
        <button className="btn btn-ghost" disabled title="Available in Phase 12">
          ↓ Download Report
        </button>
        <Link to={`/documents/${id}/analysis`} className="btn btn-ghost">
          ← Back to Analysis
        </Link>
      </div>
    </div>
  );
}
