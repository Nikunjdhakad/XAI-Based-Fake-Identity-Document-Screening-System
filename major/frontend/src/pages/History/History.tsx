import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/client';
import { RiskBadge } from '../../components/RiskBadge/RiskBadge';
import { StatusBadge } from '../../components/StatusBadge/StatusBadge';
import type { DocumentResponse, Page } from '../../types';
import styles from './History.module.css';

const PAGE_SIZE = 20;

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function formatBytes(b: number) {
  if (b < 1024) return `${b} B`;
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)} KB`;
  return `${(b / 1024 / 1024).toFixed(1)} MB`;
}

export function History() {
  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams({
      page: String(page),
      size: String(PAGE_SIZE),
      sort: 'createdAt',
    });
    if (statusFilter) params.set('status', statusFilter);

    api
      .get<Page<DocumentResponse>>(`/documents?${params.toString()}`)
      .then(p => {
        setDocs(p.content);
        setTotal(p.totalElements);
        setTotalPages(p.totalPages);
      })
      .finally(() => setLoading(false));
  }, [page, statusFilter]);

  const filtered = search
    ? docs.filter(d => d.filename.toLowerCase().includes(search.toLowerCase()))
    : docs;

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1>Screening History</h1>
          <p className={styles.subtitle}>
            {total} document{total !== 1 ? 's' : ''} screened
          </p>
        </div>
      </div>

      <div className={styles.filters}>
        <input
          type="search"
          placeholder="Search by filename…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          aria-label="Search documents"
          style={{ minWidth: 220 }}
        />
        <select
          value={statusFilter}
          onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
          aria-label="Filter by status"
          style={{ minWidth: 160 }}
        >
          <option value="">All statuses</option>
          <option value="UPLOADED">Uploaded</option>
          <option value="QUEUED">Queued</option>
          <option value="PROCESSING">Processing</option>
          <option value="COMPLETED">Completed</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>

      <div className="card" style={{ padding: 0 }}>
        {loading && (
          <p className={styles.stateMsg}>Loading…</p>
        )}
        {!loading && filtered.length === 0 && (
          <p className={styles.stateMsg} style={{ textAlign: 'center', padding: '40px 20px' }}>
            No screenings found.
          </p>
        )}
        {!loading && filtered.length > 0 && (
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
              {filtered.map(doc => (
                <tr
                  key={doc.id}
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/documents/${doc.id}`)}
                >
                  <td style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                    {doc.filename}
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
                  <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                    {formatDate(doc.uploadedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            className="btn btn-ghost"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            ← Prev
          </button>
          <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            className="btn btn-ghost"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
