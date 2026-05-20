package black.android.credentials;

import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BStaticMethod;

@BClassName("android.credentials.ICredentialManager")
public interface ICredentialManager {

    @BClassName("android.credentials.ICredentialManager$Stub")
    public interface Stub {
        @BStaticMethod
        Object asInterface(android.os.IBinder obj);
    }
}
