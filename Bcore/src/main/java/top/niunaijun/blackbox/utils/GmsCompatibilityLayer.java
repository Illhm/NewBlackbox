package top.niunaijun.blackbox.utils;

import android.content.ComponentName;
import android.content.Intent;

public final class GmsCompatibilityLayer {
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String VENDING_PKG = "com.android.vending";
    public static final String GSF_PKG = "com.google.android.gsf";

    public static final String ACTION_CHOOSE_ACCOUNT = "com.google.android.gms.common.account.CHOOSE_ACCOUNT";
    public static final String ACTION_CHOOSE_ACCOUNT_USERTILE = "com.google.android.gms.common.account.CHOOSE_ACCOUNT_USERTILE";
    public static final String ACTION_GOOGLE_SIGN_IN = "com.google.android.gms.auth.GOOGLE_SIGN_IN";

    private GmsCompatibilityLayer() {}

    public static boolean isGmsRelatedPackage(String pkg) {
        if (pkg == null) return false;
        return GMS_PKG.equals(pkg) || VENDING_PKG.equals(pkg) || GSF_PKG.equals(pkg)
                || pkg.startsWith(GMS_PKG + ".") || pkg.startsWith(VENDING_PKG + ".") || pkg.startsWith(GSF_PKG + ".");
    }

    public static boolean isGoogleLoginAction(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        return ACTION_CHOOSE_ACCOUNT.equals(action)
                || ACTION_CHOOSE_ACCOUNT_USERTILE.equals(action)
                || ACTION_GOOGLE_SIGN_IN.equals(action);
    }

    public static boolean isAccountPickerIntent(Intent intent) {
        if (intent == null) return false;
        ComponentName cn = intent.getComponent();
        if (cn == null) return isGoogleLoginAction(intent);
        return GMS_PKG.equals(cn.getPackageName())
                && "com.google.android.gms.common.account.AccountPickerActivity".equals(cn.getClassName());
    }

    public static boolean isFeedbackActivity(Intent intent) {
        if (intent == null || intent.getComponent() == null) return false;
        ComponentName cn = intent.getComponent();
        return GMS_PKG.equals(cn.getPackageName())
                && "com.google.android.gms.feedback.FeedbackActivity".equals(cn.getClassName());
    }

    public static boolean shouldBypassProxyForTrustedGms(Intent intent) {
        if (intent == null) return false;
        ComponentName cn = intent.getComponent();
        if (cn == null) return false;
        return isGmsRelatedPackage(cn.getPackageName()) && (isAccountPickerIntent(intent) || isFeedbackActivity(intent));
    }
}
