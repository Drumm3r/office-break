# Compose UI Audit Report — office-break

**Date**: 2026-04-21
**Target**: `/home/schoenenborn/projects/github/office-break`
**Modules run**: Design Tokens, Components, Accessibility, Performance
**Version audited**: v0.8.0 (commit `627bc76`), branch `v0.8.0`

## About This Audit

The `/audit-frontend` skill is calibrated for web stacks (React / HTML / CSS / Tailwind / ARIA / Core Web Vitals). This project is an Android / Jetpack Compose app — so the orchestrator remapped each module to Compose + Material 3 + Android accessibility:

- `design-tokens` → `MaterialTheme` color scheme, typography scale, `.dp` spacing, `Shapes`
- `components` → `@Composable` composition, parameter design, state hoisting, stability
- `accessibility` → `Modifier.semantics`, `contentDescription`, TalkBack, D-pad focus, touch targets, `sp` scaling
- `performance` → recomposition scope, stability inference, coroutine dispatchers, APK shrinking

Three modules were dropped:
- `seo-meta` — no HTML, no web
- `code-quality` — already covered by the companion `/audit-backend` run (same repo, 2026-04-21)
- `security` — already covered by the `/audit-backend` run (same repo, 2026-04-21)

See `docs/audits/audit-backend-report-2026-04-21.md` for those findings.

## Scorecard

| Module | PASS | NEEDS IMPROVEMENT | FAIL | N/A | Applicable | Score |
|--------|------|-------------------|------|-----|------------|-------|
| Design Tokens | 2 | 4 | 0 | 1 | 6 | 33% |
| Components | 3 | 5 | 0 | 0 | 8 | 38% |
| Accessibility | 4 | 7 | 0 | 0 | 11 | 36% |
| Performance | 3 | 3 | 0 | 2 | 6 | 50% |
| **Total** | **12** | **19** | **0** | **3** | **31** | **39%** |

Applicable = PASS + NEEDS IMPROVEMENT + FAIL. Score = PASS / Applicable.

Zero FAILs. 19 NEEDS IMPROVEMENT findings cluster around theming polish, composable API hygiene, and basic TalkBack support.

## Executive Summary

The UI layer is solid on fundamentals: Material 3 primitives used throughout, dark/light themes correctly wired, rememberSaveable used for process-death survival, German + English translations are symmetric (301 strings each), every `IconButton` has a `contentDescription`, no `Image(...)` misuses. R8 shrinking and resource shrinking are enabled. sp-based text sizing means the system font scale propagates correctly. RTL is declared even though no RTL locale ships.

Four recurring themes across the findings:

1. **Theme tokens leak at the edges** — the Glance widget (`WidgetContent.kt:159-184`) and the shareable achievement card (`AchievementShareCard.kt:64-95`) hardcode hex colors and raw `TextStyle(fontSize = ...sp)` instead of consuming the central theme. There's no `Spacing` / `Dimens` object, though in practice `.dp` values are mostly on an 8dp grid.
2. **Composable API hygiene** — 7 of 11 component composables omit `modifier: Modifier = Modifier`; three confirm dialogs are byte-for-byte duplicates; `SettingsScreen` takes 37 parameters.
3. **TalkBack support is partial** — zero `Modifier.semantics { heading() }`, sliders lack content descriptions, two `IconButton`s are below the 48dp touch target, and the circular countdown reads as "12:34" instead of "12 minutes 34 seconds".
4. **One real performance nit** — `TimerService.kt:60` runs the per-second tick loop on `Dispatchers.Main`, sharing the main thread with Compose frame rendering. A one-line change.

No findings block release. The accessibility and theme-drift items are the highest-leverage for a polished v1.0.

## NEEDS IMPROVEMENT Findings (Grouped By Theme)

### Theme-token drift between app, widget, and share card

