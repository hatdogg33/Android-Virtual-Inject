package black.pairip;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
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
            XposedHelpers.findAndHookMethod(
                    "com.pairip.licensecheck.LicenseClient",
                    cl,
                    "processResponse",
                    byte[].class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object response = param.args[0];
                            if (response == null) return;

                            try {
                                String responseStr = new String((byte[]) response);
                                if (responseStr.contains("responseCode")) {
                                    Log.d(TAG, "PairIP license response intercepted, bypassing check");
                                    param.setResult(null);
                                }
                            } catch (Throwable ignored) { }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.hasThrowable()) {
                                Throwable throwable = param.getThrowable();
                                if (throwable.getClass().getName().contains("LicenseCheckException")) {
                                    Log.d(TAG, "PairIP LicenseCheckException caught, bypassing");
                                    param.setThrowable(null);
                                    param.setResult(null);
                                }
                            }
                        }
                    }
            );
            Log.i(TAG, "PairIP LicenseClient hook installed");
        } catch (Throwable e) {
            // Class not present, expected for non-PairIP apps
        }
    }

    private static void hookGooglePlayServices(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.google.android.gms.common.GooglePlayServicesUtil",
                    cl,
                    "isGooglePlayServicesAvailable",
                    android.content.Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Log.d(TAG, "GooglePlayServicesUtil.isGooglePlayServicesAvailable -> SUCCESS");
                            param.setResult(0);
                        }
                    }
            );
            Log.i(TAG, "GooglePlayServicesUtil hook installed");
        } catch (Throwable e) {
            // Class not present
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.google.android.gms.common.GooglePlayServicesUtil",
                    cl,
                    "isGooglePlayServicesAvailable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Log.d(TAG, "GooglePlayServicesUtil.isGooglePlayServicesAvailable() -> SUCCESS");
                            param.setResult(0);
                        }
                    }
            );
        } catch (Throwable e) {
            // Class not present
        }
    }
}
