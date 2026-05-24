package top.niunaijun.blackbox.proxy.record;

import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import top.niunaijun.blackbox.utils.ServiceIntentRegistry;
import top.niunaijun.blackbox.utils.compat.BundleCompat;


public class ProxyServiceRecord {
    public Intent mServiceIntent;
    public ServiceInfo mServiceInfo;
    public IBinder mToken;
    public int mUserId;
    public int mStartId;

    public ProxyServiceRecord(Intent serviceIntent, ServiceInfo serviceInfo, IBinder token, int userId, int startId) {
        mServiceIntent = serviceIntent;
        mServiceInfo = serviceInfo;
        mUserId = userId;
        mStartId = startId;
        mToken = token;
    }

    public static void saveStub(Intent shadow, Intent target, ServiceInfo serviceInfo, IBinder token, int userId, int startId) {
        String requestId = shadow.getStringExtra("blackbox.proxy.request_id");
        if (requestId == null) {
            requestId = java.util.UUID.randomUUID().toString();
            shadow.putExtra("blackbox.proxy.request_id", requestId);
        }
        ServiceIntentRegistry.put(requestId, new ServiceIntentRegistry.ServiceDispatchRecord(target, serviceInfo, token, userId, startId));
        shadow.putExtra("_B_|_request_id_", requestId);
        shadow.putExtra("_B_|_user_id_", userId);
        shadow.putExtra("_B_|_start_id_", startId);
        BundleCompat.putBinder(shadow, "_B_|_token_", token);
    }

    public static ProxyServiceRecord create(Intent intent) {
        String requestId = intent.getStringExtra("_B_|_request_id_");
        ServiceIntentRegistry.ServiceDispatchRecord rec = ServiceIntentRegistry.get(requestId);
        if (rec != null) {
            return new ProxyServiceRecord(rec.targetIntent, rec.serviceInfo, rec.token, rec.userId, rec.startId);
        }
        Intent target = intent.getParcelableExtra("_B_|_target_");
        ServiceInfo serviceInfo = intent.getParcelableExtra("_B_|_service_info_");
        int userId = intent.getIntExtra("_B_|_user_id_", 0);
        int startId = intent.getIntExtra("_B_|_start_id_", 0);
        IBinder token = BundleCompat.getBinder(intent, "_B_|_token_");
        return new ProxyServiceRecord(target, serviceInfo, token, userId, startId);
    }
}
