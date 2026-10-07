# VeriLens — Backend

Java 21 / Spring Boot 3.x application — the central orchestration layer.

## Status

**Not yet initialized.** The Maven project will be created in Phase 1.

## Responsibilities

- Authentication and authorization (Spring Security + JWT)
- User and role management
- Document lifecycle (upload, storage, retrieval, deletion)
- Analysis job orchestration
- REST API for the frontend
- Internal REST calls to the Python AI service
- Evidence storage in PostgreSQL
- Risk-score aggregation (evidence fusion)
- Report metadata and generation
- Audit trail
- Input validation and error handling

## Technology

| Tool              | Purpose              |
| ----------------- | -------------------- |
| Java 21 LTS       | Language             |
| Spring Boot 3.x   | Framework            |
| Spring Web         | REST controllers     |
| Spring Data JPA    | Database access      |
| Spring Security    | Auth                 |
| JWT                | Token authentication |
| PostgreSQL Driver  | Database driver      |
| Maven              | Build tool           |

## Communication

```
React  →  Spring Boot  →  Python AI Service
                ↕
           PostgreSQL
```
