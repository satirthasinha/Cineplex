# CinePlex TV — Apple TV-inspired Android TV interface

This revision replaces the full-screen WebView wrapper with a native Android View-based TV shell inspired by Apple's dark, cinematic TV interface.

## What is native in this revision

- Dark cinematic home screen with a featured-content hero.
- Left navigation rail: Home, Movies, TV Series, Search, My List, Recently Watched and Settings.
- Remote-friendly focusable navigation and selectable poster cards.
- Local search over titles discovered from the live CinePlex homepage.
- Poster artwork is loaded from image URLs discovered in the portal's HTML.
- The original CinePlex site remains the content source. Selecting a title opens its existing server page inside an embedded WebView.

## Content-source limitations

The project source supplied for this work contains the homepage routes and references to `view.php`, `watch.php`, `tview.php`, `category.php`, `tcategory.php`, `search_ajax.php`, `load_more_movies.php` and `/uploads/` posters. It does not include the PHP backend, a documented JSON API, or actual media stream URLs.

Therefore:
- The app discovers available title links and artwork from the homepage HTML.
- Movie/series details and playback still use the server's existing pages.
- Native Media3 playback is not enabled because the actual stream URL and format cannot be verified from the supplied project.
- My List and Recently Watched are currently interface placeholders, not persistent features.
- Category links open the server's category pages; if the server requires category IDs, those IDs need to be inspected from its live HTML/backend.

The app does not bypass authentication, DRM, access controls, or network restrictions. Use only content you are authorized to access.

## Build with GitHub Actions

1. Push this project to GitHub.
2. The workflow at `.github/workflows/release-apk.yml` builds a debug APK on pushes to `main`/`master`.
3. Download the `CinePlexTV-APK` artifact from the workflow run.
4. To publish a GitHub Release, push a version tag:

```bash
git tag v2.0.0
git push origin v2.0.0
```

The release APK is debug-signed for testing. Configure a private signing keystore and GitHub Secrets before distributing a production APK.

## Build locally

Open the folder containing `settings.gradle.kts` in Android Studio and build the `app` debug variant. The expected APK path is:

`app/build/outputs/apk/debug/app-debug.apk`

## Server

The default content source is `http://103.136.200.66/`. This is plain HTTP; avoid sending credentials or sensitive information over an untrusted network.
