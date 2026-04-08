# Office Break

[![Unit Tests](https://github.com/Drumm3r/office-break/actions/workflows/test.yml/badge.svg)](https://github.com/Drumm3r/office-break/actions/workflows/test.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE.md)

Android app for regular movement breaks at the office. Set a timer, start it, and get a random exercise when it expires. After confirming, the timer restarts automatically -- endlessly, until you manually reset.

## Features

- Adjustable timer duration (hours + minutes) via sliders or manual input
- Configurable repetition range (min/max) with linkable sliders for fixed or randomized reps
- Circular countdown display
- Shuffle-bag exercise selection -- cycles through all enabled exercises before repeating, persisted across app restarts
- Auto-restart after exercise confirmation (configurable -- can be disabled)
- Foreground service with persistent notification (stays visible until manual reset)
- Audible beep alert on timer expiry with configurable beep count (1-5) and custom notification sound support
- Vibration alert on timer expiry
- Lock screen notification with screen wake-up on timer expiry
- TTS break announcements -- exercise names read aloud via text-to-speech (localized)
- Work schedule -- per-day start/end times, break pause with auto-pause/resume, time inheritance via link icon, weekend/off-day detection, night shift support
- End-of-shift screen -- friendly "Schönen Feierabend" / "Enjoy your time off" notification when work hours end, so the timer stop is never a surprise
- Break pause screen -- "Schöne Pause" / "Enjoy your break" screen during scheduled breaks with auto-resume info
- Freestyle mode -- automatically detected when starting outside work hours; disables auto-stop and break pausing so the timer runs freely for after-work use
- Custom notification sounds -- pick audio files via SAF, preview before saving, reset to default
- Persistent volume bar at top of timer screen
- Home screen widget (Glance) -- break count, streak, timer status, quick-start button
- Exercise management (add, remove, enable/disable)
- Achievements system with unlockable milestones
- Stats tracking -- weekly reps, week-over-week comparison, most frequent exercise (all data stored locally)
- Fitness levels with progression
- Dynamic difficulty increase with adaptive threshold (optional)
- Data export and import via JSON backup (SAF integration)
- Onboarding flow for first-time users
- Theme switcher (System, Light, Dark)
- Language switcher (German, English, System)
- Keep screen on during countdown (optional)
- Material 3 theming

## Default Exercises

- Push Ups / Liegestuetze
- Squats / Kniebeuge
- Deadlifts / Kreuzheben
- Lunges / Ausfallschritt
- Sit-Ups
- Superman Angels

Exercises can be customized in the app via the settings screen.

## Tech Stack

- Kotlin 2.2.10
- Jetpack Compose + Material 3
- Jetpack Glance (home screen widget)
- Jetpack DataStore (Preferences)
- kotlinx.serialization
- kotlinx.coroutines
- Min SDK 28 (Android 9), Target SDK 36 (Android 16)

## Build

```bash
./gradlew assembleDebug
```

### Release Build

In Android Studio: **Build > Generate Signed Bundle / APK > APK > Release**

### Run Tests

```bash
./gradlew testDebugUnitTest
```

### Lint

```bash
./gradlew lintDebug
```

## Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Security

To report a security vulnerability, please see [SECURITY.md](SECURITY.md).

## Support

If you find this app useful, consider buying me a coffee:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/drumm3r)

## Also by the developer

[mysportsmate](https://mysportsmate.de) -- your digital fitness companion.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE.md).

**Please note:** While the GPL v3 allows free use, modification, and redistribution, please don't publish rebranded or minimally modified versions of this app on Google Play or other app stores. If you'd like to improve the app, consider contributing back instead.
