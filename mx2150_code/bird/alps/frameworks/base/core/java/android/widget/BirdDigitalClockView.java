package android.widget;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews.RemoteView;
import android.widget.TextView;
import java.util.TimeZone;
import com.android.internal.R;
/*[BIRD][BIRD_DIGITAL_CLOCK_ALIGN_LEFT][数字时钟插件靠左显示][zhangaman][20170301]begin*/
import android.os.SystemProperties;
/*[BIRD][BIRD_DIGITAL_CLOCK_ALIGN_LEFT][数字时钟插件靠左显示][zhangaman][20170301]end*/
import java.util.Locale;
import android.text.format.DateFormat;
import android.util.TypedValue;
import libcore.icu.LocaleData;

/*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN*/
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentResolver;
/*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 END*/

/**@hide*/
@RemoteView
public class BirdDigitalClockView extends LinearLayout {
    final static int[] MONTH_RESIDS = {
        0, R.string.month1, R.string.month2, R.string.month3, R.string.month4, R.string.month5,
        R.string.month6, R.string.month7, R.string.month8, R.string.month9, R.string.month10,
        R.string.month11, R.string.month12,
    };

    final static int[] WEEK_RESIDS = {
        0, R.string.week_sunday, R.string.week_monday, R.string.week_tuesday,
        R.string.week_wednesday, R.string.week_thursday, R.string.week_friday, R.string.week_saturday,
    };

    private String TAG = "BirdDigitalClockView";
    private Context mContext;
    private int hour,minute,month,day,weekday,dayHour;
    private boolean mAttached;
    private LayoutInflater mLayoutInflater;
    private Calendar mCalendar;
    private LinearLayout mClickAreaLinearLayout;
    private TextView mHour;
    //private TextView mMinute;
    private TextView mTimeAmPm;
    private TextView mDateView;
    private TextView mWeekView;
    private View mTimeLayout;

    public BirdDigitalClockView(Context context) {
        // TODO Auto-generated constructor stub
        super(context);
        init(context);
    }

    public BirdDigitalClockView(Context context, AttributeSet attrs) {
        // TODO Auto-generated constructor stub
        super(context, attrs);
        init(context);
    }

    public BirdDigitalClockView(Context context, AttributeSet attrs, int defStyle) {
        // TODO Auto-generated constructor stub
        super(context, attrs, defStyle);
        init(context);
    }

