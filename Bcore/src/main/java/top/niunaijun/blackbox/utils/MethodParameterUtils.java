package top.niunaijun.blackbox.utils;

import android.content.pm.PackageManager;
import android.os.Process;

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

    public static String resolveFrameworkCallerPackage(android.content.Context context, String originalPkg) {
        int realUid = Process.myUid();
        String[] realPkgs = null;
        try {
            realPkgs = context.getPackageManager().getPackagesForUid(realUid);
        } catch (Throwable ignored) {}
        Slog.i("MethodParameterUtils", "FrameworkIdentity: processUid=" + realUid);
        Slog.i("MethodParameterUtils", "FrameworkIdentity: realPackagesForUid=" + Arrays.toString(realPkgs));
        Slog.i("MethodParameterUtils", "FrameworkIdentity: originalPkg=" + originalPkg);
        if (realPkgs == null || realPkgs.length == 0) {
            Slog.w("MethodParameterUtils", "FrameworkIdentity: no real packages for uid=" + realUid + ", skip mutation");
            return originalPkg;
        }
        String selected = null;
        if (originalPkg != null) {
            for (String p : realPkgs) if (originalPkg.equals(p)) { selected = p; break; }
        }
        String virtualPkg = BlackBoxCore.getAppPackageName();
        if (selected == null && virtualPkg != null) {
            for (String p : realPkgs) if (virtualPkg.equals(p)) { selected = p; break; }
        }
        if (selected == null) {
            for (String p : realPkgs) if (BlackBoxCore.getHostPkg().equals(p)) { selected = p; break; }
        }
        if (selected == null) selected = realPkgs[0];
        boolean belongs = false;
        for (String p : realPkgs) if (selected.equals(p)) { belongs = true; break; }
        Slog.i("MethodParameterUtils", "FrameworkIdentity: selectedPkg=" + selected);
        Slog.i("MethodParameterUtils", "FrameworkIdentity: packageBelongsToUid=" + belongs);
        return selected;
    }

    public static void fixFrameworkCallerPackage(Object[] args, String methodName) {
        if (args == null) return;
        int pkgIndex = detectFrameworkCallerPackageIndex(args, methodName);
        if (pkgIndex < 0 || !(args[pkgIndex] instanceof String)) {
            return;
        }
        String before = (String) args[pkgIndex];
        String selected = resolveFrameworkCallerPackage(BlackBoxCore.getContext(), before);
        if (selected != null) {
            args[pkgIndex] = selected;
        }
        Slog.i("MethodParameterUtils", "FrameworkArgFix: method=" + methodName + " callerPackageIndex=" + pkgIndex
                + " before=" + before + " after=" + args[pkgIndex]);
    }

    public static void fixFrameworkUserId(Object[] args, String methodName) {
        if (args == null) return;
        int userIdIndex = detectFrameworkUserIdIndex(args, methodName);
        if (userIdIndex < 0 || !(args[userIdIndex] instanceof Integer)) {
            Slog.i("MethodParameterUtils", "FrameworkArgFix: method=" + methodName + " userIdIndex=-1");
            return;
        }
        int before = (Integer) args[userIdIndex];
        int expected = getFrameworkUserId();
        if (before < 0 || before > 1000) {
            args[userIdIndex] = expected;
        }
        Slog.i("MethodParameterUtils", "FrameworkArgFix: method=" + methodName + " userIdIndex=" + userIdIndex
                + " beforeUserId=" + before + " afterUserId=" + args[userIdIndex]);
    }


    public static int getFrameworkUserId() {
        return 0;
    }
    public static void fixFrameworkUid(Object[] args, String methodName) {
        if (args == null) return;
        int uidIndex = detectFrameworkUidIndex(args, methodName);
        if (uidIndex < 0 || !(args[uidIndex] instanceof Integer)) {
            Slog.i("MethodParameterUtils", "FrameworkArgFix: method=" + methodName + " skipped unknown int arg index=-1");
            return;
        }
        int hostUid = BlackBoxCore.getHostUid() > 0 ? BlackBoxCore.getHostUid() : Process.myUid();
        int before = (Integer) args[uidIndex];
        args[uidIndex] = hostUid;
        Slog.i("MethodParameterUtils", "FrameworkArgFix: method=" + methodName + " uidIndex=" + uidIndex
                + " beforeUid=" + before + " afterUid=" + hostUid);
    }

    public static void fixFrameworkIdentityForMethod(Object[] args, String methodName) {
        fixFrameworkCallerPackage(args, methodName);
        if ("registerReceiver".equals(methodName) || "registerReceiverWithFeature".equals(methodName)
                || "registerReceiverForAllUsers".equals(methodName) || "broadcastIntent".equals(methodName)
                || "broadcastIntentWithFeature".equals(methodName)) {
            fixFrameworkUserId(args, methodName);
        }
        logFrameworkPkgCheck();
    }

    public static void logFrameworkPkgCheck() {
        int hostUid = Process.myUid();
        String hostPkg = resolveFrameworkCallerPackage(BlackBoxCore.getContext(), BlackBoxCore.getHostPkg());
        boolean belongs = packageBelongsToUid(hostPkg, hostUid);
        try {
            String[] uidPackages = BlackBoxCore.getContext().getPackageManager().getPackagesForUid(hostUid);
            Slog.i("MethodParameterUtils", "FrameworkPkgCheck: realPackagesForUid=" + Arrays.toString(uidPackages));
        } catch (Throwable ignored) {
        }
        Slog.i("MethodParameterUtils", "FrameworkPkgCheck: pkg=" + hostPkg + " uid=" + hostUid + " belongs=" + belongs);
    }

    private static int detectFrameworkCallerPackageIndex(Object[] args, String methodName) {
        if ("registerReceiverWithFeature".equals(methodName)) {
            return args.length > 1 && args[1] instanceof String ? 1 : ArrayUtils.indexOfFirst(args, String.class);
        }
        if ("registerReceiver".equals(methodName) || "registerReceiverForAllUsers".equals(methodName)) {
            return args.length > 1 && args[1] instanceof String ? 1 : ArrayUtils.indexOfFirst(args, String.class);
        }
        return ArrayUtils.indexOfFirst(args, String.class);
    }

    private static int detectFrameworkUserIdIndex(Object[] args, String methodName) {
        if ("registerReceiverWithFeature".equals(methodName)) {
            if (args.length >= 9 && args[7] instanceof Integer && args[8] instanceof Integer) return 7;
            if (args.length >= 8 && args[7] instanceof Integer) return 7;
            return -1;
        }
        if ("registerReceiver".equals(methodName) || "registerReceiverForAllUsers".equals(methodName)) {
            if (args.length >= 7 && args[6] instanceof Integer) return 6;
            return -1;
        }
        if ("broadcastIntent".equals(methodName) || "broadcastIntentWithFeature".equals(methodName)) {
            for (int i = args.length - 1; i >= 0; i--) {
                if (args[i] instanceof Integer) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int detectFrameworkUidIndex(Object[] args, String methodName) {
        if (methodName == null) return -1;
        if (methodName.contains("AppOps") || methodName.contains("check") || methodName.contains("note") || methodName.contains("startOp")) {
            int pkgIndex = ArrayUtils.indexOfFirst(args, String.class);
            if (pkgIndex > 0 && args[pkgIndex - 1] instanceof Integer) {
                return pkgIndex - 1;
            }
        }
        return -1;
    }


    public static int getRegisterReceiverUserIdIndex(Object[] args, String methodName) {
        return detectFrameworkUserIdIndex(args, methodName);
    }

    public static int getRegisterReceiverFlagsIndex(Object[] args, String methodName) {
        if ("registerReceiverWithFeature".equals(methodName) && args != null && args.length >= 9 && args[8] instanceof Integer) return 8;
        return -1;
    }

    public static int getRegisterReceiverCallerFeatureIdIndex(Object[] args, String methodName) {
        if ("registerReceiverWithFeature".equals(methodName) && args != null && args.length > 2 && args[2] instanceof String) return 2;
        return -1;
    }

    public static int getRegisterReceiverCallerPackageIndex(Object[] args, String methodName) {
        return detectFrameworkCallerPackageIndex(args, methodName);
    }
    private static boolean packageBelongsToUid(String pkg, int uid) {
        if (pkg == null) return false;
        PackageManager pm = BlackBoxCore.getContext().getPackageManager();
        try {
            String[] uidPackages = pm.getPackagesForUid(uid);
            if (uidPackages == null) return false;
            for (String it : uidPackages) {
                if (pkg.equals(it)) return true;
            }
            return false;
        } catch (Throwable ignored) {
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
