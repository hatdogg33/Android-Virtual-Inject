package com.vcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.vivo.IVivoPermissonService;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.service.base.PkgMethodProxy;
import com.vcore.utils.Slog;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:36
 **/
public class IVivoPermissionServiceProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public IVivoPermissionServiceProxy() {
        IBinder binder = ServiceManager.getService.call("vivo_permission_service");
        if (binder == null) {
            Slog.d("IVivoPermissionServiceProxy", "vivo_permission_service not found, skipping hook");
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
        return IVivoPermissonService.Stub.asInterface.call(mBaseBinder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("vivo_permission_service");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("checkPermission"));
        addMethodHook(new PkgMethodProxy("getAppPermission"));
        addMethodHook(new PkgMethodProxy("setAppPermission"));
        addMethodHook(new PkgMethodProxy("setWhiteListApp"));
        addMethodHook(new PkgMethodProxy("setBlackListApp"));
        addMethodHook(new PkgMethodProxy("noteStartActivityProcess"));
        addMethodHook(new PkgMethodProxy("isBuildInThirdPartApp"));
    }
}