#!/bin/bash

echo "========================================"
echo "GMS Bundle Integration Test Script"
echo "========================================"
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Please run this script from the Android-Virtual-Inject directory"
    exit 1
fi

echo "1. Checking required files..."
files=(
    "Bcore/src/main/java/com/vcore/core/GmsCore.java"
    "Bcore/src/main/java/com/vcore/core/GmsInstaller.java"
    "Bcore/src/main/java/com/vcore/core/GmsConfig.java"
    "Bcore/src/main/java/com/vcore/core/AccountHelper.java"
    "Bcore/src/main/java/com/vcore/core/GmsNativeBridge.java"
    "app/google-services.json"
    "Bcore/src/main/assets/gms_bundle/README.md"
)

all_files_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✓ $file"
    else
        echo "   ✗ $file (MISSING)"
        all_files_exist=false
    fi
done

if [ "$all_files_exist" = false ]; then
    echo ""
    echo "❌ Some required files are missing"
    exit 1
fi

echo ""
echo "2. Checking build.gradle dependencies..."
if grep -q "play-services-auth" Bcore/build.gradle.kts; then
    echo "   ✓ Google Play Services Auth"
else
    echo "   ✗ Google Play Services Auth (MISSING)"
fi

if grep -q "firebase-auth" Bcore/build.gradle.kts; then
    echo "   ✓ Firebase Auth"
else
    echo "   ✗ Firebase Auth (MISSING)"
fi

echo ""
echo "3. Checking HookManager no longer references removed fake proxies..."
if grep -q "IGoogleServicesProxy\|FirebaseAuthProxy\|GoogleSignInProxy" Bcore/src/main/java/com/vcore/fake/hook/HookManager.java; then
    echo "   ✗ Removed fake proxies still referenced (must be removed)"
else
    echo "   ✓ No removed fake proxies referenced"
fi

echo ""
echo "4. Checking AndroidManifest permissions..."
if grep -q "com.google.android.gms.auth.permission.SEND" Bcore/src/main/AndroidManifest.xml; then
    echo "   ✓ Firebase Auth permissions"
else
    echo "   ✗ Firebase Auth permissions (MISSING)"
fi

if grep -q "com.google.android.gms.auth.permission.FIRST_PARTY_SIGN_IN" Bcore/src/main/AndroidManifest.xml; then
    echo "   ✓ Google Sign-In permissions"
else
    echo "   ✗ Google Sign-In permissions (MISSING)"
fi

echo ""
echo "========================================"
echo "✅ All checks passed!"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. Update app/google-services.json with your Firebase project config"
echo "2. (Optional) Place GMS APKs in Bcore/src/main/assets/gms_bundle/"
echo "3. Build the project: ./gradlew assembleDebug"
echo "4. Install on device and test with a game"
echo ""
echo "For more information, see GMS_INTEGRATION.md"
