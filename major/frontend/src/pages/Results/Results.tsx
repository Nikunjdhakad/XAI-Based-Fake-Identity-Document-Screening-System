import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api/client';
import { RiskBadge } from '../../components/RiskBadge/RiskBadge';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import { parseArray, parseObject, parseRegions, parseStringArray } from '../../utils/jsonUtils';
import type {
  AiManipulationResponse,
  AnalysisTaskResponse,
  DocumentResponse,
  FaceVerificationResponse,
  ForensicResultResponse,
  IdentityConsistencyResponse,
  MetadataResultResponse,
  OcrResultResponse,
  RiskAssessmentResponse,
  SuspiciousRegion,
} from '../../types';
import styles from './Results.module.css';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function scorePercent(score: number | null | undefined): string {
  if (score == null) return '—';
  return Math.round(score * 100) + '/100';
}

function moduleRiskColor(score: number | null): string {
  if (score == null) return 'var(--text-muted)';
  if (score >= 0.67) return 'var(--risk-high)';
  if (score >= 0.34) return 'var(--risk-medium)';
  return 'var(--risk-low)';
}

function moduleRiskLabel(score: number | null): string {
  if (score == null) return '—';
  if (score >= 0.67) return 'HIGH';
  if (score >= 0.34) return 'MEDIUM';
  return 'LOW';
}

// ---------------------------------------------------------------------------
// Suspicious Region Overlay
// ---------------------------------------------------------------------------

interface RegionOverlayProps {
  regions: SuspiciousRegion[];
}

