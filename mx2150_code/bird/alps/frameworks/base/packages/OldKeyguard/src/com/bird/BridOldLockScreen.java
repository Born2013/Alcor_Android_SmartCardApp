package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.telephony.IccCardConstants.State;
import android.widget.TextView;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import java.util.Calendar;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import java.util.ArrayList;
import java.util.Map;
import android.database.ContentObserver;
import android.database.Cursor;
import java.util.HashMap;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.content.ContentResolver;
import android.provider.CallLog;
import android.widget.ListView;
import android.provider.Settings;
import android.provider.ContactsContract.PhoneLookup;
import android.view.ViewGroup;
import android.view.View;
import android.os.Message;
import com.mediatek.geocoding.GeoCodingQuery;
import android.view.MotionEvent;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Animation;
import android.util.DisplayMetrics;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.app.ActivityManagerNative;
import android.content.Intent;
import android.os.RemoteException;
import android.content.ComponentName;
import android.view.animation.BounceInterpolator;
import android.os.BatteryManager;
import android.app.WallpaperManager;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.provider.Telephony.MmsSms;
import android.content.res.Configuration;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import android.widget.Scroller;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.TextUtils;

import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Telephony;
import android.os.AsyncTask;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;
import android.content.ActivityNotFoundException;


public class BridOldLockScreen extends RelativeLayout implements KeyguardSecurityView,BirdOldMusicWdiget.VisibiliyChangeListener {
    
    private Context mContext;
    private KeyguardSecurityCallback mCallback;
    private LockPatternUtils mLockPatternUtils;
    private String mDateFormatString;
    private static final String TAG = "BridOldLockScreen";
    
    private final static Uri CALL_URI = CallLog.Calls.CONTENT_URI;
    
    private final static Uri MMS_URI = Telephony.Mms.CONTENT_URI;

    private final static String WHERE_CALL_CLAUSE = new String(
                     CallLog.Calls.TYPE + "= " + CallLog.Calls.MISSED_TYPE  + " and "
                    + CallLog.Calls.NEW + "= " + CallLog.Calls.INCOMING_TYPE);

    private final static String WHERE_MMS_CLAUSE = new String(Telephony.Mms.READ + "= 0");

    private String LuancherContact_CLUM_PHONENUMBER = "phoneNumber";
    private String LuancherContact_CLUM_NAME = "name";
    private String LuancherContact_CLUM_PHOTO = "photo";

    private  ArrayList<Contact> LuancherContact = new ArrayList<>();    
    
   private final static Uri LAUNCHER_CONTACT_URI = Uri.parse("content://com.android.bird.oldphone.contact/oldContact");
    
    private ImageView mMissCall;
    private ImageView mUnreadMMs;

    private static final int PHONES_DISPLAY_NAME_INDEX = 0;
    private static final int PHONES_NUMBER_INDEX = 1;
    
    private static final int MSG_SMS_QUERY = 1;
    private static final int MSG_MMS_QUERY = 2;
    private static final int MSG_CALL_QUERY = 3;
    
    private static final int MSG_CHANGE_BUTTON = 4;
    
    private static ArrayList<Map<String, String>> list;
    private TextView mTextViewTips;
    private Scroller mSroller;
    private float mFirstY;
    private float mDisY;

    private boolean mUnlock, mFollowAnimate = false;
    private int mScreenWidth, mScreenHeight;
    private boolean isVertical = false;
    boolean mShowingBatteryInfo = false;
    boolean mCharging = false;
    int mBatteryLevel = 100;


    private FrameLayout mLoveFrameLayout;
    private TextView mLoveCount;
    private ImageView mLovePhoto;
    private TextView mLoveName;
    private BirdOldMusicWdiget mMusicWidget;
    private BirdOldAlbumView mOldAlbumView;
    private KeyguardSecurityModel mKeyguardSecurityModel;
    
    private View mChildView;
    
    private float RATIO_FACTOR = 0.7f;
    private float FLASH_RATIO_FACTOR = 0.3f;
    
