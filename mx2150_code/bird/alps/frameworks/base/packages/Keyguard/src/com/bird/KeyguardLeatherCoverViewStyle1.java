package com.android.keyguard;

import com.android.music.IMediaPlaybackService;
import fm.xiami.aidl.SongInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.util.AttributeSet;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.text.format.DateFormat;
import android.widget.TextView;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import java.util.Locale;
import android.widget.TextClock;
import android.text.TextUtils;
import com.google.android.mms.pdu.PduHeaders;
import android.provider.MediaStore;
import android.provider.Settings;
/*[BIRD][BIRD_SMALL_VIEW_WINDOW][kusai软件开机音乐进程会启动]xujing 20170804 begin*/
import android.os.SystemProperties;
/*[BIRD][BIRD_SMALL_VIEW_WINDOW][kusai软件开机音乐进程会启动]xujing 20170804 end*/

/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
public class KeyguardLeatherCoverViewStyle1 extends RelativeLayout {
    
    private static final String TAG = "KeyguardLeatherCoverViewStyle1";
    public static final String META_CHANGED = "com.android.music.metachanged";
    public static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    public static final String QUIT_PLAYBACK = "com.android.music.quitplayback";
    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";
    private RelativeLayout mMusic;
    public BirdMarqureeTextView mMusicSong;
    public ImageButton mMusicPre;
    public ImageButton mMusicPlay;
    public ImageButton mMusicNext;
    private Resources res;
    protected static final int UPDATE_INTERNAL = 1000;
    protected static final int UPDATE_PROGRESS = 1;
    protected static final int CALLBACK_PLAY_SONG_CHANGED = 10;
    protected static final int CALLBACK_PLAY_STATE_CHANGED = 11;
    public static final int XIAMI_SONG_PREPARE = 1001;
    public static final int XIAMI_SONG_PLAY = 1002;
    private int mPlayState;
    private boolean isCoverClosed = false;
    private int[] mImageIds = { 
        R.drawable.zzzzz_leather_bg_style1_0,   
        R.drawable.zzzzz_leather_bg_style1_1, 
        R.drawable.zzzzz_leather_bg_style1_2,   
        R.drawable.zzzzz_leather_bg_style1_3, 
        R.drawable.zzzzz_leather_bg_style1_4};  

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
                updateAllInfo();
            } else if (action.equals(PLAYSTATE_CHANGED)) {
                updateAllInfo();
            /// M: Handle more status. {@
            } else if (action.equals(QUIT_PLAYBACK)) {
                
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                /// M: stop refreshing
                Log.d(TAG, "onReceive, stop refreshing ...");
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                /// M: restore refreshing
                Log.d(TAG, "onReceive, restore refreshing ...");
            }
            /// @}
        }
    };
    
	IMediaPlaybackService mService = null;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            Log.e(TAG, " onServiceConnected className " + classname
        		+ " obj " + obj);
            mService = IMediaPlaybackService.Stub.asInterface(obj);
            try {
                // Assume something is playing when the service says it is,
                // but also if the audio ID is valid but the service is paused.
                if (mService.isPlaying()){
                    updateAllInfo();
                    return;
                }
            } catch (RemoteException ex) {
            }
        }
    
        public void onServiceDisconnected(ComponentName classname) {
            Log.e(TAG, " onServiceDisconnected classname " + classname);
            mService = null;
        }
    };


	public void onScreenTurnedOff() {

	}

	private void setOnClick() {
		mMusicPre.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d("mandy", "mService" + mService);
				if (mService != null) {
					try {
						mService.prev();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});
        
        mMusicPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.d("mandy", "mMusicPlay mService" + mService);
                doPauseResume();
            }
        });
        
		mMusicNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null) {
					try {
						mService.next();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

    
    private void doPauseResume() {
        try {
            if(mService != null) {
                Boolean isPlaying = mService.isPlaying();
                Log.d(TAG, "doPauseResume: isPlaying=" + isPlaying);
                /// M: AVRCP and Android Music AP supports the FF/REWIND
                //   aways get position from service if user press pause button
                if (isPlaying) {
                    mService.pause();
                } else {
                    mService.play();
                }
                setPauseButtonImage();
            }
        } catch (RemoteException ex) {
        }
    }

    
    private void setPauseButtonImage() {
        Log.d(TAG,"setPauseButtonImage");
        try {
            if (mService != null && mService.isPlaying()) {
                Log.d(TAG,"setPauseButtonImage isPlaying");
                mMusicPlay.setImageResource(R.drawable.zzzzz_leather_music_drawable_pause_style1);
            } else {
                mMusicPlay.setImageResource(R.drawable.zzzzz_leather_music_drawable_play_style1);
            }
        } catch (RemoteException ex) {
        }
    }


	private void updateSongNameAndArtist() {
        Log.d(TAG, " updateSongNameAndArtist");
        try {
            if(mService != null) {
                String trackName = mService.getTrackName();
                String artistName = mService.getArtistName();
                Log.d(TAG, " updateSongNameAndArtist trackName = "+trackName+",artistName = "+artistName);
                updateSongNameAndArtist(trackName,artistName);
            }
        } catch (RemoteException ex) {
        }
	}
    
	private boolean isPlaying() {
        try {
            if(mService != null) {
                return mService.isPlaying();
            } else {
                if (isCoverClosed) {
                    connectService();
                    try {
                        if(mService != null) {
                            return mService.isPlaying();
                        }
                    } catch (RemoteException ex) {
                        Log.e(TAG, "Error:" + ex);
                    }
                }
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "Error:" + ex);
        }
        return false;

	}

	private void updateAllInfo() {
		// TODO Auto-generated method stub
        updateSongNameAndArtist();
        setPauseButtonImage();
	}

	public KeyguardLeatherCoverViewStyle1(Context context) {
		this(context, null, 0);
		// TODO Auto-generated constructor stub
	}

	public KeyguardLeatherCoverViewStyle1(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public KeyguardLeatherCoverViewStyle1(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		res = getContext().getResources();
		mMusic = (RelativeLayout) findViewById(R.id.bird_music_view);
		mMusicSong = (BirdMarqureeTextView) findViewById(R.id.music_song);
		mMusicPre = (ImageButton) findViewById(R.id.music_prev);
		mMusicPlay = (ImageButton) findViewById(R.id.music_play);
		mMusicNext = (ImageButton) findViewById(R.id.music_next);
		setOnClick();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();  
        connectService();
        IntentFilter f = new IntentFilter();
        f.addAction(PLAYSTATE_CHANGED);
        f.addAction(META_CHANGED);
        /// M: listen more status to update UI @{
        f.addAction(QUIT_PLAYBACK);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        /// @}
        getContext().registerReceiver(mStatusListener, new IntentFilter(f));
	}

    public void connectService() {
        /*[BIRD][BIRD_SMALL_VIEW_WINDOW][kusai软件开机音乐进程会启动]xujing 20170804 begin*/
        if (!"1".equals(SystemProperties.get("ro.bd_hall_leather"))) {
            return;
        }
        /*[BIRD][BIRD_SMALL_VIEW_WINDOW][kusai软件开机音乐进程会启动]xujing 20170804 end*/
        Intent intent = new Intent("com.android.music.MediaPlaybackService");
        ComponentName component = new ComponentName("com.android.music", "com.android.music.MediaPlaybackService");
        intent.setComponent(component);
        getContext().bindService(intent, mConnection,
        		Context.BIND_AUTO_CREATE);
    }
    
	@Override
	public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mStatusListener);
        getContext().unbindService(mConnection);
	}

	private void updateSongNameAndArtist(String name, String artist) {
        Log.e(TAG, " updateSongNameAndArtist name " + name + " artist "
        	+ artist);
        if (mMusicSong != null) {
            mMusicSong.setText(TextUtils.isEmpty(name) ? artist : TextUtils.isEmpty(artist) ? name : name + "-" + artist);
            mMusicSong.setFocusable(true);
        }
	}

	private static String formatToPlayTime(long mills) {
		int secs = (int) (mills / 1000);
		int leftSecs = secs % 60;
		int mins = secs / 60;

		return String.format("%s:%s",
				mins < 10 ? ("0" + mins) : String.valueOf(mins),
				leftSecs < 10 ? ("0" + leftSecs) : String.valueOf(leftSecs));
	}
	private boolean isButton(int x, int y){
		Rect frame = new Rect();
        boolean contain = false;
        mMusicPre.getHitRect(frame);
        contain = frame.contains(x, y);
        if (contain) {
            return true;
        }
        mMusicPlay.getHitRect(frame);
        contain = frame.contains(x, y);
        if (contain) {
            return true;
        }
        mMusicNext.getHitRect(frame);
        contain = frame.contains(x, y);
        if (contain) {
            return true;
        }
        return false;
	}
	public void onCoverClosed() {
        int index = Settings.System.getInt(getContext().getContentResolver(), Settings.System.BIRD_SMALL_WINDOW_BG_STYLE1, 0);
        setBackgroundResource(mImageIds[index]);
        isCoverClosed = true;
        if (isPlaying()) {
            //mMusic.setVisibility(View.VISIBLE);
            updateAllInfo();
        } else {
            //mMusic.setVisibility(View.GONE);
        }
	}
    public void refreshMusicView() {
        if (isPlaying()) {
            mMusic.setVisibility(View.VISIBLE);
        } else {
            mMusic.setVisibility(View.GONE);
        }
    }
    public void onCoverOpen(){
        isCoverClosed = false;
    }
    
	/*public boolean onTouchEvent(MotionEvent event) {
		if(mMusic.getVisibility() == View.VISIBLE){
		}
		return true;
    }*/
}
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
