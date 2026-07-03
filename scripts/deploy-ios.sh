#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODE_ROOT="$(cd "$REPO_ROOT/.." && pwd)"
SKIP_UPLOAD=false
BUMP_VERSION=true
BUNDLE_ID="com.markduenas.scorekeeper"
WORKSPACE="$REPO_ROOT/iosApp/iosApp.xcworkspace"
SCHEME="iosApp"
CONFIGURATION="Release"
EXPORT_OPTIONS_PLIST="$REPO_ROOT/scripts/ExportOptions.plist"
TEAM_ID="A8AAE5A4T6"

SHARED_ENV_FILE="$CODE_ROOT/.deploy-config/deploy.env"
if [[ -f "$SHARED_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$SHARED_ENV_FILE"
fi

ASC_API_KEY_PATH="${APP_STORE_CONNECT_API_KEY_PATH:-${ASC_KEY_PATH:-}}"
ASC_API_KEY_ID="${APP_STORE_CONNECT_API_KEY_ID:-${ASC_KEY_ID:-}}"
ASC_ISSUER_ID="${APP_STORE_CONNECT_API_ISSUER_ID:-${ASC_ISSUER_ID:-}}"
RELEASE_NOTES="${RELEASE_NOTES:-}"
RELEASE_NOTES_FILE="${RELEASE_NOTES_FILE:-}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/deploy-ios.sh [options]

Options:
  --skip-upload                Skip IPA export and upload (archive only)
  --no-bump-version            Do not increment build number
  --asc-key-path <path>        App Store Connect API key (.p8 file)
  --asc-key-id <id>            App Store Connect API key ID
  --asc-issuer <id>            App Store Connect Issuer ID
  --release-notes <text>       TestFlight "What to Test" text for this build
  --release-notes-file <path>  Read "What to Test" text from a file instead of --release-notes
  -h, --help                   Show help

Environment (loaded from .deploy-config/deploy.env):
  APP_STORE_CONNECT_API_KEY_PATH
  APP_STORE_CONNECT_API_KEY_ID
  APP_STORE_CONNECT_API_ISSUER_ID
  RELEASE_NOTES                Same as --release-notes
  RELEASE_NOTES_FILE           Same as --release-notes-file
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-upload)
      SKIP_UPLOAD=true
      shift
      ;;
    --no-bump-version)
      BUMP_VERSION=false
      shift
      ;;
    --asc-key-path)
      ASC_API_KEY_PATH="$2"
      shift 2
      ;;
    --asc-key-id)
      ASC_API_KEY_ID="$2"
      shift 2
      ;;
    --asc-issuer)
      ASC_ISSUER_ID="$2"
      shift 2
      ;;
    --release-notes)
      RELEASE_NOTES="$2"
      shift 2
      ;;
    --release-notes-file)
      RELEASE_NOTES_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

XCODEPROJ="$REPO_ROOT/iosApp/iosApp.xcodeproj/project.pbxproj"
XCCONFIG="$REPO_ROOT/iosApp/Configuration/Config.xcconfig"

if [[ "$BUMP_VERSION" == "true" ]]; then
  CURRENT_BUILD="$(grep -m1 'CURRENT_PROJECT_VERSION' "$XCCONFIG" | sed -E 's/CURRENT_PROJECT_VERSION=([0-9]+)/\1/')"
  if [[ -z "$CURRENT_BUILD" ]]; then
    echo "Could not read CURRENT_PROJECT_VERSION from Config.xcconfig" >&2
    exit 1
  fi
  NEXT_BUILD=$((CURRENT_BUILD + 1))
  sed -i.bak -E "s/CURRENT_PROJECT_VERSION=${CURRENT_BUILD}/CURRENT_PROJECT_VERSION=${NEXT_BUILD}/" "$XCCONFIG"
  rm -f "${XCCONFIG}.bak"
  echo "Bumped CURRENT_PROJECT_VERSION: $CURRENT_BUILD -> $NEXT_BUILD"
fi

BUILD_NUMBER="$(grep -m1 'CURRENT_PROJECT_VERSION' "$XCCONFIG" | sed -E 's/CURRENT_PROJECT_VERSION=([0-9]+)/\1/')"
MARKETING_VERSION="$(grep 'MARKETING_VERSION' "$XCCONFIG" | sed -E 's/MARKETING_VERSION=(.+)/\1/' | tr -d ' ')"
echo "Building iOS $MARKETING_VERSION ($BUILD_NUMBER)"

ARCHIVE_DIR="${TMPDIR:-/tmp}/scorekeeper-archives"
ARCHIVE_PATH="$ARCHIVE_DIR/scorekeeper-${BUILD_NUMBER}.xcarchive"
EXPORT_DIR="${TMPDIR:-/tmp}/scorekeeper-ipa-${BUILD_NUMBER}"
mkdir -p "$ARCHIVE_DIR"

