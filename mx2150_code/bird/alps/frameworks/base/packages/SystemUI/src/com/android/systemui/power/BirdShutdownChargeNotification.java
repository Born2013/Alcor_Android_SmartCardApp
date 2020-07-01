/*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]BEGIN */
package com.android.systemui.power;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.systemui.R;
import com.android.systemui.SystemUI;

import java.io.PrintWriter;
import java.text.NumberFormat;

public class BirdShutdownChargeNotification {
    private static final String TAG = "BirdShutdownChargeNotification";

    private static final String TAG_NOTIFICATION = "shutdown_charge";

    private final Context mContext;
    private final NotificationManager mNoMan;
    private boolean mShowing;
    
    private static final String ACTION_DISMISSED_NOTIFICATION = "BirdShutdownChargeNotification.dismissed";
    private static final String ACTION_NOTHING = "BirdShutdownChargeNotification.Nothing";

    private final Handler mHandler = new Handler();

    public BirdShutdownChargeNotification(Context context) {
        mContext = context;
        mNoMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showNotification() {
        final Notification.Builder nb = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.zzzzz_tip_icon)
                // Bump the notification when the bucket dropped.
                .setWhen(System.currentTimeMillis())
                .setShowWhen(false)
                .setContentTitle(mContext.getString(R.string.bird_shotdown_charge_suggestion_title))
                .setContentText(mContext.getString(R.string.bird_shotdown_charge_suggestion))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(0xfffee38a);
                        
        Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.bigText(mContext.getString(R.string.bird_shotdown_charge_suggestion));
        nb.setStyle(style);
    
        nb.setFullScreenIntent(pendingBroadcast(ACTION_NOTHING), true);
        //attachSound(nb);
        
        SystemUI.overrideNotificationAppName(mContext, nb);
        mNoMan.notifyAsUser(TAG_NOTIFICATION, R.id.bird_notification_shotdown_charge_suggestion, nb.build(), UserHandle.ALL);
        mShowing = true;
        
    }
    
    private PendingIntent pendingBroadcast(String action) {
        return PendingIntent.getBroadcastAsUser(mContext,
                0, new Intent(action), 0, UserHandle.CURRENT);
    }

    public void dismissNotification() {
        if (mShowing) {
            mNoMan.cancelAsUser(TAG_NOTIFICATION, R.id.bird_notification_shotdown_charge_suggestion, UserHandle.ALL);
            mShowing = false;
        }
    }
    
    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build();
    
    private void attachSound(Notification.Builder b) {
        final ContentResolver cr = mContext.getContentResolver();
        final String soundPath = Settings.Global.getString(cr, Settings.Global.LOW_BATTERY_SOUND);
        if (soundPath != null) {
            final Uri soundUri = Uri.parse("file://" + soundPath);
            if (soundUri != null) {
                b.setSound(soundUri, AUDIO_ATTRIBUTES);
            }
        }
    }
}
/*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]END */
