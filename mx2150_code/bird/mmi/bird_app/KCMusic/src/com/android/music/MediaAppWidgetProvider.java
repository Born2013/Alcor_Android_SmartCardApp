/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RemoteViews;
/*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 begin*/
import android.graphics.Bitmap;
/*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 end*/
/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class MediaAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "MusicAppWidget";
    
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";

    private static MediaAppWidgetProvider sInstance;

    static synchronized MediaAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        MusicLogUtils.i(TAG, "onUpdate");
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(context, MediaPlaybackService.class);
        updateIntent.setAction(MediaPlaybackService.SERVICECMD);
        updateIntent.putExtra(MediaPlaybackService.CMDNAME,
                MediaAppWidgetProvider.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.startService(updateIntent);
    }
    
    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weimi_album_appwidget); /*[BIRD_WEIMI_MUSIC]//album_appwidget */
        views.setViewVisibility(R.id.artist, View.GONE);/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314  R.id.title*/
        /// M: Set default play/pause button image resource
        views.setImageViewResource(R.id.control_play, R.drawable.weimi_widget_play); /*[BIRD_WEIMI_MUSIC] //album_appwidget */
        views.setTextViewText(R.id.title, res.getText(R.string.widget_initial_text)); /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314  R.id.artist*/

        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        MusicLogUtils.i(TAG, "pushUpdate");
        /// M: update app widget @{
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
        /// @}
    }
    
    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        /// M: log appWidgetIds status @{
        int widgetLength = (appWidgetIds == null ? 0 : appWidgetIds.length);
        MusicLogUtils.i(TAG, "hasInstances number is " + widgetLength);
        /// @}
        return (widgetLength > 0);
    }

    /**
     * Handle a change notification coming over from {@link MediaPlaybackService}
     */
    void notifyChange(MediaPlaybackService service, String what) {
        MusicLogUtils.i(TAG, "notifyChange");
        /// M: add QUIT_PLAYBACK status to update widget and log something @{
        if (hasInstances(service)) {
            if (MediaPlaybackService.META_CHANGED.equals(what) ||
                    MediaPlaybackService.PLAYSTATE_CHANGED.equals(what) ||
                    MediaPlaybackService.QUIT_PLAYBACK.equals(what)) {
                performUpdate(service, null);
            } else {
                MusicLogUtils.d(TAG, "notifyChange(" + what + "):discard!");
            }
        } else {
            MusicLogUtils.d(TAG, "notifyChange: no Instance");
        }
        /// @}
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(MediaPlaybackService service, int[] appWidgetIds) {
        MusicLogUtils.d(TAG, "performUpdate");
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.weimi_album_appwidget); /*[BIRD_WEIMI_MUSIC]//album_appwidget */
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
        CharSequence errorState = null;
        
        /// M: If artist name get from database is equal to "unknown",use unknown_artist_name replace.
        if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
            artistName = res.getString(R.string.unknown_artist_name);
        }

        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
                status.equals(Environment.MEDIA_UNMOUNTED)) {
            /// M: Remove check external storage @{
            //if (android.os.Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_busy_title);
            //} else {
            //    errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
            //}
            /// @}
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            /// M: Remove check external storage @{
            //if (android.os.Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_missing_title);
            //} else {
            //    errorState = res.getText(R.string.sdcard_missing_title_nosdcard);
            //}
            /// @}
        } else if (titleName == null) {
            errorState = res.getText(R.string.widget_initial_text);
        }
        
        if (errorState != null) {
            /// M: Show error state to user
            /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 begin*/
            views.setViewVisibility(R.id.artist, View.GONE);//View.VISIBLE
            //views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.title, errorState);
            /*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 end*/
            views.setTextViewText(R.id.artist, errorState);
            
        } else {
            /// M: No error, so show normal titles @{
            final String httpHeader = "http://";
            if ((titleName != null) && titleName.toString().startsWith(httpHeader)) {
                views.setViewVisibility(R.id.title, View.VISIBLE);
                views.setViewVisibility(R.id.artist, View.GONE);
                views.setTextViewText(R.id.title, titleName);
            } else {
                views.setViewVisibility(R.id.title, View.VISIBLE);
                views.setViewVisibility(R.id.artist, View.VISIBLE);
                views.setTextViewText(R.id.title, titleName);
                views.setTextViewText(R.id.artist, artistName);
            }
            /// @}
        }
        
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.weimi_widget_pause); /*[BIRD_WEIMI_MUSIC] //ic_appwidget_music_pause*/
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.weimi_widget_play);/*[BIRD_WEIMI_MUSIC] //ic_appwidget_music_play */
        }

        MusicLogUtils.i(TAG, "performUpdate,Track is " + titleName +
            " Artist is " + artistName + " Error is " + errorState + " Playing is " + playing);

        // Link actions buttons to intents
        linkButtons(service, views, playing);
        
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivity},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(context, MediaPlaybackService.class);
        
        if (playerActive) {
            intent = new Intent(context, MediaPlaybackActivity.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flag */);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
            views.setOnClickPendingIntent(R.id.album_bitmap, pendingIntent); /*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 add*/
        } else {
            intent = new Intent(context, MusicBrowserActivity.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent,  0 /* no flag */);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
            views.setOnClickPendingIntent(R.id.album_bitmap, pendingIntent); /*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 add*/
        }
        
        intent = new Intent(MediaPlaybackService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flag */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
        
        intent = new Intent(MediaPlaybackService.NEXT_ACTION);
        intent.setComponent(serviceName);
        /// M: set this pendingintent to use one time only
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, PendingIntent.FLAG_ONE_SHOT);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 begin*/
        intent = new Intent(MediaPlaybackService.PREVIOUS_ACTION);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);
        /*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 end*/
    }

    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 begin*/
    /**
     * Update all active widget instances by pushing changes 
     */
    void notifyArtBitmapChange(MediaPlaybackService service,Bitmap bitmap) {  
        performUpdate(service, null, bitmap);
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(MediaPlaybackService service, int[] appWidgetIds, Bitmap bitmap) {
        MusicLogUtils.d(TAG, "performUpdate");
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.weimi_album_appwidget);
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
        CharSequence errorState = null;
        
        /// M: If artist name get from database is equal to "unknown",use unknown_artist_name replace.
        if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
            artistName = res.getString(R.string.unknown_artist_name);
        }
        
        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
                status.equals(Environment.MEDIA_UNMOUNTED)) {
            /// M: Remove check external storage @{
            //if (android.os.Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_busy_title);
            //} else {
            //    errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
            //}
            /// @}
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            /// M: Remove check external storage @{
            //if (android.os.Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_missing_title);
            //} else {
            //    errorState = res.getText(R.string.sdcard_missing_title_nosdcard);
            //}
            /// @}
        } else if (titleName == null) {
            errorState = res.getText(R.string.widget_initial_text);
        }

        if (errorState != null) {
            /// M: Show error state to user
            views.setViewVisibility(R.id.artist, View.GONE);
            views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
            
        } else {
            /// M: No error, so show normal titles @{
            final String httpHeader = "http://";
            if ((titleName != null) && titleName.toString().startsWith(httpHeader)) {
                views.setViewVisibility(R.id.title, View.VISIBLE);
                views.setViewVisibility(R.id.artist, View.GONE);
                views.setTextViewText(R.id.title, titleName);
            } else {
                views.setViewVisibility(R.id.title, View.VISIBLE);
                views.setViewVisibility(R.id.artist, View.VISIBLE);        	
                
                views.setTextViewText(R.id.title, titleName);
                views.setTextViewText(R.id.artist, artistName);
            }
            /// @}
        }
        
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.weimi_widget_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.weimi_widget_play);
        }
        
        views.setImageViewBitmap(R.id.album_bitmap, bitmap);
		
        MusicLogUtils.i(TAG, "performUpdate,Track is " + titleName + 
            " Artist is " + artistName + " Error is " + errorState + " Playing is " + playing); 

        // Link actions buttons to intents
        linkButtons(service, views, playing);
        
        pushUpdate(service, appWidgetIds, views);
    }
    /*[BIRD_WEIMI_MUSIC]wangyueyue 20150317 end*/
    public static class PackageDataClearedReceiver extends BroadcastReceiver {
        private static final String ACTION_PACKAGE_DATA_CLEARED = "com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED";

        public void onReceive(Context context, Intent intent) {
            if (!ACTION_PACKAGE_DATA_CLEARED.equals(intent.getAction())) {
                return;
            }
            String pkgName = intent.getStringExtra("packageName");
            MusicLogUtils.v(TAG, "PackageDataClearedReceiver recevied pkgName = " + pkgName);
            if (pkgName != null && pkgName.equals(context.getPackageName())) {
                MediaAppWidgetProvider mediaAppWidgetProvider = MediaAppWidgetProvider
                        .getInstance();
                if (mediaAppWidgetProvider == null) {
                    MusicLogUtils.v(TAG, "mediaAppWidgetProvider is null ");
                    return;
                }
                mediaAppWidgetProvider.defaultAppWidget(context, null);
            }
        }

    }
}
