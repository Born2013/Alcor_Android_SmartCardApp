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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LocalActivityManager;
import android.app.SearchManager;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;

import com.mediatek.music.ext.Extensions;
import com.mediatek.music.ext.IMusicTrackBrowser;
import com.mediatek.music.ext.PluginUtils;

/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Button;
import android.view.LayoutInflater;
import android.view.Gravity;
import com.bird.WeimiSearchView;
import android.widget.EditText;
import java.util.Timer;
import java.util.TimerTask;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
//import com.birdroid.common.FeatureOption;
/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/

public class MusicBrowserActivity extends TabActivity implements MusicUtils.Defs, ServiceConnection, OnTabChangeListener,
        ViewPager.OnPageChangeListener {

    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
    private ImageView mNowTypeIcon;
    private TextView mNowTypeTitle;
    private ImageView mArtistTab;
    private ImageView mAlbumTab;
    private ImageView mSongTab;
    private ImageView mPlaylistTab; 
    private ImageView mSearchTab;     
    private View blankView;
    private View nowPlayingView;
    private ImageView mSearchButtonTitle;
    private boolean searchview_display = false;
    private PopupWindow searchWindow;
    private TextView mCanelView,mOkView;
    private WeimiSearchView mSearchView;
    private ImageView mAlbumCover ;
    private SeekBar mSeekBar;
    private TextView mArtistName;
    private TextView mTitleName;
    private ImageView mCtrPlay;
    private ImageView mCtrPre; 
    private ImageView mCtrNext;
    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/            

    private static final String TAG = "MusicBrowser";
    private static final String ACTIVITY_NAME = PluginUtils.MUSIC_BROWSER_ACTIVITY;

    private static final String ARTIST = "Artist";
    private static final String ALBUM = "Album";
    private static final String SONG = "Song";
    private static final String PLAYLIST = "Playlist";
    private static final String PLAYBACK = "Playback";
    private static final String SAVE_TAB = "activetab";
    static final int ARTIST_INDEX = 0;
    static final int ALBUM_INDEX = 1;
    static final int SONG_INDEX = 2;
    static final int PLAYLIST_INDEX = 3;
    static final int PLAYBACK_INDEX = 4;
    static final int VIEW_PAGER_OFFSCREEN_PAGE_NUM = 3;
    private static final int PLAY_ALL = CHILD_MENU_BASE + 3;

    private static final HashMap<String, Integer> TAB_MAP = new HashMap<String, Integer>(PLAYBACK_INDEX + 1);
    private LocalActivityManager mActivityManager;
    private ViewPager mViewPager;
    private TabHost mTabHost;
    private ArrayList<View> mPagers = new ArrayList<View>(PLAYBACK_INDEX);
    private int mTabCount;
    private int mCurrentTab;
    private MusicUtils.ServiceToken mToken;
    private IMediaPlaybackService mService = null;
    private int mOrientaiton;

    /// M: FakeMenu mFakeMenu;
    private View mOverflowMenuButton;
    private PopupMenu mPopupMenu = null;
    private boolean mPopupMenuShowing = false;
    private boolean mHasMenukey = true;
    private int mOverflowMenuButtonId;

    /// M: Whether sdcard is mounted
    private boolean mIsSdcardMounted = true;
    /// M; Indicate whether the searchview is showing
    private boolean mSearchViewShowing = false;
    /// M: Add search button in actionbar when nowplaying not exist
    MenuItem mSearchItem;
    ImageButton mSearchButton;
    IMusicTrackBrowser mMusicPlugin;
    /// M: Initial tab map hashmap
    static {
        TAB_MAP.put(ARTIST, ARTIST_INDEX);
        TAB_MAP.put(ALBUM, ALBUM_INDEX);
        TAB_MAP.put(SONG, SONG_INDEX);
        TAB_MAP.put(PLAYLIST, PLAYLIST_INDEX);
        TAB_MAP.put(PLAYBACK, PLAYBACK_INDEX);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        PDebug.Start("MusicBrowserActivity.onCreate");
        super.onCreate(savedInstanceState);
        MusicLogUtils.d(TAG, "onCreate");
        ActionBar actionBar = getActionBar();
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        if (actionBar != null) {
             actionBar.hide();
             Resources r = getResources();
             Drawable myDrawable = r.getDrawable(R.drawable.bird_actionbar_bg);
             actionBar.setBackgroundDrawable(myDrawable);
        }
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        PDebug.Start("MusicBrowserActivity.bindToService()");
        mToken = MusicUtils.bindToService(this, this);
        PDebug.End("MusicBrowserActivity.bindToService()");

        mMusicPlugin = Extensions.getPluginObject(getApplicationContext());

        mHasMenukey = ViewConfiguration.get(this).hasPermanentMenuKey();
        PDebug.Start("MusicBrowserActivity.dispatchCreate()");
        mActivityManager = new LocalActivityManager(this, false);
        mActivityManager.dispatchCreate(savedInstanceState);
        PDebug.End("MusicBrowserActivity.dispatchCreate()");

        mTabHost = getTabHost();
        PDebug.Start("MusicBrowserActivity.initTab()");
        initTab();
        PDebug.End("MusicBrowserActivity.initTab()");

        PDebug.Start("MusicBrowserActivity.setCurrentTab()");
        /*[bug-106989][BIRD_WEIMI_MUSIC]wangyueyue 20150425 begin*/
        //mCurrentTab = MusicUtils.getIntPref(this, SAVE_TAB, ARTIST_INDEX);
        mCurrentTab = SONG_INDEX;
        /*[bug-106989][BIRD_WEIMI_MUSIC]wangyueyue 20150425 end*/
        MusicLogUtils.d(TAG, "onCreate mCurrentTab: " + mCurrentTab);
        if ((mCurrentTab < 0) || (mCurrentTab >= mTabCount)) {
            mCurrentTab = ARTIST_INDEX;
        }
        /// M: reset the defalt tab value
        if (mCurrentTab == ARTIST_INDEX) {
            mTabHost.setCurrentTab(ALBUM_INDEX);
        }
        mTabHost.setOnTabChangedListener(this);
        PDebug.End("MusicBrowserActivity.setCurrentTab()");

        PDebug.Start("MusicBrowserActivity.initPager()");
        initPager();
        PDebug.End("MusicBrowserActivity.initPager()");

        PDebug.Start("MusicBrowserActivity.setAdapter()");
        mViewPager = (ViewPager) findViewById(R.id.viewpage);
        mViewPager.setAdapter(new MusicPagerAdapter());
        mViewPager.setOnPageChangeListener(this);
        //mViewPager.setOffscreenPageLimit(VIEW_PAGER_OFFSCREEN_PAGE_NUM);
        PDebug.End("MusicBrowserActivity.setAdapter()");

        IntentFilter f = new IntentFilter();
        f.addAction(MusicUtils.SDCARD_STATUS_UPDATE);
        registerReceiver(mSdcardstatustListener, f);
        createFakeMenu();
        /// M: Init search button click listener in nowplaying.
        //initSearchButton();/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 modify*/
        PDebug.End("MusicBrowserActivity.onCreate");
    }

    @Override
    public void onResume() {
        PDebug.Start("MusicBrowserActivity.onResume");
        /*[bug-106689]xujing 20150415 begin*/
        //if (FeatureOption.BIRD_WEIMI_SYSTEMUI) {
        //    mChangeStatusIconToWhite = true;
        //}
        /*[bug-106689]xujing 20150415 end*/
        super.onResume();
        MusicLogUtils.d(TAG, "onResume>>>");

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        /*[使用耳机线控音，状态不统一]huangzhangbin 20151207 begin*/
        f.addAction("bird.music.uptatestatic.broadcast");
        /*[使用耳机线控音，状态不统一]huangzhangbin 20151207 end*/
        registerReceiver(mTrackListListener, f);
        PDebug.Start("MusicBrowserActivity.setCurrentTab()");
        mTabHost.setCurrentTab(mCurrentTab);
        PDebug.End("MusicBrowserActivity.setCurrentTab()");
        
        PDebug.Start("MusicBrowserActivity.dispatchResume()");
        mActivityManager.dispatchResume();
        PDebug.End("MusicBrowserActivity.dispatchResume()");
        MusicLogUtils.d(TAG, "onResume<<<");
        PDebug.End("MusicBrowserActivity.onResume");
    }

    @Override
    public void onPause() {
        MusicLogUtils.d(TAG, "onPause");
        /*[bug-106689]xujing 20150415 begin*/
        /*[BIRD][BIRD_WEIMI_SYSTEMUI]huangzhangbin 20150313 begin
        if (FeatureOption.BIRD_WEIMI_SYSTEMUI) {
            Intent intent = new Intent("com.weimi.bird.music");
            intent.putExtra("ONMUSIC", false);
            sendBroadcast(intent);
        }
        /*[BIRD][BIRD_WEIMI_SYSTEMUI]huangzhangbin 20150313 end*/
        /*[bug-106689]xujing 20150415 end*/
        unregisterReceiver(mTrackListListener);
        mActivityManager.dispatchPause(false);
        MusicUtils.setIntPref(this, SAVE_TAB, mCurrentTab);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenuShowing = false;
        }
        mActivityManager.dispatchStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.d(TAG, "onDestroy");
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mService = null;
        }
        unregisterReceiver(mSdcardstatustListener);
        mActivityManager.dispatchDestroy(false);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /// M: Get the start activity tab index from intent which set by start activity, so that we can return
        /// result to right activity.
        int startActivityTab = mCurrentTab;
        if (data != null) {
            startActivityTab = data.getIntExtra(MusicUtils.START_ACTIVITY_TAB_ID, mCurrentTab);
        }
        MusicLogUtils.d(TAG, "onActivityResult: startActivityTab = " + startActivityTab);
        Activity startActivity = mActivityManager.getActivity(getStringId(startActivityTab));
        if (startActivity == null) {
            return;
        }
        switch (startActivityTab) {
            case ARTIST_INDEX:
                ((ArtistAlbumBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            case ALBUM_INDEX:
                ((AlbumBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            case SONG_INDEX:
                ((TrackBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            case PLAYLIST_INDEX:
                ((PlaylistBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            default:
                MusicLogUtils.d(TAG, "default");
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MusicLogUtils.d(TAG, "onConfigurationChanged>>");
        TabWidget tabWidgetTemp = mTabHost.getTabWidget();
        View tabView;
        Activity activity;
        int viewStatusForTab = View.GONE;

        mOrientaiton = newConfig.orientation;
        if (mOrientaiton == Configuration.ORIENTATION_LANDSCAPE) {
            MusicLogUtils.d(TAG, "onConfigurationChanged--LandScape");
            viewStatusForTab = View.VISIBLE;
        }
        /// M: load tab which is alive only for Landscape;
        for (int i = PLAYBACK_INDEX; i < mTabCount; i++) {
            tabView = tabWidgetTemp.getChildTabViewAt(i);
            if (tabView != null) {
                tabView.setVisibility(viewStatusForTab);
            }
        }
        /// M: notify sub Activity for configuration changed;
        for (int i = 0; i < PLAYBACK_INDEX; i++) {
            activity = mActivityManager.getActivity(getStringId(i));
            if (activity != null) {
                activity.onConfigurationChanged(newConfig);
            }
        }

        if (!mHasMenukey) {
            boolean popupMenuShowing = mPopupMenuShowing;
            if (popupMenuShowing && mPopupMenu != null) {
                mPopupMenu.dismiss();
                MusicLogUtils.d(TAG, "changeFakeMenu:mPopupMenu.dismiss()");
            }
            MusicLogUtils.d(TAG, "changeFakeMenu:popupMenuShowing=" + popupMenuShowing);
            createFakeMenu();
            if (!mSearchViewShowing) {
                mOverflowMenuButton.setEnabled(true);
            }
            if (popupMenuShowing && mOverflowMenuButton != null) {
                /**M: Disable the sound effect while the configuration changed.@**/
                mOverflowMenuButton.setSoundEffectsEnabled(false);
                mOverflowMenuButton.performClick();
                mOverflowMenuButton.setSoundEffectsEnabled(true);
                /**@}**/
                MusicLogUtils.d(TAG, "changeFakeMenu:performClick()");
            }
        }
        if (mService != null) {
            MusicLogUtils.d(TAG, "mSearchViewShowing:" + mSearchViewShowing);
            if (mSearchViewShowing) {
                mSearchButton.setVisibility(View.GONE);
            } else {
                mSearchButton.setVisibility(View.VISIBLE);
            }
            MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
            updatePlaybackTab();
        }

        /**M: Added for forcing init last visible view.@{**/
        MusicLogUtils.d(TAG, "onConfigurationChanged--mCurrentTab = " + mCurrentTab);
        mTabHost.setCurrentTab(mCurrentTab);
        mViewPager.setAdapter(new MusicPagerAdapter());
        onTabChanged(getStringId(mCurrentTab));
        /**@}**/
        MusicLogUtils.d(TAG, "onConfigurationChanged<<");
    }

    /**
     * M: Create fake menu.
     */
    private void createFakeMenu() {
        if (mHasMenukey) {
            MusicLogUtils.d(TAG, "createFakeMenu Quit when there has Menu Key");
            return;
        }
        if (mOrientaiton == Configuration.ORIENTATION_LANDSCAPE) {
            mOverflowMenuButtonId = R.id.overflow_menu;
            mOverflowMenuButton = findViewById(R.id.overflow_menu);
        } else {
            mOverflowMenuButtonId = R.id.overflow_menu_nowplaying;
            mOverflowMenuButton = findViewById(R.id.overflow_menu_nowplaying);
            View parent = (View) mOverflowMenuButton.getParent();
            if (parent != null) {
                parent.setVisibility(View.VISIBLE);
            }
        }
        mOverflowMenuButton.setVisibility(View.VISIBLE);
        mOverflowMenuButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MusicLogUtils.d(TAG, "createFakeMenu:onClick()");
                if (v.getId() == mOverflowMenuButtonId) {
                    final PopupMenu popupMenu = new PopupMenu(MusicBrowserActivity.this, mOverflowMenuButton);
                    mPopupMenu = popupMenu;
                    final Menu menu = popupMenu.getMenu();
                    onCreateOptionsMenu(menu);
                    popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            return onOptionsItemSelected(item);
                        }
                    });
                    popupMenu.setOnDismissListener(new OnDismissListener() {
                        public void onDismiss(PopupMenu menu) {
                            mPopupMenuShowing = false;
                            MusicLogUtils.d(TAG, "createFakeMenu:onDismiss() called");
                            return;
                        }
                    });
                    onPrepareOptionsMenu(menu);
                    mPopupMenuShowing = true;
                    if (popupMenu != null) {
                        MusicLogUtils.d(TAG, "createFakeMenu:popupMenu.show()");
                        popupMenu.show();
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, PLAY_ALL, 0, R.string.play_all);
        menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all);
        menu.add(0, EFFECTS_PANEL, 0, R.string.effects_list_title);
        /// M: Add search view
        //mSearchItem = MusicUtils.addSearchView(this, menu, mQueryTextListener, null);/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 add*/
        /// M:create menu ADD_FOLDER_TO_PLAY,ADD_FOLDER_AS_PLAYLIST,ADD_SONG_TO_PLAY when plugin need
        Bundle options = new Bundle();
        options.putInt(PluginUtils.TAB_INDEX, mCurrentTab);
        mMusicPlugin.onCreateOptionsMenuForPlugin(menu, ACTIVITY_NAME, options);
        return true;
    }

    /**
     * M: When edit Text ,do query follow the message of the query
     */
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(MusicBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
        super.onPrepareOptionsMenu(menu);
        if (!mIsSdcardMounted) {
            MusicLogUtils.w(TAG, "Sdcard is not mounted, don't show option menu!");
            return false;
        }
        /// M: Only show play all in song activity.
        menu.findItem(PLAY_ALL).setVisible(mCurrentTab == SONG_INDEX);
        /// M: Show shuffle all in all activity except playlist activity.
        menu.findItem(SHUFFLE_ALL).setVisible(mCurrentTab != PLAYLIST_INDEX);
        /// M: Only show effect menu when effect class is enable.
        MusicUtils.setEffectPanelMenu(getApplicationContext(), menu);
        /// M: Search button can only show on one of place between nowplaying and action bar, when action bar exist,
        /// it should show on action bar, otherwise show on nowplaying, if nowplaying not exist(such as landscape in
        /// MusicBrowserActivity), show it in option menu.
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        if (mSearchItem != null ){
            mSearchItem.setVisible(mOrientaiton == Configuration.ORIENTATION_LANDSCAPE);
        }
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        /// M: show the specific extral option menu when plugin need
        Bundle options = new Bundle();
        options.putInt(PluginUtils.TAB_INDEX, mCurrentTab);
        mMusicPlugin.onPrepareOptionsMenuForPlugin(menu, ACTIVITY_NAME, options);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Cursor cursor;
        Intent intent;
        switch (item.getItemId()) {
            case PLAY_ALL:
                cursor = MusicUtils.query(this,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media._ID },
                        MediaStore.Audio.Media.IS_MUSIC + "=1",
                        null,
                        /// M: add for chinese sorting
                        MediaStore.Audio.Media.TITLE_PINYIN_KEY);
                if (cursor != null) {
                    MusicUtils.playAll(this, cursor);
                    cursor.close();
                }
                return true;

            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                return true;

            case SHUFFLE_ALL:
                cursor = MusicUtils.query(this,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media._ID },
                        MediaStore.Audio.Media.IS_MUSIC + "=1",
                        null,
                        /// M: add for chinese sorting
                        MediaStore.Audio.Media.TITLE_PINYIN_KEY);
                if (cursor != null) {
                    MusicUtils.shuffleAll(this, cursor);
                    cursor.close();
                }
                return true;

            case EFFECTS_PANEL:
                return MusicUtils.startEffectPanel(this);

            case R.id.search:
                onSearchRequested();
                mSearchViewShowing = true;
                return true;

            default:
                /// seleted the extral option menu item when plugin need
                Bundle options = new Bundle();
                options.putInt(PluginUtils.TAB_INDEX, mCurrentTab);
                mMusicPlugin.onOptionsItemSelectedForPlugin(getApplicationContext(), item, ACTIVITY_NAME, this, null, options);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * M: Implements receive track ListListener broadcast
     */
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicLogUtils.d(TAG, "mTrackListListener");
            if (mService != null) {
                MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
                updatePlaybackTab();
            }
        }
    };

    /**
     * M: Implements receive SDCard status broadcast
     */
    private BroadcastReceiver mSdcardstatustListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsSdcardMounted = intent.getBooleanExtra(MusicUtils.SDCARD_STATUS_ONOFF, false);

            View view;
            if (mIsSdcardMounted) {
                MusicLogUtils.d(TAG, "Sdcard normal");
                view = findViewById(R.id.normal_view);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                view = findViewById(R.id.sd_message);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                view = findViewById(R.id.sd_icon);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                view = findViewById(R.id.sd_error);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                /// M: update nowplaying when sdcard mounted
                if (mService != null) {
                    MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
                }
            } else {
                MusicLogUtils.d(TAG, "Sdcard error");
                view = findViewById(R.id.normal_view);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                view = findViewById(R.id.sd_icon);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                TextView testview = (TextView) findViewById(R.id.sd_message);
                if (testview != null) {
                    testview.setVisibility(View.VISIBLE);
                    int message = intent.getIntExtra(MusicUtils.SDCARD_STATUS_MESSAGE, R.string.sdcard_error_message);
                    testview.setText(message);
                }
                view = findViewById(R.id.sd_error);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    /**
     * get current tab id though index
     *
     * @param index
     * @return
     */
    private String getStringId(int index) {
        String tabStr = ARTIST;
        switch (index) {
            case ALBUM_INDEX:
                tabStr = ALBUM;
                break;
            case SONG_INDEX:
                tabStr = SONG;
                break;
            case PLAYLIST_INDEX:
                tabStr = PLAYLIST;
                break;
            case PLAYBACK_INDEX:
                tabStr = PLAYBACK;
                break;
            case ARTIST_INDEX:
            default:
                MusicLogUtils.d(TAG, "ARTIST_INDEX or default");
                break;
        }
        return tabStr;
    }

    /**
     * initial tab host
     */
    private void initTab() {
        MusicLogUtils.d(TAG, "initTab>>");
        final TabWidget tabWidget = (TabWidget) getLayoutInflater().inflate(R.layout.bird_buttonbar, null); // buttonbar
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        mNowTypeIcon = (ImageView)findViewById(R.id.now_type_icon);
        mNowTypeTitle = (TextView)findViewById(R.id.now_type_title);
        mArtistTab = (ImageView)tabWidget.findViewById(R.id.artisttab);
        mAlbumTab = (ImageView)tabWidget.findViewById(R.id.albumtab);
        mSongTab = (ImageView)tabWidget.findViewById(R.id.songtab);
        mPlaylistTab = (ImageView)tabWidget.findViewById(R.id.playlisttab);
        mSearchButtonTitle = (ImageView)findViewById(R.id.search_menu_nowplaying);
        MusicLogUtils.d("wyy_20153030", "mArtistTab :" + mArtistTab + ", mAlbumTab = " + mAlbumTab +  ", mPlaylistTab = " + mPlaylistTab + ", mSearchButtonTitle = " + mSearchButtonTitle + ", mNowTypeIcon = " + mNowTypeIcon);
        if (mSearchButtonTitle != null) {
            mSearchButtonTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSearchWindow();
                }
            });
        }        
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        mOrientaiton = getResources().getConfiguration().orientation;
        mTabCount = tabWidget.getChildCount();
        View tabView;
        /// M:remove fake menu
        if (mHasMenukey) {
            mTabCount--;
        }
        for (int i = 0; i < mTabCount; i++) {
            tabView = tabWidget.getChildAt(0);
            if (tabView != null) {
                tabWidget.removeView(tabView);
            }
            MusicLogUtils.d(TAG, "addTab:" + i);
            mTabHost.addTab(mTabHost.newTabSpec(getStringId(i)).setIndicator(tabView).setContent(android.R.id.tabcontent));
        }
        if (mOrientaiton == Configuration.ORIENTATION_PORTRAIT) {
            TabWidget tabWidgetTemp = mTabHost.getTabWidget();
            for (int i = PLAYBACK_INDEX; i < mTabCount; i++) {
                tabView = tabWidgetTemp.getChildTabViewAt(i);
                if (tabView != null) {
                    tabView.setVisibility(View.GONE);
                }
                MusicLogUtils.d(TAG, "set tab gone:" + i);
            }
        }
        MusicLogUtils.d(TAG, "initTab<<");
    }

    /**
     * get current view
     *
     * @param index
     * @return View
     */
    private View getView(int index) {
        MusicLogUtils.d(TAG, "getView>>>index = " + index);
        View view = null;
        Intent intent = new Intent(Intent.ACTION_PICK);
        switch (index) {
            case ARTIST_INDEX:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/artistalbum");
                break;
            case ALBUM_INDEX:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                break;
            case SONG_INDEX:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                break;
            case PLAYLIST_INDEX:
                intent.setDataAndType(Uri.EMPTY, MediaStore.Audio.Playlists.CONTENT_TYPE);
                break;
            default:
                MusicLogUtils.d(TAG, "default");
                return null;
        }
        intent.putExtra("withtabs", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        view = mActivityManager.startActivity(getStringId(index), intent).getDecorView();
        MusicLogUtils.d(TAG, "getView<<<");
        return view;
    }

    /**
     * initial view pager
     */
    private void initPager() {
        mPagers.clear();
        View view = null;
        for (int i = 0; i <= PLAYLIST_INDEX; i++) {
            view = (i == mCurrentTab) ? getView(i) : null;
            mPagers.add(view);
        }
    }

    /**
     * update play back tab info
     */
    private void updatePlaybackTab() {
        final int drawalbeTopPostion = 1;
        final int opaqueFull = 255; // 100%
        final int opaqueHalf = 128; // 50%
        TabWidget tabWidgetTemp = mTabHost.getTabWidget();
		/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        //TextView tabView = (TextView) tabWidgetTemp.getChildTabViewAt(PLAYBACK_INDEX);
        ImageView tabView = (ImageView) tabWidgetTemp.getChildTabViewAt(PLAYBACK_INDEX);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        boolean enable = true;
        long id = -1;
        Drawable[] drawables;
        Drawable drawableTop = null;
        int drawableTopAlpha = opaqueFull;

        if (tabView == null) {
            return;
        }
        try {
            if (mService != null) {
                id = mService.getAudioId();
            }
        }
        catch (RemoteException ex) {
            MusicLogUtils.e(TAG, "updatePlaybackTab getAudioId remote excption:" + ex);
        }
        if (id == -1) {
            enable = false;
            drawableTopAlpha = opaqueHalf;
        }
        tabView.setEnabled(enable);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        //drawables = tabView.getCompoundDrawables();
        //drawableTop = drawables[drawalbeTopPostion];
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
        if (drawableTop != null) {
            drawableTop.setAlpha(drawableTopAlpha);
        }
        MusicLogUtils.d(TAG, "updatePlaybackTab:" + enable);
    }

    /**
     * for service connect
     */
    public void onServiceConnected(ComponentName className, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
        String shuf = getIntent().getStringExtra("autoshuffle");
        if (mService != null) {
            if (Boolean.valueOf(shuf).booleanValue()) {
                try {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                }
                catch (RemoteException ex) {
                    MusicLogUtils.e(TAG, "onServiceConnected setShuffleMode remote excption:" + ex);
                }
            }
            MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
            updatePlaybackTab();
        }
    }

    public void onServiceDisconnected(ComponentName className) {
        mService = null;
        finish();
    }

    /**
     * OnTabChangeListener for TabHost
     *
     * @param tabId
     */
    public void onTabChanged(String tabId) {
        int tabIndex = TAB_MAP.get(tabId);
        MusicLogUtils.d(TAG, "onTabChanged-tabId:" + tabId);
        // MusicLogUtils.d(TAG, "onTabChanged-tabIndex:" + tabIndex);
        if ((tabIndex >= ARTIST_INDEX) && (tabIndex <= PLAYLIST_INDEX)) {
            mViewPager.setCurrentItem(tabIndex);
            mCurrentTab = tabIndex;
        } else if (tabIndex == PLAYBACK_INDEX) {
            Intent intent = new Intent(this, MediaPlaybackActivity.class);
            startActivity(intent);
        }
    }

    /**
     * OnPageChangeListener for ViewPager
     *
     * @param position
     */
    public void onPageSelected(int position) {
        MusicLogUtils.d(TAG, "onPageSelected-position:" + position);
        mTabHost.setCurrentTab(position);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
        updateTitle(position);
        MusicLogUtils.d("wyy_20150227", "004mCurrentTab ==>>>"+mCurrentTab + ",  position -- " + position);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
    }

   /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
    public void updateTitle(int position){
          if (mAlbumTab == null ) return ;
          switch (position) {
            case ALBUM_INDEX:
                mNowTypeIcon.setBackgroundResource(R.drawable.bird_album_tab);
                mNowTypeTitle.setText(R.string.albums_title);
                mAlbumTab.setBackgroundResource(R.drawable.bird_ic_tab_selected_holo);
                mSongTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mPlaylistTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mArtistTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                break;
            case SONG_INDEX:
                mNowTypeIcon.setBackgroundResource(R.drawable.bird_song_tab);
                mNowTypeTitle.setText(R.string.tracks_title);
                mAlbumTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mSongTab.setBackgroundResource(R.drawable.bird_ic_tab_selected_holo);
                mPlaylistTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mArtistTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                break;
            case PLAYLIST_INDEX:
                mNowTypeIcon.setBackgroundResource(R.drawable.bird_playlist_tab);
                mNowTypeTitle.setText(R.string.playlists_title);
                mAlbumTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mSongTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mPlaylistTab.setBackgroundResource(R.drawable.bird_ic_tab_selected_holo);
                mArtistTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                break;
            case PLAYBACK_INDEX:
                /*mNowTypeIcon.setBackgroundResource(R.drawable.ic_tab_albums);
                mNowTypeTitle.setText(R.string.albums_title);
                mAlbumTab.setBackgroundResource(R.drawable.bird_ic_tab_selected_holo);*/
                break;
            case ARTIST_INDEX:
            default:
                MusicLogUtils.d(TAG, "ARTIST_INDEX or default");
                mNowTypeIcon.setBackgroundResource(R.drawable.bird_artist_tab);
                mNowTypeTitle.setText(R.string.artists_title);
                mAlbumTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mSongTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mPlaylistTab.setBackgroundResource(R.drawable.bird_ic_tab_unselected_holo);
                mArtistTab.setBackgroundResource(R.drawable.bird_ic_tab_selected_holo);
                break;
        }

    }

   private void showSearchWindow() {
        if(!searchview_display){
            //获取LayoutInflater实例
            LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.bird_audio_searchview, null);
            searchWindow = new PopupWindow(layout,getResources().getDimensionPixelSize(R.dimen.bird_searchwindow_width),
                getResources().getDimensionPixelSize(R.dimen.bird_searchwindow_height)); //后两个参数是width和height
            //设置如下四条信息，当点击其他区域使其隐藏，要在show之前配置
            //[bug109209][BIRD_WEIMI_MUSIC]chengshujiang 20160325 modify
            // searchWindow.setAnimationStyle(R.style.SearchAnimation);
            searchWindow.setFocusable(true);
            searchWindow.setOutsideTouchable(true);
            searchWindow.update();
            //[bug109209][BIRD_WEIMI_MUSIC]chengshujiang 20160325 modify
            searchWindow.setBackgroundDrawable(getBaseContext().getResources().getDrawable(R.drawable.search_bg));
            searchWindow.showAtLocation(this.findViewById(R.id.normal_view_title), Gravity.TOP|Gravity.CENTER_HORIZONTAL,
                0,getResources().getDimensionPixelSize(R.dimen.bird_popupWindow_height)); //设置layout在PopupWindow中显示的位置

            mSearchView = (WeimiSearchView)layout.findViewById(R.id.bbk_search_view);
            mSearchView.setOnQueryTextListener(mQueryTextListenerTitle);
            final EditText editText = (EditText) mSearchView.findViewById(R.id.search_src_text);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager)
                    mSearchView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }, 100);
            /*mOkView = (Button)layout.findViewById(R.id.search);
            mOkView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent();
                    intent.setClass(MusicBrowserActivity.this, QueryBrowserActivity.class);
                    intent.putExtra(SearchManager.QUERY, editText.getText().toString());
                    startActivity(intent);
                    if(searchWindow != null)
                        searchWindow.dismiss();
                }
            });*/
            mCanelView = (TextView)layout.findViewById(R.id.cancel);
            mCanelView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    searchWindow.dismiss();
                }
            });
            searchview_display = false;
        } else {
            //如果当前已经为显示状态，则隐藏起来
            searchWindow.dismiss();
            searchview_display = false;
        }
    }

    WeimiSearchView.OnQueryTextListener mQueryTextListenerTitle = new WeimiSearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(MusicBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            if(searchWindow != null)
            searchWindow.dismiss();
            //mSearchItem.collapseActionView();
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };   
    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/

    /**
     * onPageScrolled
     *
     * @param position
     * @param positionOffset
     * @param positionOffsetPixels
     */
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    /**
     * onPageScrollStateChanged
     *
     * @param state
     */
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * MusicPagerAdapter for scroll page
     */
    private class MusicPagerAdapter extends PagerAdapter {
        @Override
        public void destroyItem(View container, int position, Object object) {
            ViewPager viewPager = ((ViewPager) container);
            // MusicLogUtils.d(TAG, "destroyItem-position:" + position);
            viewPager.removeView(mPagers.get(position));
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ViewPager viewPager = ((ViewPager) container);
            View view = mPagers.get(position);
            MusicLogUtils.d(TAG, "instantiateItem-position:" + position);
            if (view == null) {
                view = getView(position);
                mPagers.remove(position);
                mPagers.add(position, view);
                mActivityManager.dispatchResume();
            }
            viewPager.addView(view);
            return mPagers.get(position);
        }

        public int getCount() {
            // MusicLogUtils.d(TAG, "getCount:" + mPagers.size());
            return mPagers.size();
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == null ? false : view.equals(object);
        }
    }

    /**
     * M: init search button, set on click listener and search dialog on dismiss listener, disable search button
     * when search dialog has shown and enable it after dismiss search dialog.
     */
    private void initSearchButton() {
        mSearchButton = (ImageButton) findViewById(R.id.search_menu_nowplaying);
        final View blankView = this.findViewById(R.id.blank_between_search_and_overflow);
        final View nowPlayingView = this.findViewById(R.id.nowplaying);
        if (mSearchButton != null) {
            mSearchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOverflowMenuButton != null) {
                        mOverflowMenuButton.setEnabled(false);
                    }
                    mSearchButton.setVisibility(View.GONE);
                    onSearchRequested();
                    if (blankView.getVisibility() == View.VISIBLE) {
                        blankView.setVisibility(View.GONE);
                    }
                    mSearchViewShowing = true;
                }
            });
            SearchManager searchManager = (SearchManager) this
                    .getSystemService(Context.SEARCH_SERVICE);
            searchManager.setOnDismissListener(new SearchManager.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (mOverflowMenuButton != null) {
                        mOverflowMenuButton.setEnabled(true);
                    }
                    mSearchButton.setVisibility(View.VISIBLE);
                    if (nowPlayingView.getVisibility() != View.VISIBLE && !mHasMenukey) {
                        blankView.setVisibility(View.VISIBLE);
                    }
                    mSearchViewShowing = false;
                    MusicLogUtils.d(TAG, "Search dialog on dismiss, enalbe search button");
                }
            });
        }
    }
}
