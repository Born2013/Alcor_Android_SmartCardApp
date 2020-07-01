package android.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.content.SharedPreferences;
import android.provider.Settings;
import com.android.internal.R;
import android.widget.RemoteViews.RemoteView;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.CallLog;
import android.os.AsyncTask;
import android.os.Message;
import android.net.NetworkInfo;
import android.os.Handler;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;

/**
 * @hide
 */
@RemoteView
public class BirdFamilyContact extends FrameLayout {
	static final String TAG = "BirdFamilyContact";
	private LayoutInflater mLayoutInflater;
	private RelativeLayout mFrameLayout;
	private Context mContext;
    private boolean mAttached;
    private final static int[] PICTURES = {R.drawable.zzzzz_add_contacts1_picture, R.drawable.zzzzz_add_contacts2_picture, 
        R.drawable.zzzzz_add_contacts1_picture, R.drawable.zzzzz_add_contacts2_picture,
        R.drawable.zzzzz_add_contacts3_picture, R.drawable.zzzzz_add_contacts4_picture,
        R.drawable.zzzzz_add_contacts5_picture, R.drawable.zzzzz_add_contacts6_picture};
    private String allNames;
    private int mAllUnReads;
    private Bitmap mBitmap;
    private TextView mFamilyNotify, mFamilyNames;
    private RelativeLayout photoArea;
    private ImageView mPhoto;


	public BirdFamilyContact(Context context) {
		this(context, null);
	}

	public BirdFamilyContact(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BirdFamilyContact(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		Log.i(TAG,"BirdFamilyContact");
		mContext = context;
		mLayoutInflater = LayoutInflater.from(context);
		View view = mLayoutInflater.inflate(R.layout.zzzzz_family_widget_main, this);
        initData();
	}
	
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow");
        if (!mAttached) {
            mAttached = true;
            registerReceiver();
            mHandler.removeMessages(MESSAGE_FLASH_INFORE);
            mHandler.sendEmptyMessageDelayed(MESSAGE_FLASH_INFORE, 1000);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        if (mAttached) {
            unregisterReceiver();
            mAttached = false;
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("com.bird.flash.falily.message");
        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    private void initData() {
        mFrameLayout = (RelativeLayout) findViewById(R.id.widget_layout);
        mFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComponentName compone = new ComponentName("com.android.launcher", "com.android.family.FamilyContactActivity");
                Intent i = new Intent();
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.setComponent(compone);
                mContext.startActivity(i);
            }
        });
        mFamilyNotify = (TextView) findViewById(R.id.family_notify);
        mFamilyNames = (TextView) findViewById(R.id.family_notifi_area);
        photoArea = (RelativeLayout) findViewById(R.id.family_image_area);
        mPhoto = (ImageView) findViewById(R.id.family_image);

    }

