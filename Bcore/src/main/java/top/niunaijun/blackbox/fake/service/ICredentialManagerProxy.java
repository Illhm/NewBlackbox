package top.niunaijun.blackbox.fake.service;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

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
        try {
            Class<?> clazz = Class.forName("android.credentials.ICredentialManager$Stub");
            Method asInterface = clazz.getDeclaredMethod("asInterface", android.os.IBinder.class);
            return asInterface.invoke(null, BRServiceManager.get().getService("credential"));
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

    private static void clearAllowedProvidersFromGetRequest(Object request) {
        if (request == null) return;
        try {
            Method getCredentialOptions = request.getClass().getMethod("getCredentialOptions");
            List<?> options = (List<?>) getCredentialOptions.invoke(request);
            if (options != null) {
                for (Object option : options) {
                    Method getAllowedProviders = option.getClass().getMethod("getAllowedProviders");
                    Set<?> allowedProviders = (Set<?>) getAllowedProviders.invoke(option);
                    if (allowedProviders != null && !allowedProviders.isEmpty()) {
                        allowedProviders.clear();
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to clear mAllowedProviders from GetCredentialRequest", e);
        }
    }

    private static void clearAllowedProvidersFromCreateRequest(Object request) {
        if (request == null) return;
        try {
            Method getAllowedProviders = request.getClass().getMethod("getAllowedProviders");
            Set<?> allowedProviders = (Set<?>) getAllowedProviders.invoke(request);
            if (allowedProviders != null && !allowedProviders.isEmpty()) {
                allowedProviders.clear();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to clear mAllowedProviders from CreateCredentialRequest", e);
        }
    }

    @ProxyMethod("executeGetCredential")
    public static class executeGetCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepted executeGetCredential");
            if (args != null && args.length > 0) {
                clearAllowedProvidersFromGetRequest(args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Suppressed SecurityException in executeGetCredential", e);
                return null;
            }
        }
    }

    @ProxyMethod("executePrepareGetCredential")
    public static class executePrepareGetCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepted executePrepareGetCredential");
            if (args != null && args.length > 0) {
                clearAllowedProvidersFromGetRequest(args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Suppressed SecurityException in executePrepareGetCredential", e);
                return null;
            }
        }
    }

    @ProxyMethod("executeCreateCredential")
    public static class executeCreateCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepted executeCreateCredential");
            if (args != null && args.length > 0) {
                clearAllowedProvidersFromCreateRequest(args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Suppressed SecurityException in executeCreateCredential", e);
                return null;
            }
        }
    }
}
