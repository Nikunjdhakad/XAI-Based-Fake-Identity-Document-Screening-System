const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api';

function getToken(): string | null {
  return localStorage.getItem('vl_token');
}

interface ApiErrorShape {
  status: number;
  error: string;
  message: string;
  path?: string;
}

export class ApiException extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = 'ApiException';
  }
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  isFormData = false
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (!isFormData && body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: isFormData
      ? (body as FormData)
      : body !== undefined
      ? JSON.stringify(body)
      : undefined,
  });

  if (res.status === 204) return undefined as T;

  const data = await res.json().catch(() => ({})) as ApiErrorShape;
  if (!res.ok) {
    throw new ApiException(data.message || 'Request failed', res.status);
  }
  return data as unknown as T;
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body: unknown) => request<T>('POST', path, body),
  postForm: <T>(path: string, form: FormData) => request<T>('POST', path, form, true),
  delete: <T>(path: string) => request<T>('DELETE', path),
};
