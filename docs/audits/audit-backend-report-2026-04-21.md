# Application Audit Report — office-break

**Date**: 2026-04-21
**Target**: `/home/schoenenborn/projects/github/office-break`
**Modules run**: Code Quality, Testing, Observability, Security
**Version audited**: v0.8.0 (commit `627bc76`), branch `v0.8.0`

## About This Audit

The `/audit-backend` skill was invoked against an Android/Kotlin/Compose client. The four backend-oriented modules (architecture, errors, data-access, data-contracts) were dropped — their assertions presuppose an HTTP server, handlers, and an ORM that do not exist in this codebase. The remaining four modules were re-calibrated for on-device Android:

- **Observability** is mostly N/A — assertions about structured JSON logs, request IDs, and auth events do not apply.
- **Security** is partially N/A (no server auth / CORS / sessions) and extended with Android-specific checks: `AndroidManifest.xml` component exposure, `PendingIntent` flags, foreground service type, SAF URI permissions, notification channels.
- **Testing** remaps "CRUD operations" to repository mutations and state transitions.
- **Code Quality** applies nearly as-is.

Severity is calibrated for a single-developer personal-scale published Android app.

## Scorecard

| Module | PASS | FAIL | WARN | N/A | Applicable | Score |
|--------|------|------|------|-----|------------|-------|
| Code Quality | 5 | 3 | 3 | 0 | 11 | 45% |
| Testing | 4 | 0 | 2 | 1 | 6 | 67% |
| Observability | 3 | 0 | 1 | 4 | 4 | 75% |
| Security | 5 | 2 | 3 | 5 | 10 | 50% |
| **Total** | **17** | **5** | **9** | **10** | **31** | **55%** |

Applicable = PASS + FAIL + WARN. Score = PASS / Applicable.

Dropped modules (`architecture`, `errors`, `data-access`, `data-contracts`) are not counted.

## FAIL Findings (Action Required)

### CQ-2: Duplicated code blocks across modules
**File(s):** `app/src/main/kotlin/de/mysportsmate/officebreak/OfficeBreakApp.kt:51-64`, `service/BootReceiver.kt:37-50`, `service/WorkScheduleReceiver.kt:135-149`, `ui/TimerViewModel.kt` (38 sites)

Three distinct duplication patterns:
1. **"Read work-schedule from DataStore with default"** block copied verbatim into three files.
2. **Start-timer-from-prefs intent construction** duplicated across `WidgetActionCallback.kt:19-32` and `WorkScheduleReceiver.kt:104-117`.
3. **38 near-identical `viewModelScope.launch { try { … } catch (e) { Log.e(TAG, "Failed to …", e) } }`** setter blocks in `TimerViewModel.kt` — each differs only in method body and log string.

**Fix:**
- Add `suspend fun readSchedulerConfig(): Pair<Boolean, List<DaySchedule>>` on `SettingsRepository`; call it from all three consumers.
- Route every "start timer" path through `DefaultTimerServiceController` (remove direct intent construction from widget/receiver code).
- Introduce `private inline fun launchSafely(errorMessage: String, crossinline block: suspend () -> Unit)` on `TimerViewModel`.

### CQ-5: Magic numbers and strings are not constants
**File(s):** `widget/WidgetContent.kt:102-135`, `service/TimerService.kt:118-232,533`, `service/BootReceiver.kt:30,35`, `ui/TimerViewModel.kt:671,823`

The worst offender: `WidgetTimerState.STATUS_RUNNING`/`STATUS_PAUSED`/`STATUS_EXPIRED`/`STATUS_IDLE` constants already exist but are bypassed by raw literals (`"running"`, `"paused"`, `"expired"`, `"idle"`) in `TimerService`, `WidgetContent`, `TimerScreen`, `BootReceiver`. A typo in one site silently desyncs the widget from the service. Additional magic numbers: `3600L` / `60L` repeated in `TimerViewModel`; sample-rate `44100`, `150 ms` beep, `100 ms` pause duplicated between `TimerViewModel.playPreviewBeep` and `TimerService.playAlarmSound`; `TimerService.kt:202` hardcodes `3` beep default instead of `SettingsRepository.DEFAULT_BEEP_COUNT`.

