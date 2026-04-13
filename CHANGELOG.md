# Changelog

All notable changes to Office Break are documented here. Newest on top.

## [v0.8.0] -

### New Features

- **Boot-time alarm re-registration** — A new `BootReceiver` listens for `BOOT_COMPLETED` (and `MY_PACKAGE_REPLACED`) and automatically re-registers work-start alarms if a work schedule is enabled, so alarms survive device reboots and app updates.
- **10-minute delayed work-start reminder** — The work-start notification now fires 10 minutes *before* the configured start time, giving a brief warm-up window rather than pinging exactly at shift start.
- **"Start" action on the work-start notification** — The notification now includes a direct action button that starts the break timer with the configured break interval and language, so users can begin the day without opening the app.
- **Inexact alarm fallback** — On Android S+ devices where `SCHEDULE_EXACT_ALARM` permission is denied, the app now gracefully falls back to `setAndAllowWhileIdle()` instead of failing silently, ensuring reminders still fire (just without exact-to-the-second timing).
- **App-start alarm re-registration** — On app launch, work-start alarms are re-registered if the schedule is enabled, so reminders persist across app upgrades, data clears, and alarm-manager restarts.
- **Pause/resume custom break music** — When a custom notification sound is configured, a "Pause music / Resume music" toggle now appears in the exercise dialog below the "Done" button, with a play/pause icon. Lets users silence the alarm during meetings without ending the break; tapping resume continues playback (or restarts from the beginning if the clip already finished).
- **Play/pause icons on custom-sound preview button** — The settings preview button now shows a play-arrow icon when idle and a pause icon while previewing, matching the in-break music toggle for visual consistency.

### Changes

- **Work-start notification copy clarified** — Updated to "Your work day has started" / "Do you want to start your break timer?" to align with the new inline Start action.
- **More detailed alarm-scheduling logs** — `WorkScheduleManager` now logs the next scheduled alarm time, exact-alarm permission status, and fallback path to aid debugging on OEM-restricted devices.

---

## [v0.7.0] - 2026-04-08

### New Features

- **Text-to-speech break announcements** — When a break starts, the app can speak the selected exercise aloud using device TTS. Language for speech is configurable in settings.
- **Work schedule with automatic pause and stop** — Configure work start/end and lunch start/end. The timer pauses automatically during lunch, stops automatically at end-of-shift, and an end-of-shift dialog appears to confirm the day is done.
- **Per-day work schedule with link-inheritance** — Each weekday has independent work/lunch times. A "link" toggle on a day inherits times from the previous configured day, and schedules that span midnight (night shift) are fully supported.
- **Work-start reminder** — An optional notification appears at your scheduled work start if the timer is idle, nudging you to begin breaks for the day.
- **Break pause screen** — When the timer pauses at lunch, a dedicated "Enjoy your break!" screen communicates that the timer will resume automatically when the pause ends.
- **Freestyle timer mode** — Outside scheduled work hours, the timer runs in freestyle mode (no exercise-selection dialog), allowing continuous breaks without interruption.
- **Custom notification sounds** — Users can pick and preview custom audio files as the break-start/stop sound, with a reset option to restore the default beep.
- **Persistent volume bar** — A volume slider with mute/unmute toggle sits fixed at the top of the timer screen, so audio level can be adjusted without leaving the timer.
- **Expanded onboarding flow** — Adds a work-schedule setup step, clearer fitness-level descriptions, per-day listings in the summary, and a collapsible "Show details" toggle.
- **Updated app icon** — New adaptive launcher icon with the artwork re-centered inside the adaptive viewport, plus refreshed fitness-level labels (Gentle / Moderate / Intense).
- **119 new unit tests** — Added test coverage for `DaySchedule`, `SettingsRepository`, `BackupManager`, `StatsRepository`, `TimerViewModel`, and `TimerState`.
- **Compose previews for every screen** — Every major screen now has `@Preview` entries to speed up UI iteration and design review.

### Changes

