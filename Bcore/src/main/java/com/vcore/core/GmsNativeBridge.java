package com.vcore.core;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class GmsNativeBridge {
    private static final String TAG = "GmsNativeBridge";
    private static GmsNativeBridge sInstance;
    private boolean mInitialized;
    
    static {
        System.loadLibrary("gms_bridge");
    }
    
    // Native methods
    private native boolean nativeInitialize(String baseDir);
    private native void nativeShutdown();
    private native boolean nativeStoreToken(String accountName, String authToken, 
                                           String refreshToken, String idToken,
                                           long expiryTime, int tokenType);
    private native TokenData nativeLoadToken(String accountName);
    private native boolean nativeDeleteToken(String accountName);
    private native boolean nativeClearAllTokens();
    private native boolean nativeStoreDeviceInfo(String androidId, String deviceId,
                                                String gaiaId, String model,
                                                String manufacturer, String brand);
    private native DeviceInfo nativeLoadDeviceInfo();
    private native String nativeEncryptData(String data);
    private native String nativeDecryptData(String encryptedData);
    private native String nativeCalculateSha256(String data);
    private native String nativeGenerateSecureToken(int length);
    
    // Inner classes for native data structures
    public static class TokenData {
        public String accountName;
        public String authToken;
        public String refreshToken;
        public String idToken;
        public long expiryTime;
        public int tokenType;
        
        public boolean isValid() {
            return accountName != null && !accountName.isEmpty() &&
                   authToken != null && !authToken.isEmpty() &&
                   System.currentTimeMillis() < expiryTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
    
    public static class DeviceInfo {
        public String androidId;
        public String deviceId;
        public String gaiaId;
        public String model;
        public String manufacturer;
        public String brand;
    }
    
    private GmsNativeBridge() {
    }
    
    public static synchronized GmsNativeBridge getInstance() {
        if (sInstance == null) {
            sInstance = new GmsNativeBridge();
        }
        return sInstance;
    }
    
    public boolean initialize(Context context) {
        if (mInitialized) {
            return true;
        }
        
        File gmsDir = new File(context.getFilesDir(), "gms_native");
        gmsDir.mkdirs();
        
        mInitialized = nativeInitialize(gmsDir.getAbsolutePath());
        
        if (mInitialized) {
            Log.i(TAG, "Native bridge initialized successfully");
        } else {
            Log.e(TAG, "Failed to initialize native bridge");
        }
        
        return mInitialized;
    }
    
    public void shutdown() {
        if (mInitialized) {
            nativeShutdown();
            mInitialized = false;
            Log.i(TAG, "Native bridge shutdown");
        }
    }
    
    public boolean isInitialized() {
        return mInitialized;
    }
    
    // Token operations
    public boolean storeToken(String accountName, TokenData token) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return false;
        }
        
        return nativeStoreToken(accountName, token.authToken, token.refreshToken,
                               token.idToken, token.expiryTime, token.tokenType);
    }
    
    public TokenData loadToken(String accountName) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return null;
        }
        
        return nativeLoadToken(accountName);
    }
    
    public boolean deleteToken(String accountName) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return false;
        }
        
        return nativeDeleteToken(accountName);
    }
    
    public boolean clearAllTokens() {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return false;
        }
        
        return nativeClearAllTokens();
    }
    
    // Device info operations
    public boolean storeDeviceInfo(DeviceInfo info) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return false;
        }
        
        return nativeStoreDeviceInfo(info.androidId, info.deviceId, info.gaiaId,
                                    info.model, info.manufacturer, info.brand);
    }
    
    public DeviceInfo loadDeviceInfo() {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return null;
        }
        
        return nativeLoadDeviceInfo();
    }
    
    // Encryption operations
    public String encryptData(String data) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return null;
        }
        
        return nativeEncryptData(data);
    }
    
    public String decryptData(String encryptedData) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return null;
        }
        
        return nativeDecryptData(encryptedData);
    }
    
    // Hash operations
    public String calculateSha256(String data) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return null;
        }
        
        return nativeCalculateSha256(data);
    }
    
    // Secure random
    public String generateSecureToken(int length) {
        if (!mInitialized) {
            Log.e(TAG, "Bridge not initialized");
            return null;
        }
        
        return nativeGenerateSecureToken(length);
    }
    
    // Utility methods
    public boolean storeFirebaseToken(String accountName, String authToken, 
                                     String refreshToken, String idToken) {
        TokenData token = new TokenData();
        token.accountName = accountName;
        token.authToken = authToken;
        token.refreshToken = refreshToken;
        token.idToken = idToken;
        token.expiryTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour
        token.tokenType = 1; // Firebase token type
        
        return storeToken(accountName, token);
    }
    
    public boolean storeGoogleToken(String accountName, String authToken,
                                   String refreshToken, String idToken) {
        TokenData token = new TokenData();
        token.accountName = accountName;
        token.authToken = authToken;
        token.refreshToken = refreshToken;
        token.idToken = idToken;
        token.expiryTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour
        token.tokenType = 2; // Google token type
        
        return storeToken(accountName, token);
    }
    
    public String generateFirebaseToken(String accountName) {
        String token = generateSecureToken(64);
        long expiryTime = System.currentTimeMillis() + (60 * 60 * 1000);
        
        storeFirebaseToken(accountName, token, "", "");
        
        return token;
    }
    
    public String generateGoogleAuthToken(String accountName) {
        String token = generateSecureToken(64);
        long expiryTime = System.currentTimeMillis() + (60 * 60 * 1000);
        
        storeGoogleToken(accountName, token, "", "");
        
        return token;
    }
    
    public String generateIdToken(String accountName, String clientId) {
        // Generate a mock ID token
        // In production, this would be a real JWT
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payload = "{\"iss\":\"accounts.google.com\",\"sub\":\"" + accountName.hashCode() + "\","
                        + "\"aud\":\"" + clientId + "\","
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + ","
                        + "\"iat\":" + (System.currentTimeMillis() / 1000) + ","
                        + "\"email\":\"" + accountName + "@gmail.com\","
                        + "\"email_verified\":true}";
        
        String signature = calculateSha256(header + "." + payload);
        
        return encryptData(header) + "." + encryptData(payload) + "." + encryptData(signature);
    }
}
