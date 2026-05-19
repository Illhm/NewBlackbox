package black.android.app;

import android.os.IBinder;

import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BStaticMethod;

@BClassName("android.app.ILocaleManager$Stub")
public interface BRILocaleManagerStub {
    @BStaticMethod
    Object asInterface(IBinder binder);
}
