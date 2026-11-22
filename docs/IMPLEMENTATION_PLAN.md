# Implementation and Release Gates

## Current baseline
Android Gradle project, Compose UI, LLM connector, Keystore-backed API-key encryption, plugin approval/rollback, constrained web-search operation, memory notes, audit log, resource budgets, unit test and GitHub Actions workflow have been added.

## Mandatory app-update compatibility policy
Every published SOMCHAI AI update MUST install over the previous installed version without uninstalling it.

- Keep `applicationId = "com.somchai.ai"` unchanged for the lifetime of this app.
- Sign every installable production/release APK with the exact same owner-controlled Android signing keystore and certificate. Never generate a replacement key for an update.
- Keep the signing keystore and passwords out of Git; store them in a protected owner environment or GitHub Actions secrets.
- Configure the following GitHub Actions secrets to produce an owner-signed release APK: `SOMCHAI_SIGNING_KEYSTORE_BASE64`, `SOMCHAI_SIGNING_STORE_PASSWORD`, `SOMCHAI_SIGNING_KEY_ALIAS`, and `SOMCHAI_SIGNING_KEY_PASSWORD`.
- Increase `versionCode` for every published update. Never reset or reuse a version code; update `versionName` for the human-readable release.
- Preserve app-private user data and settings during normal upgrades. Any data-schema change must use a tested, backward-safe migration.
- Before publishing, verify the new APK's application ID, version code, and signing-certificate SHA-256 against the installed/released baseline, then test installing it over the previous signed build on an Android device.
- A signature or application-ID mismatch is a release blocker. Never instruct users to uninstall the existing app or clear its data as an update workaround.
- Debug APKs are for development only. Their debug signing identity is not the production update identity; use the owner-signed release APK for the user's normal installation and future updates.

## Release signing setup
The release build reads the signing values from environment variables. The GitHub workflow only creates an owner-signed release APK when all four signing secrets are configured. If they are absent, CI still builds and tests the debug APK but explicitly warns that it did not produce an update-capable release APK.

## Release gates still open
- Configure and securely preserve the owner signing keystore and GitHub Actions secrets.
- Verify CI produces an owner-signed release APK and record its signing-certificate SHA-256 fingerprint.
- Test an in-place upgrade over the previous owner-signed APK on the target Android 16 device, confirming user data and settings remain intact.
- Run CI and resolve any build/test errors.
- Install and test on the target Android 16 device.
- Verify multiple OpenAI-compatible providers and error handling.
- Add encrypted backup/export and tested restore.
- Add tamper-evident audit chain and robust log rotation.
- Add richer search provider integration and source citations.
- Add owner-controlled memory edit/delete/export.
- Add privacy disclosure, accessibility and penetration testing.

Do not call the app production-certified until all gates have evidence.
