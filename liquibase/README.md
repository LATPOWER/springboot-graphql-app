# Liquibase MongoDB Rollback Setup

This directory contains the Liquibase configuration and changelog files for managing MongoDB schema migrations with full rollback support.

## Directory Structure

```
liquibase/
├── liquibase.properties                  # Connection and config
├── changelogs/
│   ├── db.changelog-master.json          # Root changelog
│   ├── db.changelog-1.0.0.json          # Release 1.0.0: collections, indexes, seed data
│   ├── db.changelog-1.1.0.json          # Release 1.1.0: reviews, validator updates
│   └── formatted-mongo-example.js        # Formatted Mongo example (Pro only)
└── README.md
```

## Quick Start

### Prerequisites

- Liquibase CLI 4.20+
- Liquibase MongoDB Extension (`liquibase-mongodb` JAR in `lib/`)
- MongoDB 4.4+
- Java 17+

### Deploy Changes

```bash
cd liquibase/
liquibase update
```

### Check Status

```bash
# View applied changesets
liquibase history

# View pending changesets
liquibase status
```

### Tag a Release

```bash
liquibase tag --tag=release-1.0.0
```

### Rollback

```bash
# By tag (recommended for releases)
liquibase rollback --tag=release-1.0.0

# By count (undo last N changesets)
liquibase rollback-count --count=3

# By date (undo everything after a specific date)
liquibase rollback-to-date --date=2025-06-01T10:00:00
```

### Targeted Rollback (Pro/Secure Only)

```bash
# Undo a single specific changeset without affecting later ones
liquibase rollback-one-changeset \
  --changeset-id="1.1.0-003" \
  --changeset-author="admin" \
  --changelog-file=changelogs/db.changelog-master.json \
  --force
```

## Full Documentation

See [docs/mongodb-rollback-plan/ROLLBACK_PLAN.md](../docs/mongodb-rollback-plan/ROLLBACK_PLAN.md) for the complete rollback plan with strategies, procedures, and troubleshooting.
