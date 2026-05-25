package top.niunaijun.blackbox.utils;

import android.content.Intent;
import android.os.Bundle;

import java.util.Set;

public final class IntentSanitizer {
    private IntentSanitizer() {}

    public static Intent sanitize(Intent source, boolean trustedGms) {
        if (source == null) return null;
        Intent out = new Intent(source);
        out.setFlags(out.getFlags() & ~Intent.FLAG_GRANT_READ_URI_PERMISSION & ~Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (!trustedGms) {
            out.setClipData(null);
        }
        Bundle extras = out.getExtras();
        if (extras != null) {
            sanitizeBundle(extras, trustedGms);
            out.replaceExtras(extras);
        }
        return out;
    }

    private static void sanitizeBundle(Bundle b, boolean trustedGms) {
        Set<String> keys = b.keySet();
        for (String key : keys.toArray(new String[0])) {
            Object value = b.get(key);
            if (value instanceof Intent && !trustedGms) {
                b.remove(key);
            } else if (value instanceof Bundle) {
                sanitizeBundle((Bundle) value, trustedGms);
            } else if (value instanceof android.app.PendingIntent && !trustedGms) {
                b.remove(key);
            }
        }
    }
}
