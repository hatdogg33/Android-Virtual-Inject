package com.vcore.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.vcore.BlackBoxCore;
import com.vcore.entity.pm.InstallResult;

public class GmsCore {
    private static final String TAG = "GmsCore";
    
    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();
    private static final HashSet<String> FIREBASE_AUTH = new HashSet<>();
    private static final HashSet<String> GOOGLE_SIGNIN = new HashSet<>();
    
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";
    public static final String PLAY_GAMES_PKG = "com.google.android.play.games";
    public static final String GMS_LOGIN_PKG = "com.google.android.gsf.login";
    public static final String FIREBASE_AUTH_PKG = "com.google.android.gms.auth";
    public static final String FIREBASE_MESSAGING_PKG = "com.google.android.gms.gcm";
    public static final String FIREBASE_IID_PKG = "com.google.android.gms.iid";

    static {
        // Google Play Store & Apps
        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add(PLAY_GAMES_PKG);
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");
        GOOGLE_APP.add("com.google.android.apps.chromecast.app");
        GOOGLE_APP.add("com.google.android.apps.photos");
        GOOGLE_APP.add("com.google.android.apps.maps");
        GOOGLE_APP.add("com.google.android.youtube");
        GOOGLE_APP.add("com.google.android.gm");
        GOOGLE_APP.add("com.google.android.apps.docs");
        GOOGLE_APP.add("com.google.android.apps.drive");
        GOOGLE_APP.add("com.google.android.calendar");
        GOOGLE_APP.add("com.google.android.keep");
        GOOGLE_APP.add("com.google.android.talk");

        // GMS Core Services - Must install first
        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add(GMS_LOGIN_PKG);
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
        GOOGLE_SERVICE.add("com.google.android.gms.car");
        GOOGLE_SERVICE.add("com.google.android.gms.kids");
        GOOGLE_SERVICE.add("com.google.android.gms.games");
        GOOGLE_SERVICE.add("com.google.android.gms.auth.api");
        GOOGLE_SERVICE.add("com.google.android.gms.auth.api.signin");
        GOOGLE_SERVICE.add("com.google.android.gms.fitness");
        GOOGLE_SERVICE.add("com.google.android.gms.location");
        GOOGLE_SERVICE.add("com.google.android.gms.nearby");
        GOOGLE_SERVICE.add("com.google.android.gms.wallet");

        // Firebase Auth Services
        FIREBASE_AUTH.add(FIREBASE_AUTH_PKG);
        FIREBASE_AUTH.add(FIREBASE_MESSAGING_PKG);
        FIREBASE_AUTH.add(FIREBASE_IID_PKG);
        FIREBASE_AUTH.add("com.google.android.gms.auth.cryptauth");
        FIREBASE_AUTH.add("com.google.android.gms.auth.proximity");
        FIREBASE_AUTH.add("com.google.android.gms.auth.account.be.accountauth");

        // Google Sign-In Services
        GOOGLE_SIGNIN.add("com.google.android.gms.auth.api.signin");
        GOOGLE_SIGNIN.add("com.google.android.gms.games.signin");
        GOOGLE_SIGNIN.add("com.google.android.gms.safetynet");
        GOOGLE_SIGNIN.add("com.google.android.gms.tapandpay");
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str) || 
               FIREBASE_AUTH.contains(str) || GOOGLE_SIGNIN.contains(str);
    }

    public static boolean isFirebaseAuthPackage(String str) {
        return FIREBASE_AUTH.contains(str);
    }

    public static boolean isGoogleSignInPackage(String str) {
        return GOOGLE_SIGNIN.contains(str);
    }

    private static InstallResult installPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            if (blackBoxCore.isInstalled(packageName, userId)) {
                continue;
            }

            try {
                BlackBoxCore.getContext().getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException ignored) {
                continue;
            }

            InstallResult installResult = blackBoxCore.installPackageAsUser(packageName, userId);
            if (!installResult.success) {
                return installResult;
            }
        }
        return new InstallResult();
    }

    private static void uninstallPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            blackBoxCore.uninstallPackageAsUser(packageName, userId);
        }
    }

    public static InstallResult installGApps(int userId) {
        Set<String> googleApps = new HashSet<>();

        googleApps.addAll(GOOGLE_SERVICE);
        googleApps.addAll(GOOGLE_APP);
        googleApps.addAll(FIREBASE_AUTH);
        googleApps.addAll(GOOGLE_SIGNIN);

        InstallResult installResult = installPackages(googleApps, userId);
        if (!installResult.success) {
            uninstallGApps(userId);
            return installResult;
        }
        return installResult;
    }

    public static InstallResult installGmsBundle(int userId) {
        Log.i(TAG, "Installing complete GMS bundle for user: " + userId);
        
        Set<String> gmsBundle = new HashSet<>();
        gmsBundle.addAll(GOOGLE_SERVICE);
        gmsBundle.addAll(GOOGLE_APP);
        gmsBundle.addAll(FIREBASE_AUTH);
        gmsBundle.addAll(GOOGLE_SIGNIN);

        InstallResult installResult = installPackages(gmsBundle, userId);
        if (!installResult.success) {
            Log.e(TAG, "Failed to install GMS bundle: " + installResult.error);
            uninstallGApps(userId);
            return installResult;
        }
        
        Log.i(TAG, "GMS bundle installed successfully for user: " + userId);
        return installResult;
    }

    public static void uninstallGApps(int userId) {
        uninstallPackages(GOOGLE_SERVICE, userId);
        uninstallPackages(GOOGLE_APP, userId);
        uninstallPackages(FIREBASE_AUTH, userId);
        uninstallPackages(GOOGLE_SIGNIN, userId);
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
        FIREBASE_AUTH.remove(packageName);
        GOOGLE_SIGNIN.remove(packageName);
    }

    public static boolean isSupportGms() {
        try {
            BlackBoxCore.getPackageManager().getPackageInfo(GMS_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) { }
        return false;
    }

    public static boolean isInstalledGoogleService(int userId) {
        return BlackBoxCore.get().isInstalled(GMS_PKG, userId);
    }

    public static boolean isGmsBundleInstalled(int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        return blackBoxCore.isInstalled(GMS_PKG, userId) && 
               blackBoxCore.isInstalled(GSF_PKG, userId) &&
               blackBoxCore.isInstalled(PLAY_GAMES_PKG, userId);
    }

    public static Set<String> getGoogleApps() {
        return new HashSet<>(GOOGLE_APP);
    }

    public static Set<String> getGoogleServices() {
        return new HashSet<>(GOOGLE_SERVICE);
    }

    public static Set<String> getFirebaseAuthPackages() {
        return new HashSet<>(FIREBASE_AUTH);
    }

    public static Set<String> getGoogleSignInPackages() {
        return new HashSet<>(GOOGLE_SIGNIN);
    }

    public static Set<String> getAllGmsPackages() {
        Set<String> allPackages = new HashSet<>();
        allPackages.addAll(GOOGLE_SERVICE);
        allPackages.addAll(GOOGLE_APP);
        allPackages.addAll(FIREBASE_AUTH);
        allPackages.addAll(GOOGLE_SIGNIN);
        return allPackages;
    }
}