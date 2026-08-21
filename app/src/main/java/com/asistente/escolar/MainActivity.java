package com.asistente.escolar;

import android.app.*;
import android.content.*;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.webkit.*;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    public static final String PREFS="alarm_prefs";
    public static final String KEY_HOUR="hour", KEY_MINUTE="minute", KEY_ENABLED="enabled", KEY_TONE="tone";
    public static final int REQ_NOTIF=44, REQ_TONE=45;
    private WebView web;
    private TextToSpeech tts;
    private String pendingSpeech;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=27){ getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); }
        web=new WebView(this); setupWeb(); setContentView(web);
        requestNotifications();
        if(getIntent().getBooleanExtra("alarm",false)) web.postDelayed(this::showAlarm,350);
    }

    private void setupWeb(){
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setMediaPlaybackRequiresUserGesture(false);
        web.setBackgroundColor(0xff080d19);
        web.addJavascriptInterface(new Bridge(),"AndroidBridge");
        web.setWebViewClient(new WebViewClient());
        web.loadUrl("file:///android_asset/index.html");
    }

    private void showAlarm(){ web.evaluateJavascript("window.nativeAlarm&&window.nativeAlarm();",null); }

    private void requestNotifications(){ if(Build.VERSION.SDK_INT>=33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},REQ_NOTIF); }

    public class Bridge {
        @JavascriptInterface public void scheduleAlarm(int h,int m,boolean enabled){
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt(KEY_HOUR,h).putInt(KEY_MINUTE,m).putBoolean(KEY_ENABLED,enabled).apply();
            if(enabled) MainActivity.scheduleAlarm(MainActivity.this,h,m,true); else MainActivity.cancelAlarm(MainActivity.this);
        }
        @JavascriptInterface public void stopAlarm(String message){
            AlarmService.stop(MainActivity.this);
            if(message!=null && !message.trim().isEmpty()) speak(message);
        }
        @JavascriptInterface public boolean canScheduleExact(){
            return Build.VERSION.SDK_INT<31 || ((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();
        }
        @JavascriptInterface public void openExactAlarmSettings(){
            if(Build.VERSION.SDK_INT>=31){ try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}catch(Exception ignored){} }
        }
        @JavascriptInterface public void chooseAlarmTone(){
            try{
                Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Elegir tono de alarma");
                String saved=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_TONE,null);
                if(saved!=null)i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,Uri.parse(saved));
                startActivityForResult(i,REQ_TONE);
            }catch(Exception ignored){}
        }
        @JavascriptInterface public String getAlarmToneName(){
            String saved=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_TONE,null);
            if(saved==null)return "Tono predeterminado";
            try{String n=RingtoneManager.getRingtone(MainActivity.this,Uri.parse(saved)).getTitle(MainActivity.this);return n==null?"Tono personalizado":n;}catch(Exception e){return "Tono personalizado";}
        }
        @JavascriptInterface public void testAlarmTone(){
            try{
                String saved=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_TONE,null);
                Uri u=saved==null?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(saved);
                RingtoneManager.getRingtone(MainActivity.this,u).play();
            }catch(Exception ignored){}
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_TONE && resultCode==RESULT_OK && data!=null){
            Uri u=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_TONE,u==null?null:u.toString()).apply();
            if(web!=null) web.evaluateJavascript("window.onNativeToneChanged&&window.onNativeToneChanged();",null);
        }
    }

    private void speak(String text){
        pendingSpeech=text;
        if(tts==null){
            tts=new TextToSpeech(this,status->{
                if(status==TextToSpeech.SUCCESS){
                    tts.setLanguage(new Locale("es","MX")); tts.setSpeechRate(.9f); tts.setPitch(1f); speakNow();
                }
            });
        }else speakNow();
    }
    private void speakNow(){
        if(tts==null||pendingSpeech==null)return;
        try{tts.stop(); tts.speak(pendingSpeech,TextToSpeech.QUEUE_FLUSH,null,"asistente_resumen");}catch(Exception ignored){}
    }

    public static void scheduleSavedAlarm(Context c){
        android.content.SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean(KEY_ENABLED,false))return;
        scheduleAlarm(c,p.getInt(KEY_HOUR,7),p.getInt(KEY_MINUTE,0),true);
    }
    public static void scheduleNextAlarm(Context c){
        scheduleSavedAlarm(c);
    }
    public static void scheduleAlarm(Context c,int h,int m,boolean enabled){
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p=alarmPendingIntent(c);
        if(!enabled){a.cancel(p);return;}
        Calendar cal=Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,h); cal.set(Calendar.MINUTE,m); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
        if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);
        long when=cal.getTimeInMillis();
        try{
            if(Build.VERSION.SDK_INT>=31){
                if(a.canScheduleExactAlarms())a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p); else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
            }else if(Build.VERSION.SDK_INT>=23)a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
            else a.setExact(AlarmManager.RTC_WAKEUP,when,p);
        }catch(SecurityException e){a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);}
    }
    private static PendingIntent alarmPendingIntent(Context c){
        Intent i=new Intent(c,AlarmReceiver.class);
        return PendingIntent.getBroadcast(c,77,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancelAlarm(Context c){((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(alarmPendingIntent(c));}

    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("alarm",false))web.postDelayed(this::showAlarm,250);}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();tts=null;}super.onDestroy();}
}