**Fix:** Replace all status literals with `WidgetTimerState.STATUS_*`. Use `SettingsRepository.DEFAULT_*` for fallbacks. Extract a `BeepSynth` object with the shared audio params. Define `SECONDS_PER_HOUR`/`SECONDS_PER_MINUTE` constants.

### CQ-7: Configuration scattered — DataStore keys duplicated across 7 files
**File(s):** `OfficeBreakApp.kt:52,54`, `service/BootReceiver.kt:38,40`, `service/WorkScheduleReceiver.kt:105-110,136-138`, `service/TimerService.kt:122-124,200-203,459-460`, `widget/WidgetActionCallback.kt:20-25`, `widget/OfficeBreakWidget.kt:29,37,49`, `locale/LocaleHelper.kt:45`

`SettingsRepository` privately owns 25+ named DataStore keys. At least 10 of them are re-constructed as raw string literals (`booleanPreferencesKey("work_schedule_enabled")`, `stringPreferencesKey("language")`, etc.) in seven other files. If a key is ever renamed in the repository, the consumers silently continue writing to (and reading from) the old key, breaking settings sync with no compile-time warning. Same problem for `StatsRepository` keys (`break_records`, `stats_snapshot`) used by the widget.

**Fix:** Promote the relevant keys to `internal val` or publish a `DataStoreKeys` object in `data/`. Better: add typed read helpers on the repositories (`suspend fun readTimerConfig(): TimerConfig`) so no caller touches `dataStore.data.first()` directly. This finding is coupled with **CQ-11** (control-plane magic values) — the same fix resolves both.

### SEC-8: Dependency locking declared but lockfiles missing
**File(s):** `build.gradle.kts:7-11`

`dependencyLocking { lockAllConfigurations() }` is declared but no `gradle.lockfile` or `gradle/dependency-locks/*.lockfile` is committed, making the directive inert. Mitigations: every version in `app/build.gradle.kts:47-75` is an exact string (no ranges), and `gradle/verification-metadata.xml` (3854 lines) enforces SHA-256 integrity on the resolved graph. So the build is reproducible in practice — but the locking block is dead code.

**Fix:** Either run `./gradlew dependencies --write-locks` and commit the generated files, or delete the empty `dependencyLocking` block and rely on the verification metadata alone. Pick one; don't leave both half-applied.

### SEC-9: Unbounded operations on untrusted input (DoS surface)
**File(s):** `ui/TimerViewModel.kt:912-918`, `data/BackupManager.kt:61-68`, `data/SettingsRepository.kt:200,441-444`

Real DoS / OOM vectors:
1. **Backup import reads entire file into memory** via `stream.bufferedReader().readText()` with no size cap. A 50–100 MB JSON crashes the import on mid-range devices. No pre-check of `DocumentFile.fromSingleUri(uri).length()`.
2. **Entity-count caps absent**: `data.breakRecords`, `data.dailyAggregates`, `data.yearlyAggregates`, and the three `exercisesHome*` lists are written to DataStore unchanged. A backup with 1,000,000 records poisons every subsequent stats mutation.
3. **`addExercise` has no total-count cap** (`TimerViewModel.kt:625-643`).
4. **`usedExerciseNames` set grows monotonically** with every picked exercise (`SettingsRepository.kt:200`). Long-running installs accumulate forever.

`BreakTtsManager` (uses `QUEUE_FLUSH`) and widget update frequency are bounded — no issue there.

**Fix:**
```kotlin
// Before readText(), query size and reject > 5 MB:
val size = contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
    ?.use { if (it.moveToFirst()) it.getLong(0) else -1L } ?: -1L
if (size > MAX_BACKUP_SIZE_BYTES) return BackupUiState.Error(...)

// In BackupManager.restoreFromJson, cap lists:
val sanitized = data.copy(
    breakRecords = data.breakRecords.take(MAX_RECORDS),
    exercisesHomeWorkout = data.exercisesHomeWorkout.take(MAX_EXERCISES),
    // ...
)
```
Combined with **SEC-3** (numeric bounds + string truncation on restore), this closes the primary attack surface.

## WARN Findings (Watch Items)

### CQ-1: Long service/screen functions
`TimerService.startTimer` is 118 lines of non-Compose logic combining schedule lookup, tick loop, lunch pause, and alarm playback (`service/TimerService.kt:102-220`). `SettingsScreen` (~420 lines) and `TimerScreen` (~430 lines) are lengthy Compose roots but acceptable with the lenient Compose threshold. **Fix**: extract `loadTodayScheduleIfEnabled()`, `handleScheduleTransitions(…)`, `onTimerFinished(prefs)` from `startTimer`.

