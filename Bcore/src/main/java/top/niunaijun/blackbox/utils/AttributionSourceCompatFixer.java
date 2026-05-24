package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.os.Binder;
import android.os.Process;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;

public final class AttributionSourceCompatFixer {
    private static final String TAG = "AttributionSourceFix";

    private AttributionSourceCompatFixer() {}

    public static void fixArgsForFrameworkCall(Object[] args) {
        fixArgsForFrameworkCall(args, BlackBoxCore.getAppPackageName(), BlackBoxCore.getAppProcessName());
    }

    public static void fixArgsForFrameworkCall(Object[] args, String virtualPkg, String processName) {
        if (args == null) return;
        for (Object arg : args) fixObjectRecursive(arg, virtualPkg, processName);
    }

    private static void fixObjectRecursive(Object obj, String virtualPkg, String processName) {
        if (obj == null) return;
        try {
            String n = obj.getClass().getName();
            if (n.contains("AttributionSource")) {
                fixAttributionSource(obj, virtualPkg, processName);
            } else if (obj instanceof android.os.Bundle) {
                android.os.Bundle b = (android.os.Bundle) obj;
                for (String k : b.keySet()) fixObjectRecursive(b.get(k), virtualPkg, processName);
            }
        } catch (Throwable ignored) {
        }
    }

    public static int resolveCorrectFrameworkUid(Context context, String virtualPkg, String processName, int incomingSourceUid) {
        int callerUid = Binder.getCallingUid();
        if (callerUid <= 0 || callerUid == Process.myUid()) {
            return BlackBoxCore.getHostUid();
        }
        return callerUid;
    }

    private static void fixAttributionSource(Object src, String virtualPkg, String processName) {
        int incomingUid = safeInt(readField(src, "uid"), safeInt(readField(src, "mUid"), -1));
        String oldPkg = safeString(readField(src, "packageName"), safeString(readField(src, "mPackageName"), null));
        int resolvedUid = resolveCorrectFrameworkUid(BlackBoxCore.getContext(), virtualPkg, processName, incomingUid);
        String resolvedPkg = resolvePkgForUid(resolvedUid, virtualPkg);
        if (resolvedPkg == null) resolvedPkg = BlackBoxCore.getHostPkg();
        Slog.d(TAG, "AttributionSourceFix: before uid=" + incomingUid + ", pkg=" + oldPkg + ", callerUid=" + Binder.getCallingUid());

        boolean mutated = setAttrFields(src, resolvedUid, resolvedPkg);
        if (!mutated) {
            Slog.w(TAG, "AttributionSourceFix: direct mutation failed, rebuilding source");
            Object rebuilt = rebuildAttributionSource(src, resolvedUid, resolvedPkg);
            if (rebuilt == null) {
                Slog.w(TAG, "AttributionSourceFix: rebuild failed, leaving original source");
            }
        }
        Object next = invokeNoArg(src, "getNext");
        if (next != null) fixAttributionSource(next, virtualPkg, processName);
        Slog.d(TAG, "AttributionSourceFix: after uid=" + safeInt(readField(src, "uid"), safeInt(readField(src, "mUid"), -1))
                + ", pkg=" + safeString(readField(src, "packageName"), safeString(readField(src, "mPackageName"), null))
                + ", callerUid=" + Binder.getCallingUid());

        if (oldPkg != null && !oldPkg.equals(resolvedPkg)) {
            Slog.i(TAG, "pkgChanged oldPkg=" + oldPkg + " newPkg=" + resolvedPkg + " uid=" + resolvedUid
                    + " virtualPkg=" + virtualPkg + " processName=" + processName);
        }
    }

    private static boolean setAttrFields(Object src, int uid, String pkg) {
        boolean ok = false;
        ok |= setFieldOrSetter(src, "uid", uid);
        ok |= setFieldOrSetter(src, "mUid", uid);
        ok |= setFieldOrSetter(src, "packageName", pkg);
        ok |= setFieldOrSetter(src, "mPackageName", pkg);
        Object state = readField(src, "mAttributionSourceState");
        if (state != null) {
            ok |= setFieldOrSetter(state, "uid", uid);
            ok |= setFieldOrSetter(state, "packageName", pkg);
        }
        return ok;
    }

    private static Object rebuildAttributionSource(Object src, int uid, String pkg) {
        try {
            Class<?> c = src.getClass();
            Class<?> builderClass = Class.forName("android.content.AttributionSource$Builder");
            Constructor<?> ctor = builderClass.getConstructor(int.class);
            Object builder = ctor.newInstance(uid);
            Method setPackageName = builderClass.getMethod("setPackageName", String.class);
            setPackageName.invoke(builder, pkg);
            Method build = builderClass.getMethod("build");
            Object rebuilt = build.invoke(builder);
            setAttrFields(src, uid, pkg);
            return rebuilt;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String resolvePkgForUid(int uid, String preferred) {
        try {
            String[] pkgs = BlackBoxCore.getContext().getPackageManager().getPackagesForUid(uid);
            if (pkgs == null || pkgs.length == 0) return null;
            if (preferred != null) {
                for (String p : pkgs) if (preferred.equals(p)) return p;
            }
            for (String p : pkgs) {
                if ("com.google.android.gms".equals(p) || "com.google.android.gsf".equals(p) || "com.android.vending".equals(p)) return p;
            }
            for (String p : pkgs) if (BlackBoxCore.getHostPkg().equals(p)) return p;
            return pkgs[0];
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean setFieldOrSetter(Object obj, String name, Object v) {
        try { Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj,v); return true; } catch (Throwable ignore) {}
        try { Method m=obj.getClass().getDeclaredMethod("set"+Character.toUpperCase(name.charAt(0))+name.substring(1), v instanceof Integer?int.class:String.class); m.setAccessible(true); m.invoke(obj,v); return true; } catch (Throwable ignore) {}
        return false;
    }
    private static Object readField(Object obj, String name){ try{ Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj);}catch(Throwable t){return null;}}
    private static Object invokeNoArg(Object obj, String name){ try{ Method m=obj.getClass().getDeclaredMethod(name); m.setAccessible(true); return m.invoke(obj);}catch(Throwable t){return null;}}
    private static int safeInt(Object o, int d){ return o instanceof Integer ? (Integer)o : d; }
    private static String safeString(Object a, String d){ return a instanceof String ? (String)a : d; }
}
