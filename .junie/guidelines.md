# Home Library Development Guidelines

This document provides project-specific information and guidelines for developers working on the Home Library Kotlin Multiplatform project.

## 1. Build and Configuration

The project targets Android and iOS. The Web target (Wasm) is currently disabled due to lack of Firebase Wasm support in the GitLive library.

### Prerequisites
- **JDK 17** (Temurin recommended)
- **Android Studio Ladybug+**
- **Xcode 15+** (for iOS)
- **Node 20+** and **Firebase CLI** (`npm install -g firebase-tools`)
- **xcodegen** (`brew install xcodegen`)

### Local Setup
1. Clone the repository.
2. Initialize Gradle wrapper: `gradle wrapper`.
3. Copy `local.properties.example` to `local.properties` and set `sdk.dir`.
4. **Firebase Configs**: Obtain `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) from the Firebase Console and place them in:
   - `composeApp/google-services.json`
   - `iosApp/iosApp/GoogleService-Info.plist`
5. **Database Rules**: Deploy the rules found in `shared/src/commonMain/kotlin/com/majchrosoft/homelibrary/data/firebase/database.rules.json` using `firebase deploy --only database`.

See `docs/RUN_LOCAL.md` for more detailed platform-specific instructions.

## 2. Testing

The project uses `kotlin.test` for unit testing. Shared logic tests are located in `shared/src/commonTest`.

### Running Tests
To run all unit tests in the shared module (JVM target is fastest for development):
```bash
./gradlew :shared:jvmTest
```

### Adding New Tests
1. Create a test class in `shared/src/commonTest/kotlin/...`.
2. Use `@Test` annotation from `kotlin.test`.
3. Follow the existing package structure that mirrors `commonMain`.

**Example Test:**
```kotlin
package com.majchrosoft.homelibrary.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleItemTest {
    @Test
    fun testItemCreation() {
        val item = Item(id = "test-id", ownerId = "owner-1")
        assertEquals("test-id", item.id)
    }
}
```

## 3. Development Information

### Architecture
The project follows **Clean Architecture** principles.
- **Domain**: Located in `shared/src/commonMain/.../domain`. Contains models, repository interfaces, and use cases.
- **Data**: Located in `shared/src/commonMain/.../data`. Contains Firebase implementations using the GitLive Firebase SDK.
- **Presentation**: Located in `shared/src/commonMain/.../presentation`. Contains ViewModels using an MVI-like pattern with `StateFlow`.

### Data Models and Legacy Compatibility
- **Item vs Book**: The `Book` model is deprecated and replaced by `Item` to match the Firebase schema used by the legacy Angular app.
- **Schema**: The `Item` model has a nested structure (`item` for details, `borrow` for state) to avoid data migrations.

### Code Quality
- **Formatting**: Use `./gradlew ktlintFormat` to fix formatting.
- **Static Analysis**: Use `./gradlew detekt` to check for code smells.
- **Commits**: Follow [Conventional Commits](https://www.conventionalcommits.org/). Tagging `vX.Y.Z` on `main` triggers automated releases.

## 4. Agent Constraints

To ensure security and prevent accidental exposure of secrets:
- **Prohibited Files**: The agent is strictly prohibited from reading or writing the following files:
    - `.env`