- **Color tokens bypassed in widget**: `widget/WidgetContent.kt:159-184` defines a private `WidgetColors` object with hardcoded hex (`#1C1B1F`, `#E6E1E5`, `#49454F`, `#CAC4D0`, `#003A00`) that duplicates what Material 3 already provides. Likely to silently drift from the app theme if M3 defaults update.
- **Color tokens bypassed in share card**: `ui/components/AchievementShareCard.kt` uses `Color.White` and `GreenPrimary`/`GreenPrimaryDark` directly. Partly defensible (the exported image is a fixed brand look), but the six confetti colours in `ui/components/AchievementUnlockDialog.kt:86-91` are pure one-offs.
- **`Color(0xFF003A00)` repeats 3×**: `ui/theme/Theme.kt:12`, `Theme.kt:16`, `widget/WidgetContent.kt:182`. Extract to `val OnGreenPrimaryDark` in `ui/theme/Color.kt`.
- **Typography bypassed in share card + widget**: `AchievementShareCard.kt:64,75,85,93` use raw `fontSize = 20.sp / 32.sp / 18.sp / 14.sp`; `WidgetContent.kt:55,75,89,128` use off-scale `13.sp` (should be 12 or 14 to match the M3 scale).
- **Seed color duplicated**: `res/values/colors.xml` `<color name="seed">#009900</color>` duplicates `ui/theme/Color.kt:5 Seed`. Intentional (launcher-icon resource) but undocumented coupling.
- **Primary color contrast is borderline**: `GreenPrimary (#009900)` + white `onPrimary` = ~3.19:1 contrast. That passes WCAG AA large-text (≥18pt / 14pt-bold) where it's used on filled `Button` (22sp titleLarge), but any future normal-text use would fail. Deepening `GreenPrimary` to `#007A00` or similar would give headroom.

**Fix**: lift `OnGreenPrimaryDark` and `ConfettiColors` into `Color.kt`; add `MaterialTheme.typography.headlineLarge.copy(letterSpacing = 1.sp)` references in `AchievementShareCard`; align widget text sizes to the 12/14/16 scale; re-check the primary-color contrast before a polish pass.

### Missing central spacing / shape tokens

- **No `Spacing`/`Dimens` object** — 212 inline `.dp` literals across 14 UI files. Values ARE mostly on an 8dp/4dp grid (top-7: 8, 16, 24, 32, 48, 12, 56). Outliers: `10.dp` (1), `22.dp` (1), `6.dp` (3), `2.dp` (3), `36.dp` (1).
- **Shape tokens partially used** — `SettingsScreen.kt:704` uses `MaterialTheme.shapes.small` but `TimerSetup.kt:99` hardcodes `RoundedCornerShape(12.dp)` and `WidgetContent.kt:46` hardcodes `.cornerRadius(16.dp)`. `Theme.kt:39-43` doesn't pass a custom `Shapes(...)` to `MaterialTheme`.

**Fix**: add `ui/theme/Spacing.kt` with `object Spacing { val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp; val xl = 32.dp }`. Extend the `Shapes` in `Theme.kt` and replace hardcoded `RoundedCornerShape(12.dp)`.

### Composable API hygiene

- **Three identical confirm dialogs**: `ConfirmResetDialog.kt:12-32`, `ConfirmImportDialog.kt:12-32`, `ConfirmResetStatsDialog.kt:12-32` differ only in three `stringResource` IDs. Collapse into one `ConfirmationDialog(titleRes, messageRes, confirmRes, dismissRes, onConfirm, onDismiss)`.
- **Missing `modifier: Modifier = Modifier` on 7/11 components**: `ExerciseDialog`, `AchievementUnlockDialog`, `AchievementShareCard`, `ConfirmResetDialog`, `ConfirmImportDialog`, `ConfirmResetStatsDialog`, `DynamicIncreaseDialog`. Violates the standard Compose API guideline; callers can't style, offset, or attach test tags.
- **37-parameter `SettingsScreen` and 38-parameter `TimerScreen`**: `ui/screen/SettingsScreen.kt:79-117`, `ui/screen/TimerScreen.kt:216-254`. Pure prop drilling. `TimerScreen.kt:66` already takes a `viewModel: TimerViewModel` directly — apply the same pattern to `SettingsScreen` or group related settings into immutable data classes (e.g. `TimerPrefs`, `SoundPrefs`, `SchedulePrefs`).
- **11-parameter `TimerSetup`**: `ui/components/TimerSetup.kt:46-57`. Borderline; a `TimerSetupState` + `TimerSetupActions` pair would help.
- **Repeated `Spacer + HorizontalDivider + Spacer + Text(titleMedium)` boilerplate**: 8× in `SettingsScreen.kt` (lines 192-305, 347-435, 466-474), 4× in `StatsScreen.kt` (lines 120-122, 201-203, 229-231, 266-268). Extract a `SettingsSection(title, content: @Composable ColumnScope.() -> Unit)` slot composable.
- **`SettingsToggleRow` is `private` to its screen**: promote to a shared `ui/components/LabeledSwitchRow.kt` — other screens duplicate the pattern.

