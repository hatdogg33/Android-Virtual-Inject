package com.vcore.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class GmsConfig {
    private static final String TAG = "GmsConfig";
    private static final String PREFS_NAME = "gms_config";
    private static final String CONFIG_DIR = "gms_config";
    
    private static final String KEY_ANDROID_ID = "android_id";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_GAIA_ID = "gaia_id";
    private static final String KEY_SECURITY_TOKEN = "security_token";
    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String KEY_ACCOUNT_TYPE = "account_type";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";
    private static final String KEY_DEVICE_REGISTERED = "device_registered";
    private static final String KEY_CLOUD_MESSAGING_TOKEN = "cloud_messaging_token";
    
    private final Context mContext;
    private final int mUserId;
    private final SharedPreferences mPrefs;
    private final File mConfigDir;
    
    public GmsConfig(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
        this.mPrefs = context.getSharedPreferences(PREFS_NAME + "_" + userId, Context.MODE_PRIVATE);
        this.mConfigDir = new File(context.getFilesDir(), CONFIG_DIR + "/" + userId);
        mConfigDir.mkdirs();
    }
    
    public String getAndroidId() {
        String androidId = mPrefs.getString(KEY_ANDROID_ID, null);
        if (androidId == null) {
            androidId = generateAndroidId();
            mPrefs.edit().putString(KEY_ANDROID_ID, androidId).apply();
        }
        return androidId;
    }
    
    public String getDeviceId() {
        String deviceId = mPrefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = generateDeviceId();
            mPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }
    
    public String getGaiaId() {
        return mPrefs.getString(KEY_GAIA_ID, null);
    }
    
    public void setGaiaId(String gaiaId) {
        mPrefs.edit().putString(KEY_GAIA_ID, gaiaId).apply();
    }
    
    public String getSecurityToken() {
        return mPrefs.getString(KEY_SECURITY_TOKEN, null);
    }
    
    public void setSecurityToken(String token) {
        mPrefs.edit().putString(KEY_SECURITY_TOKEN, token).apply();
    }
    
    public String getAccountName() {
        return mPrefs.getString(KEY_ACCOUNT_NAME, null);
    }
    
    public void setAccountName(String accountName) {
        mPrefs.edit().putString(KEY_ACCOUNT_NAME, accountName).apply();
    }
    
    public String getAccountType() {
        return mPrefs.getString(KEY_ACCOUNT_TYPE, "com.google");
    }
    
    public void setAccountType(String accountType) {
        mPrefs.edit().putString(KEY_ACCOUNT_TYPE, accountType).apply();
    }
    
    public String getAuthToken() {
        return mPrefs.getString(KEY_AUTH_TOKEN, null);
    }
    
    public void setAuthToken(String token) {
        mPrefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }
    
    public String getRefreshToken() {
        return mPrefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    public void setRefreshToken(String token) {
        mPrefs.edit().putString(KEY_REFRESH_TOKEN, token).apply();
    }
    
    public long getTokenExpiry() {
        return mPrefs.getLong(KEY_TOKEN_EXPIRY, 0);
    }
    
    public void setTokenExpiry(long expiry) {
        mPrefs.edit().putLong(KEY_TOKEN_EXPIRY, expiry).apply();
    }
    
    public boolean isSetupComplete() {
        return mPrefs.getBoolean(KEY_SETUP_COMPLETE, false);
    }
    
    public void setSetupComplete(boolean complete) {
        mPrefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply();
    }
    
    public boolean isDeviceRegistered() {
        return mPrefs.getBoolean(KEY_DEVICE_REGISTERED, false);
    }
    
    public void setDeviceRegistered(boolean registered) {
        mPrefs.edit().putBoolean(KEY_DEVICE_REGISTERED, registered).apply();
    }
    
    public String getCloudMessagingToken() {
        return mPrefs.getString(KEY_CLOUD_MESSAGING_TOKEN, null);
    }
    
    public void setCloudMessagingToken(String token) {
        mPrefs.edit().putString(KEY_CLOUD_MESSAGING_TOKEN, token).apply();
    }
    
    public boolean isTokenExpired() {
        long expiry = getTokenExpiry();
        return expiry > 0 && System.currentTimeMillis() > expiry;
    }
    
    public boolean hasValidToken() {
        String token = getAuthToken();
        return token != null && !token.isEmpty() && !isTokenExpired();
    }
    
    public void clearTokens() {
        mPrefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply();
    }
    
    public void clearAll() {
        mPrefs.edit().clear().apply();
        deleteConfigFiles();
    }
    
    private String generateAndroidId() {
        String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null && !androidId.equals("9774d56d682e549c")) {
            return androidId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    private String generateDeviceId() {
        String deviceId = Build.BOARD + Build.BRAND + Build.DEVICE + Build.HARDWARE;
        return UUID.nameUUIDFromBytes(deviceId.getBytes()).toString();
    }
    
    private void deleteConfigFiles() {
        File[] files = mConfigDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
    
    public File getConfigDir() {
        return mConfigDir;
    }
    
    public Properties loadProperties(String fileName) {
        Properties props = new Properties();
        File file = new File(mConfigDir, fileName);
        
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load properties: " + fileName, e);
            }
        }
        
        return props;
    }
    
    public void saveProperties(String fileName, Properties props) {
        File file = new File(mConfigDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, null);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save properties: " + fileName, e);
        }
    }
    
    public static GmsConfig getInstance(Context context, int userId) {
        return new GmsConfig(context, userId);
    }
}
