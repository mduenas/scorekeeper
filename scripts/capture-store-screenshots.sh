#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_PACKAGE="${ANDROID_PACKAGE:-com.markduenas.scorekeeper}"
ANDROID_ACTIVITY="${ANDROID_ACTIVITY:-com.markduenas.scorekeeper/.MainActivity}"
IOS_BUNDLE_ID="${IOS_BUNDLE_ID:-com.markduenas.scorekeeper}"
IOS_DEVICE="${IOS_DEVICE:-iPhone 15 Pro Max}"
IOS_IPAD_DEVICE="${IOS_IPAD_DEVICE:-iPad Pro 13-inch (M4)}"
IOS_SCHEME="${IOS_SCHEME:-iosApp}"
IOS_SCREENSHOT_WIDTH="${IOS_SCREENSHOT_WIDTH:-1284}"
IOS_SCREENSHOT_HEIGHT="${IOS_SCREENSHOT_HEIGHT:-2778}"
IOS_IPAD_SCREENSHOT_WIDTH="${IOS_IPAD_SCREENSHOT_WIDTH:-2064}"
IOS_IPAD_SCREENSHOT_HEIGHT="${IOS_IPAD_SCREENSHOT_HEIGHT:-2752}"
IOS_WORKSPACE="$REPO_ROOT/iosApp/iosApp.xcworkspace"
IOS_DERIVED_DATA="$REPO_ROOT/build/store-screenshots/ios-derived"
IOS_APP_PATH="$IOS_DERIVED_DATA/Build/Products/Debug-iphonesimulator/Scorr.app"
ANDROID_OUT="$REPO_ROOT/fastlane/metadata/android/en-US/images/phoneScreenshots"
IOS_OUT="$REPO_ROOT/fastlane/screenshots/en-US"
WAIT_SECONDS="${WAIT_SECONDS:-2}"
CAPTURE_ANDROID=true
CAPTURE_IOS_PHONE=true
CAPTURE_IOS_IPAD=true

SHOTS=(
  "01-templates:templates"
  "02-scoreboard:scoreboard"
  "03-history:history"
  "04-landscape:landscape"
)

usage() {
  cat <<'EOF'
Usage:
  ./scripts/capture-store-screenshots.sh [options]

Options:
  --android-only       Capture Android screenshots only
  --ios-only           Capture iOS phone and iPad screenshots only
  --ios-phone-only     Capture iOS phone screenshots only
  --ios-ipad-only      Capture iOS 13-inch iPad screenshots only
  --skip-build         Reuse installed app/build outputs
  -h, --help           Show help

Environment:
  ANDROID_SERIAL       adb device/emulator serial
  IOS_DEVICE           iOS phone simulator name (default: iPhone 15 Pro Max)
  IOS_IPAD_DEVICE      iOS 13-inch iPad simulator name (default: iPad Pro 13-inch (M4))
  IOS_SCREENSHOT_WIDTH App Store Connect output width (default: 1284)
  IOS_SCREENSHOT_HEIGHT App Store Connect output height (default: 2778)
  IOS_IPAD_SCREENSHOT_WIDTH App Store Connect iPad output width (default: 2064)
  IOS_IPAD_SCREENSHOT_HEIGHT App Store Connect iPad output height (default: 2752)
  WAIT_SECONDS         Delay after launching each scene (default: 2)
EOF
}

SKIP_BUILD=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --android-only)
      CAPTURE_IOS_PHONE=false
      CAPTURE_IOS_IPAD=false
      shift
      ;;
    --ios-only)
      CAPTURE_ANDROID=false
      shift
      ;;
    --ios-phone-only)
      CAPTURE_ANDROID=false
      CAPTURE_IOS_IPAD=false
      shift
      ;;
    --ios-ipad-only)
      CAPTURE_ANDROID=false
      CAPTURE_IOS_PHONE=false
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
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

adb_cmd() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    adb -s "$ANDROID_SERIAL" "$@"
  else
    adb "$@"
  fi
}

boot_ios_simulator() {
  local device_name="$1"
  local device_id
  device_id="$(xcrun simctl list devices available | grep -F "$device_name" | sed -nE 's/.*\(([A-F0-9-]+)\).*/\1/p' | head -1)"
  if [[ -z "$device_id" ]]; then
    echo "Could not find available iOS simulator named: $device_name" >&2
    exit 1
  fi
  xcrun simctl boot "$device_id" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$device_id" -b >/dev/null
  echo "$device_id"
}

