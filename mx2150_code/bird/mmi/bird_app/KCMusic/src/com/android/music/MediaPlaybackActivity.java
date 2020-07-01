/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.music;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.music.MusicUtils.ServiceToken;
//import com.mediatek.drm.OmaDrmStore;

/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;
import com.bird.lrcview.DefaultLrcParser;
import com.bird.lrcview.LrcRow;
import com.bird.lrcview.LrcView;
import com.bird.lrcview.LrcView.OnSeekToListener;
import android.os.AsyncTask;
import java.util.List;
import java.util.Scanner;
import java.util.Locale;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
//import com.birdroid.common.FeatureOption;
import android.graphics.drawable.TransitionDrawable;
/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/

/*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 begin*/
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.hardware.SystemSensorManager;
import android.provider.Settings;
/*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 end*/
/*[bug-null][gaowei][20170704]begin*/
import com.bird.PopMenu;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import com.bird.MaskImage;
/*[bug-null][gaowei][20170704]end*/
public class MediaPlaybackActivity extends Activity implements MusicUtils.Defs,
    View.OnTouchListener, View.OnLongClickListener, CreateBeamUrisCallback
{

    private Drawable mydefalutDrawable = null; /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 add*/
    private static final String TAG = "MediaPlayback";

    private static final int USE_AS_RINGTONE = CHILD_MENU_BASE;

    private boolean mSeeking = false;
    private boolean mDeviceHasDpad;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private IMediaPlaybackService mService = null;
    private RepeatingImageButton mPrevButton;
    private ImageButton mPauseButton;
    private RepeatingImageButton mNextButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mQueueButton;
    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
    private Toast mToast;
    private int mTouchSlop;
    private ServiceToken mToken;

    /// M: specific performace test case.
    private static final String PLAY_TEST = "play song";
    private static final String NEXT_TEST = "next song";
    private static final String PREV_TEST = "prev song";

    /// M: FM Tx package and activity information.
    private static final String FM_TX_PACKAGE = "com.mediatek.FMTransmitter";
    private static final String FM_TX_ACTIVITY = FM_TX_PACKAGE + ".FMTransmitterActivity";
    /// M: show album art again when configuration change
    private boolean mIsShowAlbumArt = false;
    private Bitmap mArtBitmap = null;
    private long mArtSongId = -1;

    /// M: Add queue, repeat and shuffle to action bar when in landscape
    private boolean mIsLandscape;
    private MenuItem mQueueMenuItem;
    private MenuItem mRepeatMenuItem;
    private MenuItem mShuffleMenuItem;
    /// M: Add search button in actionbar when nowplaying not exist
    MenuItem mSearchItem;

    /// M: Add playlist sub menu to music
    private SubMenu mAddToPlaylistSubmenu;

    /// M: Music performance test string which is current runing
    private String mPerformanceTestString = null;

    /// M: use to make current playing time aways showing when seeking
    private int mRepeatCount = -1;

    /// M: Some music's durations can only be obtained when playing the media.
    // As a result we must know whether to update the durations.
    private boolean mNeedUpdateDuration = true;

    /// M: aviod Navigation button respond JE if Activity is background
    private boolean mIsInBackgroud = false;

    /// M: marked in onStop(), when get  phone call  from this activity,
    // if screen off to on, this activity will call onStart() to bind service,
    // the pause button may update in onResume() and onServiceConnected(), but
    // the service is not ready in onResume(), so need to discard the update.
    private boolean mIsCallOnStop = false;
    private boolean mIsHotnotClicked = false;
    private boolean mIsConfigurationChanged = false;
    /// M: save the input of SearchView
    private CharSequence mQueryText;

    private NotificationManager mNotificationManager;
    private AudioManager mAudioManager;
    /// M: NFC feature
    NfcAdapter mNfcAdapter;

    /**M: Added for HotKnot feature.@{**/
    private HotKnotHelper mHotKnotHelper = null;
    /**@}**/

    public MediaPlaybackActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        /**M: Added for HotKnot feature.@{**/
        mHotKnotHelper = new HotKnotHelper(this);
        /**@}**/

        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());

        /// M: Get the current orientation and enable action bar to add more function to it.
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        mIsLandscape = (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
        /// M: move UI component init and update to updateUI().
        updateUI();
        mToken = MusicUtils.bindToService(this, osc);
        if (mToken == null) {
            // something went wrong
            mHandler.sendEmptyMessage(QUIT);
        }


        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        /// M: listen more status to update UI @{
        f.addAction(MediaPlaybackService.QUIT_PLAYBACK);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        /// @}
        registerReceiver(mStatusListener, new IntentFilter(f));
        updateTrackInfo();
        /// M: Set the action bar on the right to be up navigation
        ActionBar actionBar = getActionBar();
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        if (actionBar!= null) {
            actionBar.hide();
            actionBar.setDisplayHomeAsUpEnabled(true); 
        }
        /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 begin*/
        mydefalutDrawable = getResources().getDrawable(R.drawable.bird_albumart_mp_unknown); 
        this.getWindow().setBackgroundDrawable(mydefalutDrawable);
        MusicLogUtils.e("wyy_0414", "setBackgroundColor  myDrawable oncreate ");
        /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 end*/
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        /// M: Get Nfc adapter and set callback available. @{
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (mNfcAdapter == null) {
            MusicLogUtils.e(TAG, "NFC not available!");
            return;
        }
        //mNfcAdapter.setMtkBeamPushUrisCallback(this, this);
        /// @}
        /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 begin
        if (FeatureOption.BIRD_DIRECT_WITH_PROXIMITY) {
            if (getProximitySwitchEnable()) {
                if (FeatureOption.BIRD_GESTURE_SENSOR_SUPPORT == true) {
                    // do nothing
                } else {
                    mPlayMusicProxSensor = new PlayMusicProxSensor();
                }
            }
        }
        /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 end*/
    }
    
    int mInitialX = -1;
    int mLastX = -1;
    int mTextWidth = 0;
    int mViewWidth = 0;
    boolean mDraggingLabel = false;
    
    TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.artistname);
        if (vv != null) return (TextView) vv;
        vv = v.findViewById(R.id.albumname);
        if (vv != null) return (TextView) vv;
        vv = v.findViewById(R.id.trackname);
        if (vv != null) return (TextView) vv;
        return null;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            /// M: For ICS style and support for theme manager
            v.setBackgroundColor(getBackgroundColor());
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
            /// M: Because only when the text has ellipzised we need scroll the text view to show ellipsis
            /// text to user, We should get the non-ellipsized text width to determine whether need to scroll
            /// the text view. {@
            mTextWidth = (int) tv.getPaint().measureText(tv.getText().toString());
            mViewWidth = tv.getWidth();
            /// @}

            /// M: when text width large than view width, we need turn off ellipsize to show total text.
            if (mTextWidth > mViewWidth) {
                tv.setEllipsize(null);
            }
        } else if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(0);
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
            /// M: When touch finished, turn on ellipsize.
            tv.setEllipsize(TruncateAt.END);
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > mTouchSlop) {
                // start moving
                mLabelScroller.removeMessages(0, tv);
                /// M: Get the non-ellipsized text view width in event ACTION_DOWN to avoid persistently turn on/off
                /// ellipsize which will cause textview shake. @{
                /*
                // Only turn ellipsizing off when it's not already off, because it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing
                // was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether scrolling
                // should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);*/
                /// @}

                if (mViewWidth > mTextWidth) {
                    // tv.setEllipsize(TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }

                mDraggingLabel = true;
                v.cancelLongPress();
                return true;
            }
        }
        return false; 
    }

    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };
    
    public boolean onLongClick(View view) {

        CharSequence title = null;
        String mime = null;
        String query = null;
        String artist;
        String album;
        String song;
        long audioid;
        
        try {
            artist = mService.getArtistName();
            album = mService.getAlbumName();
            song = mService.getTrackName();
            audioid = mService.getAudioId();
        } catch (RemoteException ex) {
            return true;
        } catch (NullPointerException ex) {
            // we might not actually have the service yet
            return true;
        }

        if (MediaStore.UNKNOWN_STRING.equals(album) &&
                MediaStore.UNKNOWN_STRING.equals(artist) &&
                song != null &&
                song.startsWith("recording")) {
            // not music
            return false;
        }

        if (audioid < 0) {
            return false;
        }

        Cursor c = MusicUtils.query(this,
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioid),
                new String[] {MediaStore.Audio.Media.IS_MUSIC}, null, null, null);
        boolean ismusic = true;
        if (c != null) {
            if (c.moveToFirst()) {
                ismusic = c.getInt(0) != 0;
            }
            c.close();
        }
        if (!ismusic) {
            return false;
        }

        boolean knownartist =
            (artist != null) && !MediaStore.UNKNOWN_STRING.equals(artist);

        boolean knownalbum =
            (album != null) && !MediaStore.UNKNOWN_STRING.equals(album);
        
        if (knownartist && view.equals(mArtistName.getParent())) {
            title = artist;
            query = artist;
            mime = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE;
        } else if (knownalbum && view.equals(mAlbumName.getParent())) {
            title = album;
            if (knownartist) {
                query = artist + " " + album;
            } else {
                query = album;
            }
            mime = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE;
        } else if (view.equals(mTrackName.getParent()) || !knownartist || !knownalbum) {
            if ((song == null) || MediaStore.UNKNOWN_STRING.equals(song)) {
                // A popup of the form "Search for null/'' using ..." is pretty
                // unhelpful, plus, we won't find any way to buy it anyway.
                return true;
            }

            title = song;
            if (knownartist) {
                query = artist + " " + song;
            } else {
                query = song;
            }
            mime = "audio/*"; // the specific type doesn't matter, so don't bother retrieving it
        } else {
            throw new RuntimeException("shouldn't be here");
        }
        title = getString(R.string.mediasearch, title);

        Intent i = new Intent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.putExtra(SearchManager.QUERY, query);
        if(knownartist) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
        }
        if(knownalbum) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
        }
        i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, song);
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, mime);

        startActivity(Intent.createChooser(i, title));
        return true;
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            /// M: only respond when progress bar don't change from touch
            //mLastSeekEventTime = 0;
            mFromTouch = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (mService == null)) return;
            /// M: only respond when progress bar don't change from touch  {@
            //long now = SystemClock.elapsedRealtime();
            //if ((now - mLastSeekEventTime) > 250) {
            //    mLastSeekEventTime = now;
            //    mPosOverride = mDuration * progress / 1000;
            //
            //    try {
            //        mService.seek(mPosOverride);
            //    } catch (RemoteException ex) {
            //    }
            //}

            // trackball event, allow progress updates
            if (!mFromTouch) {
                mPosOverride = mDuration * progress / 1000;
                try {
                    mService.seek(mPosOverride);
                } catch (RemoteException ex) {
                    MusicLogUtils.e(TAG, "Error:" + ex);
                }
            /// @}

                refreshNow();
                mPosOverride = -1;
            }
        }
        public void onStopTrackingTouch(SeekBar bar) {
           /// M: Save the seek position, seek and update UI. @{
           if (mService != null) {
                try {
                    mPosOverride = bar.getProgress() * mDuration / 1000;
                    mService.seek(mPosOverride);
                    refreshNow();
                } catch (RemoteException ex) {
                    MusicLogUtils.e(TAG, "Error:" + ex);
                }
           }
           /// @}
            mPosOverride = -1;
            mFromTouch = false;
        }
    };
    
    private View.OnClickListener mQueueListener = new View.OnClickListener() {
        public void onClick(View v) {
            startActivity(
                    new Intent(Intent.ACTION_EDIT)
                    .setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track")
                    .putExtra("playlist", "nowplaying")
            );
        }
    };
    
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleShuffle();
        }
    };

    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
            cycleRepeat();
        }
    };

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/ 
    private View.OnClickListener mBackHomeListener = new View.OnClickListener() {
        public void onClick(View v) {
            /// M: Navigation button press back,
            /// aviod Navigation button respond JE if Activity is background
            MusicLogUtils.d("wyy_0306", "mBackHomeListener");
            if (!mIsInBackgroud) {
                Intent parentIntent = new Intent(MediaPlaybackActivity.this, MusicBrowserActivity.class);
                parentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                MediaPlaybackActivity.this.finish();
                MediaPlaybackActivity.this.startActivity(parentIntent);
            }
        }
    };
    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/

    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            /// M: performance test, response time for Prev button
            MusicLogUtils.i("MusicPerformanceTest", "[Performance test][Music] prev song start ["
                                + System.currentTimeMillis() + "]");
            mPerformanceTestString = PREV_TEST;

            /// M: Handle click event in handler to avoid ANR for continuous
            // press @{
            MusicLogUtils.d(TAG, "Prev Button onClick,Send Msg");
            Message msg = mHandler.obtainMessage(PREV_BUTTON, null);
            mHandler.removeMessages(PREV_BUTTON);
            mHandler.sendMessage(msg);
            /// @}
        }
    };

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            /// M: performance test, response time for Next button
            MusicLogUtils.i("MusicPerformanceTest", "[Performance test][Music] next song start ["
                                + System.currentTimeMillis() + "]");
            mPerformanceTestString = NEXT_TEST;

            /// M: Handle click event in handler to avoid ANR for continuous
            // press @{
            MusicLogUtils.d(TAG, "Next Button onClick,Send Msg");
            Message msg = mHandler.obtainMessage(NEXT_BUTTON, null);
            mHandler.removeMessages(NEXT_BUTTON);
            mHandler.sendMessage(msg);
            /// @}
        }
    };

    private RepeatingImageButton.RepeatListener mRewListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            MusicLogUtils.d(TAG, "music backward");
            /// M: use to make current playing time aways showing when seeking
            mRepeatCount = repcnt;
            scanBackward(repcnt, howlong);
        }
    };
    
    private RepeatingImageButton.RepeatListener mFfwdListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            MusicLogUtils.d(TAG, "music forward");
            /// M: use to make current playing time aways showing when seeking
            mRepeatCount = repcnt;
            scanForward(repcnt, howlong);
        }
    };
   
    @Override
    public void onStop() {
        if (mSearchItem != null && mIsHotnotClicked) {
            mSearchItem.collapseActionView();
            mIsHotnotClicked = false;
        }
        paused = true;
        MusicLogUtils.d(TAG, "onStop()");
        /// M: so mark mIsCallOnStop is true
        mIsCallOnStop = true;
        mHandler.removeMessages(REFRESH);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        paused = false;
        long next = refreshNow();
        queueNextRefresh(next);
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }
    
    @Override
    public void onResume() {
        /*[bug-106689]xujing 20150415 begin*/
        //if (FeatureOption.BIRD_WEIMI_SYSTEMUI) {
        //    mChangeStatusIconToWhite = true;
        //}
        /*[bug-106689]xujing 20150415 end*/
        super.onResume();
        setRepeatButtonImage();/*[bug-106604][BIRD_WEIMI_MUSIC]wangyueyue 20150314 add*/
        /*[bug-107041][BIRD_WEIMI_MUSIC]wangyueyue 20150429 begin*/
        setShuffleButtonImage();
        setPauseButtonImage();
        /*[bug-107041][BIRD_WEIMI_MUSIC]wangyueyue 20150429 end*/
        /// M: when it launch from status bar, collapse status ba first. @{
        Intent intent = getIntent();
        boolean collapseStatusBar = intent.getBooleanExtra("collapse_statusbar", false);
        MusicLogUtils.d(TAG, "onResume: collapseStatusBar=" + collapseStatusBar);
        if (collapseStatusBar) {
            StatusBarManager statusBar = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
            statusBar.collapsePanels();
        }
        ///@}

        updateTrackInfo();
        /// M: if it doesn't come from onStop(),we should update pause button. @{
        if (!mIsCallOnStop) {
            setPauseButtonImage();
        }
        mIsCallOnStop = false;
        /// @}

        /// M: When back to this activity, ask service for right position
        mPosOverride = -1;
        invalidateOptionsMenu();

        /// M: performance default test, response time for Play button
        mPerformanceTestString = PLAY_TEST;
        /// M: aviod Navigation button respond JE if Activity is background
        mIsInBackgroud = false;
        /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 begin
        if (FeatureOption.BIRD_DIRECT_WITH_PROXIMITY == true) {
            if (getProximitySwitchEnable()) {
                if (FeatureOption.BIRD_GESTURE_SENSOR_SUPPORT == true) {// 手势
                    // do nothing
                } else {
                    if (mPlayMusicProxSensor == null) {
                        mPlayMusicProxSensor = new PlayMusicProxSensor();
                    }
                    mPlayMusicProxSensor.start();
                }
            } else {
                if (FeatureOption.BIRD_GESTURE_SENSOR_SUPPORT == true) {// 手势
                    // do nothing
                } else {
                    if (mPlayMusicProxSensor != null) {
                        mPlayMusicProxSensor.stop();
                        mPlayMusicProxSensor = null;
                    }
                }
            }
        }
        /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 end*/
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mStatusListener);
        MusicUtils.unbindFromService(mToken);
        mService = null;
        mAlbumArtWorker.quit();
        super.onDestroy();
        // System.out.println("***************** playback activity onDestroy\n");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Don't show the menu items if we got launched by path/filedescriptor, or
        // if we're in one shot mode. In most cases, these menu items are not
        // useful in those modes, so for consistency we never show them in these
        // modes, instead of tailoring them to the specific file being played.
        long currentAudioId = MusicUtils.getCurrentAudioId();
        if (currentAudioId >= 0) {
            /// M: adjust menu sequence
            // menu.add(0, GOTO_START, 0, R.string.goto_start)
            //            .setIcon(R.drawable.ic_menu_music_library);
            menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
            /// M: get the object for method onPrepareOptionsMenu to keep playlist menu up-to-date
            mAddToPlaylistSubmenu = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
                    R.string.add_to_playlist).setIcon(android.R.drawable.ic_menu_add);
            // these next two are in a separate group, so they can be shown/hidden as needed
            // based on the keyguard state
            /*
            if (MusicUtils.isVoiceCapable(this)) {
                menu.add(0, USE_AS_RINGTONE, 0, R.string.ringtone_menu_short).setIcon(
                        R.drawable.ic_menu_set_as_ringtone);
            }*/
            
            if(UserHandle.myUserId() == UserHandle.USER_OWNER){
                if (MusicUtils.isVoiceCapable(this)) {
                    menu.add(0, USE_AS_RINGTONE, 0, R.string.ringtone_menu_short).setIcon(
                             R.drawable.ic_menu_set_as_ringtone);
                }
            }

            menu.add(0, DELETE_ITEM, 0, R.string.delete_item)
                    .setIcon(R.drawable.ic_menu_delete);
            /// M: move to prepare option menu to disable menu when MusicFX is disable
            menu.add(0, EFFECTS_PANEL, 0, R.string.effects_list_title)
                    .setIcon(R.drawable.ic_menu_eq);

            /// M: Add FMTransmitter option menu, and remove goto library. {@
            if (MusicFeatureOption.IS_SUPPORT_FM_TX) {
                menu.add(0, FM_TRANSMITTER, 0, R.string.music_fm_transmiter)
                    .setIcon(R.drawable.ic_menu_fmtransmitter);
            } else {
                menu.add(0, GOTO_START, 0, R.string.goto_start)
                    .setIcon(R.drawable.ic_menu_music_library);
            }
            /// @}

            // Add action bar for no physical key(different in landscape and portrait). {@
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.music_playback_action_bar, menu);
            mQueueMenuItem = menu.findItem(R.id.current_playlist_menu_item);
            mShuffleMenuItem = menu.findItem(R.id.shuffle_menu_item);
            mRepeatMenuItem = menu.findItem(R.id.repeat_menu_item);
            /// @}

            /// M: Add search view
            //mSearchItem = MusicUtils.addSearchView(this, menu, mQueryTextListener, mOnSuggestionListener);/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 add*/

            /// M: collapseActionView when search view dismiss
            /*final SearchManager searchManager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
            searchManager.setOnDismissListener(new SearchManager.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mSearchItem.collapseActionView();
                }
            });*/
            /// @}

            /** M: Added for HotKnot feature.@{ **/
            if (mHotKnotHelper != null) {
                mHotKnotHelper.createHotKnotMenu(menu);
            }
            /**@}**/
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mService == null) return false;
        MenuItem item = menu.findItem(PARTY_SHUFFLE);
        if (item != null) {
            int shuffle = MusicUtils.getCurrentShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle_off);
            } else {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle);
            }
        }
        
        /**M: disable the setRingTone menu item while current user is not owner user.@{**/
        if(UserHandle.myUserId() == UserHandle.USER_OWNER){
            /// M: DRM feature, when track is drm and not FL type, it can not set as ringtone. {@
            if (MusicFeatureOption.IS_SUPPORT_DRM && MusicUtils.isVoiceCapable(this)) {
                try {
                    menu.findItem(USE_AS_RINGTONE).setVisible(mService.canUseAsRingtone());
                } catch (RemoteException e) {
                    MusicLogUtils.e(TAG, "onPrepareOptionsMenu with RemoteException " + e);
                }
            }
        }else{
            MenuItem ringtoneItem = menu.findItem(USE_AS_RINGTONE);
            if(ringtoneItem != null){
                ringtoneItem.setVisible(false);
            }
        }
        /**@}**/
        /// M: Set effect menu visible depend the effect class whether disable or enable. {@
        MusicUtils.setEffectPanelMenu(getApplicationContext(), menu);
        /// @}

        /// M: Set FMTransmitter menu visible depend the FMTransmitter class whether
        /// disable or enable. {@
        if (MusicFeatureOption.IS_SUPPORT_FM_TX) {
            Intent intentFmTx = new Intent(FM_TX_ACTIVITY);
            intentFmTx.setClassName(FM_TX_PACKAGE, FM_TX_ACTIVITY);
            menu.findItem(FM_TRANSMITTER).setVisible(getPackageManager().resolveActivity(intentFmTx, 0) != null);
        }
        /// @}

        /// M: Keep the playlist menu up-to-date.
        MusicUtils.makePlaylistMenu(this, mAddToPlaylistSubmenu);
        mAddToPlaylistSubmenu.removeItem(MusicUtils.Defs.QUEUE);
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        menu.setGroupVisible(1, !km.inKeyguardRestrictedInputMode());

        /// M: Switch to show action bar in landscape or button in portrait. {@
        mQueueMenuItem.setVisible(mIsLandscape);
        mShuffleMenuItem.setVisible(mIsLandscape);
        mRepeatMenuItem.setVisible(mIsLandscape);
        setRepeatButtonImage();
        setShuffleButtonImage();
        /// @}
        MusicLogUtils.e(TAG, "mIsCallOnStop:" + mIsCallOnStop + ",mIsInBackgroud:" + mIsInBackgroud
                + ",mIsConfigurationChanged:" + mIsConfigurationChanged);
        if (mSearchItem != null && (mIsCallOnStop || mIsInBackgroud) && !mIsConfigurationChanged) {
            mSearchItem.collapseActionView();
            mIsCallOnStop = false;
            mQueryText = null;
        }
        
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        if (mSearchItem != null){
             MusicLogUtils.e(TAG, "isActionViewExpanded:" + mSearchItem.isActionViewExpanded());
        }   
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
       
        /// M: restore the input of SearchView when config change @{
        if (mQueryText != null && !mQueryText.toString().equals("")) {
            MusicLogUtils.e(TAG, "setQueryText:" + mQueryText);
            SearchView searchView = (SearchView) mSearchItem.getActionView();
            searchView.setQuery(mQueryText, false);
            mQueryText = null;
        }
        mIsConfigurationChanged = false;
        /// @}

        /**M:Added for HotKnot feature.@{**/
        if (mHotKnotHelper != null) {
           mHotKnotHelper.updateHotKnotMenu(mHotKnotHelper.mSendable);
        }
        /**@}**/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        try {
            switch (item.getItemId()) {
                case GOTO_START:
                    intent = new Intent();
                    intent.setClass(this, MusicBrowserActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    break;
                case USE_AS_RINGTONE: {
                    // Set the system setting to make this the current ringtone
                    if (mService != null) {
                        MusicUtils.setRingtone(this, mService.getAudioId());
                    }
                    return true;
                }
                case PARTY_SHUFFLE:
                    MusicUtils.togglePartyShuffle();
                    setShuffleButtonImage();
                    /// M: Update repeat button because will set repeat current to repeat all when open party shuffle.
                    setRepeatButtonImage();
                    break;
                    
                case NEW_PLAYLIST: {
                    intent = new Intent();
                    intent.setClass(this, CreatePlaylist.class);
                    /// M: Add to indicate the save_as_playlist and new_playlist
                    intent.putExtra(MusicUtils.SAVE_PLAYLIST_FLAG, MusicUtils.NEW_PLAYLIST);
                    startActivityForResult(intent, NEW_PLAYLIST);
                    return true;
                }

                case PLAYLIST_SELECTED: {
                    long [] list = new long[1];
                    list[0] = MusicUtils.getCurrentAudioId();
                    long playlist = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(this, list, playlist);
                    return true;
                }
                
                case DELETE_ITEM: {
                    if (mService != null) {
                        long [] list = new long[1];
                        list[0] = MusicUtils.getCurrentAudioId();
                        Bundle b = new Bundle();
                        String f;
                        /// M: Get string in DeleteItems Activity to get current language string. @{
                        //if (android.os.Environment.isExternalStorageRemovable()) {
                        //f = getString(R.string.delete_song_desc, mService.getTrackName());
                        //} else {
                        //    f = getString(R.string.delete_song_desc_nosdcard, mService.getTrackName());
                        //}
                        //b.putString("description", f);
                        b.putInt(MusicUtils.DELETE_DESC_STRING_ID, R.string.delete_song_desc);
                        b.putString(MusicUtils.DELETE_DESC_TRACK_INFO, mService.getTrackName());
                        /// @}
                        b.putLongArray("items", list);
                        intent = new Intent();
                        intent.setClass(this, DeleteItems.class);
                        intent.putExtras(b);
                        startActivityForResult(intent, -1);
                    }
                    return true;
                }

                /// M: Show effect panel and call the same method as other activities.
                case EFFECTS_PANEL:
                    return MusicUtils.startEffectPanel(this);

                /// M: Open FMTransmitter and Search view. {@
                case FM_TRANSMITTER:
                    Intent intentFMTx = new Intent(FM_TX_ACTIVITY);
                    intentFMTx.setClassName(FM_TX_PACKAGE, FM_TX_ACTIVITY);

                    try {
                        startActivity(intentFMTx);
                    } catch (ActivityNotFoundException anfe) {
                        MusicLogUtils.e(TAG, "FMTx activity isn't found!!");
                    }

                    return true;

                case R.id.search:
                    onSearchRequested();
                    return true;
                /// @}

                /// M: handle action bar and navigation up button. {@
                case android.R.id.home:
                    /// M: Navigation button press back,
                    /// aviod Navigation button respond JE if Activity is background
                    if (!mIsInBackgroud) {
                        Intent parentIntent = new Intent(this, MusicBrowserActivity.class);
                        parentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        finish();
                        startActivity(parentIntent);
                    }
                    return true;

                case R.id.current_playlist_menu_item:
                    /// M: Current playlist(queue) button
                    mQueueListener.onClick(null);
                    break;

                case R.id.shuffle_menu_item:
                    /// M: Shuffle button
                    toggleShuffle();
                    break;

                case R.id.repeat_menu_item:
                    /// M: Repeat button
                    cycleRepeat();
                    break;
                /**M: Added for HotKnot feature.@{**/
                case HOTKNOT:
                    if (mHotKnotHelper != null) {
                        mIsHotnotClicked = true;
                        mHotKnotHelper.shareViaHotKnot();
                    }
                /**@}**/
                default:
                    return true;
                /// @}
            }
        } catch (RemoteException ex) {
            MusicLogUtils.e(TAG, "onOptionsItemSelected with RemoteException " + ex);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case NEW_PLAYLIST:
                Uri uri = intent.getData();
                if (uri != null) {
                    long [] list = new long[1];
                    list[0] = MusicUtils.getCurrentAudioId();
                    int playlist = Integer.parseInt(uri.getLastPathSegment());
                    MusicUtils.addToPlaylist(this, list, playlist);
                }
                break;
        }
    }
    private final int keyboard[][] = {
        {
            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_I,
            KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,
        },
        {
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_G,
            KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_J,
            KeyEvent.KEYCODE_K,
            KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_DEL,
        },
        {
            KeyEvent.KEYCODE_Z,
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_ENTER
        }

    };

    private int lastX;
    private int lastY;

    private boolean seekMethod1(int keyCode)
    {
        if (mService == null) return false;
        for(int x=0;x<10;x++) {
            for(int y=0;y<3;y++) {
                if(keyboard[y][x] == keyCode) {
                    int dir = 0;
                    // top row
                    if(x == lastX && y == lastY) dir = 0;
                    else if (y == 0 && lastY == 0 && x > lastX) dir = 1;
                    else if (y == 0 && lastY == 0 && x < lastX) dir = -1;
                    // bottom row
                    else if (y == 2 && lastY == 2 && x > lastX) dir = -1;
                    else if (y == 2 && lastY == 2 && x < lastX) dir = 1;
                    // moving up
                    else if (y < lastY && x <= 4) dir = 1; 
                    else if (y < lastY && x >= 5) dir = -1; 
                    // moving down
                    else if (y > lastY && x <= 4) dir = -1; 
                    else if (y > lastY && x >= 5) dir = 1; 
                    lastX = x;
                    lastY = y;
                    try {
                        mService.seek(mService.position() + dir * 5);
                    } catch (RemoteException ex) {
                    }
                    refreshNow();
                    return true;
                }
            }
        }
        lastX = -1;
        lastY = -1;
        return false;
    }

    private boolean seekMethod2(int keyCode)
    {
        if (mService == null) return false;
        for(int i=0;i<10;i++) {
            if(keyboard[0][i] == keyCode) {
                int seekpercentage = 100*i/10;
                try {
                    mService.seek(mService.duration() * seekpercentage / 100);
                } catch (RemoteException ex) {
                }
                refreshNow();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!useDpadMusicControl()) {
                        break;
                    }
                    if (mService != null) {
                        if (!mSeeking && mStartSeekPos >= 0) {
                            mPauseButton.requestFocus();
                            if (mStartSeekPos < 1000) {
                                mService.prev();
                            } else {
                                mService.seek(0);
                            }
                        } else {
                            scanBackward(-1, event.getEventTime() - event.getDownTime());
                            mPauseButton.requestFocus();
                            mStartSeekPos = -1;
                        }
                    }
                    mSeeking = false;
                    mPosOverride = -1;
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!useDpadMusicControl()) {
                        break;
                    }
                    if (mService != null) {
                        if (!mSeeking && mStartSeekPos >= 0) {
                            mPauseButton.requestFocus();
                            mService.next();
                        } else {
                            scanForward(-1, event.getEventTime() - event.getDownTime());
                            mPauseButton.requestFocus();
                            mStartSeekPos = -1;
                        }
                    }
                    mSeeking = false;
                    mPosOverride = -1;
                    return true;

                /// M: handle key code center. {@
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    View curSel = getCurrentFocus();
                    if ((curSel != null && R.id.pause == curSel.getId()) ||
                            (curSel == null)) {
                        doPauseResume();
                    }
                    return true;
                /// @}
            }
        } catch (RemoteException ex) {
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean useDpadMusicControl() {
        if (mDeviceHasDpad && (mPrevButton.isFocused() ||
                mNextButton.isFocused() ||
                mPauseButton.isFocused())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        int direction = -1;
        int repcnt = event.getRepeatCount();

        if((seekmethod==0)?seekMethod1(keyCode):seekMethod2(keyCode))
            return true;

        switch(keyCode)
        {
/*
            // image scale
            case KeyEvent.KEYCODE_Q: av.adjustParams(-0.05, 0.0, 0.0, 0.0, 0.0,-1.0); break;
            case KeyEvent.KEYCODE_E: av.adjustParams( 0.05, 0.0, 0.0, 0.0, 0.0, 1.0); break;
            // image translate
            case KeyEvent.KEYCODE_W: av.adjustParams(    0.0, 0.0,-1.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_X: av.adjustParams(    0.0, 0.0, 1.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_A: av.adjustParams(    0.0,-1.0, 0.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_D: av.adjustParams(    0.0, 1.0, 0.0, 0.0, 0.0, 0.0); break;
            // camera rotation
            case KeyEvent.KEYCODE_R: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 0.0,-1.0); break;
            case KeyEvent.KEYCODE_U: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 0.0, 1.0); break;
            // camera translate
            case KeyEvent.KEYCODE_Y: av.adjustParams(    0.0, 0.0, 0.0, 0.0,-1.0, 0.0); break;
            case KeyEvent.KEYCODE_N: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 1.0, 0.0); break;
            case KeyEvent.KEYCODE_G: av.adjustParams(    0.0, 0.0, 0.0,-1.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_J: av.adjustParams(    0.0, 0.0, 0.0, 1.0, 0.0, 0.0); break;

*/

            case KeyEvent.KEYCODE_SLASH:
                seekmethod = 1 - seekmethod;
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!useDpadMusicControl()) {
                    break;
                }
                if (!mPrevButton.hasFocus()) {
                    mPrevButton.requestFocus();
                }
                scanBackward(repcnt, event.getEventTime() - event.getDownTime());
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!useDpadMusicControl()) {
                    break;
                }
                if (!mNextButton.hasFocus()) {
                    mNextButton.requestFocus();
                }
                scanForward(repcnt, event.getEventTime() - event.getDownTime());
                return true;

            case KeyEvent.KEYCODE_S:
                toggleShuffle();
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
                /// M: handle key code center.
                 return true;

            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_ENTER:
                doPauseResume();
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (mSearchItem != null) {
                    if (mSearchItem.isActionViewExpanded()) {
                        return true;
                    }
                }
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void scanBackward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos - delta;
                if (newpos < 0) {
                    // move to previous track
                    mService.prev();
                    long duration = mService.duration();
                    mStartSeekPos += duration;
                    newpos += duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
                    mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
        }
    }

    private void scanForward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos + delta;
                long duration = mService.duration();
                if (newpos >= duration) {
                    // move to next track
                    mService.next();
                    mStartSeekPos -= duration; // is OK to go negative
                    newpos -= duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
                    mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void doPauseResume() {
        try {
            if(mService != null) {
                Boolean isPlaying = mService.isPlaying();
                MusicLogUtils.d(TAG, "doPauseResume: isPlaying=" + isPlaying);
                /// M: AVRCP and Android Music AP supports the FF/REWIND
                //   aways get position from service if user press pause button
                mPosOverride = -1;
                if (isPlaying) {
                    mService.pause();
                } else {
                    mService.play();
                }
                refreshNow();
                setPauseButtonImage();
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void toggleShuffle() {
        if (mService == null) {
            return;
        }
        try {
            int shuffle = mService.getShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_NONE) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                if (mService.getRepeatMode() == MediaPlaybackService.REPEAT_CURRENT) {
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                }
                /// M: need to refresh repeat button when we modify rpeate mode.
                setRepeatButtonImage();
                showToast(R.string.shuffle_on_notif);
            } else if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL ||
                    shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                /// M: After turn off party shuffle, we should to refresh option menu to avoid user click fast to show
                /// party shuffle off when has turned off.
                //invalidateOptionsMenu();
                showToast(R.string.shuffle_off_notif);
            } else {
                MusicLogUtils.w(TAG, "Invalid shuffle mode: " + shuffle);
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
        }
    }
    
    private void cycleRepeat() {
        if (mService == null) {
            return;
        }
        try {
            int mode = mService.getRepeatMode();
            if (mode == MediaPlaybackService.REPEAT_NONE) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                showToast(R.string.repeat_all_notif);
            } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                if (mService.getShuffleMode() != MediaPlaybackService.SHUFFLE_NONE) {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    /// M: After turn off party shuffle, we should to refresh option menu to avoid user click fast to show
                    /// party shuffle off when has turned off.
                    //invalidateOptionsMenu();
                    setShuffleButtonImage();
                }
                showToast(R.string.repeat_current_notif);
            } else {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
                showToast(R.string.repeat_off_notif);
            }
            setRepeatButtonImage();
        } catch (RemoteException ex) {
        }
        
    }
    
    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }

    private void startPlayback() {

        if(mService == null)
            return;
        Intent intent = getIntent();
        String filename = "";
        Uri uri = intent.getData();
        if (uri != null && uri.toString().length() > 0) {
            // If this is a file:// URI, just use the path directly instead
            // of going through the open-from-filedescriptor codepath.
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                filename = uri.getPath();
            } else {
                filename = uri.toString();
            }
            try {
                mService.stop();
                mService.openFile(filename);
                mService.play();
                setIntent(new Intent());
            } catch (Exception ex) {
                MusicLogUtils.d(TAG, "couldn't start playback: " + ex);
            }
        }

        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }

    private ServiceConnection osc = new ServiceConnection() {
            public void onServiceConnected(ComponentName classname, IBinder obj) {
                mService = IMediaPlaybackService.Stub.asInterface(obj);
                /// M: Call this to invalidate option menu to install action bar
                invalidateOptionsMenu();
                startPlayback();
                try {
                    // Assume something is playing when the service says it is,
                    // but also if the audio ID is valid but the service is paused.
                    if (mService.getAudioId() >= 0 || mService.isPlaying() ||
                            mService.getPath() != null) {
                        // something is playing now, we're done
                        /// M: Only in portrait we need to set them to be
                        // visible {@
                        if (!mIsLandscape) {
                            mRepeatButton.setVisibility(View.VISIBLE);
                            mShuffleButton.setVisibility(View.VISIBLE);
                            mQueueButton.setVisibility(View.VISIBLE);
                        }
                        /// @}
                        setRepeatButtonImage();
                        setShuffleButtonImage();
                        setPauseButtonImage();
                        return;
                    }
                } catch (RemoteException ex) {
                }
                // Service is dead or not playing anything. If we got here as part
                // of a "play this file" Intent, exit. Otherwise go to the Music
                // app start screen.

                /// M: MTK Mark for PlayAll timing issue, if play many error file, it will back to
                /// last screen.if play one or two error file, it will go to start screen, So we
                /// unify the behavior. {@
                //if (getIntent().getData() == null) {
                //    Intent intent = new Intent(Intent.ACTION_MAIN);
                //    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //    intent.setClass(MediaPlaybackActivity.this, MusicBrowserActivity.class);
                //    startActivity(intent);
                //}
                /// @}

                finish();
            }
            public void onServiceDisconnected(ComponentName classname) {
                mService = null;
                /// M: Close the activity when service not exsit
                finish();
            }
    };

    private void setRepeatButtonImage() {
        if (mService == null) return;
        try {
            /// M: Set drawable to action bar in landscape and set it to button in
            // portrait {@
            int drawable;
            switch (mService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:
                    drawable = R.drawable.ic_mp_repeat_all_btn;
                    break;

                case MediaPlaybackService.REPEAT_CURRENT:
                    drawable = R.drawable.ic_mp_repeat_once_btn;
                    break;

                default:
                    drawable = R.drawable.ic_mp_repeat_off_btn;
                    break;

            }
            if (mIsLandscape) {
                if (mRepeatMenuItem != null) {
                    mRepeatMenuItem.setIcon(drawable);
                }
            } else {
                mRepeatButton.setImageResource(drawable);
            }
            /// @}
        } catch (RemoteException ex) {
        }
    }
    
    private void setShuffleButtonImage() {
        if (mService == null) return;
        try {
            /// M: Set drawable to action bar in landscape and set it to button in
            // portrait  {@
            int drawable;
            switch (mService.getShuffleMode()) {
                case MediaPlaybackService.SHUFFLE_NONE:
                    drawable = R.drawable.ic_mp_shuffle_off_btn;
                    break;

                case MediaPlaybackService.SHUFFLE_AUTO:
                    drawable = R.drawable.ic_mp_partyshuffle_on_btn;
                    break;

                default:
                    drawable = R.drawable.ic_mp_shuffle_on_btn;
                    break;

            }
            if (mIsLandscape) {
                if (mShuffleMenuItem != null) {
                    mShuffleMenuItem.setIcon(drawable);
                }
            } else {
                mShuffleButton.setImageResource(drawable);
            }
            /// @}
        } catch (RemoteException ex) {
        }
    }
    
    private void setPauseButtonImage() {
        try {
            if (mService != null && mService.isPlaying()) {
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                //mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                mPauseButton.setImageResource(R.drawable.bird_media_pause);
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                /// M: When not seeking, aways get position from service to
                // update current playing time.
                if (!mSeeking) {
                    mPosOverride = -1;
                }
            } else {
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                //mPauseButton.setImageResource(android.R.drawable.ic_media_play);
                mPauseButton.setImageResource(R.drawable.bird_media_play);
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
            }
        } catch (RemoteException ex) {
        }
    }

    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
    private LinearLayout mLinearLayout;
    private ImageView mBackHome;
    private TextView mArtistNameTitle;
    private TextView mTrackNameTitle;
    private LrcView mLrcView;
    private LrcTask mLrcTask = null;
    private String songPath = "";
    private String lrcPath = "";
    private String LastLrcPath = "";
    private boolean isLoadLrc = false;
    private boolean isLoadLrcFinished = false;
    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
    private ImageView mAlbum;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private ProgressBar mProgress;
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private long mDuration;
    private int seekmethod;
    private boolean paused;

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;

    /// M: Define next and prev button.
    private static final int NEXT_BUTTON = 6;
    private static final int PREV_BUTTON = 7;

    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        /// M: duration for position correction for play complete
        final int positionCorrection = 300;
        if(mService == null)
            return 500;
        try {
            MusicLogUtils.d(TAG, "refreshNow()-mPosOverride = " + mPosOverride);
            long position = mService.position();
            MusicLogUtils.d(TAG, "refreshNow()-position = " + position);
            long pos = mPosOverride < 0 ? position : mPosOverride;
            /// M: position correction for play complete @{
            if (pos + positionCorrection > mDuration) {
                MusicLogUtils.d(TAG, "refreshNow()-do a workaround for position");
                pos = mDuration;
            }
            /// @}
            /// M: update duration for specific formats
            updateDuration(pos);
            mDuration = mService.duration();
            if ((pos >= 0) && (mDuration > 0)) {
                MusicLogUtils.d(TAG, "refreshNow()-pos = " + pos);
                String time = MusicUtils.makeTimeString(this, pos / 1000);
                MusicLogUtils.d(TAG, "refreshNow()-time = " + time);
                mCurrentTime.setText(time);
                /// M: Don't need to update from touch @{
                if (!mFromTouch) {
                    int progress = (int) (1000 * pos / mDuration);
                    mProgress.setProgress(progress);
                }
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                if (isLoadLrc) {//判断是否需要加载歌词文件
                    setLrcRows();
                    isLoadLrc = false;                    
                }
                if (isLoadLrcFinished) {//只有加载完歌词文件后再进行歌词移动
                    mLrcView.seekTo((int) position, true, false);
                }
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                /// @}
                /// M: use to make current playing time aways showing when seeking
                if (mService.isPlaying() || mRepeatCount > -1) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                    mCurrentTime.setVisibility(View.VISIBLE);
                    //mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                    return 500;
                }
            } else {
                /// M: adjust the UI for error file  @{
                mCurrentTime.setVisibility(View.VISIBLE);
                String time = MusicUtils.makeTimeString(this, 0);
                mCurrentTime.setText(time);
                mTotalTime.setText("--:--");
                if (!mFromTouch) {
                    mProgress.setProgress(0);
                }
                /// @}
            }
            // calculate the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            long remaining = 1000 - (pos % 1000);

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) width = 320;
            long smoothrefreshtime = mDuration / width;

            if (smoothrefreshtime > remaining) return remaining;
            if (smoothrefreshtime < 20) return 20;
            return smoothrefreshtime;
        } catch (RemoteException ex) {
        }
        return 500;
    }
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                    //mAlbum.setImageBitmap((Bitmap)msg.obj);
                    //mAlbum.getDrawable().setDither(true);
                    /*[bug-null][gaowei][20170704]begin*/
                    BitmapDrawable bd=new BitmapDrawable(fastblur(MediaPlaybackActivity.this, (Bitmap)msg.obj, 20));
                    mAlbumAdjust = (MaskImage)findViewById(R.id.album_adjust_view);
                    if (mAlbumAdjust != null) {
                        mAlbumAdjust.setMaskBg((Bitmap)msg.obj, R.drawable.album_mask);
                    }
                    /*[bug-null][gaowei][20170704]end*/
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150330 begin*/
                    //mLinearLayout.setBackgroundDrawable(bd);
                    /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 begin*/
                    /*TransitionDrawable  transitionDrawable = new TransitionDrawable(  
                            new Drawable[] {mydefalutDrawable,new BitmapDrawable((Bitmap)msg.obj)});  
                    transitionDrawable.setCrossFadeEnabled(true); 
                    MediaPlaybackActivity.this.getWindow().setBackgroundDrawable(transitionDrawable);   
                    transitionDrawable.startTransition(300);  */
                    MediaPlaybackActivity.this.getWindow().setBackgroundDrawable(bd);
                    /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 end*/
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150330 end*/
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                    break;

                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                    
                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(MediaPlaybackActivity.this) //, AlertDialog.THEME_MATERIAL_LIGHT
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                    break;

                /// M: Handle next and prev button. {@
                case NEXT_BUTTON:
                    MusicLogUtils.d(TAG, "Next Handle");
                    if (mService == null) {
                        return;
                    }
                    mNextButton.setEnabled(false);
                    mNextButton.setFocusable(false);
                    try {
                        mService.next();
                        mPosOverride = -1;
                    } catch (RemoteException ex) {
                        MusicLogUtils.e(TAG, "Error:" + ex);
                    }
                    mNextButton.setEnabled(true);
                    mNextButton.setFocusable(true);
                    break;

                case PREV_BUTTON:
                    MusicLogUtils.d(TAG, "Prev Handle");
                    if (mService == null) {
                        return;
                    }
                    mPrevButton.setEnabled(false);
                    mPrevButton.setFocusable(false);
                    try {
                        mPosOverride = -1;
                        mService.prev();
                    } catch (RemoteException ex) {
                        MusicLogUtils.e(TAG, "Error:" + ex);
                    }
                    mPrevButton.setEnabled(true);
                    mPrevButton.setFocusable(true);
                    break;
                /// @}

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.d(TAG, "mStatusListener: " + action);
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                /// M: Refresh option menu when meta change
                invalidateOptionsMenu();

                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
                setPauseButtonImage();

                MusicLogUtils.v("MusicPerformanceTest", "[Performance test][Music] "
                        + mPerformanceTestString + " end [" + System.currentTimeMillis()
                        + "]");
                MusicLogUtils.v("MusicPerformanceTest", "[CMCC Performance test][Music] "
                        + mPerformanceTestString + " end [" + System.currentTimeMillis()
                        + "]");

                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            /// M: Handle more status. {@
            } else if (action.equals(MediaPlaybackService.QUIT_PLAYBACK)) {
                mHandler.removeMessages(REFRESH);
                finish();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                /// M: stop refreshing
                MusicLogUtils.d(TAG, "onReceive, stop refreshing ...");
                mHandler.removeMessages(REFRESH);
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                /// M: restore refreshing
                MusicLogUtils.d(TAG, "onReceive, restore refreshing ...");
                long next = refreshNow();
                queueNextRefresh(next);
            }
            /// @}
        }
    };

    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;
        AlbumSongIdWrapper(long aid, long sid) {
            albumid = aid;
            songid = sid;
        }
    }
    
    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            String path = mService.getPath();
            if (path == null) {
                finish();
                return;
            }
            
            /**M:Added for HotKnot feature.@{**/
            if (mHotKnotHelper != null) {
                mHotKnotHelper.setHotKnotUri(path);
            }
            /**@}**/

            long songid = mService.getAudioId(); 
            if (songid < 0 && path.toLowerCase().startsWith("http://")) {
                // Once we can get album art and meta data from MediaPlayer, we
                // can show that info again when streaming.
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                // ((View) mArtistName.getParent()).setVisibility(View.INVISIBLE);
                // ((View) mAlbumName.getParent()).setVisibility(View.INVISIBLE);
                mTrackNameTitle.setText(path);
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                mAlbum.setVisibility(View.GONE);
                mTrackName.setText(path);

                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(-1, -1)).sendToTarget();
            } else {
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                //((View) mArtistName.getParent()).setVisibility(View.VISIBLE);
                // ((View) mAlbumName.getParent()).setVisibility(View.VISIBLE);
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                String artistName = mService.getArtistName();
                if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
                    artistName = getString(R.string.unknown_artist_name);
                }
                mArtistName.setText(artistName);
                mArtistNameTitle.setText(artistName);/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 add*/
                String albumName = mService.getAlbumName();
                long albumid = mService.getAlbumId();
                if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
                    albumName = getString(R.string.unknown_album_name);
                    albumid = -1;
                }
                mAlbumName.setText(albumName);
                mTrackName.setText(mService.getTrackName());
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                if (mArtSongId != songid){
                    isLoadLrc = true;
                }
                mTrackNameTitle.setText(mService.getTrackName());
                /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(albumid, songid)).sendToTarget();
                mAlbum.setVisibility(View.VISIBLE);
            }
            mDuration = mService.duration();
            mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));
            /// M: For specific file, its duration need to be updated when playing.
            recordDurationUpdateStatus();
        } catch (RemoteException ex) {
            finish();
        }
    }

    public class AlbumArtHandler extends Handler {
        private long mAlbumId = -1;
        
        public AlbumArtHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg)
        {
            /// M: Keep album art in mArtBitmap to improve loading speed when config changed.
            long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
            long songid = ((AlbumSongIdWrapper) msg.obj).songid;
            if (msg.what == GET_ALBUM_ART && (mAlbumId != albumid || albumid < 0 || mIsShowAlbumArt)) {
                Message numsg = null;
                // while decoding the new image, show the default album art
                if (mArtBitmap == null || mArtSongId != songid) {
                    /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 begin*/
                    /*numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessageDelayed(numsg, 300);*/
                    /*[bug-106691][BIRD_WEIMI_MUSIC]wangyueyue 20150414 end*/

                    // Don't allow default artwork here, because we want to fall back to song-specific
                    // album art if we can't find anything for the album.
                    /// M: if don't get album art from file,or the album art is not the same
                    /// as the song ,we should get the album art again
                    mArtBitmap = MusicUtils.getArtwork(MediaPlaybackActivity.this,
                                                        songid, albumid, false);
                    MusicLogUtils.d(TAG, "get art. mArtSongId = " + mArtSongId
                                            + " ,songid = " + songid + " ");
                    mArtSongId = songid;
                }

                if (mArtBitmap == null) {
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
                    //mArtBitmap = MusicUtils.getDefaultArtwork(MediaPlaybackActivity.this);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    mArtBitmap = BitmapFactory.decodeStream(getResources().openRawResource(R.drawable.bird_albumart_mp_unknown), null, opts);
                    MusicLogUtils.d("wyy_0414", "mArtBitmap == null  01");
                    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
                    albumid = -1;
                }
                if (mArtBitmap != null) {
                    numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, mArtBitmap);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessage(numsg);
                }
                mAlbumId = albumid;
                mIsShowAlbumArt = false;
            }
        }
    }
    
    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;
        
        /**
         * Creates a worker thread with the given name. The thread
         * then runs a {@link android.os.Looper}.
         * @param name A name for the new thread
         */
        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        public Looper getLooper() {
            return mLooper;
        }
        
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }
        
        public void quit() {
            mLooper.quit();
        }
    }

    /**
     * M: move from onCreat, Update media playback activity ui. call this method
     * when activity oncreate or on configuration changed.
     */
    private void updateUI() {
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        setContentView(R.layout.bird_audio_player); //audio_player
        mLinearLayout = (LinearLayout)findViewById(R.id.album_info);
        mBackHome = (ImageView)findViewById(R.id.back_to_home);
        mBackHome.setOnClickListener(mBackHomeListener);
        mArtistNameTitle = (TextView) findViewById(R.id.title_artistname);
        mTrackNameTitle = (TextView) findViewById(R.id.title_trackname);
        mLrcView = (LrcView) findViewById(R.id.media_lrcview);
        mLrcView.setOnSeekToListener(onSeekToListener);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        /*[bug-null][gaowei][20170704]begin*/
        mMoreVert = (ImageView)findViewById(R.id.bird_more_vert);
        mMoreVert.setOnClickListener(mMenuClickListener);
        /*[bug-null][gaowei][20170704]end*/
        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);

        mAlbum = (ImageView) findViewById(R.id.album);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mAlbumName = (TextView) findViewById(R.id.albumname);
        mTrackName = (TextView) findViewById(R.id.trackname);

        View v = (View) mArtistName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View) mAlbumName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View) mTrackName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPrevButton.setRepeatListener(mRewListener, 260);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        mNextButton = (RepeatingImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);
        mNextButton.setRepeatListener(mFfwdListener, 260);
        seekmethod = 1;

        mDeviceHasDpad = (getResources().getConfiguration().navigation ==
            Configuration.NAVIGATION_DPAD);

        /// M: Only when in PORTRAIT we use button, otherwise we use action bar
        if (!mIsLandscape) {
            mQueueButton = (ImageButton) findViewById(R.id.curplaylist);
            mQueueButton.setOnClickListener(mQueueListener);
            mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
            mShuffleButton.setOnClickListener(mShuffleListener);
            mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
            mRepeatButton.setOnClickListener(mRepeatListener);
        }

        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
    /**
     * 滑动歌词的时候设置的回调函数用户通知远程服务播放进度
     */
    private OnSeekToListener onSeekToListener = new OnSeekToListener() {

        @Override
        public void onSeekTo(int progress) {
            try {
                if (mService != null)
                    mService.seek(progress);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 给歌词控件设置歌词Rows
     */
    private void setLrcRows() {
        setSongPath();
        setLrcPath();
        if (!LastLrcPath.equals(lrcPath)) {
            if (null != mLrcTask) {
                if (!mLrcTask.isCancelled()) {
                    mLrcTask.cancel(true);
                    mLrcTask = null;
                }
            }
            mLrcTask = new LrcTask();
            mLrcTask.execute();
            LastLrcPath = lrcPath;
        }
    }
    
    /**加载歌词的时候用到的异步类*/
    private class LrcTask extends AsyncTask<Void, Void, List<LrcRow>> {
           
        private int mCurRow = -1;

        @Override
        protected void onPreExecute() {
            mLrcView.setExcuteState(true);
            mLrcView.setLrcRows(null);
            isLoadLrcFinished = false;
        }

        @Override
        protected List<LrcRow> doInBackground(Void... params) {
 
            //[BIRD][BIRD_KUSAI_KCALIDESKCLOCK][bug-12472]chengci 20170703 begin
            /*long progress = 0l;
            try {
                progress = mService.position();
            } catch (RemoteException e) {
                e.printStackTrace();
            }*/
            //[BIRD][BIRD_KUSAI_KCALIDESKCLOCK][bug-12472]chengci 20170703 end
            return getLrcRows();
        }

        @Override
        protected void onPostExecute(List<LrcRow> result) {
            mLrcView.setLrcRows(result);
            //mLrcView.setCurRow(mCurRow);
            mLrcView.setExcuteState(false);
            MusicLogUtils.d("wyy_0313", "1");
            isLoadLrcFinished = true;
            this.cancel(true);
        }
    }

    /**
     * 通过远程服务获取歌曲路径
     */
    private void setSongPath() {
        if (mService != null) {
            try {
                songPath = mService.getTrackFilePathName();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置歌词路径
     */
    private void setLrcPath() {
        if (null == songPath || "".equals(songPath))
            return;
        Scanner scn = new Scanner(songPath);
        String tmp = scn.findInLine(Pattern.compile(".*/.*\\."));
        if (null != tmp) {
            lrcPath = new StringBuilder(tmp).append("lrc").toString();
        }
    }
    
    /**
     * 获取歌词信息
     * 
     * @return
     */
    private List<LrcRow> getLrcRows() {
        File file = new File(lrcPath);
        if (!file.exists()) {
             return null;
        }
        List<LrcRow> rows = null;
    
        FileInputStream lrcFileStream = null;
        BufferedReader br = null;
        try {
            lrcFileStream = new FileInputStream(file);
            if(zhGetCodeType(lrcFileStream)==4) {
                close(lrcFileStream);
                lrcFileStream = null;
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            } else {
                close(lrcFileStream);
                lrcFileStream = null;
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            }

            String line = null;
            StringBuffer sb = new StringBuffer();

            while ((line = br.readLine()) != null) {
                if (line.length() < 1) {
                    continue; // the empty line is omitted
                } else {
                    sb.append(line + "\n");
                }
                
            }
            rows = DefaultLrcParser.getIstance().getLrcRows(sb.toString());

        } catch (IOException e) {
            MusicLogUtils.d("wyy_0313", "lyric exception");
            return null;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return rows;
	}

    /**
     *解决歌词乱码问题 begin
     */
    private byte[] buffer;
    private int bufferedSize = 0;

    public static final int ISO_8859_1 = 0;

    public static final int UNICODE_LE = 1;

    public static final int UNICODE_BE = 2;

    public static final int UTF8 = 3;

    public static final int GBK = 4;

    public int zhGetCodeType(FileInputStream is) {
        //this.is = is;
        buffer = new byte[0xfff];
        try {
            bufferedSize = is.read(buffer);
        } catch (Exception e) {

        }
        return getCodeType(buffer);
    }

    private int getCodeType(byte[] head) {
        if (head[0] == (byte) 0xff && head[1] == (byte) 0xfe) {
            return UNICODE_LE;
        } else if (head[0] == (byte) 0xfe && head[1] == (byte) 0xff) {
            return UNICODE_BE;
        } else if (head[0] == (byte) 0xef && head[1] == (byte) 0xbb && head[2] == (byte) 0xbf) {
            return UTF8;
        } else {
            if (isUTF8(head)) {
                return UTF8;
            } else {
                return GBK;
            }
        }
    }

    boolean isUTF8(byte[] str) {
        int nBytes = 0;
        int chr;
        boolean bAllAscii = true;
        int i = 0;
        for (; i < bufferedSize; ++i) {
            chr = str[i] & 0xFF;
            if ((chr & 0x80) != 0)
                bAllAscii = false;
            if (nBytes == 0) {
                if (chr >= 0x80) {
                    if (chr >= 0xFC && chr <= 0xFD)
                        nBytes = 6;
                    else if (chr >= 0xF8)
                        nBytes = 5;
                    else if (chr >= 0xF0)
                        nBytes = 4;
                    else if (chr >= 0xE0)
                        nBytes = 3;
                    else if (chr >= 0xC0)
                        nBytes = 2;
                    else {
                        return false;
                    }
                    --nBytes;
                }
            } else {
                if ((chr & 0xC0) != 0x80) {
                    return false;
                }
                --nBytes;
            }
        }

        if (nBytes > 0 && i < bufferedSize) {
            return false;
        }

        if (bAllAscii) {
            return false;
        }
        return true;
    }

    public void close(FileInputStream is) throws IOException {
        if (is != null) {
            is.close();
            is = null;
            buffer = null;
        }
    }
    /**
     *解决歌词乱码问题 end
     */
    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/ 


    /**
     *  M: save the activity is in background.
     */
    @Override
    protected void onPause() {
        /*[bug-106689]xujing 20150415 begin*/
        /*[BIRD][BIRD_WEIMI_SYSTEMUI]huangzhangbin 20150313 begin
        if (FeatureOption.BIRD_WEIMI_SYSTEMUI) {
            Intent intent = new Intent("com.weimi.bird.music");
            intent.putExtra("ONMUSIC", false);
            sendBroadcast(intent);
        }
        /*[BIRD][BIRD_WEIMI_SYSTEMUI]huangzhangbin 20150313 end*/
        /*[bug-106689]xujing 20150415 end*/
        /// M: aviod Navigation button respond JE if Activity is background
        mIsInBackgroud = true;
        
        /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 begin
        if (FeatureOption.BIRD_DIRECT_WITH_PROXIMITY) {
            if (FeatureOption.BIRD_GESTURE_SENSOR_SUPPORT == true) {// 手势
                // do nothing
            } else {
                if (mPlayMusicProxSensor != null) {
                    mPlayMusicProxSensor.stop();
                    mPlayMusicProxSensor = null;
                }
            }
        }
        /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 end*/
        
        /// M: Before invalidateOptionsMenu,save the input of SearchView @{
        if (mSearchItem != null) {
            SearchView searchView = (SearchView) mSearchItem.getActionView();
            mQueryText = searchView.getQuery();
            MusicLogUtils.d(TAG, "searchText:" + mQueryText);
        }
        /// @}
        super.onPause();
    }

    /**
     *  M: handle config change.
     *
     * @param newConfig The new device configuration.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mIsConfigurationChanged = true;
        /// M: When configuration change, get the current orientation
        mIsLandscape = (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
        /// M: when configuration changed ,set mIsShowAlbumArt = true to update album art
        mIsShowAlbumArt = true;
        updateUI();
        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
        setRepeatButtonImage();
        setPauseButtonImage();
        setShuffleButtonImage();
        /// M: When back to this activity, ask service for right position
        mPosOverride = -1;
        /// M: Before invalidateOptionsMenu,save the input of SearchView @{
        if (mSearchItem != null) {
            SearchView searchView = (SearchView) mSearchItem.getActionView();
            mQueryText = searchView.getQuery();
            MusicLogUtils.d(TAG, "searchText:" + mQueryText);
        }
        /// @}
        /// M: Refresh action bar menu item
        invalidateOptionsMenu();
    }

    /**
     * M: Search view query text listener.
     */
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(MediaPlaybackActivity.this, QueryBrowserActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };
    SearchView.OnSuggestionListener mOnSuggestionListener = new SearchView.OnSuggestionListener() {
        public boolean onSuggestionClick(int position) {
            MusicLogUtils.d(TAG, "onSuggestionClick()");
            mSearchItem.collapseActionView();
            return false;
        }

        public boolean onSuggestionSelect(int position) {
            MusicLogUtils.d(TAG, "onSuggestionSelect()");
            return false;
        }
    };

    /**
     * M: get the background color when touched, it may get from thememager.
     *
     * @return Return background color
     */
    private int getBackgroundColor() {
        /// M: default background color for ICS.
        final int defaultBackgroundColor = 0xcc0099cc;
        /// M: For ICS style and support for theme manager {@
        int ret = defaultBackgroundColor;
        /*if (MusicFeatureOption.IS_SUPPORT_THEMEMANAGER) {
            Resources res = getResources();
            ret = res.getThemeMainColor();
            if (ret == 0) {
                ret = defaultBackgroundColor;
            }
        }*/
        return ret;
    }

    /**
     * M: update duration for MP3/AMR/AWB/AAC/FLAC formats.
     *
     * @param position The current positon for error check.
     */
    private void updateDuration(long position) {
        final int soundToMs = 1000;
        try {
            if (mNeedUpdateDuration && mService.isPlaying()) {
                long newDuration = mService.duration();

                if (newDuration > 0L && newDuration != mDuration) {
                    mDuration = newDuration;
                    mNeedUpdateDuration = false;
                    /// M: Update UI with new duration.
                    mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / soundToMs));
                    MusicLogUtils.i(TAG, "new duration updated!!");
                }
            } else if (position < 0 || position >= mDuration) {
                mNeedUpdateDuration = false;
            }
        } catch (RemoteException ex) {
            MusicLogUtils.e(TAG, "Error:" + ex);
        }
    }

    /**
     * M: record duration update status when playing,
     * if play mp3/aac/amr/awb/flac file, set mNeedUpdateDuration to update
     * layter in updateDuration().
     */
    private void recordDurationUpdateStatus() {
        final String mimeTypeMpeg = "audio/mpeg";
        final String mimeTypeAmr = "audio/amr";
        final String mimeTypeAmrWb = "audio/amr-wb";
        final String mimeTypeAac = "audio/aac";
        final String mimeTypeFlac = "audio/flac";
        String mimeType;
        mNeedUpdateDuration = false;
        try {
            mimeType = mService.getMIMEType();
        } catch (RemoteException ex) {
            MusicLogUtils.e(TAG, "Error:" + ex);
            mimeType = null;
        }
        if (mimeType != null) {
            MusicLogUtils.i(TAG, "mimeType=" + mimeType);
            if (mimeType.equals(mimeTypeMpeg)
                || mimeType.equals(mimeTypeAmr)
                || mimeType.equals(mimeTypeAmrWb)
                || mimeType.equals(mimeTypeAac)
                || mimeType.equals(mimeTypeFlac)) {
                mNeedUpdateDuration = true;
            }
        }
    }

    /**
     * M: Add NFC callback to provide the uri.
     */
    @Override
    public Uri[] createBeamUris(NfcEvent event) {
        Uri currentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicUtils.getCurrentAudioId());;
        MusicLogUtils.i(TAG, "NFC call for uri " + currentUri);
        return new Uri[] {currentUri};
    }

    /**
     * M: Call when search request and expand search action view.
     */
    @Override
    public boolean onSearchRequested() {
        if (mSearchItem != null) {
            mSearchItem.expandActionView();
        }
        return true;
    }

    /**M: Added for HotKnot feature.@{**/
    class HotKnotHelper {

        private Context mPlaybackContext = null;
        private MenuItem mHotknotItem = null;
        private String mHotKnotUri = null;
        private final static String STRING_ACTION = "com.mediatek.hotknot.action.SHARE";
        private final static String STRING_EXTRA = "com.mediatek.hotknot.extra.SHARE_URIS";
        private boolean mSendable = true;
        private boolean mIsDrmSd = false;

        public HotKnotHelper(Context context) {
            if (checkHotKnotEnabled()) {
                mPlaybackContext = context;

                OnClickListener onClick = new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DialogInterface.BUTTON_POSITIVE == which) {
                            MusicLogUtils.d(TAG, "hotKnot start setting");
                            Intent intent = new Intent("mediatek.settings.HOTKNOT_SETTINGS");
                            mPlaybackContext.startActivity(intent);
                            dialog.cancel();
                        } else {
                            MusicLogUtils.d(TAG, "onClick cancel dialog");
                            dialog.cancel();
                        }
                    }
                };
            }
        }

        /**
         * Creates the HitKnot share menu item
         *
         * @param menu The menu that HitKnot share menu item add to
         */
        public void createHotKnotMenu(Menu menu) {
            if (!checkHotKnotEnabled()) {
                return;
            }
            mHotknotItem = menu.add(0, HOTKNOT, 0, R.string.hotknot).setIcon(R.drawable.ic_hotknot);
            //mHotknotItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS); /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 add*/
        }

        /**
         * M: Updates the HotKnot share menu item
         *
         * @param enable Whether to enable the HotKnot share menu item
         */
        public void updateHotKnotMenu(boolean enable) {
            if (mHotknotItem == null) {
                return;
            }
            if (enable) {
                mHotknotItem.setIcon(R.drawable.ic_hotknot);
            } else {
                mHotknotItem.setIcon(R.drawable.ic_hotknot_disable);
            }
            mHotknotItem.setEnabled(enable);
            //MediaPlaybackActivity.this.invalidateOptionsMenu();
        }

        /**
         * M: Shares the audio with HotKnot
         */
        public void shareViaHotKnot() {
            if (!checkHotKnotEnabled() && mHotKnotUri != null) {
                return;
            }
            Uri[] uris = null;
            if (mIsDrmSd) {
                uris = new Uri[] {
                    Uri.parse(mHotKnotUri + "?isMimeType=no")
                };
            } else {
                uris = new Uri[] {
                    Uri.parse(mHotKnotUri)
                };
            }
            Intent intent = new Intent();
            intent.setAction(STRING_ACTION);
            intent.putExtra(STRING_EXTRA, uris);
            MediaPlaybackActivity.this.startActivity(intent);
        }

        private boolean checkHotKnotEnabled() {
            return MusicFeatureOption.IS_HOTKNOT_SUPPORTED;
        }

        /**
         * M: Set the uri in string for HotKnot sharing.
         *
         * @param path the uri in string for HotKnot sharing
         */
        public void setHotKnotUri(String path) {
            if (mSearchItem != null) {
                SearchView searchView = (SearchView) mSearchItem.getActionView();
                mQueryText = searchView.getQuery();
                MusicLogUtils.d(TAG, "setHotKnotUri,searchText:" + mQueryText);
            }
            if (!mHotKnotHelper.checkHotKnotEnabled() || path == null || path.startsWith("file")) {
                mHotKnotUri = null;
                MediaPlaybackActivity.this.invalidateOptionsMenu();
                return;
            }
            mHotKnotUri = path;
            Uri curUri = Uri.parse(mHotKnotUri);
            Cursor cursor = null;
            mIsDrmSd = false;
            mSendable = true;
/*
            try {
                cursor = getContentResolver().query(curUri, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        
                        if (MusicFeatureOption.IS_SUPPORT_DRM) {
                            int isDrm = cursor.getInt(cursor
                                    .getColumnIndexOrThrow(MediaStore.Audio.Media.IS_DRM));
                            int drmMethod = cursor.getInt(cursor
                                    .getColumnIndexOrThrow(MediaStore.Audio.Media.DRM_METHOD));
                            MusicLogUtils.v(TAG, "setHotKnotUri(),isDrm=" + isDrm + ", drmMethod="
                                    + drmMethod);
                            if (isDrm == 1) {
                                switch (drmMethod) {
                                    case OmaDrmStore.DrmMethod.METHOD_FL:
                                    case OmaDrmStore.DrmMethod.METHOD_CD:
                                    case OmaDrmStore.DrmMethod.METHOD_FLDCF:
                                        mSendable = false;
                                        break;
                                    case OmaDrmStore.DrmMethod.METHOD_SD:
                                        mIsDrmSd = true;
                                        mSendable = true;
                                        break;
                                    case OmaDrmStore.DrmMethod.METHOD_NONE:
                                        mSendable = true;
                                        break;
                                }
                            }
                        }
                    }
                }
            } catch (IllegalStateException e) {
                MusicLogUtils.d(TAG, "setHotKnotUri()-IllegalStateException");
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }*/
            MusicLogUtils.v(TAG, "setHotKnotUri(),mSendable=" + mSendable);
            MediaPlaybackActivity.this.invalidateOptionsMenu();
        }
    }
    /**@}**/
    /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 begin*/
    private static boolean isCover = false;
    private long firstTimestamp = 0;// 第1个状态发生的时间
    private long secTimestamp = 0; // 第2个状态发生的时间

    PlayMusicProxSensor mPlayMusicProxSensor;

    class PlayMusicProxSensor implements SensorEventListener {

        private final SensorManager mSensorManager;
        private Sensor mSensor;
        private float mMaxRange;
        private PowerManager mPowerManager;

        PlayMusicProxSensor() {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }

        void start() {
            firstTimestamp = 0;
            secTimestamp = 0;
            if (mSensorManager != null && mSensor == null) {
                if ((mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)) != null) {
                    mMaxRange = mSensor.getMaximumRange();
                    mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
                }
            }
        }

        void stop() {
            if (mSensorManager != null && mSensor != null) {
                mSensorManager.unregisterListener(this);
                mSensor = null;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            try {
                if (event.values != null && event.values.length > 0) {
                    firstTimestamp = secTimestamp;
                    secTimestamp = event.timestamp;
                    if (event.values[0] < this.mMaxRange) {
                        isCover = true;
                    } else if ((secTimestamp - firstTimestamp) >= 30000000l && (secTimestamp - firstTimestamp) <= 1000000000) {
                        if (isCover) {
                            if (mPowerManager != null) {
                                mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                            }
                            // do something here
                            playNextSong();
                            isCover = false;
                        }
                    } else {
                        isCover = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private boolean getProximitySwitchEnable() {
/*
        if (FeatureOption.BIRD_DIRECT_WITH_PROXIMITY == true) {
            if ((Settings.System.getInt(getContentResolver(), Settings.System.BIRD_DIRECT_KEY_NEXT_SONG, 0) == 1)
                    && (Settings.System.getInt(getContentResolver(), Settings.System.BIRD_DIRECT_TURN_ON, 0) == 1)) {
                return true;
            } else {
                return false;
            }
        }*/
        return false;
    }

    final String NEXT_ACTION = "com.android.music.musicservicecommand.next";

    private void playNextSong() {
        Intent intent = new Intent(NEXT_ACTION);
        sendBroadcast(intent);
    }
    /*[BIRD_DIRECT_WITH_PROXIMITY] wurongfu 20150326 end*/

    /*[bug-null][gaowei][20170704]begin*/
    private View.OnClickListener mMenuClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            /// M: Navigation button press back,
            /// aviod Navigation button respond JE if Activity is background
            MusicLogUtils.d("gaowei0703", "mMenuClickListener");
            showPopMenu();
        }
    };

    private PopMenu mPopMenu;
    private ImageView mMoreVert;
    private MaskImage mAlbumAdjust;
    private static final int MENU_PARTY_SHUFFLE = 400;
    private static final int MENU_ADD_TO_PLAYLIST = 401;
    private static final int MENU_RINGTONE_MENU_SHORT = 402;
    private static final int MENU_DELETE_ITEM = 403;
    private static final int MENU_EFFECTSPANEL = 404;
    private static final int MENU_GOTO_START = 405;
    private static int POP_MENU_LOCALTION = 100;
    private void showPopMenu() {
        if (mPopMenu == null) {
            POP_MENU_LOCALTION = getResources().getDimensionPixelSize(R.dimen.pop_menu_location); 
            mPopMenu = new PopMenu(this, PopMenu.DisplayMode.ACTIONSHEET_TOP_WINDOW_SPECIAL, POP_MENU_LOCALTION, true);
            //mPopMenu.addItem(POP_MENU_MUTE, getString(R.string.sound_off_string));
            mPopMenu.addItem(MENU_PARTY_SHUFFLE, getString(R.string.party_shuffle));
            mPopMenu.addItem(MENU_ADD_TO_PLAYLIST, getString(R.string.add_to_playlist));
            mPopMenu.addItem(MENU_RINGTONE_MENU_SHORT, getString(R.string.ringtone_menu_short));
            mPopMenu.addItem(MENU_DELETE_ITEM, getString(R.string.delete_item));
            mPopMenu.addItem(MENU_EFFECTSPANEL, getString(R.string.effectspanel));
            mPopMenu.addItem(MENU_GOTO_START, getString(R.string.goto_start));
            mPopMenu.setDialogListClickListener(mActionBarDialogListener);
        }
        mPopMenu.showDialog();
    }

    private PopMenu.DialogListClickListener mActionBarDialogListener = new PopMenu.DialogListClickListener() {
        @Override
        public void onClick(int which) {
            Intent intent;
            if (mPopMenu != null) {
                mPopMenu.dismiss();
                mPopMenu = null;
            }
            switch (which) {
                case MENU_PARTY_SHUFFLE:
                    MusicUtils.togglePartyShuffle();
                    setShuffleButtonImage();
                    /// M: Update repeat button because will set repeat current to repeat all when open party shuffle.
                    setRepeatButtonImage();
                    break;
                case MENU_ADD_TO_PLAYLIST:
                    intent = new Intent();
                    intent.setClass(MediaPlaybackActivity.this, CreatePlaylist.class);
                    /// M: Add to indicate the save_as_playlist and new_playlist
                    intent.putExtra(MusicUtils.SAVE_PLAYLIST_FLAG, MusicUtils.NEW_PLAYLIST);
                    startActivityForResult(intent, NEW_PLAYLIST);
                    break;
                case MENU_RINGTONE_MENU_SHORT:
                    // Set the system setting to make this the current ringtone
                    try {
                        if (mService != null) {
                            MusicUtils.setRingtone(MediaPlaybackActivity.this, mService.getAudioId());
                        }
                    } catch (RemoteException e) {
                        MusicLogUtils.e(TAG, "onPrepareOptionsMenu with RemoteException " + e);
                    }
                    break;
                case MENU_DELETE_ITEM:
                    try {
                        if (mService != null) {
                            long [] list = new long[1];
                            list[0] = MusicUtils.getCurrentAudioId();
                            Bundle b = new Bundle();
                            String f;
                            b.putInt(MusicUtils.DELETE_DESC_STRING_ID, R.string.delete_song_desc);
                            b.putString(MusicUtils.DELETE_DESC_TRACK_INFO, mService.getTrackName());
                            /// @}
                            b.putLongArray("items", list);
                            intent = new Intent();
                            intent.setClass(MediaPlaybackActivity.this, DeleteItems.class);
                            intent.putExtras(b);
                            startActivityForResult(intent, -1);
                        }
                    } catch (RemoteException e) {
                        MusicLogUtils.e(TAG, "onPrepareOptionsMenu with RemoteException " + e);
                    }
                    break;
                case MENU_EFFECTSPANEL:
                    MusicUtils.startEffectPanel(MediaPlaybackActivity.this);
                    break;
                case MENU_GOTO_START:
                    intent = new Intent();
                    intent.setClass(MediaPlaybackActivity.this, MusicBrowserActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    break;
                default:
                    break;
            }
        }
    };

    private Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        final RenderScript rs = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(rs, sentBitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs,
                Element.U8_4(rs));
        script.setRadius(radius /* e.g. 3.f */);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);
        return bitmap;
    }
    /*[bug-null][gaowei][20170704]end*/
}
