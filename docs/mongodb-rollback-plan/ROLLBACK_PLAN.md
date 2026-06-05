# MongoDB Rollback Plan with Liquibase

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture](#architecture)
4. [Liquibase MongoDB Configuration](#liquibase-mongodb-configuration)
5. [Changelog Design for Rollback](#changelog-design-for-rollback)
6. [Rollback Strategies](#rollback-strategies)
7. [Rollback Commands Reference](#rollback-commands-reference)
8. [Step-by-Step Rollback Procedures](#step-by-step-rollback-procedures)
9. [Data Backup and Recovery](#data-backup-and-recovery)
10. [Known Limitations](#known-limitations)
11. [Troubleshooting](#troubleshooting)
12. [Appendix: Example Changelogs](#appendix-example-changelogs)

---

## Overview

This document outlines a comprehensive rollback plan for managing MongoDB schema and data migrations using **Liquibase** with the **Liquibase MongoDB extension** (open-source) or the **Liquibase MongoDB Pro/Secure extension** (commercial).

Liquibase tracks all applied changesets in a `DATABASECHANGELOG` collection within MongoDB. When a rollback is executed, Liquibase reverts the changes defined in the changeset's `rollback` block and removes the corresponding entry from the tracking collection.

> **Important:** MongoDB is schema-less, so "schema migrations" typically involve collection creation/deletion, index management, validator rules, and reference data. Unlike SQL databases, Liquibase cannot auto-generate rollback statements for MongoDB — **you must define explicit rollback blocks for every changeset**.

---

## Prerequisites

| Component | Version | Notes |
|-----------|---------|-------|
| Liquibase CLI | 4.20+ | Core engine |
| Liquibase MongoDB Extension (OSS) | 4.24.0+ | For JSON/XML/YAML changelogs with modeled change types |
| Liquibase MongoDB Pro/Secure Extension | 1.3.0+ | For Formatted Mongo (`.js`) changelogs + targeted rollback |
| MongoDB Server | 4.4+ | Target database |
| Java | 17+ | Required by Liquibase |
| mongosh | 2.0+ | Required by Pro extension for Formatted Mongo changelogs |

### Installation (OSS Extension)

Add the extension JAR to your Liquibase `lib/` directory or include it as a dependency:

**Gradle:**
```groovy
dependencies {
    implementation 'org.liquibase.ext:liquibase-mongodb:4.29.2'
}
```

**Maven:**
```xml
<dependency>
    <groupId>org.liquibase.ext</groupId>
    <artifactId>liquibase-mongodb</artifactId>
    <version>4.29.2</version>
</dependency>
```

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   Liquibase CLI / API                     │
│  (update, rollback, rollback-count, rollback-to-date)    │
└──────────────────────┬───────────────────────────────────┘
                       │
          ┌────────────▼────────────┐
          │   Changelog Files       │
          │  (JSON / YAML / XML     │
          │   or Formatted Mongo)   │
          └────────────┬────────────┘
                       │
          ┌────────────▼────────────┐
          │  Liquibase MongoDB      │
          │  Extension              │
          │  (translates change     │
          │   types → mongo cmds)   │
          └────────────┬────────────┘
                       │
          ┌────────────▼────────────┐
          │   MongoDB Server        │
          │                         │
          │  ┌───────────────────┐  │
          │  │ DATABASECHANGELOG │  │  ← Tracks applied changesets
          │  └───────────────────┘  │
          │  ┌───────────────────┐  │
          │  │ DATABASECHANGELOG │  │  ← Manages deployment locks
          │  │ LOCK              │  │
          │  └───────────────────┘  │
          │  ┌───────────────────┐  │
          │  │ App Collections   │  │  ← Your data
          │  └───────────────────┘  │
          └─────────────────────────┘
```

---

## Liquibase MongoDB Configuration

### `liquibase.properties`

```properties
# Connection
url=mongodb://localhost:27017/myapp_db
# For authenticated connections:
# url=mongodb://username:password@host:27017/myapp_db?authSource=admin

# Changelog
changelog-file=changelogs/db.changelog-master.json

# Liquibase behavior
liquibase.hub.mode=off
```

> **Note:** Do not specify a `driver` property — the MongoDB extension handles this automatically.

### Connection String Formats

| Format | Example |
|--------|---------|
| Standard | `mongodb://host1:27017,host2:27017/dbname?replicaSet=myRS` |
| DNS Seed List | `mongodb+srv://cluster0.example.com/dbname` |
| Authenticated | `mongodb://user:pass@host:27017/dbname?authSource=admin` |

---

## Changelog Design for Rollback

### Golden Rule

> **Every changeset MUST have an explicit `rollback` block.** Liquibase cannot auto-generate rollback for MongoDB change types.

### Changelog Organization

```
liquibase/
├── changelogs/
│   ├── db.changelog-master.json         # Root changelog (includes others)
│   ├── db.changelog-1.0.0.json          # Release 1.0.0 changes
│   ├── db.changelog-1.1.0.json          # Release 1.1.0 changes
│   └── db.changelog-1.2.0.json          # Release 1.2.0 changes
├── rollback-scripts/
│   └── emergency-rollback.sh            # Emergency rollback helper
├── liquibase.properties
└── README.md
```

### Rollback Patterns per Change Type

| Change Type | Forward Action | Rollback Action |
|-------------|---------------|-----------------|
| `createCollection` | Create a collection | `dropCollection` the same collection |
| `dropCollection` | Drop a collection | `createCollection` + re-insert data (requires backup) |
| `createIndex` | Create an index | `dropIndex` with the same keys |
| `dropIndex` | Drop an index | `createIndex` with the same keys and options |
| `insertMany` / `insertOne` | Insert documents | `runCommand` with `delete` to remove the inserted documents |
| `runCommand` (update) | Modify documents | `runCommand` with reverse update |
| `runCommand` (collMod) | Modify collection options | `runCommand` with previous options |

---

## Rollback Strategies

### Strategy 1: Rollback by Tag (Recommended for Releases)

Tag your database after each successful deployment. Roll back to the last known-good tag.

```bash
# After a successful deployment, tag it
liquibase tag --tag=release-1.0.0

# To rollback to a tag
liquibase rollback --tag=release-1.0.0
```

**When to use:** Production releases, milestone deployments.

### Strategy 2: Rollback by Count

Roll back a specific number of recently applied changesets.

```bash
# Rollback the last 3 changesets
liquibase rollback-count --count=3
```

**When to use:** Quick fix during a deployment that partially failed.

### Strategy 3: Rollback by Date

Roll back all changesets applied after a specific date/time.

```bash
# Rollback all changes applied after a specific date
liquibase rollback-to-date --date=2025-06-01T10:00:00
```

**When to use:** Time-based recovery when you know the exact moment things went wrong.

### Strategy 4: Targeted Rollback (Pro/Secure Only)

Roll back a single changeset without reverting the ones applied after it.

```bash
# Rollback a specific changeset
liquibase rollback-one-changeset \
  --changeset-id="3" \
  --changeset-author="admin" \
  --changelog-file=changelogs/db.changelog-master.json \
  --force
```

**When to use:** A single changeset caused an issue but later ones are fine.

---

## Rollback Commands Reference

### Supported Commands (MongoDB)

| Command | Description | OSS | Pro |
|---------|------------|-----|-----|
| `rollback --tag=<tag>` | Reverts to a tagged state | ✅ | ✅ |
| `rollback-count --count=<n>` | Reverts the last N changesets | ✅ | ✅ |
| `rollback-to-date --date=<d>` | Reverts changesets applied after a date | ✅ | ✅ |
| `rollback-one-changeset` | Reverts a single targeted changeset | ❌ | ✅ |
| `tag --tag=<name>` | Tags current database state | ✅ | ✅ |
| `history` | Shows all applied changesets | ✅ | ✅ |
| `status` | Shows pending (unapplied) changesets | ✅ | ✅ |

### Unsupported Commands (MongoDB)

These SQL-output commands are **not supported** for MongoDB:

- `rollback-sql`
- `rollback-count-sql`
- `rollback-to-date-sql`
- `rollback-one-changeset-sql`
- `future-rollback-sql`

---

## Step-by-Step Rollback Procedures

### Procedure A: Planned Release Rollback

Use this when a deployment to production needs to be fully reverted.

```
Step 1: Verify the current database state
  $ liquibase history
  $ liquibase status

Step 2: Take a MongoDB backup BEFORE rolling back
  $ mongodump --uri="mongodb://host:27017/myapp_db" \
      --out=/backups/pre-rollback-$(date +%Y%m%d_%H%M%S)

Step 3: Identify the target tag
  $ liquibase history
  → Find the tag you want to rollback to (e.g., release-1.0.0)

Step 4: Execute the rollback
  $ liquibase rollback --tag=release-1.0.0 \
      --changelog-file=changelogs/db.changelog-master.json

Step 5: Verify the rollback
  $ liquibase history
  → Confirm that only changesets up to the target tag remain

Step 6: Validate application behavior
  → Run smoke tests against the database
  → Verify collections and indexes match expected state
```

### Procedure B: Emergency Hotfix Rollback

Use this when the last deployment broke something and you need to undo N changesets fast.

```
Step 1: Identify how many changesets to rollback
  $ liquibase history
  → Count the changesets applied in the last deployment

Step 2: MongoDB backup
  $ mongodump --uri="mongodb://host:27017/myapp_db" \
      --out=/backups/emergency-$(date +%Y%m%d_%H%M%S)

Step 3: Execute rollback
  $ liquibase rollback-count --count=<N>

Step 4: Verify
  $ liquibase history
  $ mongosh --eval "db.getCollectionNames()"
```

### Procedure C: Targeted Single-Changeset Rollback (Pro Only)

Use this when one changeset is the root cause but subsequent ones are safe.

```
Step 1: Identify the problematic changeset
  $ liquibase history
  → Note the changeset id, author, and changelog file

Step 2: MongoDB backup
  $ mongodump --uri="mongodb://host:27017/myapp_db" \
      --out=/backups/targeted-$(date +%Y%m%d_%H%M%S)

Step 3: Execute targeted rollback
  $ liquibase rollback-one-changeset \
      --changeset-id="<id>" \
      --changeset-author="<author>" \
      --changelog-file=<file> \
      --force

Step 4: Verify
  $ liquibase history
```

---

## Data Backup and Recovery

### Pre-Rollback Backup (Mandatory)

Always take a backup before executing any rollback. MongoDB's `mongodump` provides a point-in-time snapshot:

```bash
#!/bin/bash
# backup-before-rollback.sh
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/mongodb/pre-rollback-${TIMESTAMP}"
MONGO_URI="mongodb://localhost:27017/myapp_db"

echo "Creating backup at ${BACKUP_DIR}..."
mongodump --uri="${MONGO_URI}" --out="${BACKUP_DIR}"

if [ $? -eq 0 ]; then
    echo "Backup successful: ${BACKUP_DIR}"
else
    echo "ERROR: Backup failed. Do NOT proceed with rollback."
    exit 1
fi
```

### Restore from Backup (Last Resort)

If a Liquibase rollback fails or produces unexpected results:

```bash
mongorestore --uri="mongodb://localhost:27017/myapp_db" \
    --drop \
    /backups/mongodb/pre-rollback-<timestamp>/myapp_db
```

---

## Known Limitations

1. **No Auto-Rollback Generation:**
   Unlike SQL databases, Liquibase cannot auto-generate rollback statements for MongoDB. You must explicitly define rollback blocks for every changeset.

2. **Data Loss on `dropCollection` Rollback:**
   If a changeset creates a collection and the rollback drops it, all data in that collection is permanently lost. For collections that accumulate data post-deployment, consider a rollback strategy that preserves data.

3. **No SQL-Output Commands:**
   Commands like `rollback-sql`, `rollback-count-sql` etc. are not supported for MongoDB. You cannot preview rollback operations in advance (except by reviewing the changelog rollback blocks manually).

4. **Document-Level Rollback Complexity:**
   Rolling back `insertMany` or `update` operations requires careful query construction to match and revert only the affected documents. Use unique identifiers in your rollback queries.

5. **Historical Bug — OSS Extension:**
   Early versions (< 4.24) of the OSS extension had issues where rollback removed the `DATABASECHANGELOG` entry but did not execute the rollback block (see [GitHub Issue #38](https://github.com/liquibase/liquibase-mongodb/issues/38)). Always use version 4.24.0 or later.

6. **Formatted Mongo `.js` Changelogs:**
   Rollback in Formatted Mongo changelogs uses `//rollback` comment syntax. These require the Liquibase Pro/Secure extension and `mongosh` installed on the machine.

7. **No Transactions for Standalone Deployments:**
   MongoDB only supports multi-document transactions on replica sets. If running against a standalone instance, rollback operations are not atomic — a failure mid-rollback can leave the database in an inconsistent state. Use replica sets in production.

---

## Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| "collection already exists" after rollback + re-update | Rollback block did not drop the collection | Add explicit `dropCollection` in the rollback block; upgrade to OSS extension 4.24.0+ |
| "please run 'rollback-one-changeset-sql'" | Pro extension incorrectly suggests SQL preview | Use `--force` flag; this is a known issue (see [GitHub #4288](https://github.com/liquibase/liquibase/issues/4288)) |
| Rollback removes DATABASECHANGELOG entry but collection persists | Rollback block missing or empty | Always define explicit rollback blocks; verify with `liquibase history` |
| Lock stuck — "Could not acquire change log lock" | Previous Liquibase run crashed | Run `liquibase release-locks` |
| Connection refused | Wrong URI or MongoDB not running | Verify `url` in `liquibase.properties`; check `mongosh` connectivity |

---

## Appendix: Example Changelogs

See the example changelog files in the [`liquibase/changelogs/`](../../liquibase/changelogs/) directory:

- **`db.changelog-master.json`** — Root changelog that includes versioned changelogs.
- **`db.changelog-1.0.0.json`** — Collection creation, indexes, seed data with full rollback blocks.
- **`db.changelog-1.1.0.json`** — Schema modifications, validator updates, new indexes with rollback.
- **`formatted-mongo-example.js`** — Formatted Mongo changelog example (Pro/Secure only).

See the helper scripts in [`liquibase/rollback-scripts/`](../../liquibase/rollback-scripts/):

- **`emergency-rollback.sh`** — Automated backup + rollback script for emergency scenarios.
