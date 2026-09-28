# AM MUT Extractor

Tiny **Android phone** app that logs into Apple Music in a WebView and reads back your
**media-user-token (MUT)**.

## What it's for — Apple Music TV

This is a companion tool for **Apple Music TV**, a native Android TV / Fire TV Apple Music client.
That app needs your Apple Music **media-user-token** to stream full tracks, load lyrics, and read your
library. It normally takes the token via its on-device `:8080` page (open `http://<FireTV-IP>:8080`
on a phone and paste the token in).

The catch: getting the token means signing into Apple with your Apple ID + password, and typing that
**on a Fire TV is broken** — the on-screen keyboard mangles WebView password fields (first char
doubled, backspace broken, passwords barely register). This app sidesteps that: run it **on your
phone**, where the keyboard works, grab the token, and paste it onto the TV.

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
