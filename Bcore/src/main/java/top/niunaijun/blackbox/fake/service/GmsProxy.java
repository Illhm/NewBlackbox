package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;
import android.os.Looper;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceCompatFixer;


public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";

    public GmsProxy() {
        super(BRServiceManager.get().getService("gms"));
    }

    @Override
    protected Object getWho() {
        IBinder binder = null;
        for (int i = 0; i < 3 && binder == null; i++) {
            binder = BRServiceManager.get().getService("gms");
            if (binder == null) {
                Slog.w(TAG, "GmsProxy: waiting for gms service, attempt=" + (i + 1));
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    try { Thread.sleep(80L); } catch (InterruptedException ignored) {}
                }
            }
        }
        if (binder == null) {
            Slog.e(TAG, "GmsProxy: failed after retries: reason=binder_null");
            Slog.e(TAG, "GmsProxy: state gmsInstalled=" + BlackBoxCore.get().isInstalled("com.google.android.gms", BlackBoxCore.getUserId())
                    + " gsfInstalled=" + BlackBoxCore.get().isInstalled("com.google.android.gsf", BlackBoxCore.getUserId())
                    + " vendingInstalled=" + BlackBoxCore.get().isInstalled("com.android.vending", BlackBoxCore.getUserId())
                    + " gmsProcessRunning=" + (BlackBoxCore.getAppProcessName()!=null && BlackBoxCore.getAppProcessName().contains("com.google.android.gms"))
                    + " binderReady=false");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            if (iface != null) {
                Slog.d(TAG, "GmsProxy: acquired binder from ServiceManager:gms");
                return iface;
            } else {
                Slog.e(TAG, "Reflection succeeded but returned null interface");
                return null;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IGmsServiceBroker interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("gms");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                if (args != null && args.length > 0) {
                    String callingPackage = (String) args[0];
                    if ("com.google.android.gms".equals(callingPackage)) {
                        
                        args[0] = BlackBoxCore.getHostPkg();
                        Slog.d(TAG, "GmsProxy: Fixed calling package from com.google.android.gms to " + BlackBoxCore.getHostPkg());
                    }
                }
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getService", e);
                
                return null;
            }
        }
    }

    
    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getServiceBroker", e);
                
                return null;
            }
        }
    }

    
    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling authenticate call");
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (SecurityException se) {
                Slog.e(TAG, "GmsProxy: Authentication security error", se);
                throw se;
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Authentication error", e);
                throw e;
            }
        }
    }

    
    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getAccount call");
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getToken call");
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (SecurityException se) {
                Slog.e(TAG, "GmsProxy: GetToken security error", se);
                throw se;
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: GetToken error", e);
                throw e;
            }
        }
    }

    
    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling invalidateToken call");
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling clearToken call");
                AttributionSourceCompatFixer.fixArgsForFrameworkCall(args);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    
    private static Object createMockAuthResult() {
        try {
            
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }
}
