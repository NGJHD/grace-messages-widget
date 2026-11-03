# Grace Messages Widget

A personal, offline Android app and home-screen widget. It shows a short message drawn from
Joseph Prince's sermons, with a one- or two-paragraph reflection in the app. There are
1,004 messages from 145 sermons.

Built for personal sideloading only. It isn't affiliated with or endorsed by Joseph Prince
Ministries. The messages are paraphrases and reflections, not quotations, and the sermon
transcripts are not included in this repository.

## Usage

1. Build the APK (see below) and install it with `adb install -r app-release.apk`.
2. Open the app once and allow **Unrestricted** battery use when asked, so that rotation keeps running.
3. Add the widget from your launcher's widget picker, or use **Add widget** in Settings.
   The default size is 4×2, and it's resizable.
4. In **Settings** you can change:
   - **Font size.** The app works out how many characters fit in your placed widget(s) and shows
     only messages that fit.
   - **Background.** A solid colour or a two-colour gradient. The text is always white.
   - **Category.** All, Inspirational or Insights.
   - **Refresh.** Daily at midnight, or every 1, 2, 3, 4, 6 or 12 hours.
5. Tap the widget to open the full message, its scripture, the sermon it came from and the reflection.
   **Next** picks a new one and **Previous** steps back. **Settings ▸ Clear read history** lets
   every message come round again.

The app has no network access.

## For Developers

**Stack:** Kotlin 2.2, Jetpack Compose (Material 3), Glance 1.1 widgets, DataStore,
AGP 8.13 / Gradle 8.14, JDK 21, minSdk 31.

```
pipeline/inventory.py      scan a sermon folder → data/inventory.json
pipeline/transcribe.py     whisper.cpp (large-v3-turbo + VAD) → data/transcripts/ (not committed)
data/messages/<id>.json    curated messages: [{category, text, explanation, scripture?}]
pipeline/build_dataset.py  validate + bundle → android/app/src/main/assets/messages.json
android/                   the app (package app.gracemessages)
```

To build:

```bash
python pipeline/build_dataset.py        # only needed after editing data/messages
cd android
./gradlew assembleDebug                 # or assembleRelease with your own keystore
```

- **Release signing.** Release builds need `android/keystore.properties` (with `storeFile`,
  `storePassword`, `keyAlias` and `keyPassword`) and a keystore. Neither is committed.
- **Windows.** `android/local.properties` must use forward slashes: `sdk.dir=C:/.../Android/Sdk`.

See `AGENTS.md` for the code map and the design decisions, and `BUILD_NOTES.md` for gotchas.
