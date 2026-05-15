# Contributing

Thanks for considering a contribution!

## Workflow

1. Fork or branch from `develop`.
2. Branch naming: `feat/short-name`, `fix/short-name`, `chore/short-name`.
3. Commits follow [Conventional Commits](https://www.conventionalcommits.org/).
4. Open a PR against `develop`. CI must be green before merge.
5. `develop` is auto-PRed into `main` by [release-please](https://github.com/googleapis/release-please) for releases.

## Code style

- `./gradlew ktlintFormat` before each commit.
- `./gradlew detekt` to flag complexity / style issues.
- New `commonMain` code should ship with a `commonTest` test.
- iOS Swift code: run `swiftformat .` from `iosApp/` (config in `iosApp/.swiftformat`).

## What goes where

| Type of change                       | Edit                                                |
|--------------------------------------|-----------------------------------------------------|
| New domain entity                    | `shared/.../domain/model/`                          |
| Storage implementation               | `shared/.../data/firebase/`                         |
| New screen logic                     | `shared/.../presentation/`                          |
| Compose UI shared by Android + Web   | `composeApp/.../commonMain/ui/`                     |
| Android-only behavior                | `composeApp/.../androidMain/`                       |
| Web-only behavior (entry, JS interop)| `composeApp/.../wasmJsMain/`                        |
| iOS UI                               | `iosApp/iosApp/`                                    |

If a "shared" change requires touching more than one of the UI hosts, mention that in the PR description so reviewers know to check all three.
