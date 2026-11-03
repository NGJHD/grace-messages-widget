# Grace Messages Widget

A personal, fully offline Android app + home-screen widget that shows a short
inspirational line or teaching insight drawn from the owner's purchased sermon
collection (Joseph Prince), with a one- or two-paragraph reflection in the app.
Sideloaded onto the owner's Galaxy Z Fold 4 / 6 / 7. Never published to a store
(the sermon-derived text is bundled in the APK).

## How it works

```
T:\Y\Sermons (mp3/mka/mkv/mp4, some as CD-track folders)
  └─ pipeline/inventory.py      → data/inventory.json      (145 de-duplicated sermons, ~189 h)
  └─ pipeline/transcribe.py     → data/transcripts/<id>.txt|.json   (whisper.cpp, large-v3-turbo)
  └─ (agent writes)             → data/messages/<id>.json  (curated messages, see below)
  └─ pipeline/build_dataset.py  → android/app/src/main/assets/messages.json
  └─ android/ (Gradle)          → app-release.apk (sideload)
```

### Writing messages (done by the agent, not a local LLM: owner's choice)
- Read the whole transcript, keep only the **best** ideas (quality over quantity;
  typically 3–7 per sermon, as many as the sermon genuinely supports).
- `data/messages/<sermon-id>.json` is a list of
  `{"category": "inspirational"|"insight", "text", "explanation", "scripture"?}`.
  For a recording with no title, use `{"title": "...", "messages": [...]}` to name it
  (e.g. `2026-05-10-untitled`).
- **text**: rephrased (the preacher is long-winded), tight, standalone, widget-sized.
  Mix lengths; include some short ones (< 80 chars) because small widgets only hold ~40–60.
- **explanation**: LLM-written, 1–2 paragraphs (blank line between), faithful to what
  the sermon actually teaches, warm devotional voice addressed to "you". Don't write
  "Pastor Prince says". Don't invent claims that aren't in the sermon.
- **inspirational** = encouraging, quotable line. **insight** = teaching point
  (Greek/Hebrew word meanings, typology, scripture connections, doctrine).
- Add `scripture` when the point rests on a specific verse.
- Skip transcription glitches (misheard names), announcements, and the closing ad.
- The approved tone/length reference is
  `data/messages/2001-07-01-winning-the-battle-of-your-mind.json`.
- After adding files: `python pipeline/build_dataset.py` (validates and prints a length histogram).

## App behaviour (owner's decisions, don't change without asking)
- **Widget**: resizable, default 4×2, shows **only the text**: white, serif, centred,
  on a solid colour or a 2-colour gradient fixed top-left → bottom-right. Tap opens the app.
- **Text colour is always white.** Background presets must stay dark enough for white text.
- **Character limit is automatic**: derived from the size of the placed widget(s) at the
  chosen font size (smallest widget wins when there are several, e.g. cover + inner screen).
  **Font size is the only user lever**. Settings shows "Your widget holds about N
  characters" and "X% of messages fit". No widget placed means no limit. Shrink-to-fit
  remains as a safety net only.
- **Category filter**: All / Inspirational / Insights. Changing it picks a new message immediately.
- **Refresh**: Daily (midnight) or every 1/2/3/4/6/12 h, aligned to local clock boundaries.
  No repeats until the eligible pool is exhausted. "Show another message" (→) and "Previous" (←, steps back through up to 50 earlier messages) in the app.
- **App home**: current message on the chosen background, then category · scripture,
  sermon title + date, and the reflection.
- No favourites, history, or share (explicitly declined). Fully offline, no INTERNET permission.

## Android code map (`android/`, package `app.gracemessages`)
- `data/Models.kt`: Dataset/Sermon/Message (kotlinx.serialization), CategoryFilter.
- `data/Settings.kt`: DataStore preferences (font, colours, gradient, category, refresh,
  plus selection state: current id, period key, shown ids).
- `data/MessageRepository.kt`: loads `assets/messages.json`; `current()` (period rollover
  + re-fit), `next()` (shuffle), `rekey()`; `Period` handles the time buckets.
- `data/Presets.kt`: background presets, white-contrast check.
- `widget/MessageWidget.kt`: Glance widget (`SizeMode.Exact`) + receiver. Content
  **observes the DataStore flow**: Glance only calls `provideGlance` once per session
  (~45 s), so values read outside `provideContent` go stale. The receiver re-fits on resize
  (`onAppWidgetOptionsChanged`) and on delete.
- `widget/FitLimit.kt`, `WidgetSizes.kt`, `TextFit.kt`: read placed widget sizes
  (`OPTION_APPWIDGET_SIZES`, smallest per widget), estimate character capacity with
  `StaticLayout` + serif, gradient bitmap, padding (10 dp on 1-row, else 16 dp).