- **"Reset" renamed to "Stop"** — Timer action button and its confirmation dialog now use "Stop", better reflecting the destructive intent.
- **Break-interval hint clarified** — Copy now explains that the timer repeats until manually stopped, reducing confusion about continuous breaks.
- **Exercise toggles use switches** — The exercise list now uses Material switches instead of checkboxes for clearer on/off semantics.
- **Settings screen reorganized** — Work-schedule section placed under notifications, with grouped work/lunch subsettings. Accessibility `contentDescription` labels added to back buttons, fitness-level icons, summary rows, achievement dialogs, stats screen, and timer screen previews.
- **Onboarding fitness terminology reworked** — "Beginner" → "Gentle", "Athletic" → "Intense", with per-level detail lines showing the specific interval and rep count each level implies.
- **Audio preview no longer stacks** — Selecting a new preview stops the previous one instead of letting both play simultaneously.
- **TTS and custom-sound settings have inline hints** — New descriptions explain how TTS announcements and custom sounds fit into the break workflow.

### Bug Fixes

- **Timer no longer blinks** — Fixed visual flicker in the countdown rendering.
- **ANR risk in `WorkScheduleReceiver`** — Long-running reschedule logic now runs on a background coroutine with `goAsync()`, preventing Application-Not-Responding errors on slow devices.
- **TTS thread safety** — Init and speak calls are now synchronized, eliminating crashes from concurrent access.
- **Night-shift lunch-time validation** — Lunch windows now clamp correctly to work hours even when the shift crosses midnight.
- **CodeQL implicit-PendingIntent findings** — `WorkScheduleReceiver` / `WorkScheduleManager` intents now use explicit `setClass()` + `setPackage()` with `FLAG_IMMUTABLE` / `FLAG_CANCEL_CURRENT`, resolving the full implicit-intent warning set.
- **TTS preview rendering** — Removed the broken `OnInitListener` interface usage, fixing preview playback.

---

## [v0.6.0] - 2026-03-31

### New Features

- **Onboarding wizard with fitness levels** — First launch walks users through a three-step setup: choose fitness level (Beginner = 1h / 5 reps, Moderate = 45min / 10 reps, Athletic = 30min / 15 reps), pick enabled exercises, and review the configuration before the first timer starts.
- **Achievement system with 40+ badges** — Achievements across seven categories (Break Milestones, Streak Milestones, Rep Milestones, Variety, Daily Challenges, Fun & Seasonal, Exercise Mastery). Unlocks trigger a celebration dialog with title and description.
- **Statistics dashboard** — Dedicated Stats screen with totals (breaks, reps, current streak, longest streak), weekly view with week-over-week comparison, most-frequent exercise, exercise distribution, and recent activity timeline. Data stays on-device and is opt-in via a "Save statistics locally" toggle.
- **Dynamic difficulty increase** — After a configurable threshold of completed breaks, a dialog offers to raise reps or shorten the interval. Each option can be accepted independently, declined, or left to auto-dismiss after 60 seconds.
- **JSON backup export / import** — Full app state (settings, stats, achievements) can be exported to a JSON file via the Storage Access Framework and later imported. Import shows a confirmation dialog warning that current data will be replaced and validates format and app-version compatibility.
- **Home-screen widget (Glance)** — Widget shows today's break count, current streak, timer status (idle / running / expired), and quick-action buttons. Updates automatically as state changes. Ships disabled by default while still in preview.
- **Locale support with full German translation** — Respects system language by default and provides complete German strings for onboarding, stats, achievements, and the dynamic-increase dialog.
- **Redesigned timer icon** — A stretching-figure silhouette inside the clock ring, replacing the previous generic timer icon.
- **GitHub Actions CI + community governance** — CI workflow builds APKs, runs unit tests (45+ test classes), and lints on every PR. Adds `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODEOWNERS`, and issue / PR templates.

### Changes

- **"Sound" setting renamed to "Beep volume"** — Clearer naming for the beep audio control; beep count (1–5) remains adjustable.
- **Kotlin upgraded to 2.2** — Toolchain bump reflected in README and Gradle configuration.
- **Ko-fi support link restored** — Accidentally removed during an earlier refactor; added back to the README.

