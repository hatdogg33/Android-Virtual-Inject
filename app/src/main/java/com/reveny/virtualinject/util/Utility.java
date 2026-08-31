package com.reveny.virtualinject.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class Utility {
    public static List<String> getInstalledApps(Context context) {
        List<ApplicationInfo> packages = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> ret = new ArrayList<>();

        for (ApplicationInfo appInfo : packages) {
            if (!appInfo.packageName.equals(context.getPackageName())) {
                ret.add(appInfo.packageName);
            }
        }

        return ret;
    }
}