### Loading/error UX gaps

- **Empty `Box` splash during onboarding-state resolution**: `MainActivity.kt:70` — on cold boot there's a brief blank frame before DataStore emits `onboardingCompleted`. Show a `CircularProgressIndicator` instead.
- **No `Loading` state on `BackupUiState`** (`TimerViewModel.kt:47-52`): during a large import the user sees no feedback. Low severity given the 5-MB cap recommended by the security audit.
- **Silent fallback on `SettingsRepository` decode errors**: when a corrupt preference is decoded, the repository falls back to a default (already flagged good by `/audit-backend` OBS-6) — but there's no UI affordance telling the user it happened.
- **Silent fallback on preview-sound error**: `TimerViewModel.kt:521-522` falls back to a beep without telling the user why.

### Compose stability and recomposition scope

- **No `@Immutable`/`@Stable` annotations**: Kotlin 2.x Compose compiler infers stability for simple data classes, but `List<Exercise>` / `List<BreakRecord>` parameters (`TimerScreen.kt:203`, `StatsScreen.kt:44`) are `kotlin.collections.List` — Compose marks them unstable, defeating recomposition skipping on `ExerciseSettingsScreen` and `StatsScreen`.
- **No `derivedStateOf`** (0 occurrences). `StatsScreen.kt:127-154` recomputes week/last-week aggregates on every recomposition; `SettingsScreen.kt:288-300` recomputes `resolveEffectiveSchedule` inside a `forEachIndexed` loop. Wrap in `remember(inputs) { ... }`.
- **Per-second recomposition scope**: `TimerScreen.kt:328-492` reads `timerState` at the root of an `AnimatedContent`, so the whole running `Column` recomposes every tick. Compose's smart skipping saves most of the cost, but extracting a `TimerRunningContent(remainingSeconds: Long, totalSeconds: Long, onReset: () -> Unit)` leaf composable with only primitive Long parameters would isolate the hot path.
- **`items()` without `key` on fixed-registry list**: `AchievementsScreen.kt:98`. Add `key = { it.id }` — zero cost, forward-compatible with any future add/remove.

### Runtime / lifecycle

- **`TimerService` runs its tick loop on `Dispatchers.Main`**: `service/TimerService.kt:60` uses `CoroutineScope(Dispatchers.Main + SupervisorJob())`. The per-second tick (`startTimer` lines 116-219) calls `WidgetUpdater.requestUpdate`, `NotificationManager.notify`, and `dataStore.edit` on the main thread, sharing CPU with Compose frame rendering. All those APIs are thread-safe — switch to `Dispatchers.Default`. **One-line fix, top-ROI in this module.**
- **`TimerViewModel.onCleared()` doesn't call `stopPreview()`**: if the user tears down the ViewModel mid-preview, `AudioTrack` / `MediaPlayer` leak. Add `override fun onCleared() { stopPreview(); super.onCleared() }`.
- **`BreakTtsManager` scoped to `TimerScreen` composition**: `TimerScreen.kt:100-103` creates + destroys the TTS engine on every screen entry. On config change, the engine is shut down and re-bound — ~50-100ms delay. Hoist to the ViewModel.
- **`NotificationCompat.Builder` rebuilt every second**: `TimerService.kt:259-263`. Cache the builder, reuse via `setContentText`. Minor.

