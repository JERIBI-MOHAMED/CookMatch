# CookMatch (Java)

Starter Android project in Java for the mini-project requirements.

## Implemented now
- Java-only project structure (no Kotlin sources)
- Firebase Auth integration (email/password login + register)
- Navigation Component with:
  - SplashActivity -> AuthActivity/MainActivity routing
  - Drawer navigation
  - Bottom navigation
- 10 screens (fragments) scaffolded
- MVVM starter:
  - `UiState` class for Loading/Success/Error/Empty
  - `HomeViewModel` + `HomeFragment`
  - RecyclerView recipe list
- Data layer starter:
  - Retrofit service interface
  - Room entities/DAO/database placeholders

## Project setup
1. Open project in Android Studio.
2. Add your `google-services.json` file to `app/google-services.json`.
3. In Firebase Console, enable Authentication -> Email/Password.
4. Sync Gradle.
5. Run app.

## Firebase setup (step-by-step)
1. Create a Firebase project:
   - Go to Firebase Console.
   - Click Create project and finish the wizard.
2. Add Android app inside Firebase:
   - Package name must be `com.cookmatch.app`.
   - Download `google-services.json`.
   - Place it in `app/google-services.json`.
3. Enable Authentication:
   - Firebase Console -> Build -> Authentication -> Sign-in method.
   - Enable Email/Password.
4. Enable Firestore:
   - Firebase Console -> Build -> Firestore Database -> Create database.
   - Start in test mode for development, then lock rules later.
5. Confirm Gradle is configured:
   - Root plugin `com.google.gms.google-services` is already declared.
   - App plugin `com.google.gms.google-services` is already applied.
   - Firebase BOM + auth/firestore/messaging dependencies are already present.
6. Sync and run:
   - Click Sync Project with Gradle Files.
   - Run app on emulator/device.

## How auth works in this project
- Login screen calls Firebase sign-in:
  - `app/src/main/java/com/cookmatch/app/ui/auth/LoginFragment.java`
- Register screen creates account with Firebase:
  - `app/src/main/java/com/cookmatch/app/ui/auth/RegisterFragment.java`
- Splash checks session and routes user:
  - `app/src/main/java/com/cookmatch/app/ui/splash/SplashActivity.java`

## Minimal Firestore rules for development
Use these temporary rules only while building:

```txt
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Quick verification checklist
1. Register a new account from app.
2. Sign out from drawer, then login again.
3. Close and reopen app: Splash should go directly to main if session exists.
4. In Firebase Console, confirm user appears in Authentication users list.

## Common issues
- SHA-1 missing:
  - Usually only needed for Google Sign-In, not Email/Password.
- App keeps returning to auth:
  - Verify `google-services.json` matches package `com.cookmatch.app`.
- FIREBASE_API_KEY invalid:
  - Re-download `google-services.json` from correct Firebase app entry.
- Firestore permission denied:
  - Check Firestore rules and ensure user is authenticated before writes.

## Next implementation steps
1. Replace mock recipe loading with Retrofit TheMealDB call.
2. Persist favorites and profile data in Firestore.
3. Add Room cache fallback in repository.
4. Add proper loading/error/empty views on remaining screens.
5. Add Lottie splash and MotionLayout transition home->detail.
