export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface DocumentResponse {
  id: string;
  filename: string;
  documentType: string;
  description: string | null;
  status: 'UPLOADED' | 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIALLY_COMPLETED';
  riskLevel: string | null;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AnalysisTaskResponse {
  jobId: string;
  documentId: string;
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIALLY_COMPLETED';
  requestedModules: string | null;
  requestedAt: string;
  startedAt: string | null;
  completedAt: string | null;
}
