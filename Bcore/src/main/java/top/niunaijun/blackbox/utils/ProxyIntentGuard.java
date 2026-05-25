package top.niunaijun.blackbox.utils;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ProxyIntentGuard {
    public static final String EXTRA_DISPATCHED = "blackbox.proxy.dispatched";
    public static final String EXTRA_REQUEST_ID = "blackbox.proxy.request_id";
    private static final long TTL_MS = 8_000L;
    private static final int MAX_ENTRIES = 256;

    private static final Map<String, Long> RECENT = new LinkedHashMap<String, Long>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private ProxyIntentGuard() {}

    public static String ensureRequestId(Intent intent) {
        String id = intent.getStringExtra(EXTRA_REQUEST_ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
            intent.putExtra(EXTRA_REQUEST_ID, id);
        }
        return id;
    }

    public static boolean isSelfProxyTarget(Intent intent, String hostPkg) {
        ComponentName cn = intent.getComponent();
        return cn != null && hostPkg.equals(cn.getPackageName())
                && (cn.getClassName().contains(".proxy.ProxyActivity") || cn.getClassName().contains(".proxy.ProxyService"));
    }

    public static boolean shouldDropAsLoop(Intent intent, String callerPackage) {
        if (intent == null) return false;
        long now = SystemClock.elapsedRealtime();
        String key = buildKey(intent, callerPackage);
        synchronized (RECENT) {
            Long prev = RECENT.get(key);
            RECENT.put(key, now);
            if (prev != null && now - prev < TTL_MS) {
                return true;
            }
        }
        return false;
    }

    private static String buildKey(Intent intent, String callerPackage) {
        ComponentName cn = intent.getComponent();
        return String.valueOf(intent.getAction()) + "|"
                + (cn != null ? cn.flattenToShortString() : "") + "|"
                + String.valueOf(intent.getPackage()) + "|"
                + String.valueOf(callerPackage);
    }
}
