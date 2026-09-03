package com.vcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.utils.Slog;

public class ISystemUpdateProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public ISystemUpdateProxy() {
        IBinder binder = ServiceManager.getService.call("system_update");
        if (binder == null) {
            Slog.d("ISystemUpdateProxy", "system_update not found, skipping hook");
            mBaseBinder = null;
        } else {
            mBaseBinder = binder;
        }
    }

    @Override
    protected Object getWho() {
        if (mBaseBinder == null) {
            return null;
        }
        // system_update service interface
        return mBaseBinder;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("system_update");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }
}