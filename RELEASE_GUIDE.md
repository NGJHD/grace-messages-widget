# Release Guide

1. `python pipeline/build_dataset.py`. The build must pass and print no PENDING lines.
2. Bump `versionCode` (+1) and `versionName` in `android/app/build.gradle.kts`.
3. `cd android && ./gradlew assembleRelease`. This needs `release.jks` and `keystore.properties`;
   a backup is in `OneDrive\Files\Archive\Signing Keys\Grace Messages`. Always sign with the same key,
   or the update won't install over the existing app.
4. Copy the APK to `grace-messages-vX.Y.Z.apk`, then:
   `gh release create vX.Y.Z grace-messages-vX.Y.Z.apk --title "vX.Y.Z" --notes "..."`
5. Delete the local renamed APK copy. Commit and push. Push new transcripts to the private
   `grace-messages-transcripts` repo.
6. Install: `adb install -r app/build/outputs/apk/release/app-release.apk`, or open the APK on
   the phone. Settings and widgets are kept across updates.
