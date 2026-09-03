# GMS Bundle + Firebase Auth + Google Sign-In Integration

## Summary

This implementation adds complete Google Mobile Services (GMS) support to Android-Virtual-Inject, including Firebase Authentication and Google Sign-In for games running in the virtual environment without root.

## Files Added/Modified

# GMS Bundle + Firebase Auth + Google Sign-In Integration

## Summary

This implementation adds Google Mobile Services (GMS) support to Android-Virtual-Inject by installing the **real** GMS packages into the virtual environment. Real Google Sign-In and Firebase Auth work when a real Google account is signed in and the app has a real Firebase config, exactly like a physical phone.

## Important: Real vs Fake

- **Real**: GMS APKs are extracted from the host device's real GMS install and installed into the virtual env via `installPackageAsUser`. This keeps Google's real signatures, so GMS signature checks pass.
- **No fake/mock proxies**: The previous fake `BinderInvocationStub` proxies (`IGoogleServicesProxy`, `FirebaseAuthProxy`, `GoogleSignInProxy`) were removed — they returned null from `getWho()` (their service names aren't Android framework services) and could never provide real login. PairIp's fake `PackageInfo` hook, which caused "signature is invalid", was also removed.

## Files

### New Files:
1. **GmsInstaller.java** - Installs real GMS packages into the virtual environment
2. **GmsConfig.java** - Per-user GMS config
3. **AccountHelper.java** - Google/Firebase account helper
4. **GmsCore.java** - GMS package constants and host-GMS detection
5. **GmsNativeBridge.java** - JNI bridge (device metadata)
6. **google-services.json** - Firebase configuration template (MUST be replaced)</think>

<｜DSML｜tool_calls>
<｜DSML｜invoke name="edit">
<｜DSML｜parameter name="filePath" string="true">/root/Android-Virtual-Inject/GMS_INTEGRATION.md

## How to Use

### 1. Build the Project
```bash
cd Android-Virtual-Inject
./gradlew assembleDebug
```

### 2. Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install GMS Bundle
1. Open the app
2. Go to Settings
3. Select "Install GMS Bundle"
4. Wait for installation to complete

### 4. Test with a Game
1. Install a game that uses Firebase Auth/Google Sign-In
2. Launch the game in the virtual environment
3. The game should now be able to use Google services

## Configuration

### Update google-services.json
Replace the template `app/google-services.json` with your actual Firebase project configuration:
1. Go to Firebase Console (https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings
4. Download google-services.json
5. Replace the template file

### GMS APK Files (Optional)
Place GMS APK files in `Bcore/src/main/assets/gms_bundle/`:
- `GooglePlayServices.apk`
- `GoogleServicesFramework.apk`
- `GoogleLoginService.apk`
- `FirebaseAuth.apk`
- `FirebaseMessaging.apk`
- `FirebaseInstanceId.apk`
- `GooglePlayStore.apk`
- `GooglePlayGames.apk`

If not provided, the installer will use packages from the host device.

## Architecture

### GMS Bundle Installation
```
GmsInstaller
├── Detect GMS on host (GmsCore.isSupportGms)
├── Copy APKs from assets (or use host GMS directly)
├── Install GMS Core into virtual env (installPackageAsUser)
├── Initialize GMS Config
└── Install Firebase / Sign-In / Play packages
```

Real GMS in the virtual env talks to the same Google servers as a physical phone. Login is real — it requires a real Google account and a real Firebase project.

## Requirements for Real Login

1. **GMS must be installed on the host device** (so the installer can bring its packages into the virtual env with real signatures).
2. **A real `google-services.json`** from your Firebase project (replace `app/google-services.json`).
3. **The app's signing certificate SHA-1 must be registered** in Firebase (Google Sign-In verifies the OAuth client against the app's signature).
4. **A real Google account** to sign in with.

## Troubleshooting

### GMS Not Installing
1. Ensure GMS is installed on the host device
2. Check logcat for errors: `adb logcat -s GmsInstaller`
3. Try installing GMS APKs manually in `assets/gms_bundle/`

### Firebase Auth Not Working
1. Verify `google-services.json` is correct (real Firebase project)
2. Check that the app signing SHA-1 is registered in Firebase
3. Ensure GMS installed in the virtual env
4. Look for errors: `adb logcat -s GmsInstaller`

### Google Sign-In Not Working
1. Ensure Google Play Services is up to date in the virtual env
2. Check that `google-services.json` OAuth client matches the signing key
3. Verify permissions in AndroidManifest.xml
4. A real Google account must be added/signed in

## Notes

- GMS must be installed on the host device for the virtual environment to work
- Some games may require specific GMS versions
- This is a REAL integration, not a mock/spoof — login requires real Google/Firebase credentials
- The previous fake proxies were removed because they could not provide real login

## License

This integration follows the same license as the original Android-Virtual-Inject project.
