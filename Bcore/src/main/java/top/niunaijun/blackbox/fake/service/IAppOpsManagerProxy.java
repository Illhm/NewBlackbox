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
import top.niunaijun.blackbox.utils.MethodParameterUtils;
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

        int realUid = Process.myUid();
        int packageArgIndex = findPackageArgIndex(args);
        int uidArgIndex = findUidIndexNearPackage(args, packageArgIndex);
        int userIdArgIndex = findUserIdArgIndex(args, methodName);

        Object beforePkg = packageArgIndex >= 0 ? args[packageArgIndex] : null;
        Object beforeUid = uidArgIndex >= 0 ? args[uidArgIndex] : null;

        String selectedPkg = MethodParameterUtils.resolveFrameworkCallerPackage(BlackBoxCore.getContext(), beforePkg instanceof String ? (String) beforePkg : null);
        if (packageArgIndex >= 0 && selectedPkg != null) {
            args[packageArgIndex] = selectedPkg;
        }
        if (uidArgIndex >= 0) {
            args[uidArgIndex] = realUid;
        }
        if (userIdArgIndex >= 0 && args[userIdArgIndex] instanceof Integer) {
            int userId = (Integer) args[userIdArgIndex];
            if (userId < 0 || userId > 1000) {
                args[userIdArgIndex] = MethodParameterUtils.getFrameworkUserId();
            }
        }

        String[] realPkgs = null;
        try { realPkgs = BlackBoxCore.getContext().getPackageManager().getPackagesForUid(realUid); } catch (Throwable ignored) {}
        boolean belongs = selectedPkg != null && realPkgs != null && java.util.Arrays.asList(realPkgs).contains(selectedPkg);
        Slog.i(TAG, "AppOpsFix: method=" + methodName);
        Slog.i(TAG, "AppOpsFix: uidArgIndex=" + uidArgIndex);
        Slog.i(TAG, "AppOpsFix: packageArgIndex=" + packageArgIndex);
        Slog.i(TAG, "AppOpsFix: userIdArgIndex=" + userIdArgIndex);
        Slog.i(TAG, "AppOpsFix: before uid=" + beforeUid + ", pkg=" + beforePkg);
        Slog.i(TAG, "AppOpsFix: after uid=" + realUid + ", pkg=" + selectedPkg);
        Slog.i(TAG, "AppOpsFix: realPackagesForUid=" + java.util.Arrays.toString(realPkgs));
        Slog.i(TAG, "AppOpsFix: packageBelongsToUid=" + belongs);
    }

    private static int findPackageArgIndex(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                return i;
            }
        }
        return -1;
    }

    private static int findUserIdArgIndex(Object[] args, String methodName) {
        if ("checkPackage".equals(methodName) || "checkOperation".equals(methodName)
                || "noteOperation".equals(methodName) || "startOperation".equals(methodName)
                || "finishOperation".equals(methodName) || "checkOpNoThrow".equals(methodName)
                || "noteOpNoThrow".equals(methodName) || "startOpNoThrow".equals(methodName)
                || "noteProxyOperation".equals(methodName)) {
            return -1;
        }
        return -1;
    }


    private static int findUidIndexNearPackage(Object[] args, int packageIndex) {
        if (packageIndex > 0 && args[packageIndex - 1] instanceof Integer) {
            return packageIndex - 1;
        }
        if (packageIndex >= 0 && packageIndex + 1 < args.length && args[packageIndex + 1] instanceof Integer) {
            return packageIndex + 1;
        }
        return -1;
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
