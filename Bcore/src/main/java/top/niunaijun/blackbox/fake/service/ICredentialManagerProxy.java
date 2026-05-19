package top.niunaijun.blackbox.fake.service;

import android.content.Context;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ICredentialManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ICredentialManagerProxy";

    public ICredentialManagerProxy() {
        super(BRServiceManager.get().getService("credential"));
    }

    @Override
    protected Object getWho() {
        // Reflection to avoid compilation error since it's dynamically generated
        try {
            Class<?> clazz = Class.forName("black.android.credentials.BRICredentialManagerStub");
            Object stub = clazz.getMethod("get").invoke(null);
            return clazz.getMethod("asInterface", Object.class).invoke(stub, BRServiceManager.get().getService("credential"));
        } catch (Exception e) {
            Slog.e(TAG, "Failed to getWho for credential manager", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("credential");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("executeGetCredential")
    public static class executeGetCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepted executeGetCredential");
            return null;
        }
    }
}
