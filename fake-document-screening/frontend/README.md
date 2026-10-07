# VeriLens — Frontend

React + TypeScript + Vite application.

## Status

**Not yet initialized.** The React project will be scaffolded in Phase 1.

## Responsibilities

- Authentication screens (login / register)
- Dashboard with recent screenings and risk distribution
- Document upload workspace
- Analysis progress view
- Results and evidence display
- Suspicious-region viewer
- Screening history with search/filter
- Document detail and audit trail
- Report download

## Technology

| Tool        | Purpose            |
| ----------- | ------------------ |
| React 18+   | UI framework       |
| TypeScript   | Type safety        |
| Vite         | Build tool         |
| CSS Modules  | Scoped styling     |

## Communication

The frontend communicates **only** with the Spring Boot backend via REST/JSON.  
It must **never** call the Python AI service directly.
