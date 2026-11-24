# 🎤 HudKaraoke

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**HudKaraoke** is a lightweight, native Android karaoke and Head-Up Display (HUD) companion app for Spotify. It delivers millisecond-synced lyrics in a permanently immersive, distraction-free interface with a built-in mirror projection mode.

---

## 📱 Screenshots

| Standard Mode | HUD Mirror Mode |
| :---: | :---: |
| ![Standard Mode](docs/screenshot_normal.png) | ![HUD Mirror Mode](docs/screenshot_hud.png) |

*(Replace the image paths above with your actual screenshots or GIFs)*

---

## ✨ Features

* **Real-Time Synced Lyrics:** Fetches and synchronizes lyrics with your active Spotify playback using LRCLIB and the Spotify Web API.
* **HUD Mirror Mode:** Flips the display horizontally for clean reflection on windshields or dedicated HUD setups.
* **Permanent Immersive UI:** True edge-to-edge full-screen experience that hides system navigation and status bars permanently.
* **Keep Screen On:** Prevents the device from sleeping (`FLAG_KEEP_SCREEN_ON`) during active karaoke sessions.
* **Silent Session Recovery:** Automatically detects expired Spotify OAuth tokens and refreshes the session silently in the background without interrupting the user.
* **Integrated Playback Controls:** Play, pause, skip tracks, and monitor playback progress directly from the app.

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **Asynchronous Processing:** Kotlin Coroutines (`lifecycleScope`)
* **Networking:** Retrofit & REST APIs
* **APIs:** Spotify Android Auth SDK, Spotify Web API, LRCLIB API
* **UI/UX:** XML, `ConstraintLayout`, `WindowInsetsControllerCompat`
* **Persistence:** `SharedPreferences`

---

## 🚀 Getting Started

### Prerequisites

* Android Studio (latest stable version recommended)
* An Android device or emulator
* A [Spotify Developer](https://developer.spotify.com/dashboard) account and the Spotify app installed on your device

### Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/YOUR_USERNAME/HudKaraoke.git](https://github.com/YOUR_USERNAME/HudKaraoke.git)
   cd HudKaraoke
   ```

2. **Create a Spotify App:**
   * Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and create a new app.
   * Add the following **Redirect URI** in your app settings:
```text
hudkaraoke://callback
```
   * Copy your **Client ID**.

3. **Configure your Client ID:**
   * Open the project in Android Studio.
   * Replace the placeholder `CLIENT_ID` in `InicioFragment.kt` (or add it to your `local.properties` if configured via `BuildConfig`) with your own Spotify Client ID.

4. **Build and Run:**
   * Sync Gradle and run the app on your Android device.

---

## 🗺️ Roadmap

* [ ] Refactor architecture to MVVM with `ViewModel` and `StateFlow`
* [ ] Migrate deprecated `onActivityResult` to the Android Activity Result API
* [ ] Move `CLIENT_ID` configuration to `local.properties` / `BuildConfig`
* [ ] Add unit and instrumentation tests for lyric synchronization and token refresh flows
* [ ] Support custom font sizes and theme customization in HUD mode

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Adrian**  
* GitHub: [Adrian-Rincon](https://github.com/Adrian-Rincon)
