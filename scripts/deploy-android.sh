#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODE_ROOT="$(cd "$REPO_ROOT/.." && pwd)"
TRACK="internal"
SKIP_UPLOAD=false
BUMP_VERSION=true
SERVICE_ACCOUNT_JSON="${SERVICE_ACCOUNT_JSON:-}"
PACKAGE_NAME="${ANDROID_PACKAGE_NAME:-com.markduenas.scorekeeper}"

SHARED_ENV_FILE="$CODE_ROOT/.deploy-config/deploy.env"
if [[ -f "$SHARED_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$SHARED_ENV_FILE"
fi

usage() {
  cat <<'EOF'
Usage:
  ./scripts/deploy-android.sh [options]

Options:
  --track <internal|alpha|beta|production>   Play track (default: internal)
  --skip-upload                               Build only, do not upload
  --no-bump-version                           Do not increment versionCode in version.properties
  --service-account <path>                    Google Play JSON key path
  --package-name <id>                         Android application ID
  -h, --help                                  Show help

Environment:
  PLAY_SERVICE_ACCOUNT
  GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH
  ANDROID_KEYSTORE_BASE64
  ANDROID_KEYSTORE_PASSWORD
  ANDROID_KEY_ALIAS
  ANDROID_KEY_PASSWORD
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --track)
      TRACK="$2"
      shift 2
      ;;
    --skip-upload)
      SKIP_UPLOAD=true
      shift
      ;;
    --no-bump-version)
      BUMP_VERSION=false
      shift
      ;;
    --service-account)
      SERVICE_ACCOUNT_JSON="$2"
      shift 2
      ;;
    --package-name)
      PACKAGE_NAME="$2"
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

if [[ -z "$SERVICE_ACCOUNT_JSON" ]]; then
  SERVICE_ACCOUNT_JSON="${PLAY_SERVICE_ACCOUNT:-${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH:-$CODE_ROOT/play-store-key.json}}"
fi

pushd "$REPO_ROOT" >/dev/null

if [[ ! -x "./gradlew" ]]; then
  chmod +x ./gradlew
fi

if [[ "$BUMP_VERSION" == "true" ]]; then
  CURRENT_VERSION_CODE="$(sed -nE 's/^versionCode=([0-9]+)$/\1/p' version.properties | head -1)"
  if [[ -z "$CURRENT_VERSION_CODE" ]]; then
    echo "Could not read versionCode from version.properties" >&2
    exit 1
  fi
  NEXT_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
  sed -i.bak -E "s/^versionCode=${CURRENT_VERSION_CODE}$/versionCode=${NEXT_VERSION_CODE}/" version.properties
  rm -f version.properties.bak
  echo "Bumped versionCode: $CURRENT_VERSION_CODE -> $NEXT_VERSION_CODE"
fi

VERSION_CODE="$(sed -nE 's/^versionCode=([0-9]+)$/\1/p' version.properties | head -1)"
VERSION_NAME="$(sed -nE 's/^versionName=(.+)$/\1/p' version.properties | head -1)"
if [[ -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
  echo "Could not read versionCode/versionName from version.properties" >&2
  exit 1
fi
echo "Building versionCode=$VERSION_CODE versionName=$VERSION_NAME"

if [[ -n "${ANDROID_KEYSTORE_BASE64:-}" ]]; then
  TMP_KEYSTORE="${TMPDIR:-/tmp}/scorekeeper-release-key.jks"
  echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$TMP_KEYSTORE"
fi

echo "Building Android release bundle..."
GRADLE_ARGS=(
  ./gradlew
  :composeApp:bundleRelease
  "-PversionCode=${VERSION_CODE}"
  "-PversionName=${VERSION_NAME}"
  --no-daemon
)

if [[ -n "${TMP_KEYSTORE:-}" && -n "${ANDROID_KEYSTORE_PASSWORD:-}" && -n "${ANDROID_KEY_ALIAS:-}" && -n "${ANDROID_KEY_PASSWORD:-}" ]]; then
  GRADLE_ARGS+=(
    "-Pandroid.injected.signing.store.file=${TMP_KEYSTORE}"
    "-Pandroid.injected.signing.store.password=${ANDROID_KEYSTORE_PASSWORD}"
    "-Pandroid.injected.signing.key.alias=${ANDROID_KEY_ALIAS}"
    "-Pandroid.injected.signing.key.password=${ANDROID_KEY_PASSWORD}"
  )
fi

"${GRADLE_ARGS[@]}"

AAB_PATH="$(find composeApp/build/outputs/bundle/release -name '*.aab' | sort | tail -1)"
if [[ -z "$AAB_PATH" ]]; then
  echo "Could not find release AAB output." >&2
  exit 1
fi

echo "Built AAB: $AAB_PATH"

if [[ "$SKIP_UPLOAD" == "true" ]]; then
  echo "Upload skipped (--skip-upload)."
  popd >/dev/null
  exit 0
fi

if [[ -z "$SERVICE_ACCOUNT_JSON" ]]; then
  echo "No service account JSON configured. Set --service-account, PLAY_SERVICE_ACCOUNT, or GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH." >&2
  popd >/dev/null
  exit 1
fi

if [[ ! -f "$SERVICE_ACCOUNT_JSON" ]]; then
  echo "Service account JSON not found: $SERVICE_ACCOUNT_JSON" >&2
  popd >/dev/null
  exit 1
fi

if ! command -v fastlane >/dev/null 2>&1; then
  echo "fastlane is required for Play upload. Install with: gem install fastlane" >&2
  popd >/dev/null
  exit 1
fi

echo "Uploading to Google Play track: $TRACK"
fastlane supply \
  --aab "$AAB_PATH" \
  --json_key "$SERVICE_ACCOUNT_JSON" \
  --package_name "$PACKAGE_NAME" \
  --track "$TRACK" \
  --release_status completed \
  --skip_upload_apk true \
  --skip_upload_images true \
  --skip_upload_screenshots true \
  --skip_upload_metadata true \
  --skip_upload_changelogs true

popd >/dev/null

if [[ -n "${TMP_KEYSTORE:-}" && -f "${TMP_KEYSTORE:-}" ]]; then
  rm -f "$TMP_KEYSTORE"
fi

echo "Android deployment complete."
# shellcheck source=/dev/null
if [[ -f "$CODE_ROOT/.project-tracker/lib-deploy.sh" ]]; then
  source "$CODE_ROOT/.project-tracker/lib-deploy.sh"
  log_deploy "$CODE_ROOT" "$(basename "$REPO_ROOT")" "android" "$TRACK" "$(detect_version_android "$REPO_ROOT")"
fi
