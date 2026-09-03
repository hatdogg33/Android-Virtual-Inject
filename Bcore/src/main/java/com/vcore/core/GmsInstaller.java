package com.vcore.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.vcore.BlackBoxCore;
import com.vcore.entity.pm.InstallResult;

public class GmsInstaller {
    private static final String TAG = "GmsInstaller";
    private static final String GMS_ASSETS_DIR = "gms_bundle";
    private static final String GMS_CONFIG_DIR = "gms_config";
    
    private static final Map<String, String> GMS_APK_MAP = new HashMap<>();
    
    static {
        // Map package names to APK filenames in assets
        GMS_APK_MAP.put(GmsCore.GMS_PKG, "GooglePlayServices.apk");
        GMS_APK_MAP.put(GmsCore.GSF_PKG, "GoogleServicesFramework.apk");
        GMS_APK_MAP.put(GmsCore.VENDING_PKG, "GooglePlayStore.apk");
        GMS_APK_MAP.put(GmsCore.PLAY_GAMES_PKG, "GooglePlayGames.apk");
        GMS_APK_MAP.put(GmsCore.GMS_LOGIN_PKG, "GoogleLoginService.apk");
        GMS_APK_MAP.put(GmsCore.FIREBASE_AUTH_PKG, "FirebaseAuth.apk");
        GMS_APK_MAP.put(GmsCore.FIREBASE_MESSAGING_PKG, "FirebaseMessaging.apk");
        GMS_APK_MAP.put(GmsCore.FIREBASE_IID_PKG, "FirebaseInstanceId.apk");
    }
    
    private final Context mContext;
    private final int mUserId;
    private final File mGmsDir;
    private final File mConfigDir;
    
    public GmsInstaller(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
        this.mGmsDir = new File(context.getFilesDir(), GMS_ASSETS_DIR);
        this.mConfigDir = new File(context.getFilesDir(), GMS_CONFIG_DIR);
        
        mGmsDir.mkdirs();
        mConfigDir.mkdirs();
    }
    
    public InstallResult installCompleteGmsBundle() {
        Log.i(TAG, "Starting complete GMS bundle installation for user: " + mUserId);
        
        // Step 1: Check if GMS is already installed on host
        if (!GmsCore.isSupportGms()) {
            Log.e(TAG, "GMS not installed on host device");
            return new InstallResult().installError("GMS not installed on host device");
        }
        
        // Step 2: Copy GMS APKs from assets if available
        copyGmsApksFromAssets();
        
        // Step 3: Install GMS core services first
        InstallResult result = installGmsCoreServices();
        if (!result.success) {
            Log.e(TAG, "Failed to install GMS core services");
            return result;
        }
        
        // Step 4: Initialize GMS configuration
        initializeGmsConfig();
        
        // Step 5: Install Firebase Auth packages
        result = installFirebaseAuthPackages();
        if (!result.success) {
            Log.w(TAG, "Failed to install some Firebase Auth packages");
        }
        
        // Step 6: Install Google Sign-In packages
        result = installGoogleSignInPackages();
        if (!result.success) {
            Log.w(TAG, "Failed to install some Google Sign-In packages");
        }
        
        // Step 7: Install Google Play apps
        result = installGooglePlayApps();
        if (!result.success) {
            Log.w(TAG, "Failed to install some Google Play apps");
        }
        
        Log.i(TAG, "GMS bundle installation completed for user: " + mUserId);
        return new InstallResult();
    }
    
