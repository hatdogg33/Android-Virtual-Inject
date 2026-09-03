package com.vcore.fake.service;

import android.os.IBinder;

import black.android.net.wifi.IWifiManager;
import black.android.os.ServiceManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.utils.Slog;

public class IWifiScannerProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public IWifiScannerProxy() {
        IBinder binder = ServiceManager.getService.call("wifiscanner");
        if (binder == null) {
            Slog.d("IWifiScannerProxy", "wifiscanner not found, skipping hook");
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
        return IWifiManager.Stub.asInterface.call(mBaseBinder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("wifiscanner");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }
}