function RegionOverlay({ regions }: RegionOverlayProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  if (regions.length === 0) {
    return (
      <div className={styles.noRegions}>
        No suspicious regions detected.
      </div>
    );
  }

  // We don't have the actual document image URL from the API,
  // so we render a schematic representation showing the regions
  // relative to a representative 595×842 (A4) canvas.
  const W = 595;
  const H = 842;

  const sourceColors: Record<string, string> = {
    FORENSICS_ELA:         '#f59e0b',
    FORENSICS_NOISE:       '#3b82f6',
    FORENSICS_EDGE:        '#8b5cf6',
    AI_MANIPULATION_SPECTRAL_ANALYSIS: '#ef4444',
    AI_MANIPULATION_JPEG_GHOST:        '#f97316',
    AI_MANIPULATION_GRADIENT_CONSISTENCY: '#ec4899',
    AI_MANIPULATION_TEXTURE_REGULARITY:  '#14b8a6',
  };

  function regionColor(source: string): string {
    for (const [key, color] of Object.entries(sourceColors)) {
      if (source.toUpperCase().includes(key.split('_').slice(-1)[0])) return color;
    }
    return '#f59e0b';
  }

  function shortSource(source: string): string {
    const parts = source.split('_');
    return parts[parts.length - 1] || source;
  }

  return (
    <div className={styles.regionContainer}>
      <div className={styles.regionCanvas} ref={containerRef} style={{ aspectRatio: `${W}/${H}` }}>
        {/* Document outline */}
        <div className={styles.docOutline} />
        {/* Region boxes */}
        {regions.map((r, i) => {
          const left   = (r.x / W) * 100;
          const top    = (r.y / H) * 100;
          const width  = (r.width  / W) * 100;
          const height = (r.height / H) * 100;
          const color  = regionColor(r.source);
          return (
            <div
              key={i}
              className={styles.regionBox}
              style={{
                left:   `${left}%`,
                top:    `${top}%`,
                width:  `${Math.max(width, 1)}%`,
                height: `${Math.max(height, 1)}%`,
                borderColor: color,
                boxShadow: `0 0 0 1px ${color}22`,
              }}
              title={`${r.source}: ${r.description} (conf: ${(r.confidence * 100).toFixed(0)}%)`}
            >
              <span className={styles.regionLabel} style={{ background: color }}>
                {shortSource(r.source)} {(r.confidence * 100).toFixed(0)}%
              </span>
            </div>
          );
        })}
      </div>

      {/* Legend */}
      <div className={styles.regionLegend}>
        {Array.from(new Set(regions.map(r => r.source))).map(source => (
          <div key={source} className={styles.legendItem}>
            <span className={styles.legendDot} style={{ background: regionColor(source) }} />
            <span>{source.replace(/_/g, ' ')}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main Results page
// ---------------------------------------------------------------------------

export function Results() {
  const { id } = useParams<{ id: string }>();

  const [doc,      setDoc]      = useState<DocumentResponse | null>(null);
  const [task,     setTask]     = useState<AnalysisTaskResponse | null>(null);
  const [risk,     setRisk]     = useState<RiskAssessmentResponse | null>(null);
  const [ocr,      setOcr]      = useState<OcrResultResponse | null>(null);
  const [meta,     setMeta]     = useState<MetadataResultResponse | null>(null);
  const [identity, setIdentity] = useState<IdentityConsistencyResponse | null>(null);
  const [forensics,setForensics]= useState<ForensicResultResponse | null>(null);
  const [face,     setFace]     = useState<FaceVerificationResponse | null>(null);
  const [aiManip,  setAiManip]  = useState<AiManipulationResponse | null>(null);
  const [openMods, setOpenMods] = useState<Record<string, boolean>>({});
  const [error,    setError]    = useState('');

  useEffect(() => {
    if (!id) return;

    const safe = <T>(p: Promise<T>): Promise<T | null> => p.catch(() => null);

    Promise.all([
      api.get<DocumentResponse>(`/documents/${id}`),
      safe(api.get<AnalysisTaskResponse>(`/documents/${id}/analysis`)),
      safe(api.get<RiskAssessmentResponse>(`/documents/${id}/analysis/risk`)),
      safe(api.get<OcrResultResponse>(`/documents/${id}/analysis/ocr`)),
      safe(api.get<MetadataResultResponse>(`/documents/${id}/analysis/metadata`)),
      safe(api.get<IdentityConsistencyResponse>(`/documents/${id}/analysis/identity`)),
      safe(api.get<ForensicResultResponse>(`/documents/${id}/analysis/forensics`)),
      safe(api.get<FaceVerificationResponse>(`/documents/${id}/analysis/face`)),
      safe(api.get<AiManipulationResponse>(`/documents/${id}/analysis/ai-manipulation`)),
    ]).then(([d, t, r, o, m, ident, f, fc, ai]) => {
      setDoc(d);
      setTask(t);
      setRisk(r);
      setOcr(o);
      setMeta(m);
      setIdentity(ident);
      setForensics(f);
      setFace(fc);
      setAiManip(ai);
    }).catch(e => setError(e.message));
  }, [id]);

  const suspiciousRegions = [
    ...parseRegions(forensics?.suspiciousRegionsJson),
    ...parseRegions(aiManip?.suspiciousRegionsJson),
  ];

  const explanations   = parseStringArray(risk?.explanationJson);
  const contributions  = parseObject(risk?.moduleContributionsJson) as Record<string, number | null>;

  function toggle(key: string) {
    setOpenMods(prev => ({ ...prev, [key]: !prev[key] }));
  }

  if (error) return <div className={styles.state} style={{ color: 'var(--risk-high)' }}>{error}</div>;
  if (!doc)  return <div className={styles.state}>Loading…</div>;

  const analysisComplete = doc.status === 'COMPLETED' || doc.status === 'PARTIALLY_COMPLETED';

  // ── Module summary rows ──────────────────────────────────────────────────

  const MODULE_ROWS = [
    {
      key: 'ocr',
      label: 'OCR',
      status: ocr?.status ?? null,
      score: null,
      detail: ocr ? `${ocr.pageCount ?? 0} page(s), confidence: ${ocr.overallConfidence != null ? (ocr.overallConfidence * 100).toFixed(0) + '%' : '—'}` : null,
    },
    {
      key: 'forensics',
      label: 'Image Forensics',
      status: forensics?.status ?? null,
      score: forensics?.riskScore ?? null,
      detail: forensics ? `Risk score: ${forensics.riskScore != null ? (forensics.riskScore * 100).toFixed(0) + '/100' : '—'}` : null,
    },
    {
      key: 'metadata',
      label: 'Metadata',
      status: meta?.status ?? null,
      score: meta?.evidenceScore ?? null,
      detail: meta ? `Evidence score: ${meta.evidenceScore != null ? (meta.evidenceScore * 100).toFixed(0) + '/100' : '—'}` : null,
    },
    {
      key: 'identity',
      label: 'Identity Consistency',
      status: identity?.status ?? null,
      score: null,
      detail: identity ? (identity.consistent === true ? 'Consistent' : identity.consistent === false ? `Inconsistent (H:${identity.failedHigh} M:${identity.failedMedium})` : '—') : null,
    },
    {
      key: 'face',
      label: 'Face Verification',
      status: face?.status ?? null,
      score: face?.similarity ?? null,
      detail: face ? (face.status === 'NOT_APPLICABLE' ? 'No reference face' : `Similarity: ${face.similarity != null ? (face.similarity * 100).toFixed(0) + '%' : '—'}`) : null,
    },
    {
      key: 'ai',
      label: 'AI Screening',
      status: aiManip?.status ?? null,
      score: aiManip?.riskScore ?? null,
      detail: aiManip ? `${aiManip.manipulationType ?? '—'}, score: ${aiManip.riskScore != null ? (aiManip.riskScore * 100).toFixed(0) + '/100' : '—'}` : null,
    },
  ];

  return (
    <div className={styles.page}>
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <Link to={`/documents/${id}`}>Document</Link>
        <span className={styles.sep}>/</span>
        <span>Results</span>
      </nav>

      {/* ── Risk header ── */}
      <div className={styles.riskHeader} data-level={doc.riskLevel}>
        <div className={styles.riskLeft}>
          <span className="label">Screening Result</span>
          <RiskBadge level={doc.riskLevel} />
          <StatusBadge status={doc.status} />
        </div>
        <div className={styles.riskRight}>
          <span className="label">Risk Score</span>
          <span className={styles.scoreValue} style={{
            color: doc.riskLevel === 'HIGH' ? 'var(--risk-high)'
                 : doc.riskLevel === 'MEDIUM' ? 'var(--risk-medium)'
                 : doc.riskLevel === 'LOW' ? 'var(--risk-low)'
                 : 'var(--text-primary)',
          }}>
            {risk ? scorePercent(risk.score) : '—'}
          </span>
          {risk && <span className={styles.scoreNote}>v{risk.scoringVersion}</span>}
          {!risk && <span className={styles.scoreNote}>{analysisComplete ? 'No assessment yet' : 'Available after analysis'}</span>}
        </div>
      </div>

      {/* ── Document info + Evidence summary ── */}
      <div className={styles.grid2}>
        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Document</h3>
          <div className={styles.docName}>{doc.filename}</div>
          <div className={styles.docMeta}>{doc.documentType} · {doc.mimeType}</div>
          {doc.uploadedAt && <div className={styles.docMeta}>Uploaded {formatDate(doc.uploadedAt)}</div>}
          {task && (
            <div style={{ marginTop: 16, fontSize: 12, color: 'var(--text-muted)' }}>
              Job: <span className="mono">{task.jobId.slice(0, 8)}…</span>
              {task.startedAt && <> · Started {formatDate(task.startedAt)}</>}
              {task.completedAt && <> · Completed {formatDate(task.completedAt)}</>}
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Evidence Summary</h3>
          <div className={styles.evidenceTable}>
            {MODULE_ROWS.map(m => (
              <div key={m.key} className={styles.evidenceRow}>
                <span className={styles.moduleName}>{m.label}</span>
                <div className={styles.moduleRight}>
                  {m.status ? (
                    <>
                      {m.score != null && (
                        <span className={styles.moduleScore} style={{ color: moduleRiskColor(m.score) }}>
                          {moduleRiskLabel(m.score)}
                        </span>
                      )}
                      <span className={styles.moduleStatus} data-status={m.status}>
                        {m.status === 'NOT_APPLICABLE' ? 'N/A'
                         : m.status === 'SUCCESS' ? '✓'
                         : m.status === 'FAILED' ? '✗'
                         : m.status === 'POSSIBLE_MATCH' ? '≈ Match'
                         : m.status === 'POSSIBLE_MISMATCH' ? '≠ Mismatch'
                         : m.status}
                      </span>
                    </>
                  ) : (
                    <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                      {analysisComplete ? '—' : 'Pending'}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
          {!analysisComplete && (
            <p className={styles.evidenceNote}>
              Start analysis to populate module results.
            </p>
          )}
        </div>
      </div>

      {/* ── Suspicious region overlay ── */}
      <div className="card">
        <h3 style={{ marginBottom: 12 }}>Suspicious Regions</h3>
        {!analysisComplete ? (
          <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
            Region overlay will appear after analysis completes.
          </p>
        ) : suspiciousRegions.length > 0 ? (
          <>
            <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 16, lineHeight: 1.6 }}>
              {suspiciousRegions.length} suspicious region(s) detected.
              Coordinates are relative to document dimensions.
              Higher confidence indicates stronger indication of potential manipulation.
            </p>
            <RegionOverlay regions={suspiciousRegions} />
          </>
        ) : (
          <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.7 }}>
            No suspicious regions detected by image forensics or AI screening modules.
          </p>
        )}
      </div>

      {/* ── Risk explanation ── */}
      <div className="card">
        <h3 style={{ marginBottom: 12 }}>Risk Explanation</h3>
        {explanations.length > 0 ? (
          <ul className={styles.explanationList}>
            {explanations.map((e, i) => (
              <li key={i} className={styles.explanationItem}>{e}</li>
            ))}
          </ul>
        ) : (
          <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.8 }}>
            {analysisComplete
              ? 'No explanation available for this analysis.'
              : 'Risk explanation will appear after analysis completes.'}
          </p>
        )}
        <p style={{ color: 'var(--text-muted)', fontSize: 12, marginTop: 14, fontStyle: 'italic', lineHeight: 1.7 }}>
          VeriLens provides risk assessments — not legal determinations.
          Results indicate "potential manipulation" and "elevated risk",
          not definitive proof of authenticity or forgery.
        </p>
      </div>

      {/* ── Detailed analysis accordions ── */}
      <div className="card" style={{ padding: 0 }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
          <h3>Detailed Analysis</h3>
        </div>

        {/* OCR */}
        <ModuleAccordion
          label="OCR"
          desc="Text extraction and field recognition"
          open={openMods['ocr']}
          onToggle={() => toggle('ocr')}
          status={ocr?.status}
        >
          {ocr?.status === 'SUCCESS' && (
            <div>
              <p className={styles.detailMeta}>Pages: {ocr.pageCount} · Confidence: {ocr.overallConfidence != null ? (ocr.overallConfidence * 100).toFixed(1) + '%' : '—'} · {ocr.processingTimeMs}ms</p>
              {ocr.extractedText ? (
                <pre className={styles.codeBlock}>{ocr.extractedText.slice(0, 1500)}{ocr.extractedText.length > 1500 ? '\n…(truncated)' : ''}</pre>
              ) : (
                <p className={styles.detailNote}>No text was extracted from this document.</p>
              )}
            </div>
          )}
          {ocr?.status === 'FAILED' && <p className={styles.detailError}>{ocr.errorMessage ?? 'OCR failed.'}</p>}
          {!ocr && <p className={styles.detailNote}>OCR has not been run for this document.</p>}
        </ModuleAccordion>

        {/* Forensics */}
        <ModuleAccordion
          label="Image Forensics"
          desc="ELA, noise inconsistency, and edge density analysis"
          open={openMods['forensics']}
          onToggle={() => toggle('forensics')}
          status={forensics?.status}
          score={forensics?.riskScore ?? null}
        >
          {forensics?.status === 'SUCCESS' && (
            <div>
              <p className={styles.detailMeta}>
                Risk score: <strong style={{ color: moduleRiskColor(forensics.riskScore) }}>{scorePercent(forensics.riskScore)}</strong>
                {' '} · ELA mean error: {forensics.elaMeanError?.toFixed(3) ?? '—'}
                {' '} · {forensics.processingTimeMs}ms
              </p>
              <FindingsList json={forensics.findingsJson} />
            </div>
          )}
          {forensics?.status === 'FAILED' && <p className={styles.detailError}>{forensics.errorMessage ?? 'Forensics analysis failed.'}</p>}
          {!forensics && <p className={styles.detailNote}>Image forensics has not been run for this document.</p>}
        </ModuleAccordion>

        {/* Metadata */}
        <ModuleAccordion
          label="Metadata Analysis"
          desc="File metadata inspection and anomaly detection"
          open={openMods['metadata']}
          onToggle={() => toggle('metadata')}
          status={meta?.status}
          score={meta?.evidenceScore ?? null}
        >
          {meta?.status === 'SUCCESS' && (
            <div>
              <p className={styles.detailMeta}>
                Evidence score: <strong style={{ color: moduleRiskColor(meta.evidenceScore) }}>{scorePercent(meta.evidenceScore)}</strong>
                {' '} · Metadata available: {meta.available ? 'Yes' : 'No'}
                {' '} · {meta.processingTimeMs}ms
              </p>
              <AnomalyList json={meta.anomaliesJson} />
              <PropertiesBlock json={meta.propertiesJson} />
            </div>
          )}
          {meta?.status === 'FAILED' && <p className={styles.detailError}>{meta.errorMessage ?? 'Metadata analysis failed.'}</p>}
          {!meta && <p className={styles.detailNote}>Metadata analysis has not been run for this document.</p>}
        </ModuleAccordion>

        {/* Identity */}
        <ModuleAccordion
          label="Identity Consistency"
          desc="Internal field validation and date consistency"
          open={openMods['identity']}
          onToggle={() => toggle('identity')}
          status={identity?.status}
        >
          {identity?.status === 'SUCCESS' && (
            <div>
              <p className={styles.detailMeta}>
                Consistent: <strong style={{ color: identity.consistent ? 'var(--risk-low)' : 'var(--risk-high)' }}>
                  {identity.consistent === true ? 'Yes' : identity.consistent === false ? 'No' : '—'}
                </strong>
                {' '} · Failed: HIGH {identity.failedHigh ?? 0} / MED {identity.failedMedium ?? 0} / LOW {identity.failedLow ?? 0}
                {' '} · {identity.processingTimeMs}ms
              </p>
              <CheckList json={identity.checksJson} />
            </div>
          )}
          {identity?.status === 'NOT_APPLICABLE' && <p className={styles.detailNote}>No text was extracted by OCR — identity consistency checks could not run.</p>}
          {identity?.status === 'FAILED' && <p className={styles.detailError}>{identity.errorMessage ?? 'Identity check failed.'}</p>}
          {!identity && <p className={styles.detailNote}>Identity consistency has not been run for this document.</p>}
        </ModuleAccordion>

        {/* Face */}
        <ModuleAccordion
          label="Face Verification"
          desc="Document face compared to reference image"
          open={openMods['face']}
          onToggle={() => toggle('face')}
          status={face?.status}
        >
          {face && (
            <div>
              {face.status === 'NOT_APPLICABLE' ? (
                <p className={styles.detailNote}>{face.reason ?? 'No reference face was provided — face verification was not performed.'}</p>
              ) : (
                <>
                  <p className={styles.detailMeta}>
                    Result: <strong>{face.status}</strong>
                    {face.similarity != null && <> · Similarity: {(face.similarity * 100).toFixed(1)}%</>}
                    {face.threshold != null && <> · Threshold: {(face.threshold * 100).toFixed(0)}%</>}
                    {' '} · {face.processingTimeMs}ms
                  </p>
                  {face.note && <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 8 }}>{face.note}</p>}
                  {face.errorMessage && <p className={styles.detailError}>{face.errorMessage}</p>}
                </>
              )}
            </div>
          )}
          {!face && <p className={styles.detailNote}>Face verification has not been run for this document.</p>}
        </ModuleAccordion>

        {/* AI Manipulation */}
        <ModuleAccordion
          label="AI Screening"
          desc="Frequency-domain and statistical AI manipulation detection"
          open={openMods['ai']}
          onToggle={() => toggle('ai')}
          status={aiManip?.status}
          score={aiManip?.riskScore ?? null}
        >
          {aiManip?.status === 'SUCCESS' && (
            <div>
              <p className={styles.detailMeta}>
                Risk: <strong style={{ color: moduleRiskColor(aiManip.riskScore) }}>{scorePercent(aiManip.riskScore)}</strong>
                {' '} · Type: {aiManip.manipulationType ?? '—'}
                {' '} · Confidence: {aiManip.confidence != null ? (aiManip.confidence * 100).toFixed(0) + '%' : '—'}
                {' '} · {aiManip.processingTimeMs}ms
              </p>
              <FindingsList json={aiManip.findingsJson} />
              <ModelInfoBlock json={aiManip.modelInfoJson} />
            </div>
          )}
          {aiManip?.status === 'FAILED' && <p className={styles.detailError}>{aiManip.errorMessage ?? 'AI manipulation screening failed.'}</p>}
          {!aiManip && <p className={styles.detailNote}>AI manipulation screening has not been run for this document.</p>}
        </ModuleAccordion>
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

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

interface AccordionProps {
  label: string;
  desc: string;
  open: boolean;
  onToggle: () => void;
  status?: string | null;
  score?: number | null;
  children: React.ReactNode;
}

function ModuleAccordion({ label, desc, open, onToggle, status, score, children }: AccordionProps) {
  return (
    <div className={styles.accordion}>
      <button
        className={styles.accordionBtn}
        onClick={onToggle}
        aria-expanded={open}
      >
        <span className={styles.accordionLabel}>{label}</span>
        <span className={styles.accordionDesc}>{desc}</span>
        {score != null && (
          <span className={styles.accordionScore} style={{ color: moduleRiskColor(score) }}>
            {moduleRiskLabel(score)}
          </span>
        )}
        {status && (
          <span className={styles.accordionStatus} data-status={status}>
            {status === 'SUCCESS' ? '✓' : status === 'FAILED' ? '✗' : status === 'NOT_APPLICABLE' ? 'N/A' : status}
          </span>
        )}
        <span className={styles.chevron}>{open ? '▲' : '▼'}</span>
      </button>
      {open && <div className={styles.accordionBody}>{children}</div>}
    </div>
  );
}

function FindingsList({ json }: { json: string | null | undefined }) {
  const findings = parseArray(json);
  if (findings.length === 0) return <p className={styles.detailNote}>No findings recorded.</p>;
  return (
    <div className={styles.findingsList}>
      {findings.map((item, i) => {
        const f = item as Record<string, unknown>;
        return (
          <div key={i} className={styles.findingItem} data-suspicious={String(f['suspicious'])}>
            <span className={styles.findingType}>{String(f['type'] ?? 'Unknown').replace(/_/g, ' ')}</span>
            <span className={styles.findingSeverity} data-severity={String(f['severity'] ?? 'LOW')}>{String(f['severity'] ?? 'LOW')}</span>
            <span className={styles.findingDesc}>{String(f['description'] ?? '')}</span>
          </div>
        );
      })}
    </div>
  );
}

function AnomalyList({ json }: { json: string | null | undefined }) {
  const anomalies = parseArray(json);
  if (anomalies.length === 0) return <p className={styles.detailNote}>No anomalies detected.</p>;
  return (
    <div className={styles.findingsList}>
      {anomalies.map((item, i) => {
        const a = item as Record<string, unknown>;
        return (
          <div key={i} className={styles.findingItem} data-suspicious="true">
            <span className={styles.findingType}>{String(a['type'] ?? 'Unknown').replace(/_/g, ' ')}</span>
            <span className={styles.findingSeverity} data-severity={String(a['severity'] ?? 'LOW')}>{String(a['severity'] ?? 'LOW')}</span>
            <span className={styles.findingDesc}>{String(a['description'] ?? '')}</span>
          </div>
        );
      })}
    </div>
  );
}

function CheckList({ json }: { json: string | null | undefined }) {
  const checks = parseArray(json);
  if (checks.length === 0) return <p className={styles.detailNote}>No checks recorded.</p>;
  return (
    <div className={styles.findingsList}>
      {checks.map((item, i) => {
        const c = item as Record<string, unknown>;
        const passed = Boolean(c['passed']);
        const severity = String(c['severity'] ?? 'LOW');
        return (
          <div key={i} className={styles.findingItem} data-suspicious={String(!passed)}>
            <span className={styles.findingType}>{String(c['rule'] ?? 'Check').replace(/_/g, ' ')}</span>
            <span className={styles.findingSeverity} data-severity={passed ? 'LOW' : severity}>{passed ? '✓ PASS' : '✗ FAIL'}</span>
            <span className={styles.findingDesc}>{String(c['message'] ?? '')}</span>
          </div>
        );
      })}
    </div>
  );
}

function PropertiesBlock({ json }: { json: string | null | undefined }) {
  const props = parseObject(json);
  const keys = Object.keys(props).filter(k => props[k] != null && typeof props[k] !== 'object');
  if (keys.length === 0) return null;
  return (
    <dl className={styles.propList}>
      {keys.map(k => (
        <span key={k} className={styles.propItem}>
          <dt>{k}</dt>
          <dd className="mono">{String(props[k])}</dd>
        </span>
      ))}
    </dl>
  );
}

function ModelInfoBlock({ json }: { json: string | null | undefined }) {
  const info = parseObject(json);
  if (!info || Object.keys(info).length === 0) return null;
  const limitations = Array.isArray(info['limitations']) ? (info['limitations'] as string[]) : [];
  return (
    <div className={styles.modelInfo}>
      <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: '12px 0 4px' }}>
        Model: <strong>{String(info['name'] ?? '')}</strong> v{String(info['version'] ?? '')}
      </p>
      {limitations.length > 0 && (
        <ul className={styles.limitationList}>
          {limitations.map((l: string, i: number) => <li key={i}>{l}</li>)}
        </ul>
      )}
    </div>
  );
}
