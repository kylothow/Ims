package io.github.vvb2060.ims;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.ITelephony;

import org.lsposed.hiddenapibypass.LSPass;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class ShizukuProvider extends rikka.shizuku.ShizukuProvider {

    @Override
    public boolean onCreate() {
        LSPass.setHiddenApiExemptions("");
        Shizuku.addBinderReceivedListener(() -> {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                var subId = SubscriptionManager.getDefaultVoiceSubscriptionId();
                showVoLTE(subId);
            }
        });
        return super.onCreate();
    }

    private static void showVoLTE(int subId) {
        var binder = SystemServiceHelper.getSystemService(Context.TELEPHONY_SERVICE);
        var phone = ITelephony.Stub.asInterface(new ShizukuBinderWrapper(binder));
        var value = phone.getImsProvisioningInt(subId, 68);
        if (value == 1) return;
        phone.setImsProvisioningInt(subId, 68, 1);
    }
}
