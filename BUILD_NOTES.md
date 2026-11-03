# Build Notes

## Pipeline (2026-11-02 → 11-03)
- whisper.cpp v1.9.5 CUDA build, model large-v3-turbo + Silero VAD. ~35× realtime on the
  RTX 5060 Ti (GPU 0; run with `CUDA_DEVICE_ORDER=PCI_BUS_ID` so index 0 is the 5060 Ti).
  The 3080 (GPU 1) was busy training, so it was not used for the later runs.
- `transcribe.py` probes each part with ffprobe and skips unreadable ones. In
  `2015-04-05`, CD tracks 1-09 to 1-12 are corrupt, so that transcript has a gap.
- `2026-05-10` was untitled; it was renamed on disk (`T:\Y\Sermons`), in `inventory.json` and in
  its transcript files to *True Bible Prosperity Versus Prosperity Gospel*.
- Messages were written by the agent from full transcripts: 145 sermons, 1004 messages.
  `build_dataset.py` validates them and writes `android/app/src/main/assets/messages.json` (~0.9 MB).

## Android
- `cd android && ./gradlew assembleRelease` builds a 3.6 MB APK, R8-minified and signed with
  `release.jks` (see `keystore.properties`; neither is committed).
- Gotcha: `local.properties` must use forward slashes on Windows.
- Gotcha: Glance calls `provideGlance` once per session. Read settings from the DataStore flow
  inside `provideContent`, or the widget shows stale values.
- Gotcha: WorkManager jobs get parked by One UI. Rotation uses an inexact `AlarmManager`
  chain instead, the same design as the PSI widget.