echo "Archiving $SCHEME..."
xcodebuild archive \
  -workspace "$WORKSPACE" \
  -scheme "$SCHEME" \
  -configuration "$CONFIGURATION" \
  -archivePath "$ARCHIVE_PATH" \
  -destination "generic/platform=iOS" \
  DEVELOPMENT_TEAM="$TEAM_ID" \
  | grep -E "^(error:|warning: .*(error|cannot)|Build succeeded|\*\* ARCHIVE|Archive Succeeded)" || true

if [[ ! -d "$ARCHIVE_PATH" ]]; then
  echo "Archive not found at $ARCHIVE_PATH" >&2
  exit 1
fi
echo "Archived: $ARCHIVE_PATH"

if [[ "$SKIP_UPLOAD" == "true" ]]; then
  echo "Upload skipped (--skip-upload)."
  exit 0
fi

echo "Exporting IPA..."
xcodebuild -exportArchive \
  -archivePath "$ARCHIVE_PATH" \
  -exportOptionsPlist "$EXPORT_OPTIONS_PLIST" \
  -exportPath "$EXPORT_DIR" \
  -allowProvisioningUpdates \
  -authenticationKeyPath "$ASC_API_KEY_PATH" \
  -authenticationKeyID "$ASC_API_KEY_ID" \
  -authenticationKeyIssuerID "$ASC_ISSUER_ID" \
  | grep -E "^(error:|Export succeeded|\*\* EXPORT)" || true

IPA_PATH="$(find "$EXPORT_DIR" -name '*.ipa' | sort | tail -1)"
if [[ -z "$IPA_PATH" ]]; then
  echo "IPA not found in $EXPORT_DIR" >&2
  exit 1
fi
echo "Exported IPA: $IPA_PATH"

if [[ -z "${ASC_API_KEY_ID:-}" || -z "${ASC_ISSUER_ID:-}" ]]; then
  echo "ASC_API_KEY_ID or ASC_ISSUER_ID not set. Set them in .deploy-config/deploy.env or pass --asc-key-id / --asc-issuer." >&2
  exit 1
fi

RELEASE_NOTES_TEXT=""
if [[ -n "$RELEASE_NOTES_FILE" ]]; then
  if [[ ! -f "$RELEASE_NOTES_FILE" ]]; then
    echo "Release notes file not found: $RELEASE_NOTES_FILE" >&2
    exit 1
  fi
  RELEASE_NOTES_TEXT="$(cat "$RELEASE_NOTES_FILE")"
elif [[ -n "$RELEASE_NOTES" ]]; then
  RELEASE_NOTES_TEXT="$RELEASE_NOTES"
fi

if ! command -v fastlane >/dev/null 2>&1; then
  echo "fastlane is required for TestFlight upload with release notes. Install with: gem install fastlane" >&2
  exit 1
fi

echo "Uploading to App Store Connect (TestFlight) via fastlane pilot..."
PILOT_API_KEY_JSON=$(cat <<EOF_KEY
{"key_id":"${ASC_API_KEY_ID}","issuer_id":"${ASC_ISSUER_ID}","filepath":"${ASC_API_KEY_PATH}","in_house":false}
EOF_KEY
)
PILOT_ARGS=(
  fastlane pilot upload
  --ipa "$IPA_PATH"
  --api_key "$PILOT_API_KEY_JSON"
  --skip_waiting_for_build_processing false
  --skip_submission true
)

if [[ -n "$RELEASE_NOTES_TEXT" ]]; then
  PILOT_ARGS+=(--changelog "$RELEASE_NOTES_TEXT")
  echo "Setting TestFlight 'What to Test' notes for this build."
else
  echo "No release notes provided (--release-notes / --release-notes-file / RELEASE_NOTES). Uploading without changelog."
fi

set +e
"${PILOT_ARGS[@]}"
PILOT_STATUS=$?
set -e

if [[ $PILOT_STATUS -ne 0 ]]; then
  echo "iOS upload to TestFlight failed." >&2
  exit 1
fi

echo "iOS deployment complete. Build $BUILD_NUMBER uploaded to TestFlight."

# shellcheck source=/dev/null
if [[ -f "$CODE_ROOT/.project-tracker/lib-deploy.sh" ]]; then
  source "$CODE_ROOT/.project-tracker/lib-deploy.sh"
  log_deploy "$CODE_ROOT" "$(basename "$REPO_ROOT")" "ios" "testflight" "${MARKETING_VERSION}(${BUILD_NUMBER})"
fi
