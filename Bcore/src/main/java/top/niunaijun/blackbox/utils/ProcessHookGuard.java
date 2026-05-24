package top.niunaijun.blackbox.utils;

import android.content.Context;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

public final class ProcessHookGuard {
    private static final String TAG = "ProcessHookGuard";

    private ProcessHookGuard() {}

    public static boolean shouldInstallVirtualHooks(Context context, String processName) {
        String hostPkg = BlackBoxCore.getHostPkg();
        boolean hostProcess = processName == null
                || processName.equals(hostPkg)
                || processName.endsWith(":black")
                || processName.endsWith(":server")
                || processName.contains("SystemCallProvider");
        if (hostProcess || BActivityThread.getAppConfig() == null) {
            Slog.i(TAG, "ProcessHookGuard: host process detected, skipping virtual hooks process=" + processName);
            Slog.i(TAG, "HostIsolation: no virtual auth routing in host process");
            return false;
        }
        Slog.i(TAG, "ProcessHookGuard: virtual process detected, installing virtual hooks pkg="
                + BActivityThread.getAppPackageName() + " process=" + processName);
        return true;
    }
}
