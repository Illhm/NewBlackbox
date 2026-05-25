package top.niunaijun.blackbox.utils;

import android.content.pm.PackageManager;

import java.util.Arrays;
import java.util.HashSet;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

public class MethodParameterUtils {

    public static <T> T getFirstParam(Object[] args, Class<T> tClass) {
        if (args == null) {
            return null;
        }
        int index = ArrayUtils.indexOfFirst(args, tClass);
        if (index != -1) {
            return (T) args[index];
        }
        return null;
    }

    public static String replaceFirstAppPkg(Object[] args) {
        if (args == null) {
            return null;
        }
        if (BActivityThread.getAppConfig() != null) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                String value = (String) args[i];
                if (BlackBoxCore.get().isInstalled(value, BlackBoxCore.getUserId())) {
                    args[i] = BlackBoxCore.getHostPkg();
                    return value;
                }
            }
        }
        return null;
    }

    public static void fixPkgUidForFramework(Object[] args) {
        if (args == null) return;
        int pkgIndex = -1;
        int uidIndex = -1;
        for (int i = 0; i < args.length; i++) {
            if (pkgIndex < 0 && args[i] instanceof String) pkgIndex = i;
            if (uidIndex < 0 && args[i] instanceof Integer) uidIndex = i;
        }
        if (pkgIndex < 0 || uidIndex < 0) return;
        String pkg = (String) args[pkgIndex];
        Integer uid = (Integer) args[uidIndex];
        if (pkg == null || uid == null) return;
        Slog.i("MethodParameterUtils", "PkgUidFix: before pkg=" + pkg + ", uid=" + uid);
        int hostUid = BlackBoxCore.getHostUid() > 0 ? BlackBoxCore.getHostUid() : android.os.Process.myUid();
        args[pkgIndex] = BlackBoxCore.getHostPkg();
        args[uidIndex] = hostUid;
        String outPkg = (String) args[pkgIndex];
        int outUid = (Integer) args[uidIndex];
        boolean outBelongs = packageBelongsToUid(outPkg, outUid);
        Slog.i("MethodParameterUtils", "PkgUidFix: after pkg=" + outPkg + ", uid=" + outUid);
        Slog.i("MethodParameterUtils", "PkgUidFix: packageBelongsToUid=" + outBelongs);
    }

    private static boolean packageBelongsToUid(String pkg, int uid) {
        if (pkg == null) return false;
        PackageManager pm = BlackBoxCore.getContext().getPackageManager();
        try {
            String[] uidPackages = pm.getPackagesForUid(uid);
            if (uidPackages == null) return false;
            return pm.checkSignatures(uid, pm.getPackageUid(pkg, 0)) == PackageManager.SIGNATURE_MATCH
                    && Arrays.asList(uidPackages).contains(pkg);
        } catch (Throwable ignored) {
            String[] pkgs = pm.getPackagesForUid(uid);
            if (pkgs == null) return false;
            for (String it : pkgs) {
                if (pkg.equals(it)) return true;
            }
            return false;
        }
    }

    public static void replaceAllAppPkg(Object[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                continue;
            if (args[i] instanceof String) {
                String value = (String) args[i];
                if (BlackBoxCore.get().isInstalled(value, BlackBoxCore.getUserId())) {
                    args[i] = BlackBoxCore.getHostPkg();
                }
            }
        }
    }

    public static void replaceFirstUid(Object[] args) {
        if (args == null)
            return;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                int uid = (int) args[i];
                if (uid == BlackBoxCore.getBUid()) {
                    args[i] = BlackBoxCore.getHostUid();
                }
            }
        }
    }

    public static void replaceLastUid(Object[] args) {
        int index = ArrayUtils.indexOfLast(args, Integer.class);
        if (index != -1) {
            int uid = (int) args[index];
            if (uid == BlackBoxCore.getBUid()) {
                args[index] = BlackBoxCore.getHostUid();
            }
        }
    }

    public static String replaceLastAppPkg(Object[] args) {
        int index = ArrayUtils.indexOfLast(args, String.class);
        if (index != -1) {
            String pkg = (String) args[index];
            if (BlackBoxCore.get().isInstalled(pkg, BlackBoxCore.getUserId())) {
                args[index] = BlackBoxCore.getHostPkg();
            }
            return pkg;
        }
        return null;
    }

    public static String replaceSequenceAppPkg(Object[] args, int sequence) {
        int index = ArrayUtils.indexOf(args, String.class, sequence);
        if (index != -1) {
            String pkg = (String) args[index];
            if (BlackBoxCore.get().isInstalled(pkg, BlackBoxCore.getUserId())) {
                args[index] = BlackBoxCore.getHostPkg();
            }
            return pkg;
        }
        return null;
    }

    public static int getParamsIndex(Class[] args, Class<?> type) {
        for (int i = 0; i < args.length; i++) {
            Class obj = args[i];
            if (obj.equals(type)) {
                return i;
            }
        }
        return -1;
    }

    public static int getIndex(Object[] args, Class<?> type) {
        return getIndex(args, type, 0);
    }

    public static int getIndex(Object[] args, Class<?> type, int start) {
        for (int i = start; i < args.length; i++) {
            Object obj = args[i];
            if (obj != null && obj.getClass() == type) {
                return i;
            }
            if (type.isInstance(obj)) {
                return i;
            }
        }
        return -1;
    }

    public static Class<?>[] getAllInterface(Class clazz) {
        HashSet<Class<?>> classes = new HashSet<>();
        getAllInterfaces(clazz, classes);
        Class<?>[] result = new Class[classes.size()];
        classes.toArray(result);
        return result;
    }


    public static void getAllInterfaces(Class clazz, HashSet<Class<?>> interfaceCollection) {
        Class<?>[] classes = clazz.getInterfaces();
        if (classes.length != 0) {
            interfaceCollection.addAll(Arrays.asList(classes));
        }
        if (clazz.getSuperclass() != Object.class) {
            getAllInterfaces(clazz.getSuperclass(), interfaceCollection);
        }
    }

    public static int toInt(Object obj){
        if(obj instanceof Long){
            return ((Long) obj).intValue();
        }
        return (int)obj;
    }
}
