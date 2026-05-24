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

    private static void maybeFixHostPackageUid(Object[] args, String methodName) {
        if (args == null || args.length == 0) return;

        int hostUid = BlackBoxCore.getHostUid() > 0 ? BlackBoxCore.getHostUid() : Process.myUid();
        boolean hasHostPackage = false;

        for (Object arg : args) {
            if (arg instanceof String && BlackBoxCore.getHostPkg().equals(arg)) {
                hasHostPackage = true;
                break;
            }
        }

        if (!hasHostPackage) return;

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Integer) {
                int uid = (Integer) arg;
                if (uid != hostUid && uid > 0) {
                    args[i] = hostUid;
                    Slog.d(TAG, "Fixed AppOps uid mismatch in " + methodName + " at index " + i + ": " + uid + " -> " + hostUid);
                    return;
                }
            }
        }
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
