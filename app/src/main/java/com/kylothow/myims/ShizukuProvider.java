package com.kylothow.myims;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.UiAutomationConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.util.Log;

import org.lsposed.hiddenapibypass.LSPass;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;

public class ShizukuProvider extends rikka.shizuku.ShizukuProvider {

    @Override
    public boolean onCreate() {
        LSPass.setHiddenApiExemptions("");
        Shizuku.addBinderReceivedListener(() -> {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                startInstrument(getContext());
            }
        });
        return super.onCreate();
    }

    private static void startInstrument(Context context) {
        try {
            var binder = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            var am = IActivityManager.Stub.asInterface(new ShizukuBinderWrapper(binder));
            var name = new ComponentName(context, PrivilegedProcess.class);
            var flags = ActivityManager.INSTR_FLAG_NO_RESTART;
            var connection = new UiAutomationConnection();
            am.startInstrumentation(name, null, flags, new Bundle(), null, connection, 0, null);
        } catch (Exception e) {
            Log.e(ShizukuProvider.class.getSimpleName(), Log.getStackTraceString(e));
        }
    }
}
