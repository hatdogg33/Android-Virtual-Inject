package com.vcore.fake.service;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.os.ServiceManager;
import com.vcore.BlackBoxCore;
import com.vcore.core.GmsConfig;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.hook.MethodHook;
import com.vcore.fake.hook.ProxyMethod;
import com.vcore.utils.Slog;

public class IGoogleServicesProxy extends BinderInvocationStub {
    private static final String TAG = "IGoogleServicesProxy";
    private final IBinder mBaseBinder;

    public IGoogleServicesProxy() {
        IBinder binder = ServiceManager.getService.call("com.google.android.gms.googlehelp.internal.ITracingService");
        mBaseBinder = binder;
    }

    @Override
    protected Object getWho() {
        if (mBaseBinder == null) return null;
        return mBaseBinder;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("com.google.android.gms.googlehelp.internal.ITracingService");
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        if (mBaseBinder == null) return;

        addMethodHook(new MethodHook() {
            @Override
            protected Object hook(Object who, Method method, Object[] args) throws Throwable {
                Log.d(TAG, "Intercepted GMS method: " + method.getName());
                try {
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Log.w(TAG, "GMS call failed, returning defaults: " + e.getMessage());
                    return null;
                }
            }
        });
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }

    public static boolean isGoogleServicesPackage(String packageName) {
        return "com.google.android.gms".equals(packageName);
    }
}
