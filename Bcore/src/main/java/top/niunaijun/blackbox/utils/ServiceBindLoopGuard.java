package top.niunaijun.blackbox.utils;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ServiceBindLoopGuard {
    private static final String TAG = "ServiceBindGuard";
    private static final long TTL_MS = 8000L;
    private static final int MAX = 256;

    private static final Map<String, Long> RECENT = new LinkedHashMap<String, Long>(MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX;
        }
    };

    private ServiceBindLoopGuard() {}

    public static boolean isDuplicate(Intent intent, String callerPackage, int callerUid, int virtualUserId, String requestId) {
        String key = key(intent, callerPackage, callerUid, virtualUserId, requestId);
        long now = SystemClock.elapsedRealtime();
        synchronized (RECENT) {
            Long prev = RECENT.get(key);
            RECENT.put(key, now);
            boolean dup = prev != null && now - prev < TTL_MS;
            if (dup) {
                Slog.w(TAG, "actionTaken=BLOCK_LOOP requestId=" + requestId + " key=" + key);
            }
            return dup;
        }
    }

    private static String key(Intent i, String pkg, int uid, int userId, String reqId) {
        ComponentName cn = i != null ? i.getComponent() : null;
        String extras = i != null && i.getExtras() != null ? i.getExtras().keySet().toString() : "";
        return String.valueOf(cn) + "|" + pkg + "|" + uid + "|" + userId + "|" + reqId + "|" + extras;
    }
}
