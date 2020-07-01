package com.android.keyguard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.music.IMediaPlaybackService;
import com.android.keyguard.R;

/**
 * Created by root on 17-8-4.
 */

public class BirdOldMusicWdiget extends FrameLayout implements View.OnClickListener {


    private static final String TAG = "BirdOldMusicWdiget";

    public static final String META_CHANGED = "com.android.music.metachanged";
    public static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    public static final String QUIT_PLAYBACK = "com.android.music.quitplayback";

    private int mWidth;
    private int mHeight;

    private BirdOldMusicAblum mBackgroundAblum;

    private ImageButton mLastBtn, mStartPauseBtn, mNextBtn;

    private TextView mSongTitleTextView;

    private IMediaPlaybackService mService;

    private VisibiliyChangeListener mVisibilityListener;
    
    public void setVisibiliyChangeListener(VisibiliyChangeListener listener) {
        mVisibilityListener = listener;
    }

    public interface VisibiliyChangeListener{
        public void onVisibiliyChange(View view,int visiblity);
    }

    public BirdOldMusicWdiget(@NonNull Context context) {
        this(context, null);
    }

    public BirdOldMusicWdiget(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BirdOldMusicWdiget(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BirdOldMusicWdiget(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.zzzzz_old_music_widget, this, true);
        BitmapFactory.Options mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), R.drawable.oldphone_panel_mask, mOptions);
        mWidth = mOptions.outWidth;
        mHeight = mOptions.outHeight;

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBackgroundAblum = (BirdOldMusicAblum) findViewById(R.id.album_id);
        mSongTitleTextView = (TextView) findViewById(R.id.song_title);

        mLastBtn = (ImageButton) findViewById(R.id.player_last_id);
        mStartPauseBtn = (ImageButton) findViewById(R.id.player_start_id);
        mNextBtn = (ImageButton) findViewById(R.id.player_next_id);

        mLastBtn.setOnClickListener(this);
        mStartPauseBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

         connectService();
               
        IntentFilter filter = new IntentFilter();
        filter.addAction(PLAYSTATE_CHANGED);
        filter.addAction(META_CHANGED);
        filter.addAction(QUIT_PLAYBACK);
        
        //filter.addAction(Intent.ACTION_SCREEN_ON);
       // filter.addAction(Intent.ACTION_SCREEN_OFF);
        
        getContext().registerReceiver(mStatusListener, filter);
    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mStatusListener);
        getContext().unbindService(mConnection);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.player_last_id) {
            dolast();
        } else if (id == R.id.player_start_id) {
            startOrPause();
        } else if (id == R.id.player_next_id) {
            doNext();
        }
    }


    private void dolast() {
        if (mService != null) {
            try {
                mService.prev();
                setEnableBtn(false);
            } catch (RemoteException e) {
            }
        }
    }

    private void doNext() {
        if (mService != null) {
            try {
                mService.next();
                setEnableBtn(false);
            } catch (RemoteException e) {
            }
        }
    }

    private void startOrPause() {
        if (mService != null) {
            try {
                if (mService.isPlaying()) {
                    mService.pause();
                    mStartPauseBtn.setImageResource(R.drawable.oldphone_player_start);
                } else {
                    mService.play();
                    mStartPauseBtn.setImageResource(R.drawable.oldphone_player_pause);
                }
            } catch (RemoteException e) {
            }
        }

    }

    public void connectService() {
        
        Intent intent = new Intent("com.android.music.MediaPlaybackService");
        ComponentName component = new ComponentName("com.android.music",
                "com.android.music.MediaPlaybackService");
        intent.setComponent(component);
        
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
    }


    private void reflashStartPauseState() {
        Log.d(TAG, "reflashStartPauseState");
        try {
            if (mService != null && mService.isPlaying()) {
                Log.d(TAG, "reflashStartPauseState isPlaying");
                mStartPauseBtn.setImageResource(R.drawable.oldphone_player_pause);
            } else {
                mStartPauseBtn.setImageResource(R.drawable.oldphone_player_start);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "reflashStartPauseState RemoteException");
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setEnableBtn(true);
            }
        },2000);
        
    }

    private void reflashSongTitleView() {
        Log.d(TAG, "reflashSongTitleView");
        if (mService != null) {
            try {
                String trackName = mService.getTrackName();
                if (TextUtils.isEmpty(trackName)) {
                    trackName = mService.getArtistName();
                }
                Log.d(TAG, "reflashSongTitleView trackName = " + trackName);
                mSongTitleTextView.setText(trackName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void reflashAblumView () {
            Log.d(TAG, "reflashAblumView");
            if (mService !=null) {
                try {
                        Bitmap bitmap = mService.getAlbumCover();
                        Log.d(TAG, "bitmap = "+bitmap);
                        if (bitmap !=null) {
                            mBackgroundAblum.setmAblum(bitmap);
                        }
                 }catch (RemoteException e){

                 }
             }
    }

    private void reflashAllView(){
        reflashSongTitleView();
        reflashAblumView();
        reflashStartPauseState();        
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            Log.e(TAG, " onServiceConnected className " + classname + " obj " + obj);
            mService = IMediaPlaybackService.Stub.asInterface(obj);
            try {
                if (mService.isPlaying()) {

                    reflashAllView();
                } else {

                }
            } catch (RemoteException e) {

            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.e(TAG, " onServiceDisconnected classname " + classname);
            mService = null;
        }
    };


    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();      
            if (action == null) {
                Log.e(TAG, "onReceive: action = null");
                return;
            }
            Log.d(TAG, "mStatusListener: " + action);
            if (action.equals(META_CHANGED)) {
                reflashAllView();
            } else if (action.equals(PLAYSTATE_CHANGED)) {
            
                reflashAllView();
            } else if (action.equals(QUIT_PLAYBACK)) {
                setVisibility(GONE);
            } 
            
            /*else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "onReceive, stop refreshing ...");
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "onReceive, restore refreshing ...");
                
            }*/

        }
    };

    @Override
    protected void onVisibilityChanged(@NonNull View changedView,int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (mVisibilityListener !=null) {
                 mVisibilityListener.onVisibiliyChange(this,visibility);
        }
    }

   public boolean isPlayed() {
            if (mService !=null) {
            try {
                 return   mService.isPlaying();
             }catch (RemoteException e){
                return false;
             }
            } else {
                return false;
            }
   }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }


   private void setEnableBtn(boolean enable) {
        mStartPauseBtn.setEnabled(enable);
        mLastBtn.setEnabled(enable);
        mNextBtn.setEnabled(enable);

   }

}
