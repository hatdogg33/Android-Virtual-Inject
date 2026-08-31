package black.pairip;

import android.util.Log;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PairIpHook {
    private static final String TAG = "PairIpHook";

    public static void hookIfPresent(ClassLoader cl) {
        if (cl == null) return;

        hookPairIpLicense(cl);
        hookGooglePlayServices(cl);
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
}
