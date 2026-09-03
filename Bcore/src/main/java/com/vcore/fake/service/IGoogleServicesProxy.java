package com.vcore.fake.service;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.os.ServiceManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.hook.MethodHook;
import com.vcore.utils.Slog;

public class IGoogleServicesProxy extends BinderInvocationStub {
    private static final String TAG = "IGoogleServicesProxy";
    private final String mServiceName;

    public IGoogleServicesProxy() {
        super(ServiceManager.getService.call(getServiceName()));
        mServiceName = getServiceName();
    }

    private static String getServiceName() {
        return "com.google.android.gms.googlehelp.internal.ITracingService";
    }

    @Override
    protected Object getWho() {
        return ServiceManager.getService.call(mServiceName);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(mServiceName);
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new MethodHook() {
            @Override
            protected Object hook(Object who, Method method, Object[] args) throws Throwable {
                Log.d(TAG, "Intercepted GMS method: " + method.getName());
                try {
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Log.w(TAG, "GMS call failed: " + e.getMessage());
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