### Accessibility — touch targets & form labels

- **Touch targets below 48dp (WCAG 2.5.5 failure)**:
  - `TimerSetup.kt:148` — link/unlink reps `IconButton(modifier = Modifier.size(24.dp))`. Raise to 48dp, keep inner icon at 24dp.
  - `SettingsScreen.kt:691-697` — per-day linked toggle at 36dp. Raise to 48dp.
- **`LanguageDropdown` `OutlinedTextField` has no `label`**: `SettingsScreen.kt:520`. The `ThemeDropdown` counterpart (line 572) correctly supplies one. Inconsistent. Add `label = { Text(stringResource(R.string.settings_language)) }`.
- **Sliders lack `contentDescription`**: `VolumeBar.kt:65`, `TimerSetup.kt:272` (`SliderRow`), `SettingsScreen.kt:620-627` (`BeepCountSlider`). TalkBack announces "Slider, 50 percent" with no hint what it controls. Add `Modifier.semantics { contentDescription = label }`.
- **Number inputs missing `imeAction`**: `TimerSetup.kt:188, 210` hour/minute fields. Add `ImeAction.Done`.

### Accessibility — semantics & announcements

- **Zero `Modifier.semantics` usage in entire codebase** (grep confirmed). Section titles across `SettingsScreen.kt:180,196,211,245,307,437,470`, `StatsScreen.kt:91,156,211,234,270`, `AchievementsScreen.kt:91-95` use `titleMedium` style but no `semantics { heading() }` — TalkBack heading-jump navigation is dead.
- **Custom clickable `Text` lacks `Role.Button`**: `TimerSetup.kt:100` (tappable time display). Add `Modifier.clickable(role = Role.Button) { ... }`.
- **Expandable day card has no `stateDescription`**: `SettingsScreen.kt:663`. Add `role = Role.Button` + `stateDescription = if (expanded) "expanded" else "collapsed"`.
- **`CountdownDisplay` reads as "12:34" literally**: `ui/components/CountdownDisplay.kt:27,61-65`. A blind user hears "twelve colon thirty-four". Add `Modifier.semantics { contentDescription = "$minutes minutes $seconds seconds remaining"; progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f); mergeDescendants = true }` on the outer `Box`.
- **Exercise-name `Text` in `ExerciseDialog` has no `liveRegion`**: when TTS is disabled but TalkBack is active, users rely on focus shift. Add `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` to harden.
- **`Toast.makeText` instead of `Snackbar`**: `TimerScreen.kt:135`. Toasts are announced since API 30 but are transient and unreplayable. Replace with a `SnackbarHost` inside the `Scaffold`.

### Accessibility — motion & reflow

- **Confetti animation ignores reduced-motion setting**: `AchievementUnlockDialog.kt:109-115` runs a 4-second `awaitFrame()` loop without consulting `AccessibilityManager.isReduceMotionEnabled`. Guard the `Canvas` with `val reduceMotion = context.getSystemService<AccessibilityManager>()?.isReduceMotionEnabled == true` and skip the loop when true.
- **`DynamicIncreaseDialog` 60-second auto-dismiss (WCAG 2.2.1 adjust/extend requirement)**: `DynamicIncreaseDialog.kt:41-44`. Declining is the same as timing out, so no destructive action on timeout — acceptable. Adding an "extend" affordance would be ideal.
- **Idle `TimerScreen` is not scrollable**: `TimerScreen.kt:341-375`. At 200% font scale on a small screen, content may clip. Add `.verticalScroll(rememberScrollState())`.
- **`CountdownDisplay` has fixed 280.dp size**: `CountdownDisplay.kt:31`. At 200% font scale, the 57sp `displayLarge` text may overflow or wrap (no `maxLines = 1`). Manual QA; optionally add `autoSize`/`maxLines = 1`.

### APK build configuration

