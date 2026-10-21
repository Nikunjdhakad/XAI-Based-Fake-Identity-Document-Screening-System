import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, ApiException } from '../api/client';
import type { LoginResponse, User } from '../types';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('vl_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    api.get<User>('/auth/me')
      .then(setUser)
      .catch(() => {
        localStorage.removeItem('vl_token');
        setToken(null);
      })
      .finally(() => setLoading(false));
  }, [token]);

  const login = useCallback(async (username: string, password: string) => {
    const res = await api.post<LoginResponse>('/auth/login', { username, password });
    localStorage.setItem('vl_token', res.token);
    setToken(res.token);
    setUser(res.user);
  }, []);

  const register = useCallback(async (
    username: string,
    email: string,
    password: string,
    fullName: string
  ) => {
    await api.post('/auth/register', { username, email, password, fullName });
    await login(username, password);
  }, [login]);

  const logout = useCallback(() => {
    localStorage.removeItem('vl_token');
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

// Suppress unused import warning - ApiException may be used by consumers
export { ApiException };
