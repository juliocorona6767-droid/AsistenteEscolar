package com.asistente.escolar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_ALARM = "com.asistente.escolar.ALARM";
    @Override public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, AlarmService.class).setAction(ACTION_ALARM);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }
}
