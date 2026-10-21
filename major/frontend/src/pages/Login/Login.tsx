import { type FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiException } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import styles from './Login.module.css';

export function Login() {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, register } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else {
        await register(username, email, password, fullName);
      }
      navigate('/dashboard');
    } catch (err) {
      if (err instanceof ApiException) setError(err.message);
      else setError('An unexpected error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  function switchMode(m: 'login' | 'register') {
    setMode(m);
    setError('');
  }

  return (
    <div className={styles.page}>
      <div className={styles.panel}>
        <div className={styles.header}>
          <h1 className={styles.brand}>VeriLens</h1>
          <p className={styles.tagline}>Evidence-first document screening.</p>
        </div>

        <div className={styles.tabs} role="tablist">
          <button
            role="tab"
            aria-selected={mode === 'login'}
            className={`${styles.tab} ${mode === 'login' ? styles.tabActive : ''}`}
            onClick={() => switchMode('login')}
          >
            Sign In
          </button>
          <button
            role="tab"
            aria-selected={mode === 'register'}
            className={`${styles.tab} ${mode === 'register' ? styles.tabActive : ''}`}
            onClick={() => switchMode('register')}
          >
            Create Account
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form} noValidate>
          {mode === 'register' && (
            <div className={styles.field}>
              <label htmlFor="fullName" className={styles.fieldLabel}>Full Name</label>
              <input
                id="fullName"
                type="text"
                value={fullName}
                onChange={e => setFullName(e.target.value)}
                placeholder="Jane Smith"
                required
                autoComplete="name"
              />
            </div>
          )}

          <div className={styles.field}>
            <label htmlFor="username" className={styles.fieldLabel}>Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="username"
              required
              autoComplete="username"
            />
          </div>

          {mode === 'register' && (
            <div className={styles.field}>
              <label htmlFor="email" className={styles.fieldLabel}>Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="user@example.com"
                required
                autoComplete="email"
              />
            </div>
          )}

          <div className={styles.field}>
            <label htmlFor="password" className={styles.fieldLabel}>Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </div>

          {error && (
            <p className={styles.error} role="alert">{error}</p>
          )}

          <button
            type="submit"
            className={`btn btn-primary ${styles.submitBtn}`}
            disabled={loading}
          >
            {loading
              ? 'Please wait…'
              : mode === 'login'
              ? 'Sign In'
              : 'Create Account'}
          </button>
        </form>
      </div>
    </div>
  );
}