### CQ-10: String-based type systems at module boundaries
`TimerState` sealed class is serialised across the widget process boundary as a raw string — justified by DataStore's type limitations, but the status values should flow through the existing `WidgetTimerState.STATUS_*` constants (same root cause as **CQ-5**). `AchievementDefinition.iconName: String` drives a 15-branch `when` in `AchievementIcon.iconForName` (`ui/components/AchievementIcon.kt:20-39`) — should be an enum. **Fix**: introduce `enum class AchievementIconKey { … }` at the registry boundary.

### CQ-11: Magic return values controlling framework behaviour
Intent actions/extras are properly catalogued on `TimerService.Companion` — positive. Notification channel IDs on `OfficeBreakApp.Companion` — positive. The remaining issue is the DataStore key strings used as untyped control keys, which is **CQ-7**. Closing CQ-7 closes CQ-11.

### TEST-2: Service-layer test coverage gaps
Repo-level mutations are well tested. Service layer isn't:
- `service/TimerService.kt` — only `formatTime` + `TimerState` sealed interface tested. Tick loop, pause/lunch-window behaviour, boot-resume untested despite the recent "per-second countdown with extracted pure-state resolver" commit.
- `service/WorkScheduleManager.kt` — no test file.
- `service/WorkScheduleReceiver.kt` — no test file.
- `service/BootReceiver.kt` — no test file.
- Shuffle-bag logic is inlined in `TimerViewModel.onTimerExpired()` at lines 720-739 — tested only indirectly.

**Fix**: extract `TimerService` lunch/pause resolution into a pure helper analogous to `WidgetTimerState.resolveDisplay` and unit-test it. Same for `WorkScheduleManager`'s 7-day alarm scan (`service/WorkScheduleManager.kt:26-45`). Extract shuffle-bag into a standalone class.

### TEST-5: Oversize test files and setup boilerplate
`TimerViewModelTest.kt` is **1580 lines**, `SettingsRepositoryTest.kt` is **1040 lines** — both well over the 500-line guideline. The `collectFlows()` / `collectors.forEach { it.cancel() }` pattern repeats 50+ times in `TimerViewModelTest` — worth ~150 lines saved by a `withCollectors { … }` helper. `createMinimalBackupData` is duplicated between `SettingsRepositoryTest.kt:917` and `BackupManagerTest.kt:328`. **Fix**: split `TimerViewModelTest` into `…InputCoercionTest`, `…ShuffleTest`, `…AutoModeTest`, `…DynamicIncreaseTest`; extract `BackupDataFixtures.kt` in `app/src/test/kotlin/de/mysportsmate/officebreak/data/`.

### OBS-5: `shareAchievement` has no error handling
**File(s):** `ui/share/ShareAchievement.kt:38-48`, `ui/components/AchievementUnlockDialog.kt:209-217`, `ui/screen/AchievementsScreen.kt:207-214`

`shareAchievement` is launched via `scope.launch { shareAchievement(...) }` with zero `try`/`catch`. If cache write fails (low storage), `FileProvider.getUriForFile` throws, or no share target is installed (`ActivityNotFoundException`), the coroutine fails silently and the user sees nothing happen. The rest of the codebase has excellent error handling discipline — this is the one gap. **Fix**: wrap the render+share flow in `try`/`catch`, `Log.e` the throwable, surface a toast with a `share_error` string.

### SEC-3: Backup restore bypasses per-field validation
**File(s):** `data/BackupManager.kt:61-78`, `data/SettingsRepository.kt:394-448`

Structural JSON + `formatVersion` are validated — good. After a successful decode, field values are written straight to DataStore without bounds checking: `timerHours`/`repsMin`/`repsMax`/`beepVolume`/`beepCount` may be negative or `Int.MAX_VALUE`; exercise names aren't truncated to `MAX_EXERCISE_NAME_LENGTH`; schedule hours/minutes aren't range-checked (an out-of-range `workStartHour` throws `DateTimeException` in `TimerService.kt:146`, caught by the outer block but leaving the schedule broken). The UI paths (`TimerViewModel.setHours` uses `coerceIn(0, 23)`, `addExercise` trims to 100 chars) correctly defend — `restoreFromBackup` just skips all of it. **Fix** is coupled with **SEC-9** — sanitise in one place before writing.

