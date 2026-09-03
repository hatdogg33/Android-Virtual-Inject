package com.vcore.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.vcore.BlackBoxCore;

public class AccountHelper {
    private static final String TAG = "AccountHelper";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String FIREBASE_ACCOUNT_TYPE = "com.google";
    
    private static final ConcurrentHashMap<String, Account> sAccountCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> sTokenCache = new ConcurrentHashMap<>();
    
    private final Context mContext;
    private final int mUserId;
    private final AccountManager mAccountManager;
    private final GmsConfig mConfig;
    
    public AccountHelper(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
        this.mAccountManager = AccountManager.get(context);
        this.mConfig = GmsConfig.getInstance(context, userId);
    }
    
    public Account getGoogleAccount() {
        String accountName = mConfig.getAccountName();
        if (accountName == null) {
            return null;
        }
        
        String cacheKey = accountName + ":" + GOOGLE_ACCOUNT_TYPE;
        if (sAccountCache.containsKey(cacheKey)) {
            return sAccountCache.get(cacheKey);
        }
        
        Account account = new Account(accountName, GOOGLE_ACCOUNT_TYPE);
        sAccountCache.put(cacheKey, account);
        
        return account;
    }
    
    public Account createGoogleAccount(String accountName) {
        if (accountName == null) {
            accountName = "google_user_" + System.currentTimeMillis();
        }
        
        mConfig.setAccountName(accountName);
        mConfig.setAccountType(GOOGLE_ACCOUNT_TYPE);
        
        Account account = new Account(accountName, GOOGLE_ACCOUNT_TYPE);
        
        String cacheKey = accountName + ":" + GOOGLE_ACCOUNT_TYPE;
        sAccountCache.put(cacheKey, account);
        
        Log.i(TAG, "Created Google account: " + accountName);
        
        return account;
    }
    
    public boolean addGoogleAccount(Account account, String password) {
        if (account == null) {
            return false;
        }
        
        try {
            mAccountManager.addAccountExplicitly(account, password, null);
            
            mConfig.setAccountName(account.name);
            mConfig.setAccountType(account.type);
            
            if (password != null) {
                mConfig.setSecurityToken(password);
            }
            
            String cacheKey = account.name + ":" + account.type;
            sAccountCache.put(cacheKey, account);
            
            Log.i(TAG, "Added Google account: " + account.name);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add Google account", e);
            return false;
        }
    }
    
    public boolean removeGoogleAccount(Account account) {
        if (account == null) {
            return false;
        }
        
        try {
            mAccountManager.removeAccount(account, null, null);
            
            mConfig.clearAll();
            
            String cacheKey = account.name + ":" + account.type;
            sAccountCache.remove(cacheKey);
            sTokenCache.remove(cacheKey);
            
            Log.i(TAG, "Removed Google account: " + account.name);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove Google account", e);
            return false;
        }
    }
    
    public String getAuthToken(Account account, String authTokenType) {
        if (account == null) {
            return null;
        }
        
        String cacheKey = account.name + ":" + authTokenType;
        if (sTokenCache.containsKey(cacheKey)) {
            return sTokenCache.get(cacheKey);
        }
        
        String token = mConfig.getAuthToken();
        if (token != null) {
            sTokenCache.put(cacheKey, token);
        }
        
        return token;
    }
    
    public void setAuthToken(Account account, String authTokenType, String token) {
        if (account == null || token == null) {
            return;
        }
        
        String cacheKey = account.name + ":" + authTokenType;
        sTokenCache.put(cacheKey, token);
        
        if ("firebase".equals(authTokenType) || "id_token".equals(authTokenType)) {
            mConfig.setAuthToken(token);
        }
        
        Log.i(TAG, "Set auth token for: " + authTokenType);
    }
    
    public void invalidateAuthToken(String accountName, String authTokenType) {
        if (accountName == null || authTokenType == null) {
            return;
        }
        
        String cacheKey = accountName + ":" + authTokenType;
        sTokenCache.remove(cacheKey);
        
        Log.i(TAG, "Invalidated token for: " + authTokenType);
    }
    
    public String getPassword(Account account) {
        if (account == null) {
            return null;
        }
        
        return mConfig.getSecurityToken();
    }
    
    public void setPassword(Account account, String password) {
        if (account == null) {
            return;
        }
        
        mConfig.setSecurityToken(password);
        
        Log.i(TAG, "Set password for account: " + account.name);
    }
    
    public String getUserData(Account account, String key) {
        if (account == null || key == null) {
            return null;
        }
        
        if ("gaia_id".equals(key)) {
            return mConfig.getGaiaId();
        } else if ("auth_token".equals(key)) {
            return mConfig.getAuthToken();
        } else if ("refresh_token".equals(key)) {
            return mConfig.getRefreshToken();
        } else if ("device_id".equals(key)) {
            return mConfig.getDeviceId();
        } else if ("android_id".equals(key)) {
            return mConfig.getAndroidId();
        }
        
        return null;
    }
    
    public void setUserData(Account account, String key, String value) {
        if (account == null || key == null) {
            return;
        }
        
        if ("gaia_id".equals(key)) {
            mConfig.setGaiaId(value);
        } else if ("auth_token".equals(key)) {
            mConfig.setAuthToken(value);
        } else if ("refresh_token".equals(key)) {
            mConfig.setRefreshToken(value);
        }
        
        Log.i(TAG, "Set user data for key: " + key);
    }
    
    public boolean isSignedIn() {
        Account account = getGoogleAccount();
        return account != null && mConfig.hasValidToken();
    }
    
    public void signOut() {
        Account account = getGoogleAccount();
        if (account != null) {
            removeGoogleAccount(account);
        }
        
        mConfig.clearTokens();
        sAccountCache.clear();
        sTokenCache.clear();
        
        Log.i(TAG, "Signed out");
    }
    
    public Account[] getAccounts() {
        try {
            return mAccountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get accounts", e);
            return new Account[0];
        }
    }
    
    public boolean hasAccount(Account account) {
        if (account == null) {
            return false;
        }
        
        Account[] accounts = getAccounts();
        for (Account existingAccount : accounts) {
            if (existingAccount.name.equals(account.name) && 
                existingAccount.type.equals(account.type)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static void clearCaches() {
        sAccountCache.clear();
        sTokenCache.clear();
    }
    
    public static boolean isGoogleAccountType(String accountType) {
        return GOOGLE_ACCOUNT_TYPE.equals(accountType);
    }
    
    public static boolean isFirebaseAccountType(String accountType) {
        return FIREBASE_ACCOUNT_TYPE.equals(accountType);
    }
}