- **`material-icons-extended` pulls in thousands of vectors**: `app/build.gradle.kts:57`. R8 strips unreferenced icons in practice, but one reflection-like keep rule defeats the shrinker. Only ~6 icons are actually used in screens. Either switch to `material-icons-core` and import individual icons, or verify via an APK-size diff that R8 is doing its job.
- **Blanket keep-rule for widget package**: `app/proguard-rules.pro:6` `-keep class de.mysportsmate.officebreak.widget.** { *; }` prevents any shrinking inside widget code. Narrow to `-keep public class ... extends androidx.glance.appwidget.GlanceAppWidget` / `... extends ...Receiver`.

## Prioritised Recommendations

Ordered by impact / effort ratio.

### Priority 1 — Real user impact, small fix
1. **`TimerService.kt:60`** — change `Dispatchers.Main` to `Dispatchers.Default`. One-line, removes per-second main-thread load.
2. **Touch targets** — `TimerSetup.kt:148` 24dp → 48dp, `SettingsScreen.kt:691-697` 36dp → 48dp. WCAG 2.5.5 direct failure, trivial fix.
3. **`LanguageDropdown` missing label** — `SettingsScreen.kt:520`. Screen-reader users can't identify the field.
4. **Slider `contentDescription`** — `VolumeBar.kt:65`, `TimerSetup.kt:272`, `SettingsScreen.kt:620`.

### Priority 2 — Accessibility polish
5. **Heading semantics on section titles** — adds TalkBack heading-jump navigation across Settings/Stats/Achievements/Onboarding.
6. **`CountdownDisplay` semantics** — content description + progress range.
7. **Reduce-motion guard on confetti** — `AchievementUnlockDialog.kt:109`.
8. **`Toast` → `Snackbar`** — `TimerScreen.kt:135`.
9. **Scroll idle `TimerScreen`** — `TimerScreen.kt:341-375`. Fixes 200% font-scale clipping.

### Priority 3 — Theme consistency
10. **Extract `OnGreenPrimaryDark` + `ConfettiColors` to `Color.kt`** — removes the three repeated `Color(0xFF003A00)` literals.
11. **Align widget typography to M3 scale** — `13.sp` → 14 or 12 in `WidgetContent.kt`.
12. **Reference `MaterialTheme.typography` in `AchievementShareCard`** — via `.copy()` to preserve letterSpacing.
13. **Add `ui/theme/Spacing.kt`** with an xs/sm/md/lg/xl scale, extend `MaterialTheme.shapes`.
14. **Verify / deepen `GreenPrimary`** — current 3.19:1 against white borderline; deepen to `#007A00` for headroom.

### Priority 4 — Composable API hygiene
15. **Collapse 3 confirm dialogs into `ConfirmationDialog`** — `ui/components/`.
16. **Extract `SettingsSection` slot composable** — removes repeated divider+header boilerplate.
17. **Add `modifier: Modifier = Modifier`** to 7 component composables.
18. **Refactor `SettingsScreen` 37-param wall** — accept `viewModel` directly (like `TimerScreen`) or group into data classes.

### Priority 5 — Performance polish
19. **Extract `TimerRunningContent` leaf composable** — scopes per-second recomposition.
20. **Wrap Stats/Settings computations in `remember(inputs)`** — avoid repeated week/schedule resolution.
21. **Hoist `BreakTtsManager` into `TimerViewModel`** — survives config change.
22. **Add `TimerViewModel.onCleared()` releasing preview** — prevents `AudioTrack` leak.
23. **Cache `NotificationCompat.Builder`** in `TimerService`.
24. **Verify `material-icons-extended` shrinkage** — APK-size diff; consider switch to `material-icons-core`.
25. **Tighten widget keep-rule** — `proguard-rules.pro:6`.

### Priority 6 — Minor polish
26. **Add `@Immutable`** to read-model data classes (`Exercise`, `DaySchedule`, `StatsSnapshot`, `AchievementState`, `BreakRecord`) that land in `LazyColumn`s.
27. **Splash placeholder** instead of empty `Box` in `MainActivity.kt:70`.
28. **Fix `AchievementIcon` mapping bug** — `AchievementIcon.kt:25,32` maps `"Diversity3"` → `AutoAwesome` and `"LunchDining"` → `Today` (wrong icons).

