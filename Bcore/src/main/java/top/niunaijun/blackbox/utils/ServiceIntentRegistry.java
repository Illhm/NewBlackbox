package top.niunaijun.blackbox.utils;

import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import java.util.concurrent.ConcurrentHashMap;

public final class ServiceIntentRegistry {
    public static final class ServiceDispatchRecord {
        public final Intent targetIntent;
        public final ServiceInfo serviceInfo;
        public final IBinder token;
        public final int userId;
        public final int startId;

        public ServiceDispatchRecord(Intent targetIntent, ServiceInfo serviceInfo, IBinder token, int userId, int startId) {
            this.targetIntent = targetIntent;
            this.serviceInfo = serviceInfo;
            this.token = token;
            this.userId = userId;
            this.startId = startId;
        }
    }

    private static final ConcurrentHashMap<String, ServiceDispatchRecord> RECORDS = new ConcurrentHashMap<>();

    private ServiceIntentRegistry() {}

    public static void put(String requestId, ServiceDispatchRecord record) {
        if (requestId != null && record != null) RECORDS.put(requestId, record);
    }

    public static ServiceDispatchRecord get(String requestId) {
        return requestId == null ? null : RECORDS.get(requestId);
    }
}
