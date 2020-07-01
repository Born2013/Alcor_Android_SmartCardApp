package android.widget;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActivityThread;
import android.widget.RemoteViews.RemoteView;

/*[BIRD][BIRD_OLD_PHONE][chengci][20170828] add*/
/**
 * @hide
 */
@RemoteView
public class ToastOldPhone {
    private Toast mToast;
    private static Context mContext = ActivityThread.currentApplication().getApplicationContext();
    private static int toastMarginBottom =  (int) mContext.getResources().
                            getDimension(com.android.internal.R.dimen.oldphone_toast_margin_bottom);
    
    private ToastOldPhone(Context context, CharSequence text, int duration) {
        View v = LayoutInflater.from(context).inflate(com.android.internal.R.layout.common_toast_birdoldphone, null);
        TextView textView = (TextView) v.findViewById(com.android.internal.R.id.text_toast_content_oldphone);
        textView.setText(text);
        mToast = new Toast(context);
        mToast.setDuration(duration);
        mToast.setGravity(Gravity.BOTTOM, Gravity.CENTER, toastMarginBottom);
        mToast.setView(v);
    }
    
    private ToastOldPhone(Context context, int id, int duration) {
        View v = LayoutInflater.from(context).inflate(com.android.internal.R.layout.common_toast_birdoldphone, null);
        TextView textView = (TextView) v.findViewById(com.android.internal.R.id.text_toast_content_oldphone);
        textView.setText(id);
        mToast = new Toast(context);
        mToast.setDuration(duration);
        mToast.setGravity(Gravity.BOTTOM, Gravity.CENTER, toastMarginBottom);
        mToast.setView(v);
    }

    public static ToastOldPhone makeText(Context context, CharSequence text, int duration) {
        return new ToastOldPhone(context, text, duration);
    }
    
    public static ToastOldPhone makeText(Context context, int id, int duration) {
        return new ToastOldPhone(context, id, duration);
    }
    
    public void show() {
        if (mToast != null) {
            mToast.show();
        }
    }
    
    public void setGravity(int gravity, int xOffset, int yOffset) {
        if (mToast != null) {
            mToast.setGravity(gravity, xOffset, yOffset);
        }
    }
}

