#!/usr/bin/env bash
# =============================================================================
# emergency-rollback.sh
# Automated MongoDB backup + Liquibase rollback for emergency scenarios.
#
# Usage:
#   ./emergency-rollback.sh --tag <tag>                   # Rollback to a tag
#   ./emergency-rollback.sh --count <n>                   # Rollback last N changesets
#   ./emergency-rollback.sh --date <YYYY-MM-DDThh:mm:ss>  # Rollback to a date
#
# Prerequisites:
#   - liquibase CLI in PATH
#   - mongodump in PATH
#   - liquibase.properties configured (or pass --defaults-file)
# =============================================================================

set -euo pipefail

# ---- Configuration ----
MONGO_URI="${MONGO_URI:-mongodb://localhost:27017/myapp_db}"
BACKUP_DIR="${BACKUP_DIR:-/backups/mongodb}"
CHANGELOG_FILE="${CHANGELOG_FILE:-changelogs/db.changelog-master.xml}"
LIQUIBASE_DEFAULTS="${LIQUIBASE_DEFAULTS:-}"

# ---- Color output ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ---- Parse arguments ----
ROLLBACK_MODE=""
ROLLBACK_VALUE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --tag)    ROLLBACK_MODE="tag";   ROLLBACK_VALUE="$2"; shift 2 ;;
        --count)  ROLLBACK_MODE="count"; ROLLBACK_VALUE="$2"; shift 2 ;;
        --date)   ROLLBACK_MODE="date";  ROLLBACK_VALUE="$2"; shift 2 ;;
        --help|-h)
            echo "Usage: $0 --tag <tag> | --count <n> | --date <datetime>"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [[ -z "$ROLLBACK_MODE" ]]; then
    log_error "No rollback mode specified. Use --tag, --count, or --date."
    exit 1
fi

# ---- Step 1: Show current state ----
log_info "Current Liquibase history:"
liquibase history --changelog-file="$CHANGELOG_FILE"
echo ""

# ---- Step 2: Backup ----
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="${BACKUP_DIR}/pre-rollback-${TIMESTAMP}"
mkdir -p "$BACKUP_PATH"

log_info "Creating MongoDB backup at ${BACKUP_PATH} ..."
if mongodump --uri="$MONGO_URI" --out="$BACKUP_PATH"; then
    log_info "Backup successful."
else
    log_error "Backup FAILED. Aborting rollback."
    exit 1
fi

# ---- Step 3: Execute rollback ----
log_info "Executing rollback (mode=${ROLLBACK_MODE}, value=${ROLLBACK_VALUE}) ..."

DEFAULTS_FLAG=""
if [[ -n "$LIQUIBASE_DEFAULTS" ]]; then
    DEFAULTS_FLAG="--defaults-file=$LIQUIBASE_DEFAULTS"
fi

case $ROLLBACK_MODE in
    tag)
        liquibase $DEFAULTS_FLAG rollback \
            --tag="$ROLLBACK_VALUE" \
            --changelog-file="$CHANGELOG_FILE"
        ;;
    count)
        liquibase $DEFAULTS_FLAG rollback-count \
            --count="$ROLLBACK_VALUE" \
            --changelog-file="$CHANGELOG_FILE"
        ;;
    date)
        liquibase $DEFAULTS_FLAG rollback-to-date \
            --date="$ROLLBACK_VALUE" \
            --changelog-file="$CHANGELOG_FILE"
        ;;
esac

if [[ $? -eq 0 ]]; then
    log_info "Rollback completed successfully."
else
    log_error "Rollback FAILED. Backup available at: ${BACKUP_PATH}"
    log_error "To restore: mongorestore --uri=\"${MONGO_URI}\" --drop ${BACKUP_PATH}/$(basename ${MONGO_URI##*/})"
    exit 1
fi

# ---- Step 4: Verify ----
log_info "Post-rollback state:"
liquibase history --changelog-file="$CHANGELOG_FILE"

echo ""
log_info "Rollback complete. Backup stored at: ${BACKUP_PATH}"
log_warn "Run smoke tests to verify application behavior."
