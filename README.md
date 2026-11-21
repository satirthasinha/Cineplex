# CinePlex TV — Android TV APK project

This is an Android Studio project for a TV-friendly CinePlex BD app shell that loads the existing CinePlex portal at **http://103.136.200.66/** in an Android WebView.

## What this first build does

- Installs as **CinePlex TV** and appears in Android TV / Google TV launchers.
- Opens the CinePlex portal directly; it uses the portal's own movie, TV-series, category, search and player pages.
- Enables JavaScript, cookies and DOM storage required by the portal.
- Supports remote-control keyboard/D-pad focus styling, browser Back navigation and fullscreen HTML5 video views.
- Keeps the app landscape and allows the server's HTTP content (the supplied portal URL is HTTP).

The supplied homepage source references server routes such as `category.php`, `tcategory.php`, `view.php`, `player.php`, `watch.php`, `tview.php`, `search_ajax.php` and `load_more_movies.php`, plus poster files under `/uploads/`. This project intentionally reuses those server pages instead of inventing a separate API.

## Important scope / limitations

This is a **WebView-based first APK**, not yet the fully native Kotlin/Compose + Media3 + SQLite crawler described in the DhakaFlix guide. The uploaded HTML is the portal homepage source; it does not include the PHP implementation of detail/player pages or the actual media-stream response. A WebView wrapper is therefore the buildable approach supported by the supplied source. Actual playback depends on the CinePlex server serving pages and video formats that the Android System WebView can play. Some codecs, DRM, embedded third-party players, or server-side checks may require changes after testing on your TV.

Use only content and services you are authorized to access through your ISP/server. The app does not bypass login, access controls, DRM, or network restrictions.

## Build the APK with Android Studio

1. Install the latest stable **Android Studio** on Windows, macOS or Linux.
2. Open Android Studio → **Open** → select this `CinePlexTV` folder (the folder containing `settings.gradle.kts`).
3. Allow Gradle sync. If prompted, install **Android SDK Platform 35** and the Android SDK Build-Tools requested by Android Studio.
4. For a debug APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Android Studio will show a notification with **locate**. The APK is normally at:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Copy the APK to your Android TV and install it (for example with `adb install -r app-debug.apk`, or use a trusted sideloading app).

To make a release APK, choose **Build → Generate Signed Bundle / APK → APK** and create/use your own signing key. Do not distribute a debug-signed APK as a production release.

## Requirements

- Android Studio with JDK 17 (Android Studio's bundled JBR is suitable).
- Android SDK Platform 35.
- Internet/ISP connectivity to `103.136.200.66` from the TV. Some ISP media servers are reachable only on the ISP's own network.
- Android TV / Google TV with Android 6.0+ (API 23+); Android 11+ is the preferred test target.

## Configuration

The start URL and host are constants in:
`app/src/main/java/net/cineplexbd/tv/MainActivity.kt`

Change `HOME_URL` if the portal address changes. The app requests the site over HTTP because the supplied server address is HTTP; avoid entering passwords or sensitive information over an untrusted network.

## Troubleshooting

- **Blank page / cannot connect:** open `http://103.136.200.66/` in the TV browser on the same network. If it does not load there, the app cannot reach the server either.
- **Remote cannot select items:** test the page in the TV's browser first; some portal controls may be mouse/hover-oriented. The app adds visible focus styling, but the portal's own JavaScript/CSS may need further TV-specific adaptation.
- **Video opens but does not play:** test the same item in the TV browser. This wrapper cannot add missing codecs or bypass server restrictions. The server's player page and media delivery need to be inspected to implement a native Media3 player.
- **External download link:** the app hands it to an installed Android app when possible.

## Project structure

```text
CinePlexTV/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/net/cineplexbd/tv/MainActivity.kt
        └── res/
            ├── drawable/ic_cineplex.xml
            ├── drawable/tv_banner.xml
            └── values/styles.xml
```
