package com.vcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.vivo.ISuperResolutionManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.service.base.PkgMethodProxy;
import com.vcore.utils.Slog;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:26
 **/
public class ISuperResolutionManagerProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public ISuperResolutionManagerProxy() {
        IBinder binder = ServiceManager.getService.call("SuperResolutionManager");
        if (binder == null) {
            Slog.d("ISuperResolutionManagerProxy", "SuperResolutionManager not found, skipping hook");
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
        return ISuperResolutionManager.Stub.asInterface.call(mBaseBinder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("SuperResolutionManager");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("registerPackageSettingStateChangeListener"));
        addMethodHook(new PkgMethodProxy("unRegisterPackageSettingStateChangeListener"));
        addMethodHook(new PkgMethodProxy("registerSuperResolutionStateChange"));
        addMethodHook(new PkgMethodProxy("unRegisterSuperResolutionStateChange"));
        addMethodHook(new PkgMethodProxy("getPackageSettingState"));
        addMethodHook(new PkgMethodProxy("putPackageSettingState"));
    }
}