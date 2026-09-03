package com.vcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.vivo.ISystemDefenceManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.service.base.PkgMethodProxy;
import com.vcore.utils.Slog;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:30
 **/
public class ISystemDefenceManagerProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public ISystemDefenceManagerProxy() {
        IBinder binder = ServiceManager.getService.call("system_defence_service");
        if (binder == null) {
            Slog.d("ISystemDefenceManagerProxy", "system_defence_service not found, skipping hook");
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
        return ISystemDefenceManager.Stub.asInterface.call(mBaseBinder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("system_defence_service");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("checkTransitionTimoutErrorDefence"));
        addMethodHook(new PkgMethodProxy("checkSkipKilledByRemoveTask"));
        addMethodHook(new PkgMethodProxy("checkSmallIconNULLPackage"));
        addMethodHook(new PkgMethodProxy("checkDelayUpdate"));
        addMethodHook(new PkgMethodProxy("onSetActivityResumed"));
        addMethodHook(new PkgMethodProxy("checkReinstallPacakge"));
        addMethodHook(new PkgMethodProxy("reportFgCrashData"));
    }
}
