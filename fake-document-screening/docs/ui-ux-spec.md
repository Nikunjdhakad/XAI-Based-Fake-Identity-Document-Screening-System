# VeriLens — UI/UX Specification

**Version:** 0.1.0  
**Phase:** 0 — Specification  
**Last updated:** 2026-10-07

---

## 1. Product Identity

**Working Name:** VeriLens  
**Tagline:** "Evidence-first document screening."

The interface communicates: **upload → inspect → understand → decide**  
Not: **upload → AI magic → fake**

---

## 2. Design Direction

VeriLens must look and feel like a professional **digital document forensics workstation**.

### Inspired by
- Forensic analysis software
- Document review systems
- Enterprise security products
- Professional audit tools
- Modern editorial information design

### Must NOT resemble
- Generic AI SaaS dashboards
- AI chatbot interfaces
- Marketing landing pages with glowing effects
- Common dashboard templates

---

## 3. Visual Principles

### 3.1 Color System

Restrained, purposeful palette.

| Role          | Usage                                    |
| ------------- | ---------------------------------------- |
| Neutral base  | Backgrounds, surfaces                    |
| High-contrast | Primary text, headings                   |
| Brand primary | One accent color — actions, focus states  |
| Risk: red     | HIGH risk indicators                     |
| Risk: amber   | MEDIUM risk / warnings                   |
| Risk: green   | LOW risk indicators                      |
| Informational | Blue — neutral informational elements     |

The interface should NOT be colorful. Color is used to communicate meaning, not decoration.

### 3.2 Typography

- Professional modern sans-serif (e.g., Inter, IBM Plex Sans, or similar)
- Strong hierarchy: page title → section title → body → metadata → labels
- No oversized marketing typography inside the application
- Monospace font for extracted data, confidence values, and technical details

### 3.3 Layout Principles

- Information-dense but uncluttered
- Split panes where document + evidence are shown together
- The **document itself** is always the visual focus on analysis pages
- Consistent spacing system
- Responsive — functional on tablet and desktop (mobile is secondary)

### 3.4 Motion

- Subtle transitions only when they communicate state changes
- Upload progress indicators
- Analysis stage transitions
- Expanding/collapsing evidence sections
- Region highlighting on interaction
- **No decorative animations**

---

## 4. Screen Specifications

### 4.1 Login / Register

- Minimal, professional
- VeriLens branding — name + tagline
- Clean form, clear validation feedback
- No decorative hero images or AI graphics

### 4.2 Dashboard

Shows:
- Total screened documents (simple count)
- Recent screenings (table or list with status + risk)
- Risk distribution (simple bar or minimal chart — only if genuinely useful)
- Processing status (any currently running analyses)
- Quick upload action

**Not allowed:** Excessive stat cards, meaningless charts, decorative widgets.

### 4.3 New Screening (Upload)

The upload page should feel like a **document-analysis workspace**, not a simple file input.

Includes:
- Drag-and-drop zone
- File picker button
- Supported format information (PDF, JPG, PNG)
- Document type selector
- Optional description field
- Optional reference face upload
- Analysis settings (optional module selection)

### 4.4 Analysis Progress

Shows actual stages as they complete:

```text
Upload            ✓
Preprocessing     ✓
OCR               ✓
Image Forensics   ●  (running)
Metadata          ○  (pending)
Identity Check    ○
Face Verification ○
AI Screening      ○
Evidence Fusion   ○
Report            ○
```

- Do NOT use a fake percentage if the backend cannot provide real progress
- Show elapsed time
- Show module status as it updates

### 4.5 Results (Most Important Page)

The results page is the core product experience.

**Conceptual layout:**

```text
────────────────────────────────────────────────
RISK: HIGH                       SCORE: 74/100
────────────────────────────────────────────────

DOCUMENT PREVIEW        EVIDENCE SUMMARY

[Document image/PDF]    OCR              LOW     ●
                        IMAGE FORENSICS   HIGH    ●
                        METADATA          MEDIUM  ●
                        IDENTITY          LOW     ●
                        FACE              MATCH   ●
                        AI SCREENING      HIGH    ●

────────────────────────────────────────────────
SUSPICIOUS REGIONS

[Document with highlighted overlay regions]
[Region legend: module, severity, confidence]

────────────────────────────────────────────────
WHY THIS RESULT?

• Suspicious visual region detected in header area
• Metadata indicates re-save with editing software
• AI manipulation screening returned elevated risk

────────────────────────────────────────────────
DETAILED ANALYSIS

▶ OCR Results          [expandable]
▶ Image Forensics      [expandable]
▶ Metadata Analysis    [expandable]
▶ Identity Consistency [expandable]
▶ Face Verification    [expandable]
▶ AI Screening         [expandable]

                     DOWNLOAD REPORT
────────────────────────────────────────────────
```

Key requirements:
- Document is the visual focus
- Risk assessment is immediately visible
- Evidence summary provides at-a-glance module status
- Suspicious regions are overlaid on the document
- Explanations use careful language (not "definitely fake")
- Detailed analysis is available but not overwhelming

### 4.6 History

- Searchable/filterable list of past screenings
- Filter by: date, document type, risk level, status
- Sort options
- Click to view full results

### 4.7 Document Details

- Full metadata about the uploaded document
- Complete audit trail (upload, analysis start, completion, views, downloads)
- Link to analysis results
- Link to report download

---

## 5. Language Guidelines

### Use
- "Potential manipulation detected"
- "Elevated risk"
- "Evidence found"
- "Screening result"
- "Module unavailable"
- "Insufficient image quality"
- "Similarity: 0.87"
- "Risk assessment"

### Never use
- "Definitely fake"
- "100% AI generated"
- "Authenticity guaranteed"
- "Government verified"
- "Proven forgery"

---

## 6. Accessibility Requirements

- Keyboard navigation for all interactive elements
- Readable contrast ratios (WCAG AA minimum)
- Visible focus states
- Semantic HTML elements and ARIA labels
- Meaningful error messages
- Responsive layout (desktop-first, tablet-functional)
- Color is never the sole indicator — always paired with text/icons

---

## 7. Component Library (Planned)

Core components to be built in Phase 3:

| Component           | Purpose                                  |
| ------------------- | ---------------------------------------- |
| AppLayout           | Shell with navigation                    |
| DocumentUploader    | Drag-and-drop + file picker              |
| RiskBadge           | LOW / MEDIUM / HIGH indicator            |
| ModuleStatusCard    | Per-module status in evidence summary    |
| AnalysisProgress    | Step-by-step analysis tracker            |
| DocumentViewer      | Image/PDF viewer with region overlay     |
| EvidencePanel       | Expandable module detail sections        |
| DataTable           | Filterable, sortable history table       |
| StatusIndicator     | Job status visualization                 |
| AuditTimeline       | Chronological event trail                |

---

## 8. Implementation Notes

- The UI will be built in Phase 3
- Phase 0 defines the specification only
- This specification should be revisited before Phase 3 implementation
- Design mockups may be created before coding begins
