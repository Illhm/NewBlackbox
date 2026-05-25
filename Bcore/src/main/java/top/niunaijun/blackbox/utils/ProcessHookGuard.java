package top.niunaijun.blackbox.utils;

import android.content.Context;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

public final class ProcessHookGuard {
    private static final String TAG = "ProcessHookGuard";

    private ProcessHookGuard() {}

    public static boolean shouldInstallVirtualHooks(Context context, String processName) {
        String hostPkg = BlackBoxCore.getHostPkg();
        boolean isHostMain = processName == null || processName.equals(hostPkg);
        boolean isHostServer = processName != null && (processName.endsWith(":black") || processName.endsWith(":server"));
        boolean isProxyProcess = processName != null && processName.matches(".*:p\\d+$");
        boolean isSystemCallProviderProc = processName != null && processName.contains("SystemCallProvider");
        boolean installVirtualHooks = isProxyProcess || (!isHostMain && !isHostServer && !isSystemCallProviderProc && BActivityThread.getAppConfig() != null);
        Slog.i(TAG, "ProcessHookGuard: process=" + processName
                + ", isHostMain=" + isHostMain
                + ", isHostServer=" + isHostServer
                + ", isProxyProcess=" + isProxyProcess
                + ", installVirtualHooks=" + installVirtualHooks);
        if (!installVirtualHooks) {
            Slog.i(TAG, "ProcessHookGuard: host process detected, skipping virtual hooks process=" + processName);
            Slog.i(TAG, "HostIsolation: no virtual auth routing in host process");
            return false;
        }
        if (isProxyProcess) {
            Slog.i(TAG, "ProcessHookGuard: proxy process detected, installing virtual container hooks");
        }
        Slog.i(TAG, "ProcessHookGuard: virtual process detected, installing virtual hooks pkg="
                + BActivityThread.getAppPackageName() + " process=" + processName);
        return true;
    }
}
