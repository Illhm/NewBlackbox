import re

filepath = "Bcore/src/main/java/top/niunaijun/blackbox/fake/service/IActivityManagerProxy.java"

with open(filepath, "r") as f:
    content = f.read()

# 1. Update checkPermission
check_permission_target = """            if (permission.equals(Manifest.permission.ACCOUNT_MANAGER)
                    || permission.equals(Manifest.permission.SEND_SMS)) {
                return PackageManager.PERMISSION_GRANTED;
            }"""

check_permission_replacement = """            if (permission.equals(Manifest.permission.ACCOUNT_MANAGER)
                    || permission.equals(Manifest.permission.SEND_SMS)
                    || permission.equals("com.google.android.gms.permission.INTERNAL_BROADCAST")) {
                return PackageManager.PERMISSION_GRANTED;
            }"""

content = content.replace(check_permission_target, check_permission_replacement)


# 2. Update GetContentProvider safe invocation
# We need to replace `method.invoke(who, args)` inside GetContentProvider with a safe wrapper.
# Since it appears multiple times, we'll replace the whole GetContentProvider class method body

new_get_content_provider = """    @ProxyMethod("getContentProvider")
    public static class GetContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int authIndex = getAuthIndex();
            Object auth = args[authIndex];
            Object content = null;

            if (auth instanceof String) {
                String authStr = (String) auth;
                if (ProxyManifest.isProxy(authStr)) {
                    return safeInvoke(method, who, args, authStr);
                }

                if (BuildCompat.isQ()) {
                    args[1] = BlackBoxCore.getHostPkg();
                }

                int userId = BActivityThread.getUserId();
                boolean isGmsAuth = authStr.contains("com.google.android.gms")
                        || authStr.contains("com.android.vending")
                        || authStr.contains("com.google.android.gsf");
                boolean isSystemAuth = authStr.equals("settings")
                        || authStr.equals("media")
                        || authStr.equals("telephony")
                        || authStr.equals("com.huawei.android.launcher.settings")
                        || authStr.equals("com.hihonor.android.launcher.settings");
                boolean sandboxHasGms = isGmsAuth
                        && BlackBoxCore.get().isInstallGms(userId);
                boolean forceVirtualProvider = shouldForceVirtualProvider(authStr)
                        && sandboxHasGms;

                if (isSystemAuth || (isGmsAuth && !sandboxHasGms)) {
                    content = safeInvoke(method, who, args, authStr);
                    ContentProviderDelegate.update(content, authStr);
                    return content;
                }

                ProviderInfo providerInfo = BlackBoxCore.getBPackageManager()
                        .resolveContentProvider(authStr, GET_META_DATA, userId);
                if (providerInfo == null) {
                    if (forceVirtualProvider) {
                        Slog.w(TAG, "Force-virtual provider not declared in sandbox: " + authStr
                                + " user=" + userId + ", blocking host fallback.");
                        return null;
                    }
                    if (isGmsAuth) {
                        Slog.w(TAG, "Sandbox GMS does not declare provider " + authStr
                                + " for user " + userId + ", falling back to host.");
                        content = safeInvoke(method, who, args, authStr);
                        ContentProviderDelegate.update(content, authStr);
                        return content;
                    }
                    return null;
                }

                IBinder providerBinder = null;
                if (BActivityThread.getAppPid() != -1) {
                    AppConfig appConfig = BlackBoxCore.getBActivityManager()
                            .initProcess(
                                    providerInfo.packageName,
                                    providerInfo.processName,
                                    userId);
                    if (appConfig.bpid != BActivityThread.getAppPid()) {
                        providerBinder = BlackBoxCore.getBActivityManager()
                                .acquireContentProviderClient(providerInfo);
                    }
                    args[authIndex] = ProxyManifest.getProxyAuthorities(appConfig.bpid);
                    args[getUserIndex()] = BlackBoxCore.getHostUserId();
                }
                if (providerBinder == null) {
                    if (forceVirtualProvider) {
                        Slog.w(TAG, "Force-virtual provider bind failed: " + authStr
                                + " user=" + userId + ", blocking host fallback.");
                        return null;
                    }
                    if (isGmsAuth) {
                        Slog.w(TAG, "Sandbox GMS provider " + authStr
                                + " could not be bound for user " + userId
                                + ", falling back to host.");
                        args[authIndex] = authStr;
                        args[getUserIndex()] = userId;
                        content = safeInvoke(method, who, args, authStr);
                        ContentProviderDelegate.update(content, authStr);
                        return content;
                    }
                    return null;
                }

                content = safeInvoke(method, who, args, authStr);
                if (content != null) {
                    Reflector.with(content).field("info").set(providerInfo);
                    Reflector.with(content)
                            .field("provider")
                            .set(
                                    new ContentProviderStub()
                                            .wrapper(
                                                    BRContentProviderNative.get().asInterface(providerBinder),
                                                    providerInfo.packageName));
                }
                return content;
            }
            return method.invoke(who, args);
        }

        private Object safeInvoke(Method method, Object who, Object[] args, String authStr) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof SecurityException) {
                    Slog.w(TAG, "SecurityException when fetching provider: " + authStr + " (" + e.getCause().getMessage() + "), returning null");
                    return null;
                }
                throw e;
            } catch (SecurityException e) {
                Slog.w(TAG, "SecurityException when fetching provider: " + authStr + " (" + e.getMessage() + "), returning null");
                return null;
            }
        }

        protected int getAuthIndex() {"""

old_get_content_provider_pattern = re.compile(r'    @ProxyMethod\("getContentProvider"\)\s+public static class GetContentProvider extends MethodHook \{.*?protected int getAuthIndex\(\) \{', re.DOTALL)

content = old_get_content_provider_pattern.sub(new_get_content_provider, content)

with open(filepath, "w") as f:
    f.write(content)
