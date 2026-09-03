package com.vcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.vivo.IPhysicalFlingManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.service.base.PkgMethodProxy;
import com.vcore.utils.Slog;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:11
 **/
public class IPhysicalFlingManagerProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public IPhysicalFlingManagerProxy() {
        IBinder binder = ServiceManager.getService.call("physical_fling_service");
        if (binder == null) {
            Slog.d("IPhysicalFlingManagerProxy", "physical_fling_service not found, skipping hook");
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
        return IPhysicalFlingManager.Stub.asInterface.call(mBaseBinder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("physical_fling_service");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("isSupportPhysicalFling"));
    }
}
