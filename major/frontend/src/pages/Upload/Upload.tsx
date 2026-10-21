import {
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
  useRef,
  useState,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { api, ApiException } from '../../api/client';
import type { DocumentResponse } from '../../types';
import styles from './Upload.module.css';

const DOC_TYPES = [
  'IDENTITY',
  'CERTIFICATE',
  'EXPERIENCE',
  'INSTITUTIONAL',
  'REPORT',
];

const ALLOWED_MIME = ['application/pdf', 'image/jpeg', 'image/png'];
const ALLOWED_EXT = /\.(pdf|jpg|jpeg|png)$/i;

function formatBytes(b: number) {
  if (b < 1024) return `${b} B`;
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)} KB`;
  return `${(b / 1024 / 1024).toFixed(1)} MB`;
}

export function Upload() {
  const [file, setFile] = useState<File | null>(null);
  const [faceFile, setFaceFile] = useState<File | null>(null);
  const [docType, setDocType] = useState('IDENTITY');
  const [description, setDescription] = useState('');
  const [dragging, setDragging] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);
  const faceRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  function validateFile(f: File): boolean {
    if (!ALLOWED_MIME.includes(f.type) && !ALLOWED_EXT.test(f.name)) {
      setError(`Unsupported file type. Allowed: PDF, JPG, PNG.`);
      return false;
    }
    if (f.size > 20 * 1024 * 1024) {
      setError('File exceeds the 20 MB size limit.');
      return false;
    }
    return true;
  }

  function pickFile(f: File) {
    if (!validateFile(f)) return;
    setError('');
    setFile(f);
  }

  function onDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    if (f) pickFile(f);
  }

  function onFileChange(e: ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (f) pickFile(f);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!file) return;
    setLoading(true);
    setError('');
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('documentType', docType);
      if (description.trim()) form.append('description', description.trim());
      if (faceFile) form.append('referenceFace', faceFile);
      const doc = await api.postForm<DocumentResponse>('/documents', form);
      navigate(`/documents/${doc.id}`);
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Upload failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1>New Screening</h1>
        <p className={styles.subtitle}>Upload a document to begin multi-signal analysis</p>
      </div>

      <form onSubmit={handleSubmit} className={styles.form} noValidate>
        <div className={styles.grid}>
          {/* Main document */}
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>Document File</h3>

            <div
              className={`${styles.dropZone} ${dragging ? styles.dragging : ''} ${file ? styles.hasFile : ''}`}
              onDragOver={e => { e.preventDefault(); setDragging(true); }}
              onDragLeave={() => setDragging(false)}
              onDrop={onDrop}
              onClick={() => fileRef.current?.click()}
              role="button"
              tabIndex={0}
              aria-label="Upload document — click or drag and drop"
              onKeyDown={e => e.key === 'Enter' && fileRef.current?.click()}
            >
              <input
                ref={fileRef}
                type="file"
                accept=".pdf,.jpg,.jpeg,.png"
                hidden
                onChange={onFileChange}
              />
              {file ? (
                <div className={styles.fileInfo}>
                  <span className={styles.fileIcon}>📄</span>
                  <div className={styles.fileText}>
                    <div className={styles.fileName}>{file.name}</div>
                    <div className={styles.fileMeta}>
                      {formatBytes(file.size)} &middot; {file.type || 'file'}
                    </div>
                  </div>
                  <button
                    type="button"
                    className={styles.clearBtn}
                    onClick={e => { e.stopPropagation(); setFile(null); if (fileRef.current) fileRef.current.value = ''; }}
                    aria-label="Remove file"
                  >
                    ✕
                  </button>
                </div>
              ) : (
                <div className={styles.dropPrompt}>
                  <span className={styles.dropIcon}>↑</span>
                  <span className={styles.dropMain}>Drop document here or click to browse</span>
                  <span className={styles.dropSub}>PDF, JPG, PNG &middot; Max 20 MB</span>
                </div>
              )}
            </div>

            <div className={styles.fields}>
              <div className={styles.field}>
                <label htmlFor="docType" className="label">Document Type</label>
                <select
                  id="docType"
                  value={docType}
                  onChange={e => setDocType(e.target.value)}
                  style={{ width: '100%' }}
                >
                  {DOC_TYPES.map(t => (
                    <option key={t} value={t}>
                      {t.charAt(0) + t.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
              </div>

              <div className={styles.field}>
                <label htmlFor="description" className="label">
                  Description{' '}
                  <span style={{ color: 'var(--text-muted)', textTransform: 'none', letterSpacing: 0 }}>
                    (optional)
                  </span>
                </label>
                <textarea
                  id="description"
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  rows={2}
                  placeholder="Brief description of the document…"
                  style={{ width: '100%', resize: 'vertical' }}
                />
              </div>
            </div>
          </div>

          {/* Reference face */}
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              Reference Face{' '}
              <span className={styles.optional}>— Optional</span>
            </h3>
            <p className={styles.sectionDesc}>
              Provide a reference photo to enable face verification. If omitted, that
              module will be marked Not Applicable.
            </p>

            <div
              className={`${styles.dropZoneSm} ${faceFile ? styles.hasFile : ''}`}
              onClick={() => faceRef.current?.click()}
              role="button"
              tabIndex={0}
              aria-label="Upload reference face photo"
              onKeyDown={e => e.key === 'Enter' && faceRef.current?.click()}
            >
              <input
                ref={faceRef}
                type="file"
                accept=".jpg,.jpeg,.png"
                hidden
                onChange={e => { const f = e.target.files?.[0]; setFaceFile(f ?? null); }}
              />
              {faceFile ? (
                <div className={styles.fileInfo}>
                  <span>🖼 {faceFile.name}</span>
                  <button
                    type="button"
                    className={styles.clearBtn}
                    onClick={e => { e.stopPropagation(); setFaceFile(null); if (faceRef.current) faceRef.current.value = ''; }}
                  >
                    ✕
                  </button>
                </div>
              ) : (
                <span className={styles.dropSub}>
                  Click to add reference face (JPG, PNG)
                </span>
              )}
            </div>
          </div>
        </div>

        {error && (
          <p className={styles.error} role="alert">{error}</p>
        )}

        <div className={styles.actions}>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={!file || loading}
          >
            {loading ? 'Uploading…' : '↑ Upload & Begin Screening'}
          </button>
        </div>
      </form>
    </div>
  );
}
