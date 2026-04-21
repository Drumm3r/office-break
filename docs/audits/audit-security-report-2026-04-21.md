# Security Audit Report — office-break

**Target**: `/home/schoenenborn/projects/github/office-break`
**Date**: 2026-04-21
**Modules Run**: secrets, dependencies, cicd
**Modules Skipped**: code (covered by `/audit-backend` SEC-1..SEC-11), api (no server), frontend (no web), terraform (no .tf files)
**Version audited**: v0.8.0 (commit `627bc76`), branch `v0.8.0`

---

## Executive Summary

This report focuses on supply-chain, CI/CD, and secret-management risks that complement the earlier `/audit-backend` security module (SEC-1 through SEC-11 + Android-specific checks in `audit-backend-report-2026-04-21.md`). Three modules were applicable to this Android/Kotlin project; `code`, `api`, `frontend`, and `terraform` were skipped as either already-covered or not-present.

Findings cluster around **build-tool integrity**, not runtime code. The codebase itself passes every secrets check cleanly (no keys, no tokens, no committed certificates, no suspicious git history). The real risks are two silent trust-gaps in the build system — an unpinned Gradle wrapper distribution and overly broad "trusted-artifacts" wildcards in `verification-metadata.xml` — that together mean a supply-chain attacker could inject code into a release APK without any local check failing. Both are one-line fixes.

No Critical or High severity findings. Two Mediums worth addressing before the next Play Store release. Everything else is defence-in-depth hardening appropriate for personal-scale but not urgent.

**Overall Risk**: **Low** (Medium only if a release is imminent without addressing M-1 and M-2).

## Finding Summary

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 0 |
| Medium | 2 |
| Low | 4 |
| Informational | 4 |
| **Total** | **10** |

### Findings by Module

| Module | Critical | High | Medium | Low | Info |
|--------|----------|------|--------|-----|------|
| secrets | 0 | 0 | 0 | 0 | 0 |
| dependencies | 0 | 0 | 2 | 1 | 1 |
| cicd | 0 | 0 | 2 | 3 | 3 |
| **Net (deduplicated)** | **0** | **0** | **2** | **4** | **4** |

The 2 Mediums were reported by both dependencies and cicd agents (same root cause, same file) — merged here. All other findings are module-unique.

### Cross-Reference to Prior Audit

| Prior Finding | This Report |
|---------------|-------------|
| `/audit-backend` SEC-6 (No hardcoded secrets — PASS) | Reconfirmed. Deeper sweep found nothing. |
| `/audit-backend` SEC-8 (`dependencyLocking` declared but lockfiles missing — FAIL) | Reconfirmed. Not re-reported here. See backend report. |
| `/audit-backend` SEC-9 (Unbounded backup import) | Not in scope for this audit — code module skipped. |
| `/audit-backend` SEC-10 (Manifest backup-rules contradiction + missing FGS subtype) | Not in scope. |

---

## Critical Findings

*None.*

## High Findings

*None.*

## Medium Findings

### M-1: Gradle wrapper distribution lacks SHA-256 checksum pin

