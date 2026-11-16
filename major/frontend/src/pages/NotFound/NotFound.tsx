import { Link } from 'react-router-dom';

/**
 * 404 Not Found page — Phase 14 UX hardening.
 */
export function NotFound() {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '60vh',
      textAlign: 'center',
      gap: '16px',
      padding: '40px',
    }}>
      <div style={{ fontSize: '72px', fontWeight: 700, color: 'var(--text-muted)' }}>404</div>
      <h1 style={{ margin: 0, color: 'var(--text-primary)' }}>Page Not Found</h1>
      <p style={{ color: 'var(--text-secondary)', maxWidth: '400px' }}>
        The page you're looking for doesn't exist or you may not have access to it.
      </p>
      <Link
        to="/dashboard"
        style={{
          padding: '10px 24px',
          background: 'var(--accent)',
          color: '#fff',
          borderRadius: 'var(--radius)',
          textDecoration: 'none',
          fontWeight: 500,
          marginTop: '8px',
        }}
      >
        Go to Dashboard
      </Link>
    </div>
  );
}

