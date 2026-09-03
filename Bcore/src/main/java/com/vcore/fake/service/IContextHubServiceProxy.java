package com.vcore.fake.service;

import android.os.IBinder;

import black.android.hardware.location.IContextHubService;
import black.android.os.ServiceManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.service.base.ValueMethodProxy;
import com.vcore.utils.Slog;
import com.vcore.utils.compat.BuildCompat;

public class IContextHubServiceProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public IContextHubServiceProxy() {
        String serviceName = getServiceName();
        IBinder binder = ServiceManager.getService.call(serviceName);
        if (binder == null) {
            Slog.d("IContextHubServiceProxy", serviceName + " not found, skipping hook");
            mBaseBinder = null;
        } else {
            mBaseBinder = binder;
        }
    }

    private static String getServiceName() {
        return BuildCompat.isOreo() ? "contexthub" : "contexthub_service";
    }

    @Override
    protected Object getWho() {
        if (mBaseBinder == null) {
            return null;
        }
        return IContextHubService.Stub.asInterface.call(ServiceManager.getService.call(getServiceName()));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(getServiceName());
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("registerCallback", 0));
        addMethodHook(new ValueMethodProxy("getContextHubInfo", null));
        addMethodHook(new ValueMethodProxy("getContextHubHandles", new int[]{}));
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }
}