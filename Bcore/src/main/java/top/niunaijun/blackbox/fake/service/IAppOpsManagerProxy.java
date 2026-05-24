package top.niunaijun.blackbox.fake.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;
import android.os.Process;

import java.lang.reflect.Method;

import black.android.app.BRAppOpsManager;
import black.android.os.BRServiceManager;
import black.com.android.internal.app.BRIAppOpsServiceStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class IAppOpsManagerProxy extends BinderInvocationStub {
    public static final String TAG = "AppOpsManagerStub";

    public IAppOpsManagerProxy() {
        super(BRServiceManager.get().getService(Context.APP_OPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder call = BRServiceManager.get().getService(Context.APP_OPS_SERVICE);
        return BRIAppOpsServiceStub.get().asInterface(call);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRAppOpsManager.get(null)._check_mService() != null) {
            AppOpsManager appOpsManager = (AppOpsManager) BlackBoxCore.getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                BRAppOpsManager.get(appOpsManager)._set_mService(getProxyInvocation());
            } catch (Exception e) {
                Slog.w(TAG, "inject appops manager service failed", e);
            }
        }
        replaceSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        maybeFixHostPackageUid(args, method.getName());
        return super.invoke(proxy, method, args);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }


    private static String resolveCallingPackageForUid(Context context, int uid) {
        if (uid <= 0 || context == null) return null;
        try {
            String[] packages = context.getPackageManager().getPackagesForUid(uid);
            if (packages == null || packages.length == 0) return null;
            for (String pkg : packages) {
                if (BlackBoxCore.getHostPkg().equals(pkg)) {
                    return pkg;
                }
            }
            return packages[0];
        } catch (Throwable e) {
            Slog.w(TAG, "resolveCallingPackageForUid failed for uid=" + uid, e);
            return null;
        }
    }

    private static void maybeFixHostPackageUid(Object[] args, String methodName) {
        if (args == null || args.length == 0) return;

        int hostUid = BlackBoxCore.getHostUid() > 0 ? BlackBoxCore.getHostUid() : Process.myUid();
        int hostPkgIndex = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String && BlackBoxCore.getHostPkg().equals(args[i])) {
                hostPkgIndex = i;
                break;
            }
        }
        if (hostPkgIndex < 0) return;

        int uidIndex = findUidIndexNearPackage(args, hostPkgIndex);
        if (uidIndex < 0) return;

        int uid = (Integer) args[uidIndex];
        String resolvedPkg = resolveCallingPackageForUid(BlackBoxCore.getContext(), uid);
        if (resolvedPkg != null && !BlackBoxCore.getHostPkg().equals(resolvedPkg)) {
            args[hostPkgIndex] = resolvedPkg;
            Slog.w(TAG, "AppOps package mismatch in " + methodName + ", replacing host package with " + resolvedPkg + " for uid=" + uid);
            return;
        }
        if (uid > 0 && uid != hostUid) {
            args[uidIndex] = hostUid;
            Slog.d(TAG, "Fixed AppOps uid mismatch in " + methodName + " at index " + uidIndex + ": " + uid + " -> " + hostUid);
        }
    }

    private static int findUidIndexNearPackage(Object[] args, int packageIndex) {
        // AppOps signatures typically place uid immediately before packageName.
        if (packageIndex - 1 >= 0 && args[packageIndex - 1] instanceof Integer) {
            return packageIndex - 1;
        }
        // Fallback for variants where uid is the next field.
        if (packageIndex + 1 < args.length && args[packageIndex + 1] instanceof Integer) {
            return packageIndex + 1;
        }
        // Last resort: select the closest positive int around package index.
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer && ((Integer) args[i]) > 0) {
                int d = Math.abs(i - packageIndex);
                if (d < bestDistance) {
                    best = i;
                    bestDistance = d;
                }
            }
        }
        return best;
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "checkPackage");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "checkOperation");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "noteOperation");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startOperation")
    public static class StartOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "startOperation");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("finishOperation")
    public static class FinishOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "finishOperation");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "checkOpNoThrow");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "noteOpNoThrow");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startOpNoThrow")
    public static class StartOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "startOpNoThrow");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            maybeFixHostPackageUid(args, "noteProxyOperation");
            return method.invoke(who, args);
        }
    }
}
