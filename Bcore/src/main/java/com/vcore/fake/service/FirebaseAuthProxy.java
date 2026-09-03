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

    public FirebaseAuthProxy() {
        super(ServiceManager.getService.call(getServiceName()));
    }

    private static String getServiceName() {
        return "com.google.android.gms.auth.fallback.FallbackAuthenticationService";
    }

    @Override
    protected Object getWho() {
        return ServiceManager.getService.call(getServiceName());
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(getServiceName());
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new MethodHook() {
            @Override
            protected Object hook(Object who, Method method, Object[] args) throws Throwable {
                Log.d(TAG, "Firebase Auth hook: " + method.getName());
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
        return false;
    }
}
