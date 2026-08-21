package com.asistente.escolar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
            !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) &&
            !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        MainActivity.scheduleSavedAlarm(context);
    }
}
