# dino-brawl-tool

Android automation companion for user-configured game control experiments.

## App identity

- Display name: `dino-brawl-tool`
- Application ID: `com.kwb130212.rbxm` (kept stable so future APKs update the same installation)

## Current design

- Categorized automation dashboard
- Brawl Stars launch/profile shortcut
- Draggable overlay control-position editor
- Accessibility gesture controller for the current foreground display
- Foreground service and partial wake lock for long-running sessions
- Battery/overlay/accessibility setup helpers
- Persistent auto-farm configuration profiles
- GitHub Actions build producing an installable debug APK artifact

## Android limitations

Android and device OEM policies can restrict background execution. The app does not claim to inject touches into an arbitrary game while that game is not the current foreground display. Screen-off operation of the game itself is not guaranteed.

## Build

Open the repository in Android Studio and build the `app` module, or run the GitHub Actions workflow. The workflow publishes `dino-brawl-tool-debug.apk` as the installable test build.
