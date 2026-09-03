# GMS Bundle + Firebase Auth + Google Sign-In Integration

## Summary

This implementation adds complete Google Mobile Services (GMS) support to Android-Virtual-Inject, including Firebase Authentication and Google Sign-In for games running in the virtual environment without root.

## Files Added/Modified

### New Files:
1. **GmsInstaller.java** - Handles GMS bundle installation from assets
2. **GmsConfig.java** - Manages device registration and authentication tokens
3. **AccountHelper.java** - Manages Google/Firebase accounts
4. **IGoogleServicesProxy.java** - Hooks Google Services bindings
5. **FirebaseAuthProxy.java** - Hooks Firebase Auth calls
6. **GoogleSignInProxy.java** - Hooks Google Sign-In calls
7. **google-services.json** - Firebase configuration template

### Modified Files:
1. **GmsCore.java** - Updated with complete GMS package list
2. **HookManager.java** - Registered new proxies
3. **build.gradle.kts** - Added Firebase/Google dependencies
4. **AndroidManifest.xml** - Added required permissions and activities

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
├── Copy APKs from assets
├── Install GMS Core Services
├── Install Firebase Auth Packages
├── Install Google Sign-In Packages
└── Initialize GMS Configuration
```

### Authentication Flow
```
Game Request → FirebaseAuthProxy → Generate Token → Return to Game
                ↓
         GmsConfig (Token Cache)
                ↓
         AccountHelper (Account Management)
```

### Service Hooks
```
HookManager
├── IGoogleServicesProxy (Google Services)
├── FirebaseAuthProxy (Firebase Auth)
└── GoogleSignInProxy (Google Sign-In)
```

## Troubleshooting

### GMS Not Installing
1. Ensure GMS is installed on the host device
2. Check logcat for errors: `adb logcat -s GmsInstaller`
3. Try installing GMS APKs manually in `assets/gms_bundle/`

### Firebase Auth Not Working
1. Verify `google-services.json` is correct
2. Check if Firebase Auth package is installed
3. Look for errors: `adb logcat -s FirebaseAuthProxy`

### Google Sign-In Not Working
1. Ensure Google Play Services is up to date
2. Check account configuration
3. Verify permissions in AndroidManifest.xml

## Testing

Run the test script:
```bash
javac /tmp/TestGMS.java -d /tmp && java -cp /tmp TestGMS
```

## Notes

- GMS must be installed on the host device for the virtual environment to work
- Some games may require specific GMS versions
- Token refresh is handled automatically
- All tokens are cached for performance

## License

This integration follows the same license as the original Android-Virtual-Inject project.
