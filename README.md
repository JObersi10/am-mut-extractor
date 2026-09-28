# AM MUT Extractor

Tiny **Android phone** app that logs into Apple Music in a WebView and reads back your
**media-user-token (MUT)** so you can paste it into [Apple Music TV](../) at its `:8080` token page.

Why: typing an Apple ID + password into a WebView on a Fire TV is broken (the on-screen keyboard
mangles password fields). On a phone the keyboard works fine, so grab the token here and paste it
onto the TV.

## Use
1. Install the APK on your phone.
2. Open it, sign in with your Apple ID (2FA and all).
3. When it captures the token it shows it with a **Copy token** button.
4. On the Fire TV, open `http://<FireTV-IP>:8080` (or the in-app token page) and paste it.

The token is only shown on-screen and copied to your clipboard — nothing is sent anywhere.

## Build
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```
`local.properties` must have `sdk.dir=<your Android SDK path>`.

The release build is debug-signed so it installs without a keystore — it's a personal tool.
