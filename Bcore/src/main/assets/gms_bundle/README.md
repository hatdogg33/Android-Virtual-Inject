# GMS Bundle Assets

This directory contains Google Mobile Services (GMS) APK files for the virtual environment.

## Required APK Files

Place the following APK files in this directory:

### Core Services (Required)
- `GooglePlayServices.apk` - Google Play Services (com.google.android.gms)
- `GoogleServicesFramework.apk` - Google Services Framework (com.google.android.gsf)
- `GoogleLoginService.apk` - Google Login Service (com.google.android.gsf.login)

### Firebase Auth (Required for Firebase)
- `FirebaseAuth.apk` - Firebase Auth (com.google.android.gms.auth)
- `FirebaseMessaging.apk` - Firebase Cloud Messaging (com.google.android.gms.gcm)
- `FirebaseInstanceId.apk` - Firebase Instance ID (com.google.android.gms.iid)

### Google Play Apps (Optional)
- `GooglePlayStore.apk` - Google Play Store (com.android.vending)
- `GooglePlayGames.apk` - Google Play Games (com.google.android.play.games)

## How to Get APKs

1. **From Host Device**: If GMS is installed on your device, APKs can be extracted from:
   - `/system/priv-app/GoogleServicesFramework/`
   - `/data/app/com.google.android.gms-*/`
   - `/data/app/com.android.vending-*/`

2. **From APK Mirror**: Download from trusted sources like APKMirror.com

3. **From Google Factory Images**: Extract from official Google factory images

## Installation

The GMS Installer will automatically:
1. Copy APKs from this directory to the virtual environment
2. Install GMS core services first
3. Install Firebase Auth packages
4. Install Google Sign-In packages
5. Initialize GMS configuration

## Notes

- GMS must be installed on the host device for the virtual environment to work
- The installer will fall back to host packages if APKs are not found in this directory
- Some packages may require specific versions to work correctly
- Clear the virtual environment data if you encounter issues after updating APKs
