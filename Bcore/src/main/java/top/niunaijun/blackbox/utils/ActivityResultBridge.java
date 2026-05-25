package top.niunaijun.blackbox.utils;

import android.content.Intent;
import android.os.IBinder;

import java.util.concurrent.ConcurrentHashMap;

public final class ActivityResultBridge {
    public static final class RequestRecord {
        public final String requestId;
        public final String virtualCallerPackage;
        public final int originalRequestCode;
        public final IBinder resultTo;
        public final Intent originalIntent;

        public RequestRecord(String requestId, String virtualCallerPackage, int originalRequestCode, IBinder resultTo, Intent originalIntent) {
            this.requestId = requestId;
            this.virtualCallerPackage = virtualCallerPackage;
            this.originalRequestCode = originalRequestCode;
            this.resultTo = resultTo;
            this.originalIntent = originalIntent;
        }
    }

    private static final ConcurrentHashMap<String, RequestRecord> RECORDS = new ConcurrentHashMap<>();

    private ActivityResultBridge() {}

    public static void put(RequestRecord record) {
        if (record != null && record.requestId != null) {
            RECORDS.put(record.requestId, record);
        }
    }

    public static RequestRecord take(String requestId) {
        if (requestId == null) return null;
        return RECORDS.remove(requestId);
    }
}