    private LinearLayout mFlashViewRegion;
    private TextView mStartTextView;
    private long mLastClickTime;
    private String mFaimlyNames;
    
    private  Handler mHandler = new Handler() {
        
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SMS_QUERY:
                        Log.v(TAG, "MSG_SMS_QUERY");
                        break;
                    case MSG_MMS_QUERY:
                        Log.v(TAG, "MSG_MMS_QUERY");
                        new LoadMsg(mContext).execute(Uri.EMPTY);
                        break;
                    case MSG_CALL_QUERY:
                        Log.v(TAG, "MSG_CALL_QUERY");
                        new LoadMsg(mContext).execute(Uri.EMPTY);

                    case MSG_CHANGE_BUTTON:
                        Log.v(TAG, "MSG_CHANGE_BUTTON");
                        if (!TextUtils.isEmpty(mFaimlyNames)) {
                            mStartTextView.setText(mFaimlyNames);
                        }
                        break;
                }
            }
     };




    protected boolean mBatteryIsLow;

    private  final ContentObserver sSMSObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            Log.d(TAG,"sSMSObserver selfChange =  "+selfChange);
            if (mHandler != null) {
                mHandler.removeMessages(MSG_SMS_QUERY);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SMS_QUERY));
            }
        }
    };


    private  final ContentObserver sMMSObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            Log.d(TAG,"sMMSObserver selfChange =  "+selfChange);
            if (mHandler != null) {
                mHandler.removeMessages(MSG_MMS_QUERY);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_MMS_QUERY));
            }
        }
    };

    private  final ContentObserver sMissedCallObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            Log.d(TAG,"sMissedCallObserver selfChange =  "+selfChange);
            if (mHandler != null) {
                mHandler.removeMessages(MSG_CALL_QUERY);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_CALL_QUERY));
            }
        }
    };
    

    KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onDevicePolicyManagerStateChanged() {
        
        }

        public void onSimStateChanged(State simState) {
              Log.d(TAG,"mInfoCallback. simState = "+simState);
              new LoadMsg(mContext).execute(Uri.EMPTY);
        }

        @Override
        public void onTimeChanged() {
            Log.d(TAG,"mInfoCallback. onTimeChanged");
        }

        @Override
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {  
            mShowingBatteryInfo = status.isPluggedIn();
            mCharging = status.isCharged();
            mBatteryLevel = status.level;
            mBatteryIsLow = status.isBatteryLow();
            Log.d(TAG,"mInfoCallback. onRefreshBatteryInfo mCharging = "+mCharging);
            Log.d(TAG,"mInfoCallback. onRefreshBatteryInfo mShowingBatteryInfo = "+mShowingBatteryInfo); 
            Log.d(TAG,"mInfoCallback. onRefreshBatteryInfo mBatteryLevel = "+mBatteryLevel);
            //mTextViewTips.setText(mBatteryLevel +" mShowingBatteryInfo = "+mShowingBatteryInfo);
            if ( mShowingBatteryInfo) {
                 if (mBatteryLevel ==100) {
                     mTextViewTips.setText(getResources().getString(R.string.zzzzz_charging_full));
                 } else {
                     mTextViewTips.setText(getResources().getString(R.string.zzzzz_charging_tips,mBatteryLevel)+"% )");
                 }
            } else {
                  mTextViewTips.setText(R.string.zzzzz_unlock_tips);
            }
            
        }
       
    };

    public BridOldLockScreen(Context context, KeyguardSecurityCallback callback, LockPatternUtils utils ) {
        super(context);
        mCallback = callback;
        mLockPatternUtils = utils;
        init(context);
    }



    public void init(Context context) {
        mContext = context;
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.zzzzz_old_keyguard_screen, this, true);
        mTextViewTips = (TextView)findViewById(R.id.zzzzz_default_unlock_tips);
        mMusicWidget = (BirdOldMusicWdiget)findViewById(R.id.oldphone_music);
        mMusicWidget.setVisibiliyChangeListener(this);
        mOldAlbumView = (BirdOldAlbumView)findViewById(R.id.oldphone_album);

        
        mLoveFrameLayout = (FrameLayout)findViewById(R.id.love_msg_id);
        mLoveCount = (TextView)findViewById(R.id.count_id) ;
        mLovePhoto = (ImageView)findViewById(R.id.contact_photo) ;
        mLoveName = (TextView)findViewById(R.id.cantact_name_id) ;
        mMissCall = (ImageView) findViewById(R.id.unread_call);
        mUnreadMMs = (ImageView) findViewById(R.id.unread_mms);
        setBackgroundResource(R.drawable.oldphone_lockscreen_bg);
        mSroller = new Scroller(mContext);
        mChildView = getChildAt(0);
        
        mFlashViewRegion = (LinearLayout)findViewById(R.id.zzzzz_flashtext);    
        mStartTextView = (TextView)findViewById(R.id.cantact_name_id);
        
        mStartTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long clicktime = System.currentTimeMillis();
                mStartTextView.setText(R.string.oldphone_start_contact);
                Log.v(TAG, "(clicktime - mLastClickTime) = "+(clicktime - mLastClickTime));
                if (clicktime - mLastClickTime <=2000) {
                      startFaimlyActivity();
                }
                mLastClickTime = clicktime;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHANGE_BUTTON),2000l);
            }
        });
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //registerObserver();
        //resetStatusInfo();
        //IntentFilter filter = new IntentFilter();
       // filter.addAction("android.hardware.usb.action.USB_STATE");
       
        initLauncherContact();

    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);        
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        super.onLayout(changed, l, t, r, b);
        this.setFocusable(true);

    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        //KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        
    }

    @Override
    public void onResume(int reason) {
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);        
        resetStatusInfo();
        registerObserver();       

    }

    @Override
    public void onVisibiliyChange(View view,int visiblity) { 
        Log.d(TAG, "onVisibiliyChange mMusicWidget.getVisibility() =  "+mMusicWidget.getVisibility());
        new LoadMsg(mContext).execute(Uri.EMPTY);
    }


    private void startFaimlyActivity(){
        startUnlock();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.launcher", "com.android.family.FamilyContactActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("isLockstart",true);
        try{
             getContext().startActivity(intent);
             Log.v(TAG, "intent = "+intent);
        } catch (ActivityNotFoundException e){
              Log.v(TAG, "e = "+e);
        }
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void showPromptReason(int reason){

    }

    @Override
    public void showMessage(String message, int color) {

    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    private void resetStatusInfo() {
        Log.d(TAG, "resetStatusInfo ");
        //updateOwnerInfo();
        mHandler.removeMessages(MSG_SMS_QUERY);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SMS_QUERY));
        mHandler.removeMessages(MSG_MMS_QUERY);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_MMS_QUERY));
        mHandler.removeMessages(MSG_CALL_QUERY);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_CALL_QUERY));
    }

    private void startUnlock() {
        mCallback.userActivity();
        if (mKeyguardSecurityModel !=null && mKeyguardSecurityModel.getSecurityMode() != SecurityMode.None) {
             mCallback.dismiss(false);
        } else {
            mCallback.dismiss(true);
        }
    }

    private void registerObserver() {
        unregisterObserver();
        
        mContext.getContentResolver().registerContentObserver(MMS_URI, true, sSMSObserver);
        mContext.getContentResolver().registerContentObserver(MmsSms.CONTENT_URI, true, sMMSObserver);
        mContext.getContentResolver().registerContentObserver(Calls.CONTENT_URI, true, sMissedCallObserver);
        
        Log.d(TAG, "registerObserver");
    }

    
    private synchronized void unregisterObserver() {
        try {
            if (sSMSObserver != null) {
                getContext().getContentResolver().unregisterContentObserver( sSMSObserver);
                Log.d(TAG, "unregisterObserver:SMS");
            }

            if (sMMSObserver != null) {
                getContext().getContentResolver().unregisterContentObserver(sMMSObserver);
                Log.d(TAG, "unregisterObserver:MMS");
            }

            if (sMissedCallObserver != null) {
                getContext().getContentResolver().unregisterContentObserver(sMissedCallObserver);
                Log.d(TAG, "unregisterObserver:MissedCall");
            }
        } catch (Exception e) {
            Log.e(TAG, "unregisterObserver fail");
        }
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float tmpX = event.getX();
        float tmpY = event.getY();
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mFirstY = tmpY;
            case MotionEvent.ACTION_MOVE:
                mDisY = tmpY - mFirstY;
                if (mDisY < 0) {
                    smoothScrollTo(0, (int) (-mDisY));
                }
                return true;
            case MotionEvent.ACTION_UP:
                int abs = (int) Math.abs(mDisY);
                if (abs >= mScreenHeight / 3 && mDisY < 0) {
                      startUnlock();
                }
                mSroller.startScroll(0, 0, 0, 0, 1000);
                mDisY = 0;
                return true;
        }
        return super.onTouchEvent(event);
    }


    public void smoothScrollTo(int fx, int fy) {
        int dx = fx - mSroller.getFinalX();
        int dy = fy - mSroller.getFinalY();
        smoothScrollBy(dx, dy);
    }


    public void smoothScrollBy(int dx, int dy) {
        mSroller.startScroll(mSroller.getFinalX(), mSroller.getFinalY(), dx, dy);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mSroller.computeScrollOffset()) {
            int y = mSroller.getCurrY();
            float alpha  = (((float)(mScreenHeight-y))/ mScreenHeight)*RATIO_FACTOR; 
            float translationY = y*FLASH_RATIO_FACTOR;
            if (y == 0) {
                    alpha = 1.0f;
                    translationY = 0.0f;
            }
            mChildView.setAlpha(alpha);
            mFlashViewRegion.setTranslationY(-translationY);            
            Log.d(TAG, "y = "+y);   
            scrollTo(0,y);
            postInvalidate();
        }        
        super.computeScroll();
    }


    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    @Override
    public void startAppearAnimation() {

    }

    private void setBackDrawable() {

    }

    private Drawable getScreenWallpaper() {
        return null;
    }

    public void setKeyguardSecurityModel (KeyguardSecurityModel  keyguardmodel) {
         mKeyguardSecurityModel = keyguardmodel;
    }

    private class LoadMsg extends AsyncTask<Uri, Void, Cursor[]> {
        private Context mContext;
        private Uri mUri;
        public LoadMsg(Context context) {
            mContext = context;
        }

        @Override
        protected Cursor[] doInBackground(Uri... uris) {
            Cursor[] cursors = new Cursor[2];
            ContentResolver resolver = mContext.getContentResolver();
            cursors[0] = resolver.query(CALL_URI, null, WHERE_CALL_CLAUSE, null, null); 
            cursors[1] = resolver.query(Uri.parse("content://sms/"), null, WHERE_MMS_CLAUSE, null, null);
            return cursors;
        }

        @Override
        protected void onPostExecute(Cursor[] cursors) {
            ArrayList<Contact> lunchercontacts = new ArrayList<>();
            int numbers = 0;
            Log.d(TAG, "cursors[0] = " + cursors[0]);
            if (cursors[0] !=null &&  cursors[0].getCount() >= 1) {
                int count = cursors[0].getCount();
                Log.d(TAG, "cursors[0].count = " + count);      
                while (cursors[0].moveToNext()) {
                      String number = cursors[0].getString(cursors[0].getColumnIndex("number"));
                      Log.d(TAG, "number " + number);
                      Contact contact = LauncherContact(number);
                      Log.d(TAG, "contact " + contact);
                      if (contact !=null ) {
                           numbers++;
                           if (!lunchercontacts.contains(contact)) {
                               lunchercontacts.add(contact);
                           }
                       }
                 }
                mMissCall.setVisibility(View.VISIBLE);
            } else {
                mMissCall.setVisibility(View.GONE);
            }
            Log.d(TAG, "cursors[1] = " + cursors[1]);
            if (cursors[1] !=null && cursors[1].getCount() >= 1) {
                 int count = cursors[1].getCount();
                 Log.d(TAG, "cursors[1].getCount() = " + cursors[1].getCount());
                 while (cursors[1].moveToNext()) {
                      String address = cursors[1].getString(cursors[1].getColumnIndex("address"));
                      Log.d(TAG, "address " + address);
                      Contact contact = LauncherContact(address);
                      Log.d(TAG, "contact " + contact);
                      if (contact !=null) {
                        numbers++;
                        if (!lunchercontacts.contains(contact)) { 
                               lunchercontacts.add(contact);
                        }        
                      }
                 }
                 mUnreadMMs.setVisibility(View.VISIBLE);
            } else {
                mUnreadMMs.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "lunchercontacts.size = " + lunchercontacts.size());
            if (lunchercontacts.size() >0) {
                StringBuilder builder = new StringBuilder();
                byte[] imageByte = null;
                for(int i=0;i< lunchercontacts.size(); i ++){
                    Contact contact = lunchercontacts.get(i);
                    if (imageByte == null) {
                       imageByte = contact.getImageByte();
                    }
                    builder.append(contact.getName());
                    if (i < (lunchercontacts.size()-1)) {
                        builder.append(", ");
                    }
                }  
                mFaimlyNames = builder.toString();
                flashWidget(true,numbers,builder.toString(), imageByte,false);
            } else {
                flashWidget(false,0,null,null,false);
            }
            super.onPostExecute(cursors);
        }

    }


    protected void initLauncherContact() {
        
        LuancherContact.clear();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(LAUNCHER_CONTACT_URI, 
                new String[]{LuancherContact_CLUM_PHONENUMBER , LuancherContact_CLUM_NAME, LuancherContact_CLUM_PHOTO},
                null, null, null);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phonenumber = cursor.getString(cursor.getColumnIndex(LuancherContact_CLUM_PHONENUMBER));
                byte[] image = cursor.getBlob(cursor.getColumnIndex(LuancherContact_CLUM_PHOTO));
                String name = cursor.getString(cursor.getColumnIndex(LuancherContact_CLUM_NAME));
                Contact contact = new Contact(phonenumber, name, image);
                Log.d(TAG, "contact = " + contact);
                LuancherContact.add(contact);
            }
        }
        

    }

   private void flashWidget(boolean isLoveMsg,int count,String names,byte[] image,boolean isCall) {
         if (isLoveMsg) {
            mMusicWidget.setVisibility(View.GONE);
            mOldAlbumView.setVisibility(View.GONE);
            mLoveFrameLayout.setVisibility(View.VISIBLE);
            mLoveCount.setText(mContext.getString(R.string.oldphone_count_msg,count));
            if (image !=null && image.length >0) {
                mLovePhoto.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            } else {
               mLovePhoto.setImageDrawable(getResources().getDrawable(R.drawable.add_contacts_default_pictur));
            }
            mLoveName.setText(names);
         } else {
           mLoveFrameLayout.setVisibility(View.GONE);
           boolean isPlay = mMusicWidget.isPlayed();
           Log.d(TAG, "isPlay = " + isPlay);
           if (isPlay) {
              mMusicWidget.setVisibility(View.VISIBLE);
              mOldAlbumView.setVisibility(View.GONE);
           }  else {
              mMusicWidget.setVisibility(View.GONE);
              mOldAlbumView.setVisibility(View.VISIBLE);
           }
           
       }
         
   }

   private Contact  LauncherContact(String phoneNumber) {
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.replace("+86","").replaceAll("-", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\/", "").replaceAll(" ", "").replace("#", "").replace("(", "").replace(")", "").replace("N", "").replace(",", "").replace(";", "").replace(".", "");
        }    
        if (LuancherContact.size() <=0) {
              initLauncherContact();
        }
         if (LuancherContact.size()>0) {
              for (int i=0; i<LuancherContact.size();i++){
                    Contact contact = LuancherContact.get(i);
                    if (contact.getPhoneNumber().equals(phoneNumber)) {
                          return contact;
                    }
              }
              return null;
         } else {
             return null;
         }
   }

  
   
}

