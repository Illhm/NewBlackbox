package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;


import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;

public class ICredentialManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ICredentialManagerProxy";

    public ICredentialManagerProxy() {
        super(BRServiceManager.get().getService("credential"));
    }

    @Override
    protected Object getWho() {
        try {
            return black.android.credentials.BRICredentialManagerStub.get().asInterface(BRServiceManager.get().getService("credential"));
        } catch (Exception e) {
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

    private static void clearAllowedProviders(Object request) {
        if (request == null) return;
        try {
            // GetCredentialRequest has a list of CredentialOption
            Method getCredentialOptionsMethod = request.getClass().getMethod("getCredentialOptions");
            List<?> options = (List<?>) getCredentialOptionsMethod.invoke(request);
            if (options != null) {
                for (Object option : options) {
                    try {
                        Method getAllowedProvidersMethod = option.getClass().getMethod("getAllowedProviders");
                        Set<?> providers = (Set<?>) getAllowedProvidersMethod.invoke(option);
                        if (providers != null && !providers.isEmpty()) {
                            java.lang.reflect.Field providersField = option.getClass().getDeclaredField("mAllowedProviders");
                            providersField.setAccessible(true);
                            providersField.set(option, Collections.emptySet());
                        }
                    } catch (Throwable t) {
                    }
                }
            }
        } catch (Throwable t) {
            try {
                // CreateCredentialRequest has getAllowedProviders
                Method getAllowedProvidersMethod = request.getClass().getMethod("getAllowedProviders");
                Set<?> providers = (Set<?>) getAllowedProvidersMethod.invoke(request);
                if (providers != null && !providers.isEmpty()) {
                    java.lang.reflect.Field providersField = request.getClass().getDeclaredField("mAllowedProviders");
                    providersField.setAccessible(true);
                    providersField.set(request, Collections.emptySet());
                }
            } catch (Throwable th) {
            }
        }
    }

    @ProxyMethod("executeGetCredential")
    public static class ExecuteGetCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null && arg.getClass().getName().equals("android.credentials.GetCredentialRequest")) {
                        clearAllowedProviders(arg);
                    }
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.e(TAG, "executeGetCredential error", t);
                return null;
            }
        }
    }

    @ProxyMethod("executePrepareGetCredential")
    public static class ExecutePrepareGetCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null && arg.getClass().getName().equals("android.credentials.GetCredentialRequest")) {
                        clearAllowedProviders(arg);
                    }
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.e(TAG, "executePrepareGetCredential error", t);
                return null;
            }
        }
    }

    @ProxyMethod("executeGetCandidateCredentials")
    public static class ExecuteGetCandidateCredentials extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null && arg.getClass().getName().equals("android.credentials.GetCredentialRequest")) {
                        clearAllowedProviders(arg);
                    }
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.e(TAG, "executeGetCandidateCredentials error", t);
                return null;
            }
        }
    }

    @ProxyMethod("executeCreateCredential")
    public static class ExecuteCreateCredential extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null && arg.getClass().getName().equals("android.credentials.CreateCredentialRequest")) {
                        clearAllowedProviders(arg);
                    }
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.e(TAG, "executeCreateCredential error", t);
                return null;
            }
        }
    }
}