    void init(Context context){
        Log.i(TAG,"init(Context context)");

        mContext = context;
        //mCalendar = Calendar.getInstance(TimeZone.getDefault());
        mCalendar = Calendar.getInstance();
        mLayoutInflater = LayoutInflater.from(context);
        /*[BIRD][BIRD_DIGITAL_CLOCK_ALIGN_LEFT][数字时钟插件靠左显示][zhangaman][20170301]begin*/
        //[BIRD][BIRD_KUSAI_DIGITAL_CLOCK][数字时钟库赛风格][chenguangxiang][20170626] begin
        View view;
        if (SystemProperties.get("ro.bd_digi_clock_left").equals("1")) {
            view = mLayoutInflater.inflate(R.layout.bird_digital_clock_layout_align_left, this);
        } else if (SystemProperties.get("ro.bd_kusai_dialog_clock").equals("1")) {
            view = mLayoutInflater.inflate(R.layout.bird_kusai_digital_clock_layout, this);
        } else {
            view = mLayoutInflater.inflate(R.layout.bird_digital_clock_layout, this);
        }
        //[BIRD][BIRD_KUSAI_DIGITAL_CLOCK][数字时钟库赛风格][chenguangxiang][20170626] end
        /*[BIRD][BIRD_DIGITAL_CLOCK_ALIGN_LEFT][数字时钟插件靠左显示][zhangaman][20170301]end*/
        mTimeLayout = view.findViewById(R.id.time_layout);
        mClickAreaLinearLayout = (LinearLayout) view.findViewById(R.id.click_area);
        mHour = (TextView) findViewById(R.id.hour);
        //mMinute = (TextView) findViewById(R.id.minute);
        mTimeAmPm = (TextView) findViewById(R.id.time_am_pm);
        mDateView = (TextView) findViewById(R.id.date_view);
        mWeekView = (TextView) findViewById(R.id.week_view);

        mClickAreaLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"onClick BIRD_AIMEI_STUDENT = "+BIRD_AIMEI_STUDENT);
                // TODO Auto-generated method stub
                /*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN*/
                if (BIRD_AIMEI_STUDENT) {
                       if (!isForbid(mContext)) {
                            Intent intentDate = new Intent("android.settings.DATE_SETTINGS");
                            intentDate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                            mContext.startActivity(intentDate);
                       }
                } else {
                    Intent intentDate = new Intent("android.settings.DATE_SETTINGS");
                    intentDate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    mContext.startActivity(intentDate);
                }
               /*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN*/
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            registerReceiver();
            onTimeChanged();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            unregisterReceiver();
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"onReceive");
            String action = intent.getAction();
            Log.i(TAG,"action : "+action);
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                //mCalendar = Calendar.getInstance(TimeZone.getDefault());
                final String timeZone = intent.getStringExtra("time-zone");
                createCalendar(timeZone);
            }
            onTimeChanged();
        }
    };

    private void createCalendar(String timeZone) {
        Log.d(TAG,"createCalendar = "+timeZone);
        if (timeZone != null) {
            mCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            mCalendar = Calendar.getInstance();
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    private void onTimeChanged() {
        Log.i(TAG,"onTimeChanged");
        createTime();
        refreshTime();
        refreshDate();
    }

    void createTime(){
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        if(getTimeFormat24()){
            hour = mCalendar.get(Calendar.HOUR_OF_DAY);
            minute = mCalendar.get(Calendar.MINUTE);
        }else{
            hour = mCalendar.get(Calendar.HOUR);
            minute = mCalendar.get(Calendar.MINUTE);
            if(hour == 0){
                hour = 12;
            }
        }
        month = mCalendar.get(Calendar.MONTH) + 1;
        day = mCalendar.get(Calendar.DAY_OF_MONTH);
        weekday = mCalendar.get(Calendar.DAY_OF_WEEK);
        dayHour = mCalendar.get(Calendar.HOUR_OF_DAY);
    }

    void refreshTime(){
        Log.i(TAG,"refreshTime");
        CharSequence pattern;
        LocaleData localeData = LocaleData.get(Locale.getDefault());
        //mMinute.setText(getMinuteString(minute));
        if(getTimeFormat24() == true){
            pattern = getDateFormatText("Hm");
            mTimeAmPm.setVisibility(View.GONE);
        } else {
            pattern = getDateFormatText("hm");
            mTimeAmPm.setVisibility(View.VISIBLE);
            /*if (mCalendar.get(Calendar.AM_PM) == 0) {
                String am = mContext.getResources().getString(R.string.am);
                mTimeAmPm.setText(am);
            } else {
                String pm = mContext.getResources().getString(R.string.pm);
                mTimeAmPm.setText(pm);
            }*/

            String ampm = localeData.amPm[mCalendar.get(Calendar.AM_PM) - Calendar.AM];
            mTimeAmPm.setText(ampm);
            Log.d(TAG,"getDateFormatText ampm = "+ampm);
        }
        mHour.setText(pattern);
    }

    public CharSequence getDateFormatText(String format) {
        Log.d(TAG,"getDateFormatText format = "+format);
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), format);
        if (!format.contains("a")) {
            pattern = pattern.replaceAll("a", "").trim();
        }
        CharSequence text = DateFormat.format(pattern, mCalendar);
        return text;
    }

    void refreshDate(){
        Log.i(TAG,"refreshDate");
    	mDateView.setText(getDateFormatText("MMMd"));
    	//[BIRD][BIRD_KUSAI_DIGITAL_CLOCK][数字时钟库赛风格][chenguangxiang][20170626] begin
    	if (SystemProperties.get("ro.bd_kusai_dialog_clock").equals("1")) {
    	    mWeekView.setText("   "+getDateFormatText("EE"));
    	} else {
    	    mWeekView.setText(getDateFormatText("EEEE"));
    	}
    	//[BIRD][BIRD_KUSAI_DIGITAL_CLOCK][数字时钟库赛风格][chenguangxiang][20170626] end
        Log.i(TAG,"refreshDate-->mDateView:"+mDateView+" mWeekView:"+mWeekView);
    }

    String getDateString(int monthIndex,int day){
        String month = mContext.getResources().getString(MONTH_RESIDS[monthIndex]);
        Log.i(TAG,"getDateString->month:"+month);
        return month + day + mContext.getResources().getString(R.string.end_mark);
    }

    String getWeekString(int weekIndex){
        String week = mContext.getResources().getString(WEEK_RESIDS[weekIndex]);
        Log.i(TAG,"getWeekString->week:"+week);
        return week;
    }
    String getHourString(int hour){
        String Hour = Integer.toString(hour);
        Hour = String.format("%02d", Integer.parseInt(Hour));
        Log.i(TAG,"getHourString->Hour:"+Hour);
        return Hour;
    }
    String getMinuteString(int minute){
        String Minute = Integer.toString(minute);
        Minute = String.format("%02d", Integer.parseInt(Minute));
        Log.i(TAG,"getMinuteString->Minute:"+Minute);
        return Minute;
    }

    boolean getTimeFormat24(){
        boolean is24Format = android.text.format.DateFormat.is24HourFormat(mContext);
        return is24Format;
    }

    /*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN*/
    public static final boolean BIRD_AIMEI_STUDENT =  SystemProperties.get("ro.bd_aimei_student").equals("1");
    private static final Uri mAppUri =
            Uri.parse("content://com.xycm.schoolbased.AppContentProvider/apps");
    private static final String COL_STATE = "state";
    private static final String[] COL_S = {
            COL_STATE
    };
    private static final String SELECTION = "pkgname='com.android.settings'";
    private static final int LOCK = 1;

    public static  boolean ISFORBID_SETTINGS = false;

    public  boolean isForbid(Context context) {
       ContentResolver resolver = context.getApplicationContext().getContentResolver();
        if (resolver !=null) {
            Cursor cursor = resolver.query(mAppUri, COL_S, SELECTION, null, null);
            android.util.Log.d("CAOYUANGUI", "cursor = "+cursor);
            if (cursor != null) {
                int stateIndex = cursor.getColumnIndex(COL_STATE);
                android.util.Log.d("CAOYUANGUI", "stateIndex = "+stateIndex);
                while (cursor.moveToNext()) {
                int state = cursor.getInt(stateIndex);
                android.util.Log.d("CAOYUANGUI", "state = "+state);
                    if (state == LOCK) {
                    return true;
                    }
                }
            }
            return false;
        } else {
            return true;
        }

    }
    /*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 END*/

}