## Module Details

### Design Tokens (2 PASS, 4 NEEDS IMPROVEMENT, 0 FAIL, 1 N/A)

| ID | Category | Rating |
|----|----------|--------|
| DT-1 | Color token architecture | NEEDS IMPROVEMENT |
| DT-2 | Typography system | NEEDS IMPROVEMENT |
| DT-3 | Spacing & layout tokens | NEEDS IMPROVEMENT |
| DT-4 | Dark mode implementation | PASS |
| DT-5 | Token consistency | NEEDS IMPROVEMENT |
| DT-6 | Tailwind-specific | N/A (Compose) |
| DT-7 | String resource hygiene | PASS |

**Highlights**: 301 strings per locale, EN/DE key sets are identical (diff empty). Dark/light schemes are concrete and distinct (`Theme.kt:10-30`). 203 `MaterialTheme.colorScheme.*` usages vs. 23 raw `Color(0x…)` sites — tokens ARE consumed at the core, drift is localised to the widget and share card.

**Gaps**: widget and share card hardcode palettes; one-off literals repeat 3×; no central `Spacing` / `Shapes`; `AchievementShareCard` bypasses typography; seed color duplicated between `Color.kt` and `colors.xml`.

### Components (3 PASS, 5 NEEDS IMPROVEMENT, 0 FAIL)

| ID | Category | Rating |
|----|----------|--------|
| C-1 | Composition & reusability | NEEDS IMPROVEMENT |
| C-2 | Type safety & parameter design | NEEDS IMPROVEMENT |
| C-3 | State management & hoisting | PASS |
| C-4 | File structure | PASS |
| C-5 | Shared component library | PASS |
| C-6 | Error & loading states | NEEDS IMPROVEMENT |
| C-7 | Constants & configuration | PASS |
| C-8 | Composable-specific patterns | NEEDS IMPROVEMENT |

**Highlights**: zero `Any`/`Any?` in UI layer. Sealed interfaces (`BackupUiState`, `DynamicIncreaseOffer`) used well. `StateFlow` collected via `collectAsState` at 33 sites in `TimerScreen.kt` — proper hoisting. `rememberSaveable` for process-death survival. One Material 3 primitive for every need; no reinvented buttons / cards / dialogs. All `LaunchedEffect` keys are correct.

**Gaps**: three duplicate confirm dialogs; 7/11 components skip `modifier` param; `SettingsScreen` prop-drilling; repeated section/header boilerplate across 12 call sites; no `@Immutable` annotations; empty `Box` on cold-boot splash.

### Accessibility (4 PASS, 7 NEEDS IMPROVEMENT, 0 FAIL)

| ID | Category | Rating |
|----|----------|--------|
| A-1 | Semantic structure | NEEDS IMPROVEMENT |
| A-2 | ARIA/semantics equivalents | PASS |
| A-3 | Keyboard / D-pad navigation | PASS |
| A-4 | Form accessibility | NEEDS IMPROVEMENT |
| A-5 | Image / icon accessibility | PASS |
| A-6 | Colour & contrast | NEEDS IMPROVEMENT |
| A-7 | Dynamic content announcements | NEEDS IMPROVEMENT |
| A-8 | Screen reader support | NEEDS IMPROVEMENT |
| A-9 | Reflow / text scaling | PASS |
| A-10 | Motion & timing | NEEDS IMPROVEMENT |
| A-11 | Android-specific (touch, locale, IME) | NEEDS IMPROVEMENT |

**Highlights**: every `IconButton` has a `contentDescription`; decorative icons correctly pass `null`; no `Image(...)` composables to leak; all fonts are `sp`-based; RTL declared; `attachBaseContext` applies the user locale before composition. Material 3 defaults handle focus, ARIA dialog semantics, and keyboard traversal out of the box.

