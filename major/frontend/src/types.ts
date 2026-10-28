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

// ── Phase 10: Risk Assessment ──────────────────────────────────────────────

export interface RiskAssessmentResponse {
  id: string;
  score: number;
  level: 'LOW' | 'MEDIUM' | 'HIGH';
  explanationJson: string | null;       // JSON array string
  moduleContributionsJson: string | null; // JSON object string
  scoringVersion: string;
  createdAt: string;
}

// ── Phase 4: OCR ──────────────────────────────────────────────────────────

export interface OcrResultResponse {
  id: string;
  status: string;
  extractedText: string | null;
  overallConfidence: number | null;
  pageCount: number | null;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
}

// ── Phase 5: Metadata ─────────────────────────────────────────────────────

export interface MetadataResultResponse {
  id: string;
  status: string;
  available: boolean | null;
  propertiesJson: string | null;
  anomaliesJson: string | null;
  evidenceScore: number | null;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
}

// ── Phase 6: Identity ─────────────────────────────────────────────────────

export interface IdentityConsistencyResponse {
  id: string;
  status: string;
  consistent: boolean | null;
  checksJson: string | null;
  failedHigh: number | null;
  failedMedium: number | null;
  failedLow: number | null;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
}

// ── Phase 7: Forensics ────────────────────────────────────────────────────

export interface ForensicResultResponse {
  id: string;
  status: string;
  riskScore: number | null;
  findingsJson: string | null;
  suspiciousRegionsJson: string | null;
  elaMeanError: number | null;
  noiseMean: number | null;
  noiseStd: number | null;
  edgeMean: number | null;
  edgeStd: number | null;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
}

// ── Phase 8: Face ─────────────────────────────────────────────────────────

export interface FaceVerificationResponse {
  id: string;
  status: string;
  similarity: number | null;
  threshold: number | null;
  documentFaceDetected: boolean | null;
  referenceFaceDetected: boolean | null;
  note: string | null;
  reason: string | null;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
}

// ── Phase 9: AI Manipulation ──────────────────────────────────────────────

export interface AiManipulationResponse {
  id: string;
  status: string;
  riskScore: number | null;
  manipulationType: string | null;
  confidence: number | null;
  findingsJson: string | null;
  suspiciousRegionsJson: string | null;
  modelInfoJson: string | null;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
}

// ── Suspicious Region (shared shape used by forensics + AI manipulation) ──

export interface SuspiciousRegion {
  x: number;
  y: number;
  width: number;
  height: number;
  source: string;
  confidence: number;
  description: string;
}
