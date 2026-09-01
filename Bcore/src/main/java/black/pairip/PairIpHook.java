package black.pairip;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PairIpHook {
    private static final String TAG = "PairIpHook";

    public static void hookIfPresent(ClassLoader cl) {
        if (cl == null) return;

        hookPairIpLicense(cl);
        hookGooglePlayServices(cl);
        hookOrientation(cl);
        hookRaspBypass(cl);
    }

    public static void hookGmsOnly(ClassLoader cl) {
        if (cl == null) return;
        hookGooglePlayServices(cl);

        try {
            Class<?> gmsCoreUtil = XposedHelpers.findClass("com.google.android.gms.common.GoogleApiAvailability", cl);
            XposedBridge.hookAllMethods(gmsCoreUtil, "isGooglePlayServicesAvailable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "GoogleApiAvailability.isGooglePlayServicesAvailable -> SUCCESS");
                    param.setResult(0);
                }
            });
            Log.i(TAG, "GoogleApiAvailability hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "GoogleApiAvailability class not found: " + e.getMessage());
        }

        try {
            Class<?> googleApiClient = XposedHelpers.findClass("com.google.android.gms.common.api.GoogleApiClient", cl);
            XposedBridge.hookAllMethods(googleApiClient, "blockingConnect", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "GoogleApiClient.blockingConnect intercepted, returning SUCCESS");
                    Class<?> resultClass = XposedHelpers.findClass("com.google.android.gms.common.ConnectionResult", cl);
                    java.lang.reflect.Constructor<?> ctor = resultClass.getConstructor(int.class);
                    param.setResult(ctor.newInstance(0));
                }
            });
            Log.i(TAG, "GoogleApiClient.blockingConnect hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "GoogleApiClient class not found: " + e.getMessage());
        }

        hookGoogleSignatureVerifier(cl);
        hookRunningProcesses();
        hookServiceManager(cl);
        hookGmsSecurityException(cl);
    }

    private static void hookGmsSecurityException(ClassLoader cl) {
        try {
            Class<?> zzaaClass = XposedHelpers.findClass("com.google.android.gms.common.internal.zzaa", cl);
            XposedBridge.hookAllMethods(zzaaClass, "getService", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.hasThrowable()) {
                        Throwable t = param.getThrowable();
                        if (t instanceof java.lang.SecurityException) {
                            String msg = t.getMessage();
                            if (msg != null && msg.contains("Unknown calling package")) {
                                Log.w(TAG, "zzaa.getService SecurityException caught: " + msg);
                                param.setThrowable(null);
                            }
                        }
                    }
                }
            });
            Log.i(TAG, "zzaa.getService SecurityException hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "zzaa.getService hook failed: " + e.getMessage());
        }

        try {
            Class<?> baseGmsClientClass = XposedHelpers.findClass("com.google.android.gms.common.internal.BaseGmsClient", cl);
            XposedBridge.hookAllMethods(baseGmsClientClass, "getRemoteService", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.hasThrowable()) {
                        Throwable t = param.getThrowable();
                        if (t instanceof java.lang.SecurityException) {
                            String msg = t.getMessage();
                            if (msg != null && msg.contains("Unknown calling package")) {
                                Log.w(TAG, "BaseGmsClient.getRemoteService SecurityException caught: " + msg);
                                param.setThrowable(null);
                            }
                        }
                    }
                }
            });
            Log.i(TAG, "BaseGmsClient.getRemoteService SecurityException hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "BaseGmsClient.getRemoteService hook failed: " + e.getMessage());
        }
    }

    private static void hookServiceManager(ClassLoader cl) {
        try {
            System.out.println("PairIpHook: hookServiceManager START, cl=" + cl);
            Log.i(TAG, "hookServiceManager START, cl=" + cl);
        } catch (Throwable t) { System.err.println("PairIpHook: hookServiceManager even println failed: " + t); }

        Class<?> smClass = null;

        try {
            smClass = Class.forName("android.os.ServiceManager");
            Log.i(TAG, "Found ServiceManager via boot classloader");
            System.out.println("PairIpHook: Found ServiceManager via boot classloader: " + smClass);
        } catch (Throwable e1) {
            Log.w(TAG, "ServiceManager not found via boot: " + e1.getMessage());
            try {
                smClass = Class.forName("android.os.ServiceManager", false, cl);
                Log.i(TAG, "Found ServiceManager via app classloader");
                System.out.println("PairIpHook: Found ServiceManager via app classloader: " + smClass);
            } catch (Throwable e2) {
                Log.w(TAG, "ServiceManager not found via app cl: " + e2.getMessage());
                try {
                    smClass = Class.forName("com.android.server.ServiceManager", false, null);
                    Log.i(TAG, "Found ServiceManager via com.android.server path");
                    System.out.println("PairIpHook: Found ServiceManager via server path: " + smClass);
                } catch (Throwable e3) {
                    Log.w(TAG, "hookServiceManager failed: ServiceManager class not found via any classloader");
                    System.out.println("PairIpHook: hookServiceManager FAILED: class not found via any classloader");
                    return;
                }
            }
        }

        final Class<?> serviceManagerClass = smClass;
        Log.i(TAG, "hookServiceManager: about to hookAllMethods on " + smClass.getName());
        System.out.println("PairIpHook: hookServiceManager: about to hookAllMethods on " + smClass.getName());

        try {
            XposedBridge.hookAllMethods(smClass, "getService", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String name = (String) param.args[0];
                    if ("package".equals(name)) {
                        Log.d(TAG, "ServiceManager.getService(package) called!");
                        System.out.println("PairIpHook: ServiceManager.getService(package) CALLED");
                        try {
                            Object ourProxy = black.android.app.ActivityThread.sPackageManager.get();
                            if (ourProxy != null) {
                                param.setResult(ourProxy);
                                Log.d(TAG, "ServiceManager.getService(package) -> intercepted, returning our proxy");
                                System.out.println("PairIpHook: ServiceManager.getService(package) -> INTERCEPTED");
                                return;
                            }

                            java.lang.reflect.Field sCacheField = serviceManagerClass.getDeclaredField("sCache");
                            sCacheField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            Map<String, IBinder> cache = (Map<String, IBinder>) sCacheField.get(null);
                            if (cache != null) {
                                IBinder cached = cache.get("package");
                                if (cached != null) {
                                    param.setResult(cached);
                                    Log.d(TAG, "ServiceManager.getService(package) -> returned cached: " + cached.getClass().getName());
                                }
                            }
                        } catch (Throwable e) {
                            Log.w(TAG, "Could not intercept getService(package): " + e.getMessage());
                        }
                    }
                }
            });
            Log.i(TAG, "ServiceManager.getService hook installed");
            System.out.println("PairIpHook: ServiceManager.getService hook INSTALLED");
        } catch (Throwable e) {
            Log.w(TAG, "hookServiceManager hookAllMethods failed: " + e.getMessage());
            System.out.println("PairIpHook: hookServiceManager hookAllMethods FAILED: " + e.getMessage());
        }
    }

    private static void hookPairIpLicense(ClassLoader cl) {
        try {
            Class<?> licenseClientClass = XposedHelpers.findClass("com.pairip.licensecheck.LicenseClient", cl);
            Log.i(TAG, "Found LicenseClient class, enumerating methods...");

            for (Method m : licenseClientClass.getDeclaredMethods()) {
                Log.d(TAG, "  Method: " + m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ") return=" + m.getReturnType().getName());
            }

            XposedBridge.hookAllMethods(licenseClientClass, "processResponse", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "processResponse called with " + param.args.length + " args, intercepting");
                    param.setResult(null);
                }
            });
            Log.i(TAG, "PairIP LicenseClient hook installed (hookAllMethods)");
        } catch (Throwable e) {
            Log.w(TAG, "LicenseClient class not found or hook failed: " + e.getMessage());
        }

        try {
            Class<?> licenseActivityClass = XposedHelpers.findClass("com.pairip.licensecheck.LicenseActivity", cl);
            XposedBridge.hookAllMethods(licenseActivityClass, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "LicenseActivity.onCreate intercepted, finishing activity");
                    android.app.Activity activity = (android.app.Activity) param.thisObject;
                    activity.finish();
                    param.setResult(null);
                }
            });
            Log.i(TAG, "PairIP LicenseActivity hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "LicenseActivity class not found: " + e.getMessage());
        }

        try {
            Class<?> licV2ListenerClass = XposedHelpers.findClass("com.pairip.licensecheck.ILicenseV2ResultListener$Stub", cl);
            XposedBridge.hookAllMethods(licV2ListenerClass, "onTransact", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "ILicenseV2ResultListener.onTransact intercepted");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.hasThrowable()) {
                        Throwable t = param.getThrowable();
                        if (t.getClass().getName().contains("LicenseCheckException")) {
                            Log.d(TAG, "LicenseCheckException intercepted in onTransact, suppressing");
                            param.setThrowable(null);
                        }
                    }
                }
            });
            Log.i(TAG, "ILicenseV2ResultListener hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "ILicenseV2ResultListener class not found: " + e.getMessage());
        }
    }

    private static void hookGooglePlayServices(ClassLoader cl) {
        try {
            Class<?> gmsClass = XposedHelpers.findClass("com.google.android.gms.common.GooglePlayServicesUtil", cl);
            XposedBridge.hookAllMethods(gmsClass, "isGooglePlayServicesAvailable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "GooglePlayServicesUtil.isGooglePlayServicesAvailable -> SUCCESS");
                    param.setResult(0);
                }
            });
            Log.i(TAG, "GooglePlayServicesUtil hook installed (hookAllMethods)");
        } catch (Throwable e) {
            Log.w(TAG, "GooglePlayServicesUtil class not found: " + e.getMessage());
        }
    }

    private static void hookOrientation(ClassLoader cl) {
        try {
            Field tokenField = Activity.class.getDeclaredField("mToken");
            tokenField.setAccessible(true);

            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    try {
                        IBinder token = (IBinder) tokenField.get(activity);
                        if (token == null) return;

                        ActivityInfo targetInfo = com.vcore.fake.service.HCallbackProxy.peekTargetActivityInfo(token);
                        if (targetInfo == null) return;

                        int orientation = targetInfo.screenOrientation;
                        if (orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                || orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) {
                            return;
                        }

                        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LOCKED) {
                            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        }

                        int current = activity.getRequestedOrientation();
                        if (current != orientation) {
                            Log.d(TAG, "Setting orientation " + orientation + " for " + targetInfo.name);
                            activity.setRequestedOrientation(orientation);
                        }
                    } catch (Throwable e) {
                        Log.w(TAG, "hookOrientation error: " + e.getMessage());
                    }
                }
            });
            Log.i(TAG, "Activity.onResume orientation hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookOrientation failed: " + e.getMessage());
        }
    }

    private static void hookRaspBypass(ClassLoader cl) {
        hookDebugCheck();
        hookRuntimeExec(cl);
        hookFileExists(cl);
        hookMapsReading(cl);
        hookNativeLoad(cl);
        hookGoogleApiClient(cl);
        hookRunningProcesses();
    }

    private static void hookDebugCheck() {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "isDebuggerConnected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
            Log.i(TAG, "Debug.isDebuggerConnected hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookDebugCheck failed: " + e.getMessage());
        }
    }

    private static void hookRuntimeExec(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String cmd = (String) param.args[0];
                    if (cmd != null) {
                        String lower = cmd.toLowerCase();
                        if (lower.contains("which") && lower.contains("su")) {
                            param.setResult(null);
                        } else if (lower.contains("magisk") || lower.contains("supersu")) {
                            param.setResult(null);
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String[] cmds = (String[]) param.args[0];
                    if (cmds != null && cmds.length > 0) {
                        String cmd = cmds[0].toLowerCase();
                        if (cmd.contains("which") && (cmds.length > 1 && cmds[1].contains("su"))) {
                            param.setResult(null);
                        } else if (cmd.contains("magisk") || cmd.contains("supersu")) {
                            param.setResult(null);
                        }
                    }
                }
            });
            Log.i(TAG, "Runtime.exec hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookRuntimeExec failed: " + e.getMessage());
        }
    }

    private static void hookFileExists(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File file = (File) param.thisObject;
                    String path = file.getAbsolutePath();
                    if (path.contains("/proc/") && path.contains("/maps")) {
                        return;
                    }
                    if (path.contains("xposed") || path.contains("Xposed") ||
                        path.contains("/su") || path.contains("magisk") ||
                        path.contains("Magisk") || path.contains("supersu") ||
                        path.contains("SuperSU") || path.contains("Superuser")) {
                        param.setResult(false);
                    }
                }
            });
            Log.i(TAG, "File.exists hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookFileExists failed: " + e.getMessage());
        }
    }

    private static void hookMapsReading(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(BufferedReader.class, "readLine", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String line = (String) param.getResult();
                    if (line == null) return;
                    String lower = line.toLowerCase();
                    if (lower.contains("xposed") || lower.contains("libxposed") ||
                        lower.contains("edxposed") || lower.contains("lsposed") ||
                        lower.contains("libpine") || lower.contains("liblspd") ||
                        lower.contains("substrate") || lower.contains("frida") ||
                        lower.contains("gadget") || lower.contains("gmain")) {
                        param.setResult(null);
                    }
                }
            });
            Log.i(TAG, "BufferedReader.readLine hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookMapsReading failed: " + e.getMessage());
        }
    }

    private static void hookNativeLoad(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "loadLibrary", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String libName = (String) param.args[0];
                    if (libName != null) {
                        String lower = libName.toLowerCase();
                        if (lower.contains("xposed") || lower.contains("frida") ||
                            lower.contains("substrate") || lower.contains("gadget")) {
                            Log.w(TAG, "Blocked suspicious native library load: " + libName);
                            param.setResult(null);
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Runtime.class, "loadLibrary0", ClassLoader.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String libName = (String) param.args[1];
                    if (libName != null) {
                        String lower = libName.toLowerCase();
                        if (lower.contains("xposed") || lower.contains("frida") ||
                            lower.contains("substrate") || lower.contains("gadget")) {
                            Log.w(TAG, "Blocked suspicious native library load (0): " + libName);
                            param.setResult(null);
                        }
                    }
                }
            });
            Log.i(TAG, "Runtime.loadLibrary hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookNativeLoad failed: " + e.getMessage());
        }
    }

    private static void hookGoogleApiClient(ClassLoader cl) {
        try {
            Class<?> gmsCoreUtil = XposedHelpers.findClass("com.google.android.gms.common.GoogleApiAvailability", cl);
            XposedBridge.hookAllMethods(gmsCoreUtil, "isGooglePlayServicesAvailable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "GoogleApiAvailability.isGooglePlayServicesAvailable -> SUCCESS");
                    param.setResult(0);
                }
            });
            Log.i(TAG, "GoogleApiAvailability hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "GoogleApiAvailability class not found: " + e.getMessage());
        }

        try {
            Class<?> googleApiClient = XposedHelpers.findClass("com.google.android.gms.common.api.GoogleApiClient", cl);
            XposedBridge.hookAllMethods(googleApiClient, "blockingConnect", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "GoogleApiClient.blockingConnect intercepted, returning SUCCESS");
                    Class<?> resultClass = XposedHelpers.findClass("com.google.android.gms.common.ConnectionResult", cl);
                    java.lang.reflect.Constructor<?> ctor = resultClass.getConstructor(int.class);
                    param.setResult(ctor.newInstance(0));
                }
            });
            Log.i(TAG, "GoogleApiClient.blockingConnect hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "GoogleApiClient class not found: " + e.getMessage());
        }

        hookGoogleSignatureVerifier(cl);
    }

    private static void hookRunningProcesses() {
        try {
            XposedHelpers.findAndHookMethod(ActivityManager.class, "getRunningAppProcesses", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    List<ActivityManager.RunningAppProcessInfo> result =
                            (List<ActivityManager.RunningAppProcessInfo>) param.getResult();
                    if (result == null) return;
                    for (ActivityManager.RunningAppProcessInfo proc : result) {
                        if (proc.processName != null && proc.processName.contains("virtualinject")) {
                            proc.processName = proc.processName.replace("virtualinject", "criticalops");
                        }
                        if (proc.pkgList != null) {
                            for (int i = 0; i < proc.pkgList.length; i++) {
                                if (proc.pkgList[i] != null && proc.pkgList[i].contains("virtualinject")) {
                                    proc.pkgList[i] = proc.pkgList[i].replace("virtualinject", "criticalops");
                                }
                            }
                        }
                    }
                }
            });
            Log.i(TAG, "getRunningAppProcesses hook installed");
        } catch (Throwable e) {
            Log.w(TAG, "hookRunningProcesses failed: " + e.getMessage());
        }
    }

    private static void hookGoogleSignatureVerifier(ClassLoader cl) {
        try {
            Class<?> verifierClass = XposedHelpers.findClass("com.google.android.gms.common.GoogleSignatureVerifier", cl);
            for (java.lang.reflect.Method m : verifierClass.getDeclaredMethods()) {
                String name = m.getName();
                if (name.startsWith("verify") || name.equals("isGooglePlayServicesSigned")) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Log.d(TAG, "GoogleSignatureVerifier." + m.getName() + " intercepted, returning true");
                            param.setResult(true);
                        }
                    });
                }
            }
            Log.i(TAG, "GoogleSignatureVerifier hooks installed");
        } catch (Throwable e) {
            Log.w(TAG, "GoogleSignatureVerifier class not found: " + e.getMessage());
        }
    }
}
