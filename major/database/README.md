# VeriLens — Database

PostgreSQL 16 schema, migrations, and seed data.

## Status

**Not yet initialized.** Migrations and init scripts will be created starting in Phase 1.

## Structure

```
database/
├── migrations/       # Versioned schema migrations
├── init/             # Docker entrypoint init scripts
└── README.md
```

## Principles

- Use UUIDs for public-facing identifiers
- Store timestamps on all mutable entities
- Use foreign-key relationships — do not duplicate data
- Never store passwords in plaintext (use bcrypt)
- All schema changes are versioned migrations
