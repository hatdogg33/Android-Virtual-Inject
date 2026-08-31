package com.reveny.virtualinject.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class Utility {

    public static class AppInfo {
        public final String appName;
        public final String packageName;

        public AppInfo(String appName, String packageName) {
            this.appName = appName;
            this.packageName = packageName;
        }
    }

    public static List<AppInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> ret = new ArrayList<>();

        for (ApplicationInfo appInfo : packages) {
            if (!appInfo.packageName.equals(context.getPackageName())) {
                String appName = pm.getApplicationLabel(appInfo).toString();
                ret.add(new AppInfo(appName, appInfo.packageName));
            }
        }

        return ret;
    }
}
