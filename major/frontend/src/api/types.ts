// Shared API type definitions for VeriLens

export type DocumentStatus =
  | 'UPLOADED'
  | 'QUEUED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'PARTIALLY_COMPLETED'

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export type DocumentType =
  | 'IDENTITY'
  | 'CERTIFICATE'
  | 'EXPERIENCE'
  | 'INSTITUTIONAL'
  | 'REPORT'

export type AnalysisTaskStatus =
  | 'QUEUED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'PARTIALLY_COMPLETED'

export type ModuleStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'NOT_APPLICABLE'

export interface ErrorResponse {
  status: number
  error: string
  message: string
  timestamp: string
  path: string
}

export interface UserResponse {
  id: string
  username: string
  email: string
  fullName: string
  role: string
  createdAt: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  fullName: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserResponse
}

export interface DocumentResponse {
  id: string
  filename: string
  documentType: string
  description: string | null
  status: DocumentStatus
  riskLevel: RiskLevel | null
  fileSize: number
  mimeType: string
  uploadedAt: string
  uploadedBy: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface AnalysisTaskResponse {
  jobId: string
  documentId: string
  status: AnalysisTaskStatus
  requestedAt: string
}

export interface AnalysisStatusProgress {
  ocr: ModuleStatus
  forensics: ModuleStatus
  metadata: ModuleStatus
  identity: ModuleStatus
  face: ModuleStatus
  aiManipulation: ModuleStatus
}

export interface AnalysisStatusResponse {
  jobId: string
  documentId: string
  status: AnalysisTaskStatus
  progress: AnalysisStatusProgress
  requestedModules: string[]
  requestedAt: string
  startedAt: string | null
  completedAt: string | null
}

export interface RiskAssessment {
  score: number
  level: RiskLevel
  explanation: string[]
  moduleContributions: Record<string, number | null>
  version: string
}

export interface AnalysisResult {
  jobId: string
  documentId: string
  status: AnalysisTaskStatus
  riskAssessment: RiskAssessment | null
  modules: Record<string, unknown>
  suspiciousRegions: unknown[]
  completedAt: string | null
  processingTimeMs: number
}

export interface HealthResponse {
  status: string
  service: string
  timestamp: string
  database: string
  aiService: string
}
