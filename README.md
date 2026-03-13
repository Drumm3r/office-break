# Office Break

Android app for regular movement breaks at the office. Set a timer, start it, and get a random exercise when it expires. After confirming, the timer restarts automatically — endlessly, until you manually reset.

## Features

- Adjustable timer duration (hours + minutes) via sliders or manual input
- Configurable repetition count
- Circular countdown display
- Random exercise selection from customizable list
- Auto-restart after exercise confirmation
- Foreground service with persistent notification
- Lock screen notification with screen wake-up on timer expiry
- Exercise management (add, remove, enable/disable)
- Language switcher (German, English, System)
- Material 3 theming

## Default Exercises

- Push Ups / Liegestütze
- Squats / Kniebeuge
- Deadlifts / Kreuzheben
- Lunges / Ausfallschritt
- Sit Ups
- Superman Angels

Exercises can be customized in the app via the settings screen.

## Tech Stack

- Kotlin 2.1
- Jetpack Compose + Material 3
- Jetpack DataStore (Preferences)
- kotlinx.serialization
- Min SDK 28 (Android 9), Target SDK 36 (Android 16)

## Build

```bash
./gradlew assembleDebug
```

### Release Build

In Android Studio: **Build → Generate Signed Bundle / APK → APK → Release**

## Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Support

If you find this app useful, consider buying me a coffee:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/drumm3r)

## Also by the developer

[mysportsmate](https://mysportsmate.de) — your digital fitness companion.

## License

MIT
