package com.kylothow.myims;

import android.app.IActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.ServiceManager;
import android.system.Os;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import rikka.shizuku.ShizukuBinderWrapper;

public class PrivilegedProcess extends Instrumentation {

    @Override
    public void onCreate(Bundle arguments) {
        try {
            overrideConfig();
        } catch (Exception e) {
            Log.e(PrivilegedProcess.class.getSimpleName(), Log.getStackTraceString(e));
        }
        finish(0, new Bundle());
    }

    private void overrideConfig() throws Exception {
        var binder = ServiceManager.getService(Context.ACTIVITY_SERVICE);
        var am = IActivityManager.Stub.asInterface(new ShizukuBinderWrapper(binder));
        am.startDelegateShellPermissionIdentity(Os.getuid(), null);
        try {
            var cm = getContext().getSystemService(CarrierConfigManager.class);
            var sm = getContext().getSystemService(SubscriptionManager.class);
            var values = getConfig();
            for (var subId : sm.getActiveSubscriptionIdList()) {
                var bundle = cm.getConfigForSubId(subId, "vvb2060_config_version");
                if (bundle.getInt("vvb2060_config_version", 0) != BuildConfig.VERSION_CODE) {
                    values.putInt("vvb2060_config_version", BuildConfig.VERSION_CODE);
                    cm.overrideConfig(subId, values);
                }
            }
        } finally {
            am.stopDelegateShellPermissionIdentity();
        }
    }

    private static PersistableBundle getConfig() {
        var bundle = new PersistableBundle();
        // Voice & IMS
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);
        // Network Icons
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false);
        bundle.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, true);
        // 5G Settings
        bundle.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA});
        return bundle;
    }
}