### SEC-7: Raw exception messages in user-facing error toasts
**File(s):** `ui/TimerViewModel.kt:905-906,932`

`BackupUiState.Error(app.getString(R.string.backup_error, e.message ?: "Unknown error"))` surfaces raw `e.message` content (can include file paths, URIs, parser position info) in snackbars. Not a real security boundary on a single-user device app, but inconsistent UX. `BackupManager.restoreFromJson` itself correctly returns user-safe string resource IDs; only the I/O wrapper leaks. **Fix**: keep the `Log.e(TAG, …, e)` for diagnostics, show a fixed `R.string.backup_error_generic`.

### SEC-10: Manifest backup rules contradict `allowBackup=false`
**File(s):** `AndroidManifest.xml:15-16`, `res/xml/backup_rules.xml`

`android:allowBackup="false"` is set (good) but `android:dataExtractionRules="@xml/backup_rules"` points at a rules file that enables `<cloud-backup><include domain="sharedpref" path="." /></cloud-backup>` and `<device-transfer>`. On API 31+ `dataExtractionRules` is authoritative and re-enables cloud backup despite `allowBackup=false`. Pick one policy. **Also**: `foregroundServiceType="specialUse"` at `AndroidManifest.xml:35-38` is missing the `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" … />` declaration that Google Play's `specialUse` review typically requires on API 34+. And `startForeground(NOTIFICATION_ID, notif)` in `TimerService.kt:110` does not pass the type argument (works via manifest inference, but explicit `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` is current guidance). **Fix**: either set `allowBackup=true` to match the rules file, or remove/exclude the cloud-backup block; add the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` manifest property; pass the type constant to `startForeground`.

## Prioritised Recommendations

Ordered by impact / effort ratio.

