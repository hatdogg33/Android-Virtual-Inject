package com.vcore.fake.service;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.os.ServiceManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.hook.MethodHook;
import com.vcore.utils.Slog;

public class FirebaseAuthProxy extends BinderInvocationStub {
    private static final String TAG = "FirebaseAuthProxy";
    private final IBinder mBaseBinder;

    public FirebaseAuthProxy() {
        IBinder binder = ServiceManager.getService.call("com.google.android.gms.auth.fallback.FallbackAuthenticationService");
        mBaseBinder = binder;
    }

    @Override
    protected Object getWho() {
        if (mBaseBinder == null) return null;
        return mBaseBinder;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("com.google.android.gms.auth.fallback.FallbackAuthenticationService");
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        if (mBaseBinder == null) return;

        addMethodHook(new MethodHook() {
            @Override
            protected Object hook(Object who, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                Log.d(TAG, "Firebase Auth hook: " + name);
                try {
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Log.w(TAG, "Firebase Auth call failed: " + e.getMessage());
                    return null;
                }
            }
        });
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }
}
