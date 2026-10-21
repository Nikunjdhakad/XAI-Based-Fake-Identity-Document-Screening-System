import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import styles from './AppLayout.module.css';

const NAV = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/upload', label: 'New Screening' },
  { to: '/history', label: 'History' },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.brandName}>VeriLens</span>
          <span className={styles.brandTag}>Document Screening</span>
        </div>
        <nav className={styles.nav} aria-label="Main navigation">
          {NAV.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `${styles.navItem} ${isActive ? styles.active : ''}`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>
        <div className={styles.userSection}>
          <div className={styles.userName}>{user?.username}</div>
          <div className={styles.userRole}>{user?.role}</div>
          <button
            className={`btn btn-ghost ${styles.logoutBtn}`}
            onClick={handleLogout}
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}