### Priority 1 — Ship-blocker for Play Store
1. **Fix `specialUse` foreground service declaration** (SEC-10 subset) — add `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. Without this the next Play Console review for API 34+ targets will reject the release.

### Priority 2 — Safety
2. **Bound backup import** (SEC-9 + SEC-3) — file-size cap before `readText()`, list-size caps in `restoreFromJson`, numeric clamping + string truncation in `SettingsRepository.restoreFromBackup`. Single largest real risk.
3. **Reconcile backup policy** (SEC-10) — `allowBackup` vs `dataExtractionRules` contradict each other.

### Priority 3 — Structural debt with broad reach
4. **Centralise DataStore keys** (CQ-7 + CQ-11) — promote `SettingsRepository` keys to `internal val` or a `DataStoreKeys` object; route all seven consumer files through named keys (or better, typed read helpers). This one change closes two FAILs and prevents a class of silent-desync bugs.
5. **Use the existing `WidgetTimerState.STATUS_*` constants** (CQ-5 + CQ-10) — remove all string literals `"running"`, `"paused"`, `"expired"`, `"idle"`, `"work_ended"` from `TimerService`, `WidgetContent`, `TimerScreen`, `BootReceiver`.
6. **Extract `launchSafely` helper in `TimerViewModel`** (CQ-2) — collapses 38 copy-pasted setter wrappers.
7. **Resolve dependency locking** (SEC-8) — either write and commit the lockfiles or delete the inert block.

### Priority 4 — Test leverage
8. **Extract and test the service-layer pure cores** (TEST-2) — `TimerService` pause/lunch resolver, `WorkScheduleManager` next-alarm calculator, shuffle-bag. These are the exact places the last two release commits added complexity.
9. **Split the two oversize test files** (TEST-5) and extract `BackupDataFixtures.kt`.

### Priority 5 — Hygiene
10. **Wrap `shareAchievement` with try/catch + toast** (OBS-5) — silent failure of a user-initiated action is the last rough edge in otherwise-clean error handling.
11. **Static error strings in backup toasts** (SEC-7) — stop echoing `e.message`.
12. **Break up `TimerService.startTimer`** (CQ-1) — extract `loadTodayScheduleIfEnabled`, `handleScheduleTransitions`, `onTimerFinished`.

## Module Details

### Code Quality (5 PASS, 3 FAIL, 3 WARN, 0 N/A)

| ID | Assertion | Result |
|----|-----------|--------|
| CQ-1 | Functions stay reasonably short | WARN |
| CQ-2 | No duplicated code blocks | **FAIL** |
| CQ-3 | Naming consistent and descriptive | PASS |
| CQ-4 | No dead code or commented-out code | PASS |
| CQ-5 | Magic numbers and strings are constants | **FAIL** |
| CQ-6 | Imports clean and organised | PASS |
| CQ-7 | Configuration not scattered | **FAIL** |
| CQ-8 | No global mutable state mutated at event time | PASS |
| CQ-9 | No `when … is` chains that should be sealed dispatch | PASS |
| CQ-10 | No string-based type systems | WARN |
| CQ-11 | No magic return values controlling framework behaviour | WARN |

Strengths: clean naming, zero wildcard imports, `TimerStateHolder` correctly uses `MutableStateFlow` for thread-safety, sealed-class dispatch is used wherever the type is owned internally.

Themes: DataStore key strings + status strings leak out of their owning modules and get rebuilt inline in many places; `TimerViewModel` setters repeat a single error-handling shape 38 times.

### Testing (4 PASS, 0 FAIL, 2 WARN, 1 N/A)

| ID | Assertion | Result |
|----|-----------|--------|
| TEST-1 | Business logic testable without Android infrastructure | PASS |
| TEST-2 | Tests exist for all repo mutations / state transitions | WARN |
| TEST-3 | Negative / edge cases covered | PASS |
| TEST-4 | Auth / access control tested | N/A |
| TEST-5 | Test setup clean and maintainable | WARN |
| TEST-6 | Tests run independently and deterministically | PASS |
| TEST-7 | Assertions are specific, not "no crash" | PASS |

**Standout pattern**: the in-memory `FakeDataStore` + `FakeTimerServiceController` + `MainDispatcherRule` combination lets 22 of 23 tests run as pure JVM unit tests without Robolectric. Edge cases for backup parsing, achievement boundaries, night-shift schedule wrap-around, and corrupted DataStore fallbacks are well covered.

Gaps: the service layer (`TimerService` tick loop, `WorkScheduleManager` alarm scan, `BootReceiver`, `WorkScheduleReceiver`) and the shuffle-bag (inlined in `TimerViewModel`) have no direct unit tests. The empty `app/src/androidTest/` directory suggests instrumentation tests were planned but never added; CI runs `testDebugUnitTest` only.

#### Test coverage map (excerpt)

| File | Test | Status |
|------|------|--------|
| `data/AchievementEngine.kt` | `AchievementEngineTest.kt` | Covered |
| `data/BackupManager.kt` | `BackupManagerTest.kt` | Covered |
| `data/StatsRepository.kt` | `StatsRepositoryTest.kt` (with `Clock.fixed`) | Covered |
| `data/SettingsRepository.kt` | `SettingsRepositoryTest.kt` (1040 lines — oversize) | Covered |
| `data/DaySchedule.kt` | `DayScheduleTest.kt` | Covered |
| `widget/WidgetTimerState.kt` | `WidgetTimerStateTest.kt` | Covered |
| `ui/TimerViewModel.kt` | `TimerViewModelTest.kt` (1580 lines — oversize) | Covered |
| `service/TimerStateHolder.kt` | `TimerStateHolderTest.kt` | Covered |
| `service/TimerService.kt` | `TimerStateTest.kt` (only sealed + `formatTime`) | **Partial** |
| `service/WorkScheduleManager.kt` | — | **Missing** |
| `service/BootReceiver.kt` | — | **Missing** |
| `service/WorkScheduleReceiver.kt` | — | **Missing** |
| Shuffle-bag (inline in `TimerViewModel`) | via `TimerViewModelTest` | Indirect |
| `tts/BreakTtsManager.kt` | — | Missing |
| `ui/screen/*.kt` | — | Missing (no instrumentation tests) |

### Observability (3 PASS, 0 FAIL, 1 WARN, 4 N/A)

| ID | Assertion | Result |
|----|-----------|--------|
| OBS-1 | Structured JSON logs | N/A (Android Logcat, no Crashlytics/Sentry) |
| OBS-2 | Request ID propagation | N/A |
| OBS-3 | Request ID returned to client | N/A |
| OBS-4 | Auth events logged | N/A |
| OBS-5 | User-visible errors surfaced with actionable context | WARN |
| OBS-6 | Error logs include debugging context | PASS |
| OBS-7 | Log levels used appropriately | PASS |
| OBS-8 | No sensitive data in logs | PASS |

Notably clean logging discipline for a single-dev project: throwables are consistently passed as the third argument to `Log.e`/`Log.w`; `CancellationException` is correctly rethrown before a generic catch in `TimerService.kt:212-213`; silent `catch (_: Exception)` blocks exist only in benign media-resource teardown paths (`AudioTrack.release`, `MediaPlayer.release`) where sibling `Log.w` calls already cover the real decode paths. No user-entered text is logged verbatim. The `BackupUiState.Error` type distinguishes `import_error_invalid_format`, `import_error_newer_version`, and a generic fallback — exactly the right pattern.

Sole gap: `shareAchievement` has no `try`/`catch` at all (see WARN above).

For a Play Store release, adding Crashlytics or Sentry (OBS-1) would be a real improvement; for a personal-scale app it is an acceptable omission.

### Security (5 PASS, 2 FAIL, 3 WARN, 5 N/A)

| ID | Assertion | Result |
|----|-----------|--------|
| SEC-1 | Server auth enforced | N/A |
| SEC-2 | RBAC separate from authN | N/A |
| SEC-3 | User input validated before use | WARN |
| SEC-4 | Data scoping per user | N/A |
| SEC-5 | Session token hashing | N/A |
| SEC-6 | No hardcoded secrets | PASS |
| SEC-7 | Error responses don't leak internal details | WARN |
| SEC-8 | Dependencies pinned and auditable | **FAIL** |
| SEC-9 | Bounded operations | **FAIL** |
| SEC-10 | Secure defaults (manifest, backup rules) | WARN |
| SEC-11 | Runtime environment sniffing | N/A |
| SEC+ | `PendingIntent` flags (`FLAG_IMMUTABLE`) | PASS |
| SEC+ | Foreground service type declared | PASS (missing `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` — covered in SEC-10) |
| SEC+ | SAF URI permission lifetime | PASS |
| SEC+ | Notification channels | PASS |

Positives: every `PendingIntent` sets `FLAG_IMMUTABLE`, every intent is explicit (`setClass` + `setPackage`), every notification is posted to one of two explicitly created channels (`timer_channel` LOW / `timer_alert_channel` HIGH), the custom sound URI is correctly persisted via `takePersistableUriPermission`, `POST_NOTIFICATIONS` is requested at runtime on API 33+. No hardcoded secrets of any kind. Exported components are justified (`BootReceiver` needs `BOOT_COMPLETED` delivery and early-returns on non-matching actions; `WorkScheduleReceiver` is `exported=false`; `TimerService` is `exported=false`).

The two FAILs concentrate on one attack surface: backup import. A JSON file the user picks is untrusted content that currently flows straight into DataStore without size or shape limits. The fix is localised (one method in `BackupManager`) and closes both SEC-9 and SEC-3 together.

## Out of Scope (Modules Dropped)

| Module | Reason |
|--------|--------|
| architecture | Assumes handler → service → data-access layering with HTTP entry. No analogue. |
| errors | Assumes exception-to-HTTP mapping and client error response contracts. No server. |
| data-access | Assumes SQL/NoSQL ORM with N+1, connection pool, index concerns. DataStore-only. |
| data-contracts | Assumes API request/response schemas and versioning. Only on-device JSON (already handled by the covered `BackupData` format-version logic). |

A focused Android architecture review (Compose MVI/MVVM boundaries, unidirectional data flow, `ViewModel` scope hygiene, process-separation between the main app and the Glance widget) would be a worthwhile follow-up.

## Context Packet Referenced By Sub-Agents

- **Stack**: Kotlin 2.2.10, Jetpack Compose + Material 3, Jetpack Glance 1.1.1, DataStore Preferences 1.1.4, kotlinx.serialization 1.8.1, kotlinx.coroutines 1.10.1. Min SDK 28, Target SDK 36. Single `:app` Gradle module.
- **Source counts**: 57 production `.kt`, 23 unit test `.kt` (`~5900` test LOC), 0 instrumented tests (directory present but empty).
- **CI**: `.github/workflows/test.yml` runs `assembleDebug`, `testDebugUnitTest`, `lintDebug` on push to main/develop/release + all PRs.
- **Release hygiene**: `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard rules present, Java 17.
- **Persistence**: DataStore on-device only. No network layer.
