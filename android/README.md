# MeetSpot Android

Native Android client for MeetSpot — a Kotlin + Jetpack Compose port of the
web app (`public/index.html` / `public/app.js`) with the same functionality:
a two-person quick search, Firebase-backed group rooms with realtime sync and
an AI (Gemini) group pick, and a profile with Google sign-in and on-device
Google Timeline processing. It talks to the same backend
(`/api/recommend`, `/api/meetings/*`) and the same Firebase project
(`meetspot-production`) as the web app — no backend or Firestore rule changes
were made.

## Requirements

- Android Studio (Koala+) or the command line with `ANDROID_HOME` set
- Android SDK Platform 34, Build-Tools 34.0.0
- JDK 17

## One-time setup: Firebase

The app needs its own registration in the `meetspot-production` Firebase
project before Google sign-in, Firestore-backed group rooms, or profiles will
work at runtime (everything else — the "Find a spot" two-person search — works
without this, since it only talks to the backend API).

1. In the [Firebase console](https://console.firebase.google.com/), open the
   `meetspot-production` project → Project settings → Add app → Android.
2. Package name: `com.meetspot.app`.
3. Debug SHA-1 (from the default debug keystore): run
   `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   and paste the `SHA1:` value. Add your release keystore's SHA-1 too before
   shipping a release build.
4. Download the generated `google-services.json` and save it as
   `app/google-services.json` (this path is gitignored — it carries live
   project credentials; `app/google-services.json.example` shows the shape).
5. Confirm Google is enabled as a sign-in provider under Authentication →
   Sign-in method (it already is for the web app, so this is likely already done).

Until you do this, the project builds and its unit tests pass (they use a
placeholder `google-services.json` with fake credentials so the Google
Services Gradle plugin has something to parse), but sign-in/Firestore calls
will fail at runtime with an authentication error.

## Build & test

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run unit tests (JVM, via Robolectric where needed)
./gradlew lintDebug            # static analysis
```

## Architecture

- **UI**: Jetpack Compose screens under `ui/screens` (`Find`, `Group`, `Room`,
  `Profile`), mirroring the three tabs plus the `/r/:id` room route in
  public/index.html, and shared components under `ui/components`
  (`ResultsPanel`, `DateTimeField`, `AppScaffold`).
- **State**: one `ViewModel` per screen, each exposing a single
  `StateFlow<UiState>`.
- **Backend networking**: Retrofit + OkHttp + kotlinx.serialization
  (`data/remote/ApiService.kt`), mirroring the `fetch` calls in
  public/app.js — `/api/recommend` and the legacy `/api/meetings/:id`
  invite endpoints.
- **Firebase**: the official Android SDKs (`firebase-auth`, `firebase-firestore`),
  not a REST wrapper — this is the same approach the web app takes with the
  Firebase JS SDK, just the Android equivalent. `data/repository/AuthRepository.kt`
  wraps sign-in (via Credential Manager, the current replacement for the
  deprecated `GoogleSignInClient`); `GroupRoomRepository`/`ProfileRepository`
  wrap the `meetings`/`users` collections.
- **Firestore document shape**: since Kotlin data classes aren't POJOs
  Firestore can map automatically without reflection setup, documents are
  read/written as plain `Map<String, Any?>`, with explicit mapper functions in
  `data/model/MeetingRoom.kt`. A `RecommendResponse` (a typed Kotlin class) is
  converted to/from that shape via `data/FirestoreJson.kt`, which round-trips
  through kotlinx.serialization's `JsonElement`.
- **Timeline processing**: `data/TimelineProcessor.kt` is a straight Kotlin
  port of `processTimeline` in public/app.js — parses a Google Timeline JSON
  export entirely on-device and produces only a compact summary (visit/activity
  counts, top places) for Firestore. The raw file is never uploaded, matching
  the web app's stated privacy behavior.
- **Dependency injection**: hand-rolled (`data/AppContainer.kt`) — the app is
  small enough that Hilt/Dagger would add ceremony without real benefit.

### Why there's no App Links / deep-link auto-open for `/r/:id`

The manifest declares a non-autoVerified `VIEW` intent-filter for the
production host, so the app is offered as a disambiguation option when a room
link is opened, but isn't set as the automatic handler. Making it automatic
requires Android App Link verification (a `.well-known/assetlinks.json` file
hosted on the production domain, keyed to this app's signing certificate) —
an infra change outside this app's scope. `MainActivity` still handles the
link correctly if the user picks it from the disambiguation sheet.

## Tests

- `data/` — `TimelineProcessor`, `FirestoreJson`, and the `MeetingRoom`/
  `Participant` Firestore-map mappers (pure JVM, no emulator needed), plus
  `RecommendRepository` against MockWebServer (exercising the real Retrofit/
  kotlinx.serialization wiring, including error-body parsing).
- `ui/screens/` — one test class per `ViewModel`, using in-memory fake
  repositories (`FakeRecommendRepository`, `FakeGroupRoomRepository`,
  `FakeProfileRepository`, `FakeAuthRepository`) and Mockito for the
  `FirebaseUser` stand-in, covering the business rules that matter most:
  already-joined / room-full checks, the "N more participants" waiting
  message, and combined-preference computation when the group room fills up.

Firebase Auth/Firestore themselves aren't exercised against a live backend in
these tests (that would need the Firebase emulator suite or real project
credentials) — the repository interfaces exist specifically so the ViewModels
that use them can be tested without one.
