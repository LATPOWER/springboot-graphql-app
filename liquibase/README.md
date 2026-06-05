# Liquibase MongoDB Rollback Setup

This directory contains the Liquibase configuration and changelog files for managing MongoDB schema migrations with full rollback support.

## Directory Structure

```
liquibase/
├── liquibase.properties                  # Connection and config
├── changelogs/
│   ├── db.changelog-master.xml           # Root changelog
│   ├── db.changelog-1.0.0.xml           # Release 1.0.0: collections, indexes, seed data
│   ├── db.changelog-1.1.0.xml           # Release 1.1.0: reviews, validator updates
│   └── formatted-mongo-example.js        # Formatted Mongo example (Pro only)
├── rollback-scripts/
│   └── emergency-rollback.sh             # Backup + rollback automation
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

### Tag a Release

```bash
liquibase tag --tag=release-1.0.0
```

### Rollback

```bash
# By tag
liquibase rollback --tag=release-1.0.0

# By count
liquibase rollback-count --count=3

# By date
liquibase rollback-to-date --date=2025-06-01T10:00:00
```

### Emergency Rollback (with backup)

```bash
chmod +x rollback-scripts/emergency-rollback.sh
./rollback-scripts/emergency-rollback.sh --tag release-1.0.0
./rollback-scripts/emergency-rollback.sh --count 5
```

## Full Documentation

See [docs/mongodb-rollback-plan/ROLLBACK_PLAN.md](../docs/mongodb-rollback-plan/ROLLBACK_PLAN.md) for the complete rollback plan with strategies, procedures, and troubleshooting.