    private void copyGmsApksFromAssets() {
        Log.i(TAG, "Copying GMS APKs from assets");
        
        for (Map.Entry<String, String> entry : GMS_APK_MAP.entrySet()) {
            String packageName = entry.getKey();
            String apkFileName = entry.getValue();
            
            File destFile = new File(mGmsDir, apkFileName);
            if (destFile.exists()) {
                continue;
            }
            
            try {
                InputStream is = mContext.getAssets().open(GMS_ASSETS_DIR + "/" + apkFileName);
                FileOutputStream fos = new FileOutputStream(destFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                
                fos.close();
                is.close();
                
                Log.i(TAG, "Copied APK: " + apkFileName);
            } catch (IOException e) {
                Log.w(TAG, "Failed to copy APK from assets: " + apkFileName + ", using host package");
            }
        }
    }
    
    private InstallResult installGmsCoreServices() {
        Log.i(TAG, "Installing GMS core services");
        
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        
        // Install GMS first (required for all other Google services)
        String[] coreServices = {
            GmsCore.GMS_PKG,
            GmsCore.GSF_PKG,
            GmsCore.GMS_LOGIN_PKG
        };
        
        for (String packageName : coreServices) {
            if (blackBoxCore.isInstalled(packageName, mUserId)) {
                Log.i(TAG, "Already installed: " + packageName);
                continue;
            }
            
            InstallResult result = installPackage(packageName);
            if (!result.success) {
                Log.e(TAG, "Failed to install core service: " + packageName);
                return result;
            }
            
            Log.i(TAG, "Installed core service: " + packageName);
        }
        
        return new InstallResult();
    }
    
    private InstallResult installFirebaseAuthPackages() {
        Log.i(TAG, "Installing Firebase Auth packages");
        
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        
        String[] firebasePackages = {
            GmsCore.FIREBASE_AUTH_PKG,
            GmsCore.FIREBASE_MESSAGING_PKG,
            GmsCore.FIREBASE_IID_PKG
        };
        
        for (String packageName : firebasePackages) {
            if (blackBoxCore.isInstalled(packageName, mUserId)) {
                continue;
            }
            
            InstallResult result = installPackage(packageName);
            if (!result.success) {
                Log.w(TAG, "Failed to install Firebase package: " + packageName);
                // Continue with other packages
            }
        }
        
        return new InstallResult();
    }
    
    private InstallResult installGoogleSignInPackages() {
        Log.i(TAG, "Installing Google Sign-In packages");
        
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        
        String[] signInPackages = {
            "com.google.android.gms.auth.api.signin",
            "com.google.android.gms.games.signin",
            "com.google.android.gms.safetynet"
        };
        
        for (String packageName : signInPackages) {
            if (blackBoxCore.isInstalled(packageName, mUserId)) {
                continue;
            }
            
            InstallResult result = installPackage(packageName);
            if (!result.success) {
                Log.w(TAG, "Failed to install Sign-In package: " + packageName);
            }
        }
        
        return new InstallResult();
    }
    
    private InstallResult installGooglePlayApps() {
        Log.i(TAG, "Installing Google Play apps");
        
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        
        String[] playApps = {
            GmsCore.VENDING_PKG,
            GmsCore.PLAY_GAMES_PKG
        };
        
        for (String packageName : playApps) {
            if (blackBoxCore.isInstalled(packageName, mUserId)) {
                continue;
            }
            
            InstallResult result = installPackage(packageName);
            if (!result.success) {
                Log.w(TAG, "Failed to install Play app: " + packageName);
            }
        }
        
        return new InstallResult();
    }
    
    private InstallResult installPackage(String packageName) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        
        // First try to install from host
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
            String apkPath = packageInfo.applicationInfo.sourceDir;
            
            InstallResult result = blackBoxCore.installPackageAsUser(apkPath, mUserId);
            if (result.success) {
                return result;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package not found on host: " + packageName);
        }
        
        // Try to install from assets
        String apkFileName = GMS_APK_MAP.get(packageName);
        if (apkFileName != null) {
            File apkFile = new File(mGmsDir, apkFileName);
            if (apkFile.exists()) {
                InstallResult result = blackBoxCore.installPackageAsUser(apkFile.getAbsolutePath(), mUserId);
                if (result.success) {
                    return result;
                }
            }
        }
        
        return new InstallResult().installError("Package not available: " + packageName);
    }
    
    private void initializeGmsConfig() {
        Log.i(TAG, "Initializing GMS configuration");
        
        // Create GMS config directory structure
        File gmsConfigDir = new File(mConfigDir, "com.google.android.gms");
        gmsConfigDir.mkdirs();
        
        // Create device registration file
        File deviceInfoFile = new File(gmsConfigDir, "device_info.xml");
        if (!deviceInfoFile.exists()) {
            createDeviceInfoFile(deviceInfoFile);
        }
        
        // Create GMS settings file
        File gmsSettingsFile = new File(gmsConfigDir, "gms_settings.xml");
        if (!gmsSettingsFile.exists()) {
            createGmsSettingsFile(gmsSettingsFile);
        }
    }
    
    private void createDeviceInfoFile(File file) {
        try {
            String deviceInfo = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<device_info>\n" +
                "    <android_id>" + getAndroidId() + "</android_id>\n" +
                "    <device_id>" + getDeviceId() + "</device_id>\n" +
                "    <model>" + android.os.Build.MODEL + "</model>\n" +
                "    <manufacturer>" + android.os.Build.MANUFACTURER + "</manufacturer>\n" +
                "    <brand>" + android.os.Build.BRAND + "</brand>\n" +
                "    <product>" + android.os.Build.PRODUCT + "</product>\n" +
                "    <device>" + android.os.Build.DEVICE + "</device>\n" +
                "    <board>" + android.os.Build.BOARD + "</board>\n" +
                "    <hardware>" + android.os.Build.HARDWARE + "</hardware>\n" +
                "    <display>" + android.os.Build.DISPLAY + "</display>\n" +
                "    <finger_print>" + android.os.Build.FINGERPRINT + "</finger_print>\n" +
                "    <host>" + android.os.Build.HOST + "</host>\n" +
                "    <type>" + android.os.Build.TYPE + "</type>\n" +
                "    <tags>" + android.os.Build.TAGS + "</tags>\n" +
                "</device_info>";
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(deviceInfo.getBytes());
            fos.close();
            
            Log.i(TAG, "Created device info file");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create device info file", e);
        }
    }
    
    private void createGmsSettingsFile(File file) {
        try {
            String gmsSettings = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<gms_settings>\n" +
                "    <version>1</version>\n" +
                "    <user_id>" + mUserId + "</user_id>\n" +
                "    <gms_version>24.30.14</gms_version>\n" +
                "    <setup_wizard_done>true</setup_wizard_done>\n" +
                "    <device_registered>true</device_registered>\n" +
                "    <account_added>true</account_added>\n" +
                "</gms_settings>";
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(gmsSettings.getBytes());
            fos.close();
            
            Log.i(TAG, "Created GMS settings file");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create GMS settings file", e);
        }
    }
    
    private String getAndroidId() {
        return android.provider.Settings.Secure.getString(
            mContext.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
    }
    
    private String getDeviceId() {
        return android.os.Build.BOARD + android.os.Build.BRAND + android.os.Build.DEVICE;
    }
    
    public boolean isGmsBundleInstalled() {
        return GmsCore.isGmsBundleInstalled(mUserId);
    }
    
    public void uninstallGmsBundle() {
        Log.i(TAG, "Uninstalling GMS bundle for user: " + mUserId);
        GmsCore.uninstallGApps(mUserId);
        
        // Clean up config files
        deleteDirectory(mGmsDir);
        deleteDirectory(mConfigDir);
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
