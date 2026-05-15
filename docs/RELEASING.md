# Releasing

Releases are triggered by **tags of the form `vX.Y.Z`** on `main`. The release-please workflow opens a "release PR" that bumps the version and updates `CHANGELOG.md` from [Conventional Commits](https://www.conventionalcommits.org/). Merging that PR creates the tag, which fans out to three workflows:

```
v1.4.0 tag
    │
    ├─► release-android.yml  → AAB → Play Store (internal track by default)
    ├─► release-ios.yml      → IPA → TestFlight
    └─► release-web.yml      → Wasm bundle → Firebase Hosting (live channel)
```

## Conventional commits cheatsheet

| Commit                            | Result                       |
|-----------------------------------|------------------------------|
| `feat: add loan request screen`   | Minor bump                   |
| `fix: handle empty author list`   | Patch bump                   |
| `feat!: rebuild auth API`         | Major bump (breaking)        |
| `chore:`, `ci:`, `build:`         | No release (hidden in changelog) |

## Required GitHub secrets

Configured under **Settings → Secrets and variables → Actions**, scoped to environments `play-store`, `app-store`, and `web`.

### `play-store`
| Secret                                  | Format                       |
|-----------------------------------------|------------------------------|
| `ANDROID_GOOGLE_SERVICES_JSON_BASE64`   | `base64` of the file         |
| `ANDROID_KEYSTORE_BASE64`               | `base64` of the upload .jks  |
| `ANDROID_KEYSTORE_PASSWORD`             | Plain                        |
| `ANDROID_KEY_ALIAS`                     | Plain                        |
| `ANDROID_KEY_PASSWORD`                  | Plain                        |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON`       | Plain JSON of the SA key     |

### `app-store`
| Secret                                  | Format                       |
|-----------------------------------------|------------------------------|
| `IOS_GOOGLE_SERVICES_PLIST_BASE64`      | `base64` of the .plist       |
| `APP_STORE_CONNECT_KEY_ID`              | Plain                        |
| `APP_STORE_CONNECT_ISSUER_ID`           | Plain                        |
| `APP_STORE_CONNECT_KEY_CONTENT_BASE64`  | `base64` of the `.p8` file   |
| `APP_STORE_CONNECT_TEAM_ID`             | Plain                        |
| `APPLE_DEVELOPER_TEAM_ID`               | Plain                        |
| `MATCH_PASSWORD`                        | Plain                        |
| `MATCH_GIT_URL`                         | Plain                        |
| `MATCH_GIT_BASIC_AUTHORIZATION`         | `base64("user:token")`       |

### `web`
| Secret                                  | Format                       |
|-----------------------------------------|------------------------------|
| `FIREBASE_WEB_CONFIG_JS`                | The JS body (see firebase-config.example.js) |
| `FIREBASE_SERVICE_ACCOUNT_JSON`         | Plain JSON of the SA key     |

## Cutting a release manually

```bash
# Verify clean main
git checkout main && git pull
./gradlew clean check                 # local sanity

# Either: let release-please open the version-bump PR (preferred)
# Or: tag manually and push (will skip the PR step)
git tag -a v0.2.0 -m "Loan flow GA"
git push origin v0.2.0
```

The Play Store release defaults to the `internal` track. To promote, run the
`release-android.yml` workflow with `workflow_dispatch` and pick the desired
track (`alpha`/`beta`/`production`).

## Promoting an iOS build

`release-ios.yml` ships every release tag to TestFlight automatically. Promote
to App Store Connect production from the App Store Connect UI when ready.
