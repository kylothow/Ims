package com.kylothow.myims;

import static rikka.shizuku.ShizukuProvider.METHOD_GET_BINDER;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.permission.PermissionManager;
import android.system.Os;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import rikka.shizuku.ShizukuBinderWrapper;

public class PrivilegedProcess extends Instrumentation {
    static final String TAG = "vvb";

    @Override
    public void onCreate(Bundle arguments) {
        var context = getContext();
        if (Process.isSdkSandbox()) {
            var extras = makeExtras(context);
            var cr = getContext().getContentResolver();
            cr.call(BuildConfig.APPLICATION_ID + ".shizuku", METHOD_GET_BINDER, null, extras);
        } else if (arguments.getInt("pid", 0) == Process.myPid()) {
            var binder = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            var am = IActivityManager.Stub.asInterface(new ShizukuBinderWrapper(binder));
            try {
                am.startDelegateShellPermissionIdentity(Os.getuid(), null);
                grantPermission(context);
                overrideConfig(context, false);
                am.stopDelegateShellPermissionIdentity();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            finish(0, new Bundle());
        } else {
            finish(0, new Bundle());
        }
    }

    private Bundle makeExtras(Context context) {
        var binder = new Binder() {
            @Override
            protected boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
                if (code == 1) {
                    try {
                        grantPermission(context);
                        overrideConfig(context, true);
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    var handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> finish(0, new Bundle()), 1000);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            }
        };
        var extras = new Bundle();
        extras.putBinder("binder", binder);
        return extras;
    }

    @SuppressLint("MissingPermission")
    private static void grantPermission(Context context) {
        var pm = context.getSystemService(PermissionManager.class);
        pm.grantRuntimePermission(BuildConfig.APPLICATION_ID,
                android.Manifest.permission.READ_PHONE_STATE, Process.myUserHandle());
    }

    @SuppressLint("MissingPermission")
    private static void overrideConfig(Context context, boolean persistent) {
        var cm = context.getSystemService(CarrierConfigManager.class);
        var sm = context.getSystemService(SubscriptionManager.class);
        var values = getConfig();
        for (var subId : sm.getActiveSubscriptionIdList()) {
            values.putInt("vvb2060_config_version", BuildConfig.VERSION_CODE);
            try {
                cm.overrideConfig(subId, values, persistent);
            } catch (SecurityException e) {
                Log.w(TAG, "overrideConfig failed for subId " + subId, e);
                if (persistent) {
                    persistent = false;
                    cm.overrideConfig(subId, values, persistent);
                }
            }
            var bundle = cm.getConfigForSubId(subId, "vvb2060_config_version");
            if (bundle.getInt("vvb2060_config_version", 0) == BuildConfig.VERSION_CODE) {
                Log.i(TAG, "overrideConfig succeeded for subId " + subId + ", persistent=" + persistent);
            } else {
                Log.e(TAG, "overrideConfig failed for subId " + subId + ", persistent=" + persistent);
            }
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
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA});
        return bundle;
    }
}
