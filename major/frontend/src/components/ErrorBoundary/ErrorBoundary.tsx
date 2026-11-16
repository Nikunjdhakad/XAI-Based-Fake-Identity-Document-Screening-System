import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

/**
 * Error Boundary — Phase 14 UX hardening.
 * Catches React rendering errors and shows a fallback UI
 * instead of a blank white screen.
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('ErrorBoundary caught:', error, info.componentStack);
  }

  handleReset = (): void => {
    this.setState({ hasError: false, error: null });
  };

  render(): ReactNode {
    if (this.state.hasError) {
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
          color: 'var(--text-primary)',
        }}>
          <div style={{ fontSize: '48px' }}>⚠️</div>
          <h2 style={{ margin: 0 }}>Something went wrong</h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '400px' }}>
            An unexpected error occurred while rendering this page.
            Please try refreshing or navigating back.
          </p>
          <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
            <button
              onClick={this.handleReset}
              style={{
                padding: '10px 24px',
                background: 'var(--accent)',
                color: '#fff',
                border: 'none',
                borderRadius: 'var(--radius)',
                cursor: 'pointer',
                fontWeight: 500,
              }}
            >
              Try Again
            </button>
            <button
              onClick={() => { window.location.href = '/dashboard'; }}
              style={{
                padding: '10px 24px',
                background: 'var(--bg-surface-2)',
                color: 'var(--text-primary)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius)',
                cursor: 'pointer',
                fontWeight: 500,
              }}
            >
              Go to Dashboard
            </button>
          </div>
          {this.state.error && (
            <details style={{ marginTop: '16px', color: 'var(--text-muted)', fontSize: '12px', maxWidth: '600px' }}>
              <summary style={{ cursor: 'pointer' }}>Error details</summary>
              <pre style={{
                textAlign: 'left',
                padding: '12px',
                background: 'var(--bg-surface)',
                borderRadius: 'var(--radius-sm)',
                overflow: 'auto',
                maxHeight: '200px',
              }}>
                {this.state.error.message}
              </pre>
            </details>
          )}
        </div>
      );
    }
    return this.props.children;
  }
}

