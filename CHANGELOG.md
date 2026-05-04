# Changelog

All notable changes to Office Break are documented here. Newest on top.

## [v0.8.0] - 2026-04-24

### New Features

- **One-day mode override** — When "Auto mode by weekday" is active, manually picking a mode in the exercise settings now stores a transient override for today only. The override survives backgrounding and process death (persisted in DataStore keyed by `LocalDate.now()`) so app-resume no longer reverts the selection to the weekday plan. It is cleared automatically on explicit timer reset ("Stop"), on end-of-shift dismissal, and on the natural day boundary (stored date no longer matches today). A dezent "Today only" / "Nur heute" hint under the segmented mode buttons signals that an override is in effect without referencing the overridden plan mode. The weekly schedule is never modified. Example use case: Thursday plan says Home Workout, but this Thursday the user is in the office and wants Office mode for the day -- one tap, no permanent config change. Unit tests cover `modeOverrideForToday` flow (stale-date handling, invalid mode payload, clear, overwrite) and `TimerViewModel` (override write gated on autoMode toggle, `applyDayDefaultModeIfEnabled` respects override, `resetTimer` + `dismissWorkEnded` clear).
- **Accessibility overhaul** — TalkBack now announces section headings across Settings, Stats, and Achievements (13 titles gain `heading()` semantics); the circular countdown reads as "X minutes Y seconds remaining" with progress-bar range info instead of "twelve colon thirty-four"; sliders (volume, reps, beep count) announce their label; the language dropdown gains a visible label; time-input hour/minute fields set `ImeAction.Next` / `Done`. Two undersized touch targets raised to 48 dp (link/unlink reps icon, per-day toggle). Clickable time display declares `Role.Button`.
- **Reduce-motion support** — The achievement-unlock confetti animation now checks `ANIMATOR_DURATION_SCALE` and skips the 4-second particle loop when the user has "Remove animations" enabled system-wide.
- **Scrollable idle timer screen** — The start screen now wraps its column in `verticalScroll`, so content stays reachable at 200 % system font scale on small devices.
- **Backup feedback as Snackbar** — Replaced the transient, unreplayable `Toast` on backup success/error with a `SnackbarHost` inside the Scaffold — announced and dismissible.
- **Per-day default exercise mode** — A master "Auto mode by weekday" toggle in work-schedule settings binds a preferred mode (Workout / Mobility / Office) to each weekday. The same link-inheritance as work times applies — linked days inherit the mode from the first unlinked day in the week. Today's effective mode is applied automatically on app foreground, onboarding completion, schedule edits, and toggle-on, so the user no longer needs to flip the mode manually every day. Toggling the master on seeds every day with the currently active mode as a starting point; users then customize individual days.
- **Onboarding per-day mode configuration** — The work-schedule step now includes the auto-mode toggle and per-day mode selector when enabled, so users can set their full hybrid rhythm (e.g. Mon/Tue Office, Wed–Fri Home Workout) before completing onboarding.
- **Onboarding summary grouped by schedule + mode** — The summary now groups weekdays by identical work hours *and* mode. Each group shows its work window, lunch window (indented under the day line), and — when auto-mode is active — a mode icon with name indented to the same level. When auto-mode is active the redundant global mode line is hidden.
- **Exercise context modes** — Three switchable exercise modes (Home Office Workout, Home Office Mobility, Office) replace the flat exercise list. Each mode pre-selects exercises suited to the context — bodyweight strength for home workouts, yoga-inspired mobility stretches for flexibility, and subtle desk-friendly exercises for the office. Users switch modes instantly via a segmented button in the exercise settings. All 25 exercises are always visible; modes control which are enabled. Custom toggles, additions, and removals persist independently per mode. Onboarding now asks users to pick their exercise style instead of toggling individual exercises.
- **19 new built-in exercises** — Plank, Glute Bridge, Cat-Cow Stretch, Child's Pose, Downward Dog, Seated Spinal Twist, Hip Circles, Standing Forward Fold, Thread the Needle, Pigeon Stretch, Shoulder Blade Squeeze, Chest Opener, Neck Stretch, Calf Raises, Seated Leg Extension, Wrist Circles, Ankle Circles, Seated Cat-Cow, and Seated Core Bracing — all with English and German translations.
- **Boot-time alarm re-registration** — A new `BootReceiver` listens for `BOOT_COMPLETED` (and `MY_PACKAGE_REPLACED`) and automatically re-registers work-start alarms if a work schedule is enabled, so alarms survive device reboots and app updates.
- **10-minute delayed work-start reminder** — The work-start notification now fires 10 minutes *before* the configured start time, giving a brief warm-up window rather than pinging exactly at shift start.
- **"Start" action on the work-start notification** — The notification now includes a direct action button that starts the break timer with the configured break interval and language, so users can begin the day without opening the app.
- **Inexact alarm fallback** — On Android S+ devices where `SCHEDULE_EXACT_ALARM` permission is denied, the app now gracefully falls back to `setAndAllowWhileIdle()` instead of failing silently, ensuring reminders still fire (just without exact-to-the-second timing).
- **App-start alarm re-registration** — On app launch, work-start alarms are re-registered if the schedule is enabled, so reminders persist across app upgrades, data clears, and alarm-manager restarts.
- **Pause/resume custom break music** — When a custom notification sound is configured, a "Pause music / Resume music" toggle now appears in the exercise dialog below the "Done" button, with a play/pause icon. Lets users silence the alarm during meetings without ending the break; tapping resume continues playback (or restarts from the beginning if the clip already finished).
- **Play/pause icons on custom-sound preview button** — The settings preview button now shows a play-arrow icon when idle and a pause icon while previewing, matching the in-break music toggle for visual consistency.
- **Share achievements** — Unlocked achievements can be shared as a branded image card (green gradient with trophy icon, title, and description) via Android's share sheet to WhatsApp, Instagram Stories, social media, and more. A share button appears in the unlock celebration dialog and on each unlocked achievement in the achievements list.
- **Home-screen widget re-enabled with live countdown** — The Glance widget from v0.6.0 is now active and shipped on by default. It displays today's break count, current streak, and the live timer status — counting down second-by-second while a break interval is running, showing "Paused" during scheduled lunch, "Break time!" on expiry, and "Timer not running" when idle. A Start button launches the timer directly from the home screen; once running, the button switches to Open. Widget state is persisted per Glance instance and survives device reboots via `BootReceiver`.
- **In-app donation prompt for store-installed users** — After 21 days of use, a one-time dialog suggests supporting the project on Ko-fi so users who install via stores (and never see the README's Ko-fi link) get a friendly, non-intrusive reminder. "Support" opens `https://ko-fi.com/drumm3r` and permanently dismisses; "Later" snoozes 60 days; "No thanks" hides it forever. The popup only appears when the timer is idle and no other dialog is visible, so it never interrupts an active break. A `DonationPromptResolver` pure helper with 10 unit tests covers the threshold/snooze/dismiss/clock-drift logic. Install timestamp is persisted in DataStore on first launch; all donation flags are excluded from backup export.
- **Permanent "Support the project" entry at the top of Settings** — A prominent Ko-fi row with a heart icon sits as the first entry in Settings, always visible — so users who dismissed the popup, or who want to contribute earlier than 21 days, always have a one-tap path to the donation page.
- **Developer mode (7× tap on "Statistics" header)** — Modeled after Android's "You are now a developer!" gesture. Revealing a new Developer section under "Data" with: reset donation popup (clears dismiss/snooze and rewinds install date so the popup reappears on next launch), reset onboarding, show DataStore dump with copy-to-clipboard for bug reports, wipe all app data (confirmation-gated), and disable dev mode. State persists in DataStore across launches. Hidden by default; counter resets after 3 seconds of tap inactivity.
- **Build info embedded for bug reports** — `BuildConfig` now carries `GIT_SHA` (short commit hash, read from `git rev-parse` at configure time, falls back to `"unknown"`) and `BUILD_TIMESTAMP` (ISO-8601 UTC). Both are surfaced alongside `VERSION_NAME` / `VERSION_CODE` / build type in the developer-mode DataStore dump, so users can paste a complete build + state snapshot into GitHub issues with one tap.

### Improvements

- **German texts polished** — Spell-check pass over `values-de/strings.xml` fixing missing commas (subordinate clauses with `bis`, infinitives with `um`), compound nouns (`Pause Beginn` → `Pausenbeginn`, `Pause Ende` → `Pausenende`), Duden-style `-mal` suffix on numerals, and terminology consistency (`Auszeichnung` → `Erfolg` to match the rest of the app, `nicht umkehrbar` → `unwiderruflich`).
- **Backup import hardened** — `BackupManager.restoreFromJson` now rejects files larger than 5 MB before reading, caps `breakRecords` at 100k entries, per-mode exercise lists at 500 entries, and truncates exercise names to 100 chars. `SettingsRepository.restoreFromBackup` clamps all numeric fields (timer hours/minutes, reps, beep volume/count, schedule hours/minutes) to valid ranges. Closes the primary DoS/OOM surface identified by the 2026-04-21 security audit.
- **Foreground service subtype declared** — `AndroidManifest` now carries `PROPERTY_SPECIAL_USE_FGS_SUBTYPE=timer` on `TimerService` and `startForeground` passes `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 30+, required for Play Store review of `specialUse` foreground services on API 34+.
- **Backup policy reconciled** — `@xml/backup_rules` now `<exclude>` all domains to match `android:allowBackup="false"`. The previous rules re-enabled cloud backup on API 31+, contradicting the manifest.
- **Gradle wrapper integrity-pinned** — Added `distributionSha256Sum` to `gradle-wrapper.properties` so a MITM'd wrapper download fails closed instead of executing arbitrary bootstrap code.
- **`verification-metadata.xml` narrowed** — Dropped the `org.apache.*`, `commons-*`, and `org.codehaus.*` blanket `<trusted-artifacts>` entries; every dependency now verifies against a pinned SHA-256. Inert `dependencyLocking { … }` block removed since verification-metadata is now authoritative.
- **Supply-chain hardening in CI** — Added `.github/dependabot.yml` (weekly gradle + github-actions), pinned every GitHub Action to a commit SHA with version comment, disabled `persist-credentials` on `actions/checkout`, pinned runner to `ubuntu-24.04`, added a `concurrency` block, and added an `assembleRelease` tooling-leak guard that fails the job if `ui-tooling` / `ui-test-manifest` classes sneak into the release APK. `debugImplementation("ui-test-manifest")` regrouped to prevent future mis-scoping.
- **DataStore keys centralised** — `SettingsRepository.KEY_*` + `StatsRepository.KEY_*` are now `internal val` rather than `private val`, and the seven consumer files (`TimerService`, `BootReceiver`, `WorkScheduleReceiver`, `OfficeBreakApp`, `LocaleHelper`, `WidgetActionCallback`, `OfficeBreakWidget`) reference them by name instead of reconstructing raw string literals. Prevents a class of silent-desync bugs on rename.
- **Widget status strings unified** — The `WidgetTimerState.STATUS_*` constants are now the single source of truth; 25+ raw `"running"` / `"paused"` / `"expired"` / `"idle"` / `"work_ended"` literals in `TimerService`, `TimerScreen`, `BootReceiver`, `WidgetContent`, and `OfficeBreakWidget` replaced.
- **Pure-helper extractions + tests** — `TimerPauseResolver.decide(now, today): TimerTickDecision` captures the tick-loop schedule logic; `WorkScheduleManager.computeNextWorkStartTime(schedule, now)` captures the 7-day alarm scan; `ShuffleBag` captures the exercise-picking algorithm with an injectable `randomPicker` for deterministic tests. 21 new unit tests cover night-shift wraparound, weekend skip, refill-without-immediate-repeat, and other edge cases previously only reachable through `TimerViewModel`.
- **`launchSafely` helper** — Collapsed 14 copy-pasted `viewModelScope.launch { try { … } catch (e) { Log.e(TAG, …, e) } }` wrappers in `TimerViewModel` onto one shared `private inline fun launchSafely(errorMessage, block)` call site.
- **Timer-start path unified** — `WidgetActionCallback` and `WorkScheduleReceiver` now route through `DefaultTimerServiceController` instead of constructing `Intent(context, TimerService::class)` inline, removing two code duplication paths flagged by the backend audit.
- **Compose API hygiene** — The three confirm dialogs (Reset, Import, Reset Stats) collapsed into a single `ConfirmationDialog(titleRes, messageRes, confirmRes, dismissRes, …)`. `SettingsToggleRow` promoted to a shared `LabeledSwitchRow`. `modifier: Modifier = Modifier` added to `ExerciseDialog`, `AchievementUnlockDialog`, `AchievementShareCard`, `DynamicIncreaseDialog`, `ConfirmationDialog`, `LabeledSwitchRow`, and the new `SettingsSection` slot composable. `AchievementShareCard` now consumes `MaterialTheme.typography` instead of raw `sp` literals.
- **Theme tokens** — Added `ui/theme/Spacing.kt` (xs/sm/md/lg/xl/xxl), `ConfettiColors` palette, and an `OnGreenPrimaryDark` token shared between `Theme.kt` and the widget `WidgetColors`. Replaces three repeated `Color(0xFF003A00)` literals.
- **Runtime performance polish** — `TimerService` tick loop moved from `Dispatchers.Main` → `Dispatchers.Default` so per-second widget/notification/DataStore work no longer shares the main thread with Compose frame rendering. `TimerViewModel.onCleared()` now releases preview `AudioTrack` / `MediaPlayer`, preventing audio leaks on configuration change. `StatsScreen` weekly aggregates hoisted to a pure `computeWeeklyAggregates` function wrapped in `remember(records, today)`. `@Immutable` annotated on `Exercise`, `DaySchedule`, `StatsSnapshot`, `AchievementState`, `BreakRecord` so Compose marks `List<T>` composable params as skippable. `AchievementsScreen` list items gain `key = { it.id }`.
- **Consistent error messaging** — `TimerViewModel.importData` / `exportData` no longer echo raw `e.message` (which could leak file paths or URIs) in the snackbar; they now show the static `backup_error_generic` string. `shareAchievement` wrapped in try/catch with fallback `share_error` toast on `ActivityNotFoundException` or render failure — previously failed silently.
- **Cold-boot splash** — `MainActivity` now shows a centred `CircularProgressIndicator` instead of an empty `Box` while `onboardingCompleted` resolves from DataStore.
- **Test fixtures de-duplicated** — New `test/.../data/BackupDataFixtures.kt`; removed duplicate `createMinimalBackupData` / `createSampleBackupData` from `SettingsRepositoryTest` and `BackupManagerTest`.
- **Backup format bumped to v3** — Exports now include the `autoModeByDayEnabled` master toggle and each `DaySchedule` carries its `defaultMode`. v2 and v1 backups import seamlessly (missing fields fall back to defaults — master off, mode Home Workout).
- **`DaySchedule` gains a `defaultMode` field** — Non-null with `HOME_WORKOUT` default so existing persisted week schedules decode without migration. Only consumed when the master toggle is on.
- **Stricter onboarding-complete fallback** — The `onboardingCompleted` flow no longer treats any non-empty DataStore as a completed onboarding. Completion is now explicit (`KEY_ONBOARDING_COMPLETED` set via "Let's go") or inferred from prior usage (`KEY_TIMER_HOURS` present), so writes during onboarding no longer auto-skip the flow.
- **Backup format bumped to v2** — Exports now include per-mode exercise lists and the active mode. v1 backups import seamlessly (exercises map to Home Workout mode).
- **Exercise settings use checkboxes** — Replaced switches with checkboxes for consistent sizing. Enabled exercises sort to the top of the list.
- **Onboarding step 1 redesigned** — Individual exercise toggles replaced by three mode-selection cards (Workout / Mobility / Office) with icons and descriptions.
- **Work-start notification copy clarified** — Updated to "Your work day has started" / "Do you want to start your break timer?" to align with the new inline Start action.
- **More detailed alarm-scheduling logs** — `WorkScheduleManager` now logs the next scheduled alarm time, exact-alarm permission status, and fallback path to aid debugging on OEM-restricted devices.
- **Android Gradle Plugin upgraded to 9.2.0** — Bumped from 9.1.0 for the latest stability and tooling fixes.
- **Typographic dashes replaced with plain hyphen in source** — All 29 em-dash (`—`) and en-dash (`–`) occurrences in user-facing strings, Kotlin code, and tests replaced with ASCII `-`. Simplifies grep / diff workflows and avoids invisible whitespace-looking characters in PRs. Markdown docs (`CHANGELOG.md`, `README.md`) keep typographic dashes for readability.

### Bug Fixes

- **Landscape layout overhaul** — Fixed five overlapping regressions in landscape orientation on phones: the top-aligned Idle navigation icons (stats / achievements / exercises / settings) were declared before the main centered `Column` in the Scaffold `Box`, so the column drew over them and swallowed all pointer events — icons were invisibly covered and unclickable. The same z-order bug applied to the running/paused `VolumeBar`. Scrollable Idle content also slid under the icons because no space was reserved for the top bar. `CountdownDisplay` was hard-coded to 280dp in both width and height, overflowing the ~360dp landscape canvas and overlapping the `VolumeBar`. `DonationPromptDialog` had no scroll container and no height cap, so the Ko-fi prompt clipped below the viewport with no way to reach the buttons. `OnboardingScreen`'s `FitnessLevelStep`, `ExerciseModeSelectionStep`, and `SummaryStep` were non-scrolling columns (only `WorkScheduleStep` had scroll), cutting off content in short landscape height. Fixed by: re-declaring the top-bar container as the LAST child of the outer `Box` so it wins stacking + hit-testing; measuring its height via `onSizeChanged` + `LocalDensity` and applying it as `padding(top = …)` on the main `Column`; rewriting `CountdownDisplay` around `BoxWithConstraints` with `sizeDp = minOf(maxWidth, maxHeight, 280.dp)` and passing `Modifier.weight(1f, fill = false).fillMaxWidth()` at the running/paused call sites so the circle shrinks with available height; adding `usePlatformDefaultWidth = false` + an outer full-size `Box` + `widthIn(max = 420.dp)` / `heightIn(max = screenHeight - 48.dp)` + `verticalScroll(rememberScrollState())` to the donation dialog; and adding `verticalScroll` to the three onboarding steps.
- **Active break exercise changed on app resume** — When the user backgrounded the app mid-break and reopened it, `TimerScreen`'s `LaunchedEffect(timerState)` re-fired against the still-`Expired` state, triggering `TimerViewModel.onTimerExpired()` a second time and repicking a different exercise. Fixed in two layers: `onTimerExpired()` is now idempotent (early-returns when `_currentExercise` is already set), and the active break's exercise + reps are now persisted to DataStore in addition to `SavedStateHandle` so they survive task removal (swipe from recents) and full process kill, not only rotation. On VM init the DataStore payload is restored synchronously as a fallback when `SavedStateHandle` is empty, and it is cleared when the user taps Done or resets the timer.
- **Achievement icons for "Diversity3" and "LunchDining" showed wrong glyphs** — `AchievementIcon.iconForName` mapped both strings to stand-ins (`AutoAwesome` and `Today` respectively). Now maps to `Icons.Default.Diversity3` and `Icons.Default.LunchDining` from `material-icons-extended`. Matching unit tests updated.
- **AudioTrack "Pinning deprecated since Android Q" warning** — Switched beep playback from `AudioTrack.MODE_STATIC` to `MODE_STREAM` with play-before-write order in both `TimerService` alarm and `TimerViewModel` preview, silencing the framework warning on modern Android.
- **Widget timer stuck on "Timer not running"** — Widget Glance state was never actually persisted because `WidgetUpdater` mutated a discarded `toMutablePreferences()` copy instead of the `MutablePreferences` the Glance callback passes in. Every state transition (`running`, `paused`, `expired`, `idle`) now lands correctly.
- **Widget countdown updated only every 30 seconds** — `TimerService` previously batched widget pushes on a 30-tick counter, so the home-screen countdown jumped in 30-second chunks. Widget is now updated every second for a live, smooth countdown.
- **Widget briefly flashed "Timer not running" on timer expiry** — A race between the async running-push at the final tick (with `remaining=0` and therefore `endRealtime=0`) and the synchronous expired-push could overwrite the "Break time!" state with stale running data, which the widget's fallback logic then rendered as idle. Running-pushes are now synchronous and guarded by `remaining > 0`, so expiry reliably shows "Break time!".

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

### Improvements

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

### Improvements

- **"Sound" setting renamed to "Beep volume"** — Clearer naming for the beep audio control; beep count (1–5) remains adjustable.
- **Kotlin upgraded to 2.2** — Toolchain bump reflected in README and Gradle configuration.
- **Ko-fi support link restored** — Accidentally removed during an earlier refactor; added back to the README.

### Bug Fixes

- **Implicit PendingIntent warnings** — Every intent in `TimerService` now carries an explicit package, satisfying Android 12+ security requirements.
- **Achievement strings showing raw keys in release builds** — Runtime `getIdentifier()` lookups replaced with compile-time `R.string` references, so ProGuard/R8 no longer strips them.
- **Lint errors on `LocalActivity` / German translations** — Replaced `LocalContext` casts with `LocalActivity` and filled in missing German translations for new UI.

---

## [v0.5.0] - 2026-03-25

### Improvements

- **Package rebranded to `de.mysportsmate.officebreak`** — Renamed from `com.drumm3r.officebreak`, reflecting the new organization. All source files, tests, and build configs migrated accordingly.
- **License switched to GPL v3** — Codebase now distributed under the GNU General Public License v3 (full text in `LICENSE.md`), introducing strong copyleft requirements for derivative works.

---

## [v0.4.0] - 2026-03-17

### New Features

- **Theme setting (System / Light / Dark)** — Users can force Light or Dark mode independently of device setting, or follow the system.
- **Keep screen on during countdown** — Optional toggle preventing the screen from sleeping while the timer runs, so the countdown stays visible at a glance.
- **Configurable beep count** — Number of alarm beeps on expiry is now selectable from 1 to 5.
- **Toggle auto-restart timer** — Disabling auto-restart stops the timer from restarting after an exercise is confirmed, giving users manual control over break cadence.

### Improvements

- **Dedicated Settings screen** — All timer and notification preferences moved into a single Settings screen accessible from the timer UI, replacing the scattered inline controls.
- **More reliable screen wake-up on expiry** — Activity launch on timer expiry now uses explicit intents with the correct flags, fixing cases where the screen stayed dim.

---

## [v0.3.0] - 2026-03-16

### New Features

- **Shuffle-bag exercise selection** — Instead of pure random picks, the app now cycles through every enabled exercise once before reshuffling, eliminating close-repeat duplicates inside a session.
- **Min / max repetition range with link toggle** — Users can set a minimum and maximum rep count; each expiry draws a random value in the range. A link icon lets both sliders move together when users want a fixed rep count.

### Improvements

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
