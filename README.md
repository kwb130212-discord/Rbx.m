# Rbx.m AI

Android on-device adaptive controller for a user-owned/test game environment.

## Architecture

- **Accessibility controller**: performs foreground-only gestures.
- **VisionEngine**: analyzes screenshots locally on the device.
- **AiBrain**: chooses move/dodge/attack actions from the current state.
- **OnlineLearner**: updates action values during use.
- **LearningStore**: persists the policy in app-private storage.
- **Python trainer**: `python/train_policy.py` provides an offline telemetry trainer.

## Build

```bash
gradle --no-daemon :app:assembleDebug
```

The GitHub Actions workflow also uploads the debug APK as an artifact.

## Android requirements

- Android 11+ for screenshot-driven vision.
- Accessibility service enabled by the user.
- The target game must be in the foreground before automation actions are issued.

## Notes

The app does not inspect game network packets, credentials, cookies, or authentication tokens. Vision and policy data stay on-device. The current vision implementation is a lightweight heuristic baseline; a trained detector can replace it later without changing the controller/learning interfaces.
