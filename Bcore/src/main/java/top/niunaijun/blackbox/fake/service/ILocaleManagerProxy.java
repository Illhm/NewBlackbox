package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ILocaleManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ILocaleManagerProxy";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService("locale"));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService("locale");
        if (binder == null) {
            Slog.e(TAG, "Failed to get locale service binder");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("android.app.ILocaleManager$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            return asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ILocaleManager interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("locale");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Suppressed getApplicationLocales exception", e);
                try {
                    Class<?> localeListClass = Class.forName("android.os.LocaleList");
                    Method getEmptyLocaleList = localeListClass.getMethod("getEmptyLocaleList");
                    return getEmptyLocaleList.invoke(null);
                } catch (Exception ex) {
                    return null;
                }
            }
        }
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Suppressed setApplicationLocales exception", e);
                return null;
            }
        }
    }
}
