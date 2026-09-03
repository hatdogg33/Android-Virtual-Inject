package com.vcore.fake.service;

import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.vcore.BlackBoxCore;
import com.vcore.core.GmsConfig;
import com.vcore.core.GmsCore;
import com.vcore.fake.hook.MethodHook;
import com.vcore.fake.hook.ProxyMethod;

public class IGoogleServicesProxy extends MethodHook {
    private static final String TAG = "IGoogleServicesProxy";
    private static final String GOOGLE_SERVICES_PACKAGE = "com.google.android.gms";
    
    private static final Map<String, String> SERVICE_BINDINGS = new HashMap<>();
    
    static {
        // Google Play Services bindings
        SERVICE_BINDINGS.put("com.google.android.gms.auth.APPAUTH", "auth_apppauth");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.CREDENTIAL", "auth_credential");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.MAXENT", "auth_maxent");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.PROXY", "auth_proxy");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.SIGN_IN", "auth_sign_in");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE", "play_games");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_SNAPSHOT", "play_games_snapshot");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_MULTIPLAYER", "play_games_multiplayer");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_REQUESTS", "play_games_requests");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_STATS", "play_games_stats");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_NOTIFICATIONS", "play_games_notifications");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_VOICE", "play_games_voice");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_IMAGE", "play_games_image");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_ACHIEVEMENTS", "play_games_achievements");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_LEADERBOARDS", "play_games_leaderboards");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_EVENTS", "play_games_events");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_SAVED_GAMES", "play_games_saved_games");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_NEARBY", "play_games_nearby");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_CAST", "play_games_cast");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_MATURE", "play_games_mature");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_PLAYER_STATS", "play_games_player_stats");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_TURN_BASED_MULTIPLAYER", "play_games_turn_based");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_REAL_TIME_MULTIPLAYER", "play_games_real_time");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_APP_STATE", "play_games_app_state");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_PLUS", "play_games_plus");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_DRIVE", "play_games_drive");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE_SNAPSHOTS", "play_games_snapshots");
        SERVICE_BINDINGS.put("com.google.android.gms.games.PLAY_GAMES_SERVICE喹", "play_games_advertising");
        
        // Firebase bindings
        SERVICE_BINDINGS.put("com.google.android.gms.auth.key.AUTH_KEY", "auth_key");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.key.GET_TOKEN", "auth_get_token");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.key.REVOKE_ACCESS", "auth_revoke");
        SERVICE_BINDINGS.put("com.google.android.gms.auth.key.CLEAR_TOKEN", "auth_clear_token");
        SERVICE_BINDINGS.put("com.google.android.gms.gcm.GCM_SERVICE", "gcm_service");
        SERVICE_BINDINGS.put("com.google.android.gms.gcm.MESSAGING_EVENT", "gcm_messaging_event");
        SERVICE_BINDINGS.put("com.google.android.gms.iid.INSTANCE_ID_SERVICE", "iid_service");
        
        // Location services
        SERVICE_BINDINGS.put("com.google.android.gms.location.internal.LOCATION_MANAGER", "location_manager");
        SERVICE_BINDINGS.put("com.google.android.gms.location.internal.FUSED_PROVIDER", "fused_provider");
        
        // Wearable services
        SERVICE_BINDINGS.put("com.google.android.gms.wearable.BIND", "wearable_bind");
    }
    
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Log.d(TAG, "Hooking Google Services method: " + methodName);
        
        // Handle service binding
        if ("bind".equals(methodName) || "getService".equals(methodName)) {
            return handleServiceBinding(args);
        }
        
        // Handle device registration
        if ("register".equals(methodName) || "getDeviceId".equals(methodName)) {
            return handleDeviceRegistration(args);
        }
        
        // Handle authentication
        if ("getAuthToken".equals(methodName) || "authenticate".equals(methodName)) {
            return handleAuthentication(args);
        }
        
        // Handle token refresh
        if ("refreshToken".equals(methodName) || "invalidateToken".equals(methodName)) {
            return handleTokenRefresh(args);
        }
        
        // Default: pass through to original method
        return method.invoke(who, args);
    }
    
    private Object handleServiceBinding(Object[] args) {
        Log.d(TAG, "Handling Google Services binding");
        
        if (args != null && args.length > 0) {
            String serviceName = args[0].toString();
            Log.d(TAG, "Binding to service: " + serviceName);
            
            // Check if service is available
            if (SERVICE_BINDINGS.containsKey(serviceName)) {
                Log.d(TAG, "Service available: " + serviceName);
                return true;
            }
        }
        
        return true;
    }
    
    private Object handleDeviceRegistration(Object[] args) {
        Log.d(TAG, "Handling device registration");
        
        GmsConfig config = GmsConfig.getInstance(BlackBoxCore.getContext(), 
            BlackBoxCore.get().getHostUserId());
        
        // Return cached device ID if available
        String deviceId = config.getDeviceId();
        if (deviceId != null) {
            return deviceId;
        }
        
        // Generate new device ID
        deviceId = config.getDeviceId();
        config.setDeviceRegistered(true);
        
        return deviceId;
    }
    
    private Object handleAuthentication(Object[] args) {
        Log.d(TAG, "Handling Google authentication");
        
        GmsConfig config = GmsConfig.getInstance(BlackBoxCore.getContext(), 
            BlackBoxCore.get().getHostUserId());
        
        // Check for valid token
        if (config.hasValidToken()) {
            return config.getAuthToken();
        }
        
        // Return empty token (will be refreshed later)
        return "";
    }
    
    private Object handleTokenRefresh(Object[] args) {
        Log.d(TAG, "Handling token refresh");
        
        GmsConfig config = GmsConfig.getInstance(BlackBoxCore.getContext(), 
            BlackBoxCore.get().getHostUserId());
        
        // Clear existing token
        config.clearTokens();
        
        // Return empty token
        return "";
    }
    
    public static boolean isGoogleServicesPackage(String packageName) {
        return GOOGLE_SERVICES_PACKAGE.equals(packageName);
    }
    
    public static Map<String, String> getServiceBindings() {
        return new HashMap<>(SERVICE_BINDINGS);
    }
}