    private void flashMessage() {
        ContentResolver cr = mContext.getContentResolver();
        Uri uri = Uri.parse("content://com.android.bird.oldphone.contact/oldContact");
        Cursor mCursor = cr.query(uri, new String[] { "_id", "name", "simSubId",
                    "phoneNumber", "isnewmms", "isnewdial", "photo", "uri", "position"}, null, null, null);
        allNames = "";
        int allUnReads = 0;
        mBitmap = null;
        long mseeageTime = 0;
        try {
            while (mCursor.moveToNext()) {
                String name = mCursor.getString(mCursor.getColumnIndex("name"));
                String phoneNumber = mCursor.getString(mCursor.getColumnIndex("phoneNumber"));
                byte[] photo = mCursor.getBlob(mCursor.getColumnIndex("photo"));
                int position = mCursor.getInt(mCursor.getColumnIndex("position"));
                int unreadMms = getSmsCount(phoneNumber);
                int unreadCall = readMissCall(phoneNumber);
                long time = 0;
                if (unreadMms > 0) {
                    time = getSmsDate(phoneNumber);
                    if (mseeageTime < time) {
                        mseeageTime = time;
                        if (photo != null) {
                            mBitmap = toRoundBitmap(BitmapFactory.decodeByteArray(photo, 0, photo.length));
                        } else {
                            mBitmap = BitmapFactory.decodeResource(mContext.getResources(), PICTURES[position - 1]);
                        }
                    }
                }
                if (unreadCall > 0) {
                    time = getMissCallDate(phoneNumber);
                    if (mseeageTime < time) {
                        mseeageTime = time;
                        if (photo != null) {
                            mBitmap = toRoundBitmap(BitmapFactory.decodeByteArray(photo, 0, photo.length));
                        } else {
                            mBitmap = BitmapFactory.decodeResource(mContext.getResources(), PICTURES[position - 1]);
                        }
                    }
                }
                int unRead = unreadMms + unreadCall;
                if (unRead > 0) {
                    if (allNames == null){
                        allNames = "";
                    }
                    if (allNames.length() == 0) {
                        allNames = name;
                    } else {
                        allNames = allNames + ", " + name;
                    }
                    allUnReads = allUnReads + unRead;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (mCursor != null) {
                    mCursor.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        Log.d(TAG,"allUnReads = " + allUnReads + ", allNames = " + allNames + ", mAllUnReads = " + mAllUnReads);
        mAllUnReads = allUnReads;
        mHandler.removeMessages(MESSAGE_FLASH_VIEW);
        mHandler.sendEmptyMessageDelayed(MESSAGE_FLASH_VIEW, 1000);
    }

    private int getSmsCount(String phoneNumber) {
        int result = 0;
        Cursor csr = mContext.getContentResolver().query(Uri.parse("content://sms"), new String[]{"address"},
                    "type=? and read=?", new String[]{"1", "0"}, null);
        if (csr != null) {
            while(csr.moveToNext()) {
                String address = csr.getString(csr.getColumnIndexOrThrow("address"));
                address = address.replace("+86","").replaceAll("-", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\/", "").replaceAll(" ", "").replace("#", "").replace("(", "").replace(")", "").replace("N", "").replace(",", "").replace(";", "").replace(".", "");
                if (address.equals(phoneNumber)) {
                    result = result + 1;
                }
            }
            csr.close();
        }
        Log.d(TAG, "getSmsCount result = " + result + ", phoneNumber = " + phoneNumber);
        return result;
    }

    private int readMissCall(String phoneNumber) {
        int result = 0;
        Cursor cursor = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[] { Calls.TYPE, "number"}, " type=? and new=?",
                new String[] { Calls.MISSED_TYPE + "", "1"}, "date desc");

        if (cursor != null) {
            while(cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                number = number.replace("+86","").replaceAll("-", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\/", "").replaceAll(" ", "").replace("#", "").replace("(", "").replace(")", "").replace("N", "").replace(",", "").replace(";", "").replace(".", "");
                if (number.equals(phoneNumber)) {
                    result = result + 1;
                }
            }
            cursor.close();
        }
        Log.d(TAG, "readMissCall result = " + result + ", phoneNumber = " + phoneNumber);
        return result;
    }

    private Bitmap toRoundBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
        if (width <= height) {
            roundPx = width / 2;
            top = 0;
            bottom = width;
            left = 0;
            right = width;
            height = width;
            dst_left = 0;
            dst_top = 0;
            dst_right = width;
            dst_bottom = width;
        } else {
            roundPx = height / 2;
            float clip = (width - height) / 2;
            left = clip;
            right = width - clip;
            top = 0;
            bottom = height;
            width = height;
            dst_left = 0;
            dst_top = 0;
            dst_right = height;
            dst_bottom = height;
        }
        Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect src = new Rect((int) left, (int) top, (int) right,
                (int) bottom);
        final Rect dst = new Rect((int) dst_left, (int) dst_top,
                (int) dst_right, (int) dst_bottom);
        final RectF rectF = new RectF(dst);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);
        return output;
    }

    private class BackgroundFlash extends AsyncTask<Uri, Void, Cursor[]> {
        public BackgroundFlash() {
        }

        @Override
        protected Cursor[] doInBackground(Uri... uris) {
            Cursor[] cursors = new Cursor[2];
            flashMessage();
            return cursors;
        }

        @Override
        protected void onPostExecute(Cursor[] cursors) {
            super.onPostExecute(cursors);
        }
    }

    private final static int MESSAGE_FLASH_INFORE = 0;
    private final static int MESSAGE_FLASH_VIEW = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_FLASH_INFORE) {
                new BackgroundFlash().execute(Uri.EMPTY);
            } else if (msg.what == MESSAGE_FLASH_VIEW) {
                if (mAllUnReads > 0) {
                    mFamilyNotify.setText(String.format(mContext.getResources().getString(R.string.zzzzz_oldphone_family_notifi), mAllUnReads));
                    mFamilyNames.setText(allNames);
                    photoArea.setVisibility(View.VISIBLE);
                    mPhoto.setImageBitmap(mBitmap);
                } else {
                    mFamilyNotify.setText(mContext.getResources().getString(R.string.zzzzz_family_notify_text));
                    mFamilyNames.setText(mContext.getResources().getString(R.string.zzzzz_family_notifi_area_text));
                    photoArea.setVisibility(View.GONE);
                    mPhoto.setImageBitmap(null);
                }
            }
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,"action : "+action);
            mHandler.removeMessages(MESSAGE_FLASH_INFORE);
            mHandler.sendEmptyMessageDelayed(MESSAGE_FLASH_INFORE, 1000);
        }
    };

    private long getSmsDate(String phoneNumber) {//date_sent
        long result = 0;
        Cursor csr = mContext.getContentResolver().query(Uri.parse("content://sms"), new String[]{"address", "date_sent"},
                    "type=? and read=?", new String[]{"1", "0"}, null);
        if (csr != null) {
            while(csr.moveToNext()) {
                String address = csr.getString(csr.getColumnIndexOrThrow("address"));
                address = address.replace("+86","").replaceAll("-", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\/", "").replaceAll(" ", "").replace("#", "").replace("(", "").replace(")", "").replace("N", "").replace(",", "").replace(";", "").replace(".", "");
                Log.d(TAG, "getSmsDate address = " + address + ", phoneNumber = " + phoneNumber);
                if (address.equals(phoneNumber)) {
                    result = csr.getLong(csr.getColumnIndexOrThrow("date_sent"));
                }
            }
            csr.close();
        }
        Log.d(TAG, "getSmsDate result = " + result + ", phoneNumber = " + phoneNumber);
        return result;
    }

    private long getMissCallDate(String phoneNumber) {
        long result = 0;
        Cursor cursor = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[] { Calls.TYPE, "number", "date"}, " type=? and new=?",
                new String[] { Calls.MISSED_TYPE + "", "1"}, "date desc");

        if (cursor != null) {
            while(cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                number = number.replace("+86","").replaceAll("-", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\/", "").replaceAll(" ", "").replace("#", "").replace("(", "").replace(")", "").replace("N", "").replace(",", "").replace(";", "").replace(".", "");
                Log.d(TAG, "getMissCallDate number = " + number + ", phoneNumber = " + phoneNumber);
                if (number.equals(phoneNumber)) {
                    result = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                }
            }
            cursor.close();
        }
        Log.d(TAG, "getMissCallDate result = " + result + ", phoneNumber = " + phoneNumber);
        return result;
    }
}
