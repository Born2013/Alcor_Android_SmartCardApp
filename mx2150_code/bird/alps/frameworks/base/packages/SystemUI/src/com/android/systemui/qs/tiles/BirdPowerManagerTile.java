/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.qs.QSTile;
import com.android.systemui.R;
import com.android.systemui.screenshot.TakeScreenshotService;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import android.os.UserHandle;
import android.provider.Settings;

/** Quick settings tile: Control screenshot **/
public class BirdPowerManagerTile extends QSTile<QSTile.BooleanState> {

    private static final String TAG = "BirdPowerManagerTile";
    private final static String ACTION_GENERAL_SAVING_STATUS_CHANGE = "com.bird.powermanager.ACTION_GENERAL_SAVING_STATUS_CHANGE";
    private final KeyguardMonitor mKeyguard;

    public BirdPowerManagerTile(Host host) {
        super(host);
        mKeyguard = host.getKeyguardMonitor();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_CELLULAR;
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }
    
    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    protected void handleLongClick() {
        Log.i(TAG, "BIRD, handleLongClick");
    }

    @Override
    protected void handleClick() {

        if (mKeyguard.isShowing()) {
            mHost.refresh();
        } else {
            mHost.collapsePanels();
        }

        /*Intent service = new Intent(mHost.getContext(), com.android.systemui.screenshot.TakeScreenshotService.class);
        service.putExtra(TakeScreenshotService.BIRD_SCREENSHOT_EXTRA, 3);
        //mHost.getContext().startService(service);
        mHost.getContext().startServiceAsUser(service, UserHandle.CURRENT);*/
        boolean isChecked = Settings.System.getInt(mHost.getContext().getContentResolver(), Settings.System.BIRD_GENERAL_SAVING_STATUS, 0) == 1;
        Intent intent = new Intent(ACTION_GENERAL_SAVING_STATUS_CHANGE);
        intent.putExtra("general_status", isChecked ? "general_status_off" : "general_status_on");
        mHost.getContext().sendBroadcast(intent);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mHost.getContext().getString(R.string.bird_powermanager_name);
        //state.visible = true;
        /*if (arg instanceof Boolean) {
            boolean value = (Boolean) arg;
            if (value == state.value) {
                return;
            }
            state.value = value;
        }*/
        state.value = Settings.System.getInt(mHost.getContext().getContentResolver(), Settings.System.BIRD_GENERAL_SAVING_STATUS, 0) == 1;
        final int iconIdOn = R.drawable.zzzzz_ic_qs_saverpover_on;
        final int iconIdOff = R.drawable.zzzzz_ic_qs_saverpover_off;
        state.icon = state.value ? ResourceIcon.get(iconIdOn) : ResourceIcon.get(iconIdOff);

    }
    
    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.bird_powermanager_name);
    }
}
