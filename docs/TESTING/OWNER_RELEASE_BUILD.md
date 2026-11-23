# Owner-only Release APK build and in-place update

This procedure keeps the existing **com.somchai.ai** identity and owner signing certificate. Never send the keystore or any password to chat, and never commit them.

## 1. Protect the original signing material

- Use the exact keystore and key alias that signed the currently installed production app. Do not create a replacement key.
- Keep the keystore outside the Git working tree, for example:
  - Linux/macOS: /secure/somchai/current-owner-release.jks
  - Windows: D:\AndroidSigning\current-owner-release.jks
- The Gradle signing config accepts environment variables:
  - SOMCHAI_SIGNING_KEYSTORE — absolute path to the existing owner keystore
  - SOMCHAI_SIGNING_STORE_PASSWORD
  - SOMCHAI_SIGNING_KEY_ALIAS
  - SOMCHAI_SIGNING_KEY_PASSWORD
- Keep shell history, terminal recordings, CI logs and shared screenshots free of passwords.

## 2. Set signing variables locally

Linux/macOS (in the repository root, current terminal only):

~~~bash
export SOMCHAI_SIGNING_KEYSTORE="/secure/somchai/current-owner-release.jks"
read -s -p "Keystore password: " SOMCHAI_SIGNING_STORE_PASSWORD; echo
export SOMCHAI_SIGNING_STORE_PASSWORD
read -p "Key alias: " SOMCHAI_SIGNING_KEY_ALIAS
read -s -p "Key password: " SOMCHAI_SIGNING_KEY_PASSWORD; echo
export SOMCHAI_SIGNING_KEY_PASSWORD
~~~

Windows PowerShell: set the same four environment variables in the current session only. Do not save them in scripts or tracked files. Use a secure local password prompt/helper that supplies the actual password text to Gradle; do not pass a serialized SecureString as the Gradle password.

## 3. Check versionCode before building

The current checked-in configuration is applicationId **com.somchai.ai**, versionCode **10**, versionName **1.9.0**. Compare it with the installed app first:

~~~bash
adb shell dumpsys package com.somchai.ai
~~~

The release versionCode must be strictly greater than the installed app's versionCode. If it is not, increment only version metadata in app/build.gradle.kts; do not change applicationId or signing config. Record the old/new values in the test log.

## 4. Build the owner-signed release APK

From repository root:

~~~bash
./gradlew :app:assembleRelease
~~~

Expected output: app/build/outputs/apk/release/app-release.apk

Do not use the debug APK for updating production: debug has the .debug applicationId suffix and a different signing identity.

## 5. Verify package, version and signing certificate BEFORE installing

With Android SDK Build Tools installed:

~~~bash
apkanalyzer manifest application-id app/build/outputs/apk/release/app-release.apk
apkanalyzer manifest version-code app/build/outputs/apk/release/app-release.apk
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
~~~

Required:
- Application ID output is exactly com.somchai.ai.
- Version code is strictly greater than the installed version code.
- SHA-256 certificate digest exactly matches the digest from the currently installed app.

To extract the installed APK without uninstalling:

~~~bash
adb shell pm path com.somchai.ai
adb pull /data/app/<returned-package-path>/base.apk installed-before-update.apk
apksigner verify --print-certs installed-before-update.apk
~~~

Compare the SHA-256 certificate digest printed for installed-before-update.apk with the new release APK. If they differ, STOP. Do not uninstall, clear app data, or attempt to bypass Android's signature check.

## 6. Install as an in-place update — preserve app data

Only after all checks above match:

~~~bash
adb install -r app/build/outputs/apk/release/app-release.apk
~~~

The -r option requests replacement while retaining app data. Do not run adb uninstall, pm clear, factory reset, or any data-clearing command. If installation reports INSTALL_FAILED_UPDATE_INCOMPATIBLE, stop: the signing certificate does not match. Do not remove the installed app to force installation.

After installation, confirm package/version and open the app. Verify old chats/memory and other existing app data remain present before running the Phase 1 device checklist.

## Safety stop conditions

- Keystore unavailable, password/alias uncertain, or certificate mismatch: stop and locate the original signing material locally.
- Never generate a new keystore as a workaround.
- Never upload the keystore or passwords to GitHub, chat, issue comments, or build artifacts.
- Never uninstall the existing app or clear its data to make an update succeed.
