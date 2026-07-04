# Authentication Notes

MoneyMate uses Firebase Authentication for:

- Email/password sign-in.
- Google sign-in through a Google ID token.

The login screen currently uses the legacy Google Sign-In API and suppresses the deprecation warning locally in `LoginScreen.kt`.

Reason:

- Credential Manager was tested, but the current Firebase/OAuth configuration returned `DEVELOPER_ERROR` on the test device.
- The legacy Google Sign-In flow works with the existing OAuth client and keeps the demo stable.

The Google Web Client ID is not hardcoded in the screen. It is read from:

```kotlin
R.string.default_web_client_id
```

That value is generated from `app/google-services.json` by the Google Services Gradle plugin.

Future migration path:

- Verify SHA-1 and SHA-256 fingerprints in Firebase Console.
- Re-test Credential Manager with the same web client ID.
- Remove the legacy Google Sign-In dependency after Credential Manager sign-in is stable.
