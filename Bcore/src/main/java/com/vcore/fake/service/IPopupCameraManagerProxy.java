package com.vcore.fake.service;

import android.os.IBinder;

import black.android.os.ServiceManager;
import black.oem.vivo.IPopupCameraManager;
import com.vcore.fake.hook.BinderInvocationStub;
import com.vcore.fake.service.base.PkgMethodProxy;
import com.vcore.utils.Slog;

/**
 * @author Findger
 * @function
 * @date :2023/10/8 20:19
 **/
public class IPopupCameraManagerProxy extends BinderInvocationStub {
    private final IBinder mBaseBinder;

    public IPopupCameraManagerProxy() {
        IBinder binder = ServiceManager.getService.call("popup_camera_service");
        if (binder == null) {
            Slog.d("IPopupCameraManagerProxy", "popup_camera_service not found, skipping hook");
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
        return IPopupCameraManager.Stub.asInterface.call(mBaseBinder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("popup_camera_service");
    }

    @Override
    public boolean isBadEnv() {
        return mBaseBinder == null;
    }

    @Override
    protected void onBindMethod() {
        addMethodHook(new PkgMethodProxy("notifyCameraStatus"));
    }
}