**Gaps**: zero `Modifier.semantics` calls in the entire codebase (verified by grep) — no heading navigation, no progress-range info, no live regions. Sliders announce as "Slider, 50 percent" with no context. Circular countdown reads as "12:34" (colon). Two touch targets below 48dp. LanguageDropdown unlabeled. Confetti ignores reduce-motion. Primary green on white = 3.19:1 — borderline for non-large text.

### Performance (3 PASS, 3 NEEDS IMPROVEMENT, 0 FAIL, 2 N/A)

| ID | Category | Rating |
|----|----------|--------|
| P-1 | Bundle size / APK shrinking | NEEDS IMPROVEMENT |
| P-2 | Asset optimization | PASS |
| P-3 | Rendering / recomposition | NEEDS IMPROVEMENT |
| P-4 | Network performance | N/A |
| P-5 | Build configuration | PASS |
| P-6 | Runtime patterns (dispatchers, leaks) | NEEDS IMPROVEMENT |
| P-7 | CSS performance | N/A |
| P-8 | Core Web Vitals equivalents (cold start, frame time, ANR) | PASS |

**Highlights**: two tiny vector drawables, no bitmap assets, adaptive icon, no custom fonts. R8 + resource shrinking enabled. Kotlin 2.2.10 Compose compiler. No `BuildConfig.DEBUG` gating needed (R8 strips `Log.d/v`). `Dispatchers.IO` correctly used for backup import and share bitmap generation. Scope and wake-lock cleanup on `onDestroy` is tight. `SharingStarted.Eagerly` is applied deliberately where needed.

**Gaps**: the single highest-leverage finding in this module — `TimerService` tick loop runs on `Dispatchers.Main`. `TimerViewModel.onCleared` doesn't release preview audio. Unstable `List<>` parameters defeat recomposition skipping on `StatsScreen` / `ExerciseSettingsScreen`. `LazyColumn items()` key missing on `AchievementsScreen.kt:98`.

## Overlap With `/audit-backend` Report (2026-04-21)

These findings reinforce earlier backend-audit items rather than replace them:

- Theme-token drift (this report) ≈ CQ-5 "magic numbers and strings are constants" (backend) — same root, different lens. Backend focused on DataStore key / status strings; this report focused on color / typography.
- `TimerService.startTimer` 118-line warning (backend CQ-1) — this report adds the dispatcher concern and the per-tick notification-builder rebuild as specific costs inside that function.
- `shareAchievement` has no error handling (backend OBS-5 WARN) — this report also surfaces it via "silent fallback" in the components module.
- `TimerViewModelTest.kt` 1580-line warning (backend TEST-5) — this report notes the 37-parameter `SettingsScreen` as a related symptom of the same VM-level prop-drilling pattern.

## Out Of Scope (Modules Dropped)

| Module | Reason |
|--------|--------|
| `seo-meta` | No HTML. Android apps don't ship `<head>` tags, `robots.txt`, or Open Graph metadata. |
| `code-quality` | Covered by `/audit-backend` CQ-1 through CQ-11 in `audit-backend-report-2026-04-21.md`. |
| `security` | Covered by `/audit-backend` SEC-1 through SEC-11 (+ 4 Android-specific) in the same report. |

## Context Packet

- **Stack**: Kotlin 2.2.10, Jetpack Compose BOM 2025.03.00, Material 3, Jetpack Glance 1.1.1, DataStore Preferences 1.1.4, Coroutines 1.10.1. Min SDK 28 / Target 36.
- **UI layer** (17 files): 6 screens, 11 components, 3 theme files (`Color.kt`, `Theme.kt`, `Type.kt`), 1 `TimerViewModel`, 1 share helper.
- **Resources**: 301 strings each in `values/strings.xml` (default/EN) and `values-de/strings.xml`. 2 vector drawables. Adaptive launcher icon via `mipmap-anydpi-v26`.
- **Build**: `isMinifyEnabled = true`, `isShrinkResources = true`. Java 17. `dependencyLocking` declared but lockfiles missing (see backend SEC-8).
- **Tests**: 23 unit tests, mostly pure-JVM via `FakeDataStore`. Empty `androidTest/` directory — no instrumentation or Compose UI tests.