### Bug Fixes

- **Implicit PendingIntent warnings** — Every intent in `TimerService` now carries an explicit package, satisfying Android 12+ security requirements.
- **Achievement strings showing raw keys in release builds** — Runtime `getIdentifier()` lookups replaced with compile-time `R.string` references, so ProGuard/R8 no longer strips them.
- **Lint errors on `LocalActivity` / German translations** — Replaced `LocalContext` casts with `LocalActivity` and filled in missing German translations for new UI.

---

## [v0.5.0] - 2026-03-25

### Changes

- **Package rebranded to `de.mysportsmate.officebreak`** — Renamed from `com.drumm3r.officebreak`, reflecting the new organization. All source files, tests, and build configs migrated accordingly.
- **License switched to GPL v3** — Codebase now distributed under the GNU General Public License v3 (full text in `LICENSE.md`), introducing strong copyleft requirements for derivative works.

---

## [v0.4.0] - 2026-03-17

### New Features

- **Theme setting (System / Light / Dark)** — Users can force Light or Dark mode independently of device setting, or follow the system.
- **Keep screen on during countdown** — Optional toggle preventing the screen from sleeping while the timer runs, so the countdown stays visible at a glance.
- **Configurable beep count** — Number of alarm beeps on expiry is now selectable from 1 to 5.
- **Toggle auto-restart timer** — Disabling auto-restart stops the timer from restarting after an exercise is confirmed, giving users manual control over break cadence.

### Changes

- **Dedicated Settings screen** — All timer and notification preferences moved into a single Settings screen accessible from the timer UI, replacing the scattered inline controls.
- **More reliable screen wake-up on expiry** — Activity launch on timer expiry now uses explicit intents with the correct flags, fixing cases where the screen stayed dim.

---

## [v0.3.0] - 2026-03-16

### New Features

- **Shuffle-bag exercise selection** — Instead of pure random picks, the app now cycles through every enabled exercise once before reshuffling, eliminating close-repeat duplicates inside a session.
- **Min / max repetition range with link toggle** — Users can set a minimum and maximum rep count; each expiry draws a random value in the range. A link icon lets both sliders move together when users want a fixed rep count.

### Changes

- **Persistent foreground notification** — Timer notification now stays visible across the entire break cycle and disappears only on manual reset, keeping the timer's state visible.
- **README refreshed** — Feature descriptions updated to reflect shuffle-bag selection and the rep-range system.

---

## [v0.2.0] - 2026-03-13

### New Features

- **Audible beep alert on timer expiry** — A triple-beep tone (1000 Hz, 150 ms each) plays at alarm volume when the timer reaches zero, making expiry noticeable even when the device isn't in hand.

### Bug Fixes

- **Exercise dialog not appearing after backgrounded timer** — Fixed a race between the DataStore subscription and exercise loading that caused the exercise dialog to fail silently when the app was backgrounded during countdown. Exercise list is now eagerly subscribed so a random exercise reliably appears on expiry.
- **Screen not waking on timer expiry** — The foreground service now launches the activity directly instead of relying solely on the full-screen intent notification (which requires a special permission on Android 14+), so the screen wakes reliably.

---

## [v0.1.0] - 2026-03-13

### New Features

- **Countdown timer with adjustable duration** — Break interval set via hour / minute sliders or direct input; timer auto-restarts after confirming an exercise.
- **Random exercise on expiry** — When the countdown reaches zero, the app displays a randomly selected exercise from the user's configured list.
- **Customizable exercise library** — Add, remove, and enable / disable exercises from the settings screen.
- **Foreground service with persistent notification** — Timer keeps running in the background with a notification showing remaining time.
- **Lock-screen notification with auto-wake** — Timer expiry raises a full-screen notification that wakes the device and dismisses the keyguard.
- **English and German localization** — Full UI translations shipping in the initial release, following system language.