normalize_ios_screenshot() {
  local screenshot_path="$1"
  local target_width="$2"
  local target_height="$3"
  python3 - "$screenshot_path" "$target_width" "$target_height" <<'PY'
import sys
from PIL import Image, ImageOps

path = sys.argv[1]
target_width = int(sys.argv[2])
target_height = int(sys.argv[3])

with Image.open(path) as image:
    image = image.convert("RGBA")
    width, height = image.size
    if width > height:
        target_size = (target_height, target_width)
    else:
        target_size = (target_width, target_height)
    fitted = ImageOps.fit(image, target_size, method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))
    fitted.save(path)
PY
}

capture_android() {
  mkdir -p "$ANDROID_OUT"
  if [[ "$SKIP_BUILD" == "false" ]]; then
    pushd "$REPO_ROOT" >/dev/null
    ./gradlew :composeApp:assembleDebug --no-daemon
    popd >/dev/null
    adb_cmd install -r "$REPO_ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk" >/dev/null
  fi

  for entry in "${SHOTS[@]}"; do
    local file="${entry%%:*}"
    local shot="${entry##*:}"
    adb_cmd shell am force-stop "$ANDROID_PACKAGE" >/dev/null
    adb_cmd shell am start -n "$ANDROID_ACTIVITY" --es screenshotName "$shot" >/dev/null
    sleep "$WAIT_SECONDS"
    adb_cmd exec-out screencap -p > "$ANDROID_OUT/$file.png"
    echo "Android screenshot: $ANDROID_OUT/$file.png"
  done
}

build_ios_if_needed() {
  local device_id="$1"
  if [[ "$SKIP_BUILD" == "false" ]]; then
    xcodebuild build \
      -workspace "$IOS_WORKSPACE" \
      -scheme "$IOS_SCHEME" \
      -configuration Debug \
      -destination "platform=iOS Simulator,id=$device_id" \
      -derivedDataPath "$IOS_DERIVED_DATA"
  fi
}

install_ios_if_available() {
  local device_id
  device_id="$1"
  if [[ -d "$IOS_APP_PATH" ]]; then
    xcrun simctl install "$device_id" "$IOS_APP_PATH"
  elif [[ "$SKIP_BUILD" == "true" ]]; then
    echo "Could not find built iOS app at $IOS_APP_PATH. Run without --skip-build first." >&2
    exit 1
  fi
}

capture_ios_device() {
  local device_name="$1"
  local file_prefix="$2"
  local target_width="$3"
  local target_height="$4"
  local should_build="$5"
  mkdir -p "$IOS_OUT"
  local device_id
  device_id="$(boot_ios_simulator "$device_name")"

  if [[ "$should_build" == "true" ]]; then
    build_ios_if_needed "$device_id"
  fi
  install_ios_if_available "$device_id"

  for entry in "${SHOTS[@]}"; do
    local file="${entry%%:*}"
    local shot="${entry##*:}"
    local output="$IOS_OUT/$file_prefix$file.png"
    xcrun simctl terminate "$device_id" "$IOS_BUNDLE_ID" >/dev/null 2>&1 || true
    xcrun simctl launch "$device_id" "$IOS_BUNDLE_ID" --args --screenshot-name "$shot" >/dev/null
    sleep "$WAIT_SECONDS"
    xcrun simctl io "$device_id" screenshot "$output" >/dev/null
    normalize_ios_screenshot "$output" "$target_width" "$target_height"
    echo "iOS screenshot: $output"
  done
}

if [[ "$CAPTURE_ANDROID" == "true" ]]; then
  capture_android
fi

if [[ "$CAPTURE_IOS_PHONE" == "true" ]]; then
  capture_ios_device "$IOS_DEVICE" "" "$IOS_SCREENSHOT_WIDTH" "$IOS_SCREENSHOT_HEIGHT" "true"
fi

if [[ "$CAPTURE_IOS_IPAD" == "true" ]]; then
  IPAD_SHOULD_BUILD=true
  if [[ "$CAPTURE_IOS_PHONE" == "true" ]]; then
    IPAD_SHOULD_BUILD=false
  fi
  capture_ios_device "$IOS_IPAD_DEVICE" "ipad-13-" "$IOS_IPAD_SCREENSHOT_WIDTH" "$IOS_IPAD_SCREENSHOT_HEIGHT" "$IPAD_SHOULD_BUILD"
fi

echo "Store screenshots captured."
