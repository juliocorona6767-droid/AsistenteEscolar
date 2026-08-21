package com.asistente.escolar;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;

public class AlarmService extends Service {
    private MediaPlayer player;
    private Vibrator vibrator;
    private static final int CHANNEL=7001, NOTIF=7002;

    @Override public void onCreate(){ super.onCreate(); createChannel(); }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel("alarm","Alarmas",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("Alarmas del Asistente Escolar");
            c.setSound(null,null);
            c.enableVibration(false);
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        Intent full=new Intent(this,MainActivity.class)
            .putExtra("alarm",true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,99,full,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"alarm"):new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_launcher)
         .setContentTitle("⏰ Asistente Escolar")
         .setContentText("¡Hora de levantarse!")
         .setCategory(Notification.CATEGORY_ALARM)
         .setPriority(Notification.PRIORITY_MAX)
         .setOngoing(true)
         .setAutoCancel(false)
         .setVisibility(Notification.VISIBILITY_PUBLIC)
         .setFullScreenIntent(pi,true);
        startForeground(NOTIF,b.build());
        startRinging();
        MainActivity.scheduleNextAlarm(this);
        return START_NOT_STICKY;
    }

    private void startRinging(){
        try{
            if(player!=null)return;
            String saved=getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE).getString(MainActivity.KEY_TONE,null);
            Uri u=null;
            if(saved!=null) try{u=Uri.parse(saved);}catch(Exception ignored){}
            if(u==null)u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(u==null)u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this,u);
            player.setLooping(true);
            player.setVolume(1f,1f);
            player.prepare();
            player.start();
        }catch(Exception e){
            try{ if(player!=null){player.release();player=null;} }catch(Exception ignored){}
        }
        try{
            vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
            if(vibrator!=null){
                long[] pattern={0,600,300,600,300,1000};
                if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createWaveform(pattern,0));
                else vibrator.vibrate(pattern,0);
            }
        }catch(Exception ignored){}
    }

    public static void stop(Context c){ c.stopService(new Intent(c,AlarmService.class)); }

    @Override public void onDestroy(){
        try{if(player!=null){player.stop();player.release();player=null;}}catch(Exception ignored){}
        try{if(vibrator!=null)vibrator.cancel();}catch(Exception ignored){}
        try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIF);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