- `widget/RefreshScheduler.kt`: rotation tick = **AlarmManager chain** (`setWindow(RTC, …)`,
  inexact, non-wakeup: held while asleep and delivered at next wake), same design as the
  owner's PSI widget (`C:\GitHub\psi-widget`, `Scheduling.kt`), because WorkManager jobs get
  parked by One UI. `TickReceiver` handles the tick plus BOOT_COMPLETED / MY_PACKAGE_REPLACED /
  TIME_SET / TIMEZONE_CHANGED and re-arms. Backstop: `updatePeriodMillis` 30 min, and every
  render re-checks the clock. Android enforces a ~10 min minimum window, so ticks can be a few
  minutes late.
- `ui/PowerSetup.kt`: unskippable battery "Unrestricted" gate on every resume (copied from the
  PSI widget; Back closes the app). No Data Saver gate: the app has no network access.
- `ui/`: Compose Material 3 (dynamic colour). `App.kt` (state + actions), `HomeScreen`,
  `SettingsScreen`, `WidgetFit` (capacity card + "Add widget" pin button), `ColorPicker`, `Common`.

Stack: AGP 8.13.0, Gradle 8.14.3, Kotlin 2.2.20, compileSdk/targetSdk 36, minSdk 31,
Compose BOM 2025.09.00, Glance 1.1.1, DataStore 1.1.7, JDK 21.

## Commands
```bash
# Build (release is R8-minified and signed with the debug key for sideloading)
cd android && ./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk

# Emulator for testing: AVD "pm25test" (API 36)
"$ANDROID_HOME/emulator/emulator" -avd pm25test -no-snapshot-save -no-audio

# Pipeline
python pipeline/inventory.py                         # re-scan T:\Y\Sermons
python pipeline/transcribe.py --gpu 0 --shard 0/2    # resumable; run 1/2 on --gpu 1 in parallel
python pipeline/build_dataset.py
```

## Environment notes
- Windows 11; shells: PowerShell 5.1 and Git Bash. `android/local.properties` must use forward
  slashes (`sdk.dir=C:/Users/ngjhd/AppData/Local/Android/Sdk`).
- Models: `C:\llama\models\ggml-large-v3-turbo.bin`, `ggml-silero-v5.1.2.bin` (VAD).
  whisper.cpp v1.9.5 (release `b5454`) binaries in `tools/whisper-cpu` and `tools/whisper-cuda`
  (git-ignored). Running freshly downloaded binaries needs the owner's explicit approval.
- GPUs: RTX 5060 Ti (idx 0) + RTX 3080 (idx 1), both work with the CUDA 12.4 build (~35×
  realtime). They're shared with the owner's other work, so ask before using them. CPU (Ryzen 9 3900X) does ~3× realtime.
- `data/audio/` holds temporary WAVs only (git-ignored); transcripts and messages are kept.

## Adding new sermons (what's processed is tracked by files, not a list)
A sermon is **done** when `data/messages/<id>.json` exists. To add more:
1. Drop the files into `T:\Y\Sermons` (`YYYY-MM-DD Title.ext`, or a track folder).
2. `python pipeline/inventory.py`. Ids are stable (date + title slug), so existing ones are unchanged.
   Dry run: `python pipeline/inventory.py T:\Y\Sermons %TEMP%\inv.json` and compare ids.
3. `python pipeline/transcribe.py --gpu 0` skips anything already transcribed. Check the VRAM rule
   and ask about the GPUs first.
4. `python pipeline/build_dataset.py` prints **PENDING transcription / PENDING messages**: that is the
   work queue. Write messages for each pending id (read the full transcript, avoid repeating themes
   already used in other files), rerun the build, then bump `versionCode`/`versionName` and release.
Transcripts are backed up in the private repo `NGJHD/grace-messages-transcripts`
(`data/transcripts` is its own git repo, ignored by this one); commit and push new ones there.

## Status (2026-11-03)
- App: v1.0.0 released. In daily use on the owner's Galaxy Z Folds since 2026-11-02, working well.
- Release signing: `android/release.jks` (alias `gracemessages`) + `android/keystore.properties`,
  both git-ignored; backed up to `OneDrive\Files\Archive\Signing Keys\Grace Messages`.
- Transcription: all 145 done (2015-04-05 is missing CD tracks 1-09 to 1-12, which are unreadable).
- Messages: all 145 sermons written, 1004 messages (10% ≤ 80 chars, 98% ≤ 160).
  Guest preachers use the `{"title": "... (Ps Name)", "messages": [...]}` form.
