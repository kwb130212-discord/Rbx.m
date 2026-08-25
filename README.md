# Rbx.m

Android companion app for personal/family Roblox macro experiments.

## Current design

- Game profile list
- Roblox launch shortcut
- Accessibility-service based tap automation
- Foreground service for persistent macro state
- Keep-screen-on option
- Permission status/setup screen
- GitHub Actions APK build

> Android does not allow an ordinary app to inject a touch into Roblox while another app is in the foreground. This project therefore does not claim to control Roblox invisibly in the background. The accessibility gesture targets the current foreground display.

## Build

Open this repository in Android Studio and build the `app` module, or use the GitHub Actions workflow.