**Status:** OPEN
**File:** `gradle/wrapper/gradle-wrapper.properties:3`
**Affected files:**
- `gradle/wrapper/gradle-wrapper.properties`
**Severity:** Medium
**Confidence:** HIGH
**Category:** Supply-chain integrity / build-tool tampering
**Standards:** OWASP A08:2025 (Software & Data Integrity Failures), CWE-494 (Download of Code Without Integrity Check), CICD-SEC-3, CICD-SEC-7, SLSA Build L2
**Modules:** dependencies, cicd
**Description:** The Gradle wrapper bootstraps the entire build from `services.gradle.org`. `distributionSha256Sum` is **missing**, so `gradlew` will accept whatever bytes the distribution URL returns. `validateDistributionUrl=true` only validates the URL string pattern, not the payload. `verification-metadata.xml` protects dependencies *after* the wrapper runs — but not the wrapper bootstrap itself. If the wrapper ZIP is tampered with in transit (compromised CDN edge, MITM in a corporate proxy, DNS hijack) the build host will execute arbitrary code with the developer/CI-runner's privileges before any dependency hashes are consulted.
**Evidence:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
# no distributionSha256Sum line
```
**Current controls:** HTTPS distribution URL; `verification-metadata.xml` protects everything *after* the wrapper runs but not the bootstrap.
**Exploit scenario:** A CI runner fetches the wrapper on a fresh agent. An attacker with transient control of a transparent proxy or compromised mirror serves a malicious `gradle-9.3.1-bin.zip` whose `init.d/*.gradle` script exfiltrates `ANDROID_KEYSTORE_PASSWORD` (from a future release pipeline) or rewrites the APK's bytecode. The developer never notices because all downstream dependencies still pass verification.
**Fix:**
1. Look up the official SHA-256 for Gradle 9.3.1 `-bin.zip` at https://gradle.org/release-checksums/.
2. Add to `gradle/wrapper/gradle-wrapper.properties`:
   ```properties
   distributionSha256Sum=<official-sha256-from-gradle.org>
   ```
3. Refresh on every wrapper upgrade via `./gradlew wrapper --gradle-version X.Y.Z --distribution-type bin` (this step regenerates the checksum automatically if the new version is fetched with `--distribution-sha256-sum`).
**Remediation notes:** *(to be filled during triage)*

---

### M-2: `gradle/verification-metadata.xml` accepts unsigned artifacts via broad `trusted-artifacts` wildcards

**Status:** OPEN
**File:** `gradle/verification-metadata.xml:5-20`
**Affected files:**
- `gradle/verification-metadata.xml`
**Severity:** Medium
**Confidence:** HIGH
**Category:** Improper artifact integrity validation
**Standards:** OWASP A08:2025, CWE-345 (Insufficient Verification of Data Authenticity), CICD-SEC-3, CICD-SEC-9
**Modules:** dependencies, cicd
**Description:** Verification metadata is enabled (good — 3854 lines of SHA-256 for listed components), but:
1. `<verify-signatures>false</verify-signatures>` — PGP signatures on Maven artifacts are ignored; only SHA-256s are checked.
2. `<trusted-artifacts>` whitelists entire group namespaces with regex: `^androidx\..*`, `^com\.android.*`, `^com\.google.*`, `^org\.jetbrains.*`, `^org\.junit.*`, `^org\.apache.*`, `^commons-.*`, `^org\.codehaus.*`. Any artifact in those groups bypasses SHA-256 verification entirely. This is self-defeating: the 3854-line checksum file provides false assurance because the bulk of the actual dependency tree (all AndroidX, all Kotlin stdlib, all Google libs) is exempt from verification.
3. `.*-javadoc\.jar` / `.*-sources\.jar` / `.*-src\.zip` are trusted unconditionally across every group — less critical at build time but worth noting.

Together these reduce the enforcement surface from "every dependency verified" to "only non-Android, non-Kotlin, non-Google deps verified" — which is very few artifacts in an Android app.
**Evidence:**
```xml
<verify-metadata>true</verify-metadata>
<verify-signatures>false</verify-signatures>
<trusted-artifacts>
    <trust group="^androidx\..*" regex="true"/>
    <trust group="^com\.android.*" regex="true"/>
    <trust group="^com\.google.*" regex="true"/>
    <trust group="^org\.jetbrains.*" regex="true"/>
    <trust group="^org\.junit.*" regex="true"/>
    <trust group="^org\.apache.*" regex="true"/>
    <trust group="^commons-.*" regex="true"/>
    <trust group="^org\.codehaus.*" regex="true"/>
    ...
</trusted-artifacts>
```
**Current controls:** HTTPS to Google Maven and Maven Central; `RepositoriesMode.FAIL_ON_PROJECT_REPOS` prevents sub-module repo injection; SHA-256 entries exist for the concrete artifacts that ARE verified.
**Exploit scenario:** A credential compromise or namespace takeover at Google Maven (unlikely but the kind of threat this file exists to stop) serves poisoned `com.google.android.material:material` or `androidx.compose.runtime:runtime` artifacts. Because the group is blanket-trusted, Gradle accepts the new artifact with no checksum comparison. Compare to a tight build where every artifact has a pinned SHA — the attacker would need a hash collision. A compromise in the `org.apache.*` or `commons-*` groups (both have historical CVEs — log4shell, commons-collections RCE) is the more realistic angle.
**Fix:**
1. Remove (or drastically narrow) the broad `<trust group="..." regex="true"/>` entries. Minimum cleanup: drop `org.apache.*`, `commons-*`, `org.codehaus.*` — those ecosystems have had historical compromises.
2. Regenerate per-artifact SHA pins: `./gradlew --write-verification-metadata sha256 help --refresh-dependencies`.
3. Optionally enable `<verify-signatures>true</verify-signatures>` and import keyring fingerprints via `./gradlew --write-verification-metadata sha256,pgp help` and manage `trusted-keys` in the same file.
**Remediation notes:** *(to be filled during triage)*

---

## Low Findings

### L-1: GitHub Actions pinned to mutable tags instead of commit SHAs

**Status:** OPEN
**File:** `.github/workflows/test.yml:16,18,23,36`
**Affected files:**
- `.github/workflows/test.yml`
**Severity:** Low
**Confidence:** HIGH
**Category:** Dependency chain abuse
**Standards:** CICD-SEC-3, CWE-1357
**Modules:** cicd
**Description:** All four `uses:` steps are pinned to floating major-version tags (`@v4`). GitHub tags are mutable — if a maintainer's account or action repository is compromised, `v4` can be force-pushed to a malicious commit and the next CI run executes it. First-party `actions/*` and `gradle/actions/*` are verified publishers (lower risk), but for a workflow that touches a Play-Store-bound codebase, SHA pinning is the industry-standard hardening.
**Evidence:**
```yaml
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
- uses: gradle/actions/setup-gradle@v4
- uses: actions/upload-artifact@v4
```
**Current controls:** Only vendor-published, verified actions are used; no marketplace long-tail; `permissions: contents: read` caps token blast radius.
**Exploit scenario:** Compromise of an action maintainer → malicious tag re-point → next CI run exfiltrates repo contents or injects code into the build via a tampered setup step.
**Fix:** Pin each `uses:` to a full commit SHA with the tag as a trailing comment (Dependabot keeps these up to date — see L-3):
```yaml
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2
- uses: actions/setup-java@8df1039502a15bceb9433410b1a100fbe190c53b # v4.5.0
- uses: gradle/actions/setup-gradle@<sha>                          # v4.x.x
- uses: actions/upload-artifact@<sha>                              # v4.x.x
```
Look up current SHAs at each action's GitHub releases page.
**Remediation notes:** *(to be filled during triage)*

---

### L-2: `actions/checkout` leaves GITHUB_TOKEN in git config (persist-credentials default)

**Status:** OPEN
**File:** `.github/workflows/test.yml:16`
**Affected files:**
- `.github/workflows/test.yml`
**Severity:** Low
**Confidence:** HIGH
**Category:** Pipeline-Based Access Controls / credential hygiene
**Standards:** CICD-SEC-5, CICD-SEC-6, CWE-522
**Modules:** cicd
**Description:** `actions/checkout@v4` defaults to `persist-credentials: true`, writing `GITHUB_TOKEN` into `.git/config` on the runner. For the remainder of the job — which includes `./gradlew assembleDebug`, executing arbitrary plugin code from the build graph — any step can read the token via `git config --get http.https://github.com/.extraheader`. Workflow-level `permissions: contents: read` limits blast radius (the token cannot push), but a malicious Gradle plugin could still read private-repo contents or make read-only API calls as the workflow.
**Evidence:**
```yaml
- uses: actions/checkout@v4
# persist-credentials defaults to true; no `with:` overrides
```
**Current controls:** `permissions: contents: read` at workflow level.
**Exploit scenario:** A malicious transitive Gradle plugin reads the persisted token and uses it to enumerate private-repo data (limited to read scope) or pivot via GitHub API side-channels.
**Fix:** No step in this workflow pushes to git, so disable credential persistence:
```yaml
- uses: actions/checkout@<sha>  # v4
  with:
    persist-credentials: false
```
**Remediation notes:** *(to be filled during triage)*

---

### L-3: No Dependabot / automated dependency updates

**Status:** OPEN
**File:** `.github/dependabot.yml` (missing)
**Affected files:**
- `.github/dependabot.yml` (new file)
**Severity:** Low
**Confidence:** HIGH
**Category:** Dependency chain abuse / CVE monitoring
**Standards:** CICD-SEC-3, OWASP A06:2025
**Modules:** cicd, dependencies
**Description:** No `dependabot.yml`, no Renovate config, no automated update workflow. Gradle libraries and GitHub Actions will drift; known-CVE versions are only caught when the developer remembers to upgrade. For a Play-Store-shipped app this is the primary supply-chain hygiene gap.
**Evidence:** `ls .github/dependabot.yml` → not found.
**Current controls:** None automated; `./gradlew lintDebug` runs but does not flag outdated deps.
**Exploit scenario:** A CVE lands in a Compose or AndroidX dependency; the app ships the vulnerable version to end users for months before a manual bump.
**Fix:** Create `.github/dependabot.yml`:
```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 5
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 5
```
Combines with L-1 fix — Dependabot will keep the SHA-pinned actions fresh.
**Remediation notes:** *(to be filled during triage)*

---

### L-4: `debugImplementation("androidx.compose.ui:ui-test-manifest")` mis-grouped with androidTest deps

**Status:** OPEN
**File:** `app/build.gradle.kts:75`
**Affected files:**
- `app/build.gradle.kts`
**Severity:** Low
**Confidence:** MEDIUM
**Category:** Dev-vs-prod scoping
**Standards:** OWASP A05:2025 (Security Misconfiguration), CWE-1269 (Product Released in Non-Release Configuration)
**Modules:** dependencies
**Description:** `debugImplementation("androidx.compose.ui:ui-tooling")` (line 64) and `debugImplementation("androidx.compose.ui:ui-test-manifest")` (line 75) are correctly scoped — release APKs do not contain them. However, line 75 sits in the `androidTestImplementation` block (lines 71-75), which is stylistically misleading and makes it easy for a future refactor to mis-scope it to `implementation`. Additionally, the `buildTypes { release { ... } }` block does not declare an explicit `debug` counterpart with `isDebuggable = false`, so the default `assembleDebug` is debuggable — fine for dev, but worth documenting so a debug APK is never shipped to users.
**Evidence:**
```kotlin
debugImplementation("androidx.compose.ui:ui-tooling")          // line 64
…
androidTestImplementation("androidx.compose.ui:ui-test-junit4") // line 74
debugImplementation("androidx.compose.ui:ui-test-manifest")     // line 75  — scoped correctly but visually grouped
```
**Current controls:** `isMinifyEnabled = true` and `isShrinkResources = true` on release strip dead code even if tooling were accidentally pulled in; ProGuard would likely break on tooling classes, providing a loud failure mode.
**Exploit scenario:** A rooted device with the debug APK could use Compose inspection hooks to introspect UI state or hook callbacks. Realistic only with physical/root access.
**Fix:** Re-group line 75 next to line 64:
```kotlin
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```
Consider adding a CI guard: `./gradlew :app:assembleRelease && unzip -l app/build/outputs/apk/release/*.apk | grep -i tooling` should return nothing.
**Remediation notes:** *(to be filled during triage)*

---

## Informational Findings

### I-1: CODEOWNERS does not specifically protect `.github/workflows/`

**Status:** OPEN
**File:** `CODEOWNERS:1-2`
**Affected files:**
- `CODEOWNERS`
**Severity:** Informational
**Confidence:** MEDIUM
**Category:** Flow control / governance
**Standards:** CICD-SEC-1, CICD-SEC-5
**Modules:** cicd
**Description:** `CODEOWNERS` assigns the whole repo to `@Drumm3r` via `*`. For a single-maintainer personal project this is correct; but if a collaborator is ever added, the CI pipeline becomes as editable as any other file — a supply-chain risk.
**Evidence:**
```
# Default owner for everything
* @Drumm3r
```
**Current controls:** Default wildcard ownership; solo maintainer.
**Fix (future-proofing):**
```
* @Drumm3r
/.github/        @Drumm3r
/CODEOWNERS      @Drumm3r
```
Combine with a GitHub branch-protection rule requiring CODEOWNERS review on protected branches.
**Remediation notes:** *(to be filled during triage)*

---

### I-2: Runner not version-pinned (`ubuntu-latest`)

**Status:** OPEN
**File:** `.github/workflows/test.yml:13`
**Affected files:**
- `.github/workflows/test.yml`
**Severity:** Informational
**Confidence:** MEDIUM
**Category:** Insecure system configuration
**Standards:** CICD-SEC-7
**Modules:** cicd
**Description:** `runs-on: ubuntu-latest` is a moving target (currently `ubuntu-24.04`, will roll to `ubuntu-26.04` per GitHub's cadence). Builds silently change toolchains when the label rolls, which can mask a supply-chain or reproducibility issue.
**Evidence:** `runs-on: ubuntu-latest`
**Current controls:** JDK pinned (Temurin 17); wrapper pins Gradle 9.3.1.
**Fix:** `runs-on: ubuntu-24.04` (bump explicitly when GitHub announces the next LTS).
**Remediation notes:** *(to be filled during triage)*

---

### I-3: No `concurrency:` block

**Status:** OPEN
**File:** `.github/workflows/test.yml`
**Affected files:**
- `.github/workflows/test.yml`
**Severity:** Informational
**Confidence:** HIGH
**Category:** Operational hygiene
**Standards:** CICD-SEC-1 (weak tie)
**Modules:** cicd
**Description:** Rapid successive pushes spawn concurrent workflow runs, wasting CI minutes. No security impact for a test-only workflow.
**Fix:** Add at workflow top:
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```
**Remediation notes:** *(to be filled during triage)*

---

### I-4: Pinned dependencies — ongoing CVE monitoring follow-up

**Status:** OPEN
**File:** `app/build.gradle.kts:46-75`
**Affected files:**
- `app/build.gradle.kts`
**Severity:** Informational
**Confidence:** MEDIUM
**Category:** CVE monitoring
**Standards:** OWASP A06:2025 (Vulnerable & Outdated Components)
**Modules:** dependencies
**Description:** Spot-review of the 17 direct dependencies against known-CVE corpora (knowledge cutoff Jan 2026):
- **AGP 9.1.0 / Kotlin 2.2.10** — leading edge, no public CVEs known at this pin.
- **Compose BOM 2025.03.00** — recent, no known CVEs in the bundled Compose artifact set.
- **androidx.datastore-preferences 1.1.4** — clean.
- **kotlinx-serialization-json 1.8.1** — clean; historic deserialization issues were untrusted-input-driven and this app only decodes local JSON (backup import is covered by backend SEC-3/SEC-9).
- **kotlinx-coroutines 1.10.1** — clean.
- **mockk 1.13.16** (test scope) — pulls `byte-buddy` transitively; no known exploitable issue; test-scope only.
- **JUnit 4.13.2** — CVE-2020-15250 (`TemporaryFolder`) does NOT apply (confirmed no `TemporaryFolder` usage under `app/src/test/` or `app/src/androidTest/`).
- **androidx.glance:glance-appwidget 1.1.1** — clean.

No HIGH/CRITICAL CVEs at pinned versions. Point-in-time review; CVE data for recent artifacts is sparse.
**Fix / follow-up:** Add Dependabot (see L-3), and/or run periodically:
```bash
./gradlew :app:dependencies > deps.txt  # full transitive tree for manual OSV review
# Or install the OWASP dependency-check plugin, or run trivy:
trivy fs --scanners vuln .
```
Schedule quarterly re-scans. Subscribe to `security@android.com` and Kotlin security advisories.
**Remediation notes:** *(to be filled during triage)*

---

## Prioritised Action List

Ordered by impact / effort.

### Before next Play Store release
1. **Pin Gradle wrapper SHA** (M-1) — 1-line fix closes the only non-theoretical supply-chain hole.
2. **Tighten `verification-metadata.xml`** (M-2) — drop `org.apache.*`/`commons-*`/`org.codehaus.*` blanket trusts first, then narrow the rest. Regenerate per-artifact SHA pins.
3. **Commit Gradle lockfiles** (prior SEC-8, not re-reported here) — `./gradlew dependencies --write-locks`.

### Hardening (can ship without, but enable incrementally)
4. **Enable Dependabot** (L-3) — one new YAML file. Automates L-1 and catches future dependency CVEs.
5. **SHA-pin GitHub Actions** (L-1) — paired with L-3 so Dependabot keeps SHAs fresh.
6. **`persist-credentials: false` on checkout** (L-2).
7. **Move `debugImplementation` lines together** (L-4) + add CI grep guard against `ui-tooling` in release APK.

### Defence in depth
8. Pin `runs-on: ubuntu-24.04` (I-2).
9. Add `concurrency:` block (I-3).
10. Restrict CODEOWNERS to `.github/` (I-1) if ever adding collaborators.

---

## Methodology

- Three parallel sub-agents: secrets (deep grep + git history), dependencies (Gradle manifests + verification metadata), cicd (GitHub Actions workflow + CODEOWNERS + wrapper).
- Findings from the parent `/audit-backend` security module (SEC-1..SEC-11) were cross-referenced but not re-reported.
- `--include-low` flag NOT used; only MEDIUM and HIGH confidence findings included in the finding body. One Informational (I-4) retained because it documents a proactive monitoring action rather than a current risk.
- Secrets agent ran grep + git-history scans; found zero issues.

## Finding Format Reference

Each finding uses:

- **Status**: OPEN (default)
- **File**: Primary source file + line
- **Affected files**: All files touched by remediation
- **Severity** / **Confidence** / **Category** / **Standards** / **Modules**
- **Description** / **Evidence** / **Current controls** / **Exploit scenario** / **Fix**
- **Remediation notes**: Blank on creation; filled during triage.

### Status values

| Status | Meaning |
|--------|---------|
| OPEN | Not yet triaged or remediation not started |
| IN PROGRESS | Remediation underway — see remediation notes |
| RESOLVED | Fix implemented and deployed |
| ACCEPTED RISK | Risk acknowledged, no fix planned — see remediation notes for rationale |

When updating during triage: strikethrough the title and append a status badge (e.g. `### M-1: ~~Original Title~~ **RESOLVED**`), update `**Status:**` with date + brief description, fill in `**Remediation notes:**`.
