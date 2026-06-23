# Store screenshots

Capture deterministic App Store and Play Store screenshots with:

```bash
./scripts/capture-store-screenshots.sh
```

Outputs:

- Android: `fastlane/metadata/android/en-US/images/phoneScreenshots`
- iOS phone: `fastlane/screenshots/en-US` normalized to `1284 x 2778` by default
- iOS 13-inch iPad: `fastlane/screenshots/en-US` normalized to `2064 x 2752` with `ipad-13-` filename prefixes

Useful options:

- `--android-only`: build/install/capture Android only
- `--ios-only`: build/install/capture iOS phone and iPad only
- `--ios-phone-only`: build/install/capture iOS phone only
- `--ios-ipad-only`: build/install/capture iOS 13-inch iPad only
- `--skip-build`: reuse the currently installed app

Useful environment:

- `ANDROID_SERIAL`: target a specific Android emulator or device
- `IOS_DEVICE`: target a specific iOS phone simulator name, default `iPhone 15 Pro Max`
- `IOS_IPAD_DEVICE`: target a specific 13-inch iPad simulator name, default `iPad Pro 13-inch (M4)`
- `IOS_SCREENSHOT_WIDTH` / `IOS_SCREENSHOT_HEIGHT`: override the App Store Connect output size
- `IOS_IPAD_SCREENSHOT_WIDTH` / `IOS_IPAD_SCREENSHOT_HEIGHT`: override the App Store Connect iPad output size
- `WAIT_SECONDS`: increase if the simulator needs more time to settle before capture

The screenshots are selected through a launch-only `screenshotName` parameter. Normal app launches do not use these scenes.
