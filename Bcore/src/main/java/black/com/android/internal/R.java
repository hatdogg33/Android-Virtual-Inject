package black.com.android.internal;

import android.util.Log;

import black.Reflector;

public class R {
    private static final String TAG = "BlackR";

    public static class styleable {
        public static Reflector.FieldWrapper<Integer[]> AccountAuthenticator;
        public static Reflector.FieldWrapper<Integer> AccountAuthenticator_accountPreferences;
        public static Reflector.FieldWrapper<Integer> AccountAuthenticator_accountType;
        public static Reflector.FieldWrapper<Integer> AccountAuthenticator_customTokens;
        public static Reflector.FieldWrapper<Integer> AccountAuthenticator_icon;
        public static Reflector.FieldWrapper<Integer> AccountAuthenticator_label;
        public static Reflector.FieldWrapper<Integer> AccountAuthenticator_smallIcon;
        public static Reflector.FieldWrapper<Integer[]> Window;
        public static Reflector.FieldWrapper<Integer> Window_windowFullscreen;
        public static Reflector.FieldWrapper<Integer> Window_windowIsTranslucent;
        public static Reflector.FieldWrapper<Integer> Window_windowShowWallpaper;

        static {
            try {
                Reflector REF = Reflector.on("com.android.internal.R$styleable");
                if (REF != null && REF.getClazz() != null) {
                    AccountAuthenticator = REF.field("AccountAuthenticator");
                    AccountAuthenticator_accountPreferences = REF.field("AccountAuthenticator_accountPreferences");
                    AccountAuthenticator_accountType = REF.field("AccountAuthenticator_accountType");
                    AccountAuthenticator_customTokens = REF.field("AccountAuthenticator_customTokens");
                    AccountAuthenticator_icon = REF.field("AccountAuthenticator_icon");
                    AccountAuthenticator_label = REF.field("AccountAuthenticator_label");
                    AccountAuthenticator_smallIcon = REF.field("AccountAuthenticator_smallIcon");
                    Window = REF.field("Window");
                    Window_windowFullscreen = REF.field("Window_windowFullscreen");
                    Window_windowIsTranslucent = REF.field("Window_windowIsTranslucent");
                    Window_windowShowWallpaper = REF.field("Window_windowShowWallpaper");
                } else {
                    Log.w(TAG, "com.android.internal.R$styleable not accessible, styleable fields will be null");
                }
            } catch (Throwable e) {
                Log.w(TAG, "Failed to init R.styleable", e);
            }
        }
    }
}
