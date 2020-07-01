//[BIRD][BIRD_CLEAN_RECENT_APP_WOTU][喔图 每小时自动清理recent app][yangheng][20170508] BEGIN
package com.android.systemui.recents;

import android.content.Context;
import android.content.Intent;

import android.app.Service;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;

import android.os.SystemClock;
import android.os.IBinder;

public class BirdCleanService extends Service {
    
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Long firstTime = SystemClock.elapsedRealtime();
        Intent intent = new Intent(this, com.android.systemui.recents.BootCompeleteBroadcastReceiver.class);
        intent.setAction("com.android.systemui.recent.cleanallapp");
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, AlarmManager.INTERVAL_HOUR, sender);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent i = new Intent(this, BirdCleanService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(i);
    }

}
//[BIRD][BIRD_CLEAN_RECENT_APP_WOTU][喔图 每小时自动清理recent app][yangheng][20170508] END
