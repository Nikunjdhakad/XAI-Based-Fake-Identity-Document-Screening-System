import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api/client';
import type { AnalysisTaskResponse, DocumentResponse } from '../../types';
import styles from './AnalysisProgress.module.css';

const STAGES = [
  { key: 'upload',        label: 'Upload',               desc: 'Document received and stored securely' },
  { key: 'preprocessing', label: 'Preprocessing',        desc: 'Image normalisation and format conversion' },
  { key: 'ocr',           label: 'OCR',                  desc: 'Extracting text and fields from document' },
  { key: 'forensics',     label: 'Image Forensics',      desc: 'Analysing image for signs of manipulation' },
  { key: 'metadata',      label: 'Metadata Analysis',    desc: 'Inspecting file metadata and properties' },
  { key: 'identity',      label: 'Identity Consistency', desc: 'Checking internal field consistency' },
  { key: 'face',          label: 'Face Verification',    desc: 'Comparing document face to reference' },
  { key: 'ai',            label: 'AI Screening',         desc: 'Deep learning manipulation detection' },
  { key: 'fusion',        label: 'Evidence Fusion',      desc: 'Combining module evidence into risk assessment' },
  { key: 'report',        label: 'Report Generation',    desc: 'Compiling final screening report' },
];

type StageState = 'done' | 'active' | 'pending';

function getStageState(taskStatus: string | undefined, idx: number): StageState {
  if (!taskStatus || taskStatus === 'QUEUED') {
    return idx === 0 ? 'done' : 'pending';
  }
  if (taskStatus === 'PROCESSING') {
    if (idx < 1) return 'done';
    if (idx === 1) return 'active';
    return 'pending';
  }
  if (taskStatus === 'COMPLETED') return 'done';
  return 'pending';
}

export function AnalysisProgress() {
  const { id } = useParams<{ id: string }>();
  const [doc, setDoc] = useState<DocumentResponse | null>(null);
  const [task, setTask] = useState<AnalysisTaskResponse | null>(null);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Load document info
  useEffect(() => {
    if (!id) return;
    api.get<DocumentResponse>(`/documents/${id}`)
      .then(setDoc)
      .catch(e => setError(e.message));
  }, [id]);

  // Try to load an existing task on mount
  useEffect(() => {
    if (!id) return;
    api.get<AnalysisTaskResponse>(`/documents/${id}/analysis/status`)
      .then(setTask)
      .catch(() => { /* no task yet — that's fine */ });
  }, [id]);

  // Start polling when we have a task that isn't finished
  useEffect(() => {
    if (!id || !task) return;
    if (task.status === 'COMPLETED' || task.status === 'FAILED') return;

    pollRef.current = setInterval(async () => {
      try {
        const updated = await api.get<AnalysisTaskResponse>(`/documents/${id}/analysis/status`);
        setTask(updated);
        if (updated.status === 'COMPLETED' || updated.status === 'FAILED') {
          clearInterval(pollRef.current!);
          clearInterval(timerRef.current!);
        }
      } catch { /* ignore transient errors */ }
    }, 5000);

    timerRef.current = setInterval(() => setElapsed(e => e + 1), 1000);

    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [id, task?.status]); // eslint-disable-line react-hooks/exhaustive-deps

  async function startAnalysis() {
    if (!id) return;
    setStarting(true);
    setError('');
    try {
      const t = await api.post<AnalysisTaskResponse>(`/documents/${id}/analysis`, {});
      setTask(t);
      setElapsed(0);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to start analysis');
    } finally {
      setStarting(false);
    }
  }

  const isDone = task?.status === 'COMPLETED' || task?.status === 'FAILED';

  return (
    <div className={styles.page}>
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to={`/documents/${id}`}>Document</Link>
        <span className={styles.sep}>/</span>
        <span>Analysis</span>
      </nav>

      <div className={styles.pageHeader}>
        <h1>Screening Analysis</h1>
        {doc && <p className={styles.docName}>{doc.filename}</p>}
      </div>

      {error && <p className={styles.error} role="alert">{error}</p>}

      {/* No task yet — show start button */}
      {!task && !error && (
        <div className="card" style={{ maxWidth: 500 }}>
          <h3 style={{ marginBottom: 10 }}>Ready to analyse</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.7, marginBottom: 20 }}>
            Start the screening to run all analysis modules: OCR, image forensics,
            metadata inspection, identity consistency, face verification, and AI
            manipulation detection.
          </p>
          <button
            className="btn btn-primary"
            onClick={startAnalysis}
            disabled={starting}
          >
            {starting ? 'Starting…' : '▶ Start Screening'}
          </button>
        </div>
      )}

      {task && (
        <>
          {/* Status bar */}
          <div className={styles.statusBar}>
            <span className={styles.taskStatus} data-status={task.status}>
              {task.status === 'QUEUED'
                ? 'Queued'
                : task.status === 'PROCESSING'
                ? 'Processing…'
                : task.status === 'COMPLETED'
                ? 'Completed'
                : 'Failed'}
            </span>

            {!isDone && (
              <span className={styles.elapsed}>
                Elapsed: {elapsed}s
              </span>
            )}

            {task.status === 'COMPLETED' && (
              <Link to={`/documents/${id}/results`} className="btn btn-primary" style={{ marginLeft: 'auto' }}>
                View Results →
              </Link>
            )}
          </div>

          {/* Pipeline steps */}
          <div className={styles.pipeline} aria-label="Analysis pipeline">
            {STAGES.map((stage, idx) => {
              const s = getStageState(task.status, idx);
              return (
                <div key={stage.key} className={`${styles.stage} ${styles[s]}`}>
                  <div className={styles.stageIcon} aria-hidden="true">
                    {s === 'done' ? '✓' : s === 'active' ? '●' : '○'}
                  </div>
                  <div className={styles.stageBody}>
                    <div className={styles.stageLabel}>{stage.label}</div>
                    <div className={styles.stageDesc}>{stage.desc}</div>
                  </div>
                </div>
              );
            })}
          </div>

          <p className={styles.note}>
            Detailed per-module progress will be available in Phases 4–9 when AI modules
            are connected. The current status reflects the analysis job lifecycle only.
          </p>
        </>
      )}
    </div>
  );
}
