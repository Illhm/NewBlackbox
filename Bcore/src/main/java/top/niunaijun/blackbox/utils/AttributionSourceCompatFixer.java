package top.niunaijun.blackbox.utils;

import android.os.Binder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;

public final class AttributionSourceCompatFixer {
    private static final String TAG = "AttributionSourceFix";

    private AttributionSourceCompatFixer() {}

    public static void fixArgsForFrameworkCall(Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            fixObjectRecursive(arg);
        }
    }

    public static void fixObjectRecursive(Object obj) {
        if (obj == null) return;
        try {
            String name = obj.getClass().getName();
            if (name.contains("AttributionSource")) {
                fixAttributionSource(obj);
            } else if (obj instanceof android.os.Bundle) {
                android.os.Bundle b = (android.os.Bundle) obj;
                for (String k : b.keySet()) {
                    fixObjectRecursive(b.get(k));
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static void fixAttributionSource(Object src) {
        if (src == null) return;
        int callerUid = Binder.getCallingUid();
        int uid = callerUid > 0 ? callerUid : BlackBoxCore.getHostUid();
        String pkg = resolvePkgForUid(uid);
        if (pkg == null) pkg = BlackBoxCore.getHostPkg();
        logBefore(src, callerUid);
        setFieldOrSetter(src, "uid", uid);
        setFieldOrSetter(src, "mUid", uid);
        setFieldOrSetter(src, "packageName", pkg);
        setFieldOrSetter(src, "mPackageName", pkg);
        Object state = readField(src, "mAttributionSourceState");
        if (state != null) {
            setFieldOrSetter(state, "uid", uid);
            setFieldOrSetter(state, "packageName", pkg);
            Object next = readField(state, "next");
            if (next != null) fixAttributionSource(next);
        }
        Object next = invokeNoArg(src, "getNext");
        if (next != null) fixAttributionSource(next);
        logAfter(src, callerUid);
    }

    private static String resolvePkgForUid(int uid) {
        try {
            String[] pkgs = BlackBoxCore.getContext().getPackageManager().getPackagesForUid(uid);
            if (pkgs != null && pkgs.length > 0) return pkgs[0];
        } catch (Throwable ignore) {}
        return null;
    }

    private static void setFieldOrSetter(Object obj, String name, Object v) {
        try { Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj,v); return; } catch (Throwable ignore) {}
        try { Method m=obj.getClass().getDeclaredMethod("set"+Character.toUpperCase(name.charAt(0))+name.substring(1), v instanceof Integer?int.class:String.class); m.setAccessible(true); m.invoke(obj,v);} catch (Throwable ignore) {}
    }
    private static Object readField(Object obj, String name){ try{ Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj);}catch(Throwable t){return null;}}
    private static Object invokeNoArg(Object obj, String name){ try{ Method m=obj.getClass().getDeclaredMethod(name); m.setAccessible(true); return m.invoke(obj);}catch(Throwable t){return null;}}
    private static void logBefore(Object src, int callerUid){ Slog.d(TAG,"AttributionSourceFix: before uid="+readField(src,"uid")+", pkg="+readField(src,"packageName")+", callerUid="+callerUid);}
    private static void logAfter(Object src, int callerUid){ Slog.d(TAG,"AttributionSourceFix: after uid="+readField(src,"uid")+", pkg="+readField(src,"packageName")+", callerUid="+callerUid);}
}
