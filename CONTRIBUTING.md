# Contributing to Office Break

Thanks for your interest in contributing! This guide explains how to get
involved.

## Getting Started

1. **Fork** the repository and clone your fork
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature main
   ```
3. Build and run the project:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Development Setup

- **JDK**: 17 (Temurin recommended)
- **Android SDK**: API 36 (compile), API 28 (min)
- **Kotlin**: 2.1
- **IDE**: Android Studio Ladybug or later

## Making Changes

### Code Style

- Follow standard Kotlin conventions
- Use Jetpack Compose for all new UI
- Use `stringResource()` for all user-facing text — no hardcoded strings
- Add `contentDescription` to icons for accessibility

### Commit Messages

Use the format: `IMP // Description of change`

Examples:
- `IMP // Add dark mode toggle to settings`
- `IMP // Fix timer not restarting after exercise confirmation`

### Testing

Run tests before submitting:

```bash
./gradlew testDebugUnitTest
```

New features should include unit tests where practical. Tests live in
`app/src/test/`.

## Submitting a Pull Request

1. Push your branch to your fork
2. Open a PR against `main`
3. Fill out the PR template
4. Ensure CI passes (build + tests + lint)
5. Wait for review

### PR Guidelines

- Keep PRs focused — one feature or fix per PR
- Update `README.md` if you add a user-facing feature
- Add screenshots for UI changes
- Link related issues (e.g., `Closes #42`)

## Reporting Bugs

Use the [Bug Report](https://github.com/Drumm3r/office-break/issues/new?template=bug_report.yml) issue template. Include:

- Device model and Android version
- Steps to reproduce
- Expected vs. actual behavior
- Screenshots or screen recordings if applicable

## Requesting Features

Use the [Feature Request](https://github.com/Drumm3r/office-break/issues/new?template=feature_request.yml) issue template. Describe:

- The problem you want to solve
- Your proposed solution
- Any alternatives you considered

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).
By participating, you agree to uphold it.

## License

By contributing, you agree that your contributions will be licensed under the
[GNU General Public License v3.0](LICENSE.md).

## Questions?

Open a [discussion](https://github.com/Drumm3r/office-break/issues) or reach
out via the issue tracker.
