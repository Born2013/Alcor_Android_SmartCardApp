/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.annotation.DrawableRes;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.statusbar.util.FeatureOptions;

import java.util.ArrayList;
import java.util.List;

//[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
import android.graphics.PorterDuff;
import com.android.systemui.FeatureOption;
//[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END
/*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]BEGIN */
import android.telecom.TelecomManager;
/*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]END */

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkControllerImpl.SignalCallback,
        SecurityController.SecurityControllerCallback, Tunable {

    static final String TAG = "SignalClusterView";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String SLOT_AIRPLANE = "airplane";
    private static final String SLOT_MOBILE = "mobile";
    private static final String SLOT_WIFI = "wifi";
    private static final String SLOT_ETHERNET = "ethernet";

    NetworkControllerImpl mNC;
    SecurityController mSC;

    private boolean mNoSimsVisible = false;
    private boolean mVpnVisible = false;
    private boolean mEthernetVisible = false;
    private int mEthernetIconId = 0;
    private int mLastEthernetIconId = -1;
    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0;
    private int mLastWifiStrengthId = -1;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private int mLastAirplaneIconId = -1;
    private String mAirplaneContentDescription;
    private String mWifiDescription;
    private String mEthernetDescription;
    private ArrayList<PhoneState> mPhoneStates = new ArrayList<PhoneState>();
    private int mIconTint = Color.WHITE;
    private float mDarkIntensity;
    private final Rect mTintArea = new Rect();

    ViewGroup mEthernetGroup, mWifiGroup;
    View mNoSimsCombo;
    ImageView mVpn, mEthernet, mWifi, mAirplane, mNoSims, mEthernetDark, mWifiDark, mNoSimsDark;
    View mWifiAirplaneSpacer;
    View mWifiSignalSpacer;
    LinearLayout mMobileSignalGroup;

    private final int mMobileSignalGroupEndPadding;
    private final int mMobileDataIconStartPadding;
    private final int mWideTypeIconStartPadding;
    private final int mSecondaryTelephonyPadding;
    private final int mEndPadding;
    private final int mEndPaddingNothingVisible;
    private final float mIconScaleFactor;

    private boolean mBlockAirplane;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    private boolean mBlockEthernet;



    /// M: Add for Plugin feature @ {
    private ISystemUIStatusBarExt mStatusBarExt;
    /// @ }

    /// M: for vowifi
    boolean mIsWfcEnable;

    //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
    private Drawable mMobileDrawable, mSignalNetworkTypeDrawable, mMobileTypeDrawable;
    //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END

    //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
    static final int[] DATA_ACTIVITY = {
        R.drawable.stat_sys_signal_in,
        R.drawable.stat_sys_signal_out,
        R.drawable.stat_sys_signal_inout
    };
    //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END
    
    /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]BEGIN */
    private TelecomManager mTelecomManager;
    /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]END */

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Resources res = getResources();
        mMobileSignalGroupEndPadding =
                res.getDimensionPixelSize(R.dimen.mobile_signal_group_end_padding);
        mMobileDataIconStartPadding =
                res.getDimensionPixelSize(R.dimen.mobile_data_icon_start_padding);
        mWideTypeIconStartPadding = res.getDimensionPixelSize(R.dimen.wide_type_icon_start_padding);
        mSecondaryTelephonyPadding = res.getDimensionPixelSize(R.dimen.secondary_telephony_padding);
        mEndPadding = res.getDimensionPixelSize(R.dimen.signal_cluster_battery_padding);
        mEndPaddingNothingVisible = res.getDimensionPixelSize(
                R.dimen.no_signal_cluster_battery_padding);

        TypedValue typedValue = new TypedValue();
        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        mIconScaleFactor = typedValue.getFloat();

        /// M: Add for Plugin feature @ {
        mStatusBarExt = PluginManager.getSystemUIStatusBarExt(context);
        /// @ }
        mIsWfcEnable = SystemProperties.get("persist.mtk_wfc_support").equals("1");
        /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]BEGIN */
        if (FeatureOption.BIRD_AIS_NETWORKICON_TASK) {
            mTelecomManager = TelecomManager.from(mContext);
        }
        /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]END */
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!StatusBarIconController.ICON_BLACKLIST.equals(key)) {
            return;
        }
        ArraySet<String> blockList = StatusBarIconController.getIconBlacklist(newValue);
        boolean blockAirplane = blockList.contains(SLOT_AIRPLANE);
        boolean blockMobile = blockList.contains(SLOT_MOBILE);
        boolean blockWifi = blockList.contains(SLOT_WIFI);
        boolean blockEthernet = blockList.contains(SLOT_ETHERNET);

        if (blockAirplane != mBlockAirplane || blockMobile != mBlockMobile
                || blockEthernet != mBlockEthernet || blockWifi != mBlockWifi) {
            mBlockAirplane = blockAirplane;
            mBlockMobile = blockMobile;
            mBlockEthernet = blockEthernet;
            mBlockWifi = blockWifi;
            // Re-register to get new callbacks.
            mNC.removeSignalCallback(this);
            mNC.addSignalCallback(this);
        }
    }

    public void setNetworkController(NetworkControllerImpl nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    public void setSecurityController(SecurityController sc) {
        if (DEBUG) Log.d(TAG, "SecurityController=" + sc);
        mSC = sc;
        mSC.addCallback(this);
        mVpnVisible = mSC.isVpnEnabled();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mVpn            = (ImageView) findViewById(R.id.vpn);
        mEthernetGroup  = (ViewGroup) findViewById(R.id.ethernet_combo);
        mEthernet       = (ImageView) findViewById(R.id.ethernet);
        mEthernetDark   = (ImageView) findViewById(R.id.ethernet_dark);
        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiDark       = (ImageView) findViewById(R.id.wifi_signal_dark);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        mNoSims         = (ImageView) findViewById(R.id.no_sims);
        mNoSimsDark     = (ImageView) findViewById(R.id.no_sims_dark);
        mNoSimsCombo    =             findViewById(R.id.no_sims_combo);
        mWifiAirplaneSpacer =         findViewById(R.id.wifi_airplane_spacer);
        mWifiSignalSpacer =           findViewById(R.id.wifi_signal_spacer);
        mMobileSignalGroup = (LinearLayout) findViewById(R.id.mobile_signal_group);

        maybeScaleVpnAndNoSimsIcons();
    }

    /**
     * Extracts the icon off of the VPN and no sims views and maybe scale them by
     * {@link #mIconScaleFactor}. Note that the other icons are not scaled here because they are
     * dynamic. As such, they need to be scaled each time the icon changes in {@link #apply()}.
     */
    private void maybeScaleVpnAndNoSimsIcons() {
        if (mIconScaleFactor == 1.f) {
            return;
        }

        mVpn.setImageDrawable(new ScalingDrawableWrapper(mVpn.getDrawable(), mIconScaleFactor));

        mNoSims.setImageDrawable(
                new ScalingDrawableWrapper(mNoSims.getDrawable(), mIconScaleFactor));
        mNoSimsDark.setImageDrawable(
                new ScalingDrawableWrapper(mNoSimsDark.getDrawable(), mIconScaleFactor));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        for (PhoneState state : mPhoneStates) {
            mMobileSignalGroup.addView(state.mMobileGroup);
        }

        int endPadding = mMobileSignalGroup.getChildCount() > 0 ? mMobileSignalGroupEndPadding : 0;
        mMobileSignalGroup.setPaddingRelative(0, 0, endPadding, 0);

        TunerService.get(mContext).addTunable(this, StatusBarIconController.ICON_BLACKLIST);

        /// M: Add for Plugin feature @ {
        mStatusBarExt.setCustomizedNoSimView(mNoSims);
        mStatusBarExt.setCustomizedNoSimView(mNoSimsDark);
        mStatusBarExt.addSignalClusterCustomizedView(mContext, this,
                indexOfChild(findViewById(R.id.mobile_signal_group)));
        /// @ }

        apply();
        applyIconTint();
        mNC.addSignalCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        mMobileSignalGroup.removeAllViews();
        TunerService.get(mContext).removeTunable(this);
        mSC.removeCallback(this);
        mNC.removeSignalCallback(this);

        super.onDetachedFromWindow();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Re-run all checks against the tint area for all icons
        applyIconTint();
    }

    // From SecurityController.
    @Override
    public void onStateChanged() {
        post(new Runnable() {
            @Override
            public void run() {
                mVpnVisible = mSC.isVpnEnabled();
                apply();
            }
        });
    }

    @Override
    public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
            boolean activityIn, boolean activityOut, String description) {
        mWifiVisible = statusIcon.visible && !mBlockWifi;
        mWifiStrengthId = statusIcon.icon;
        mWifiDescription = statusIcon.contentDescription;

        apply();
    }
    ///M: Support[Network Type and volte on StatusBar]. Add more parameter networkType and volte .
    @Override
    public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
            int networkType, int volteIcon, int qsType, boolean activityIn, boolean activityOut,
            String typeContentDescription, String description, boolean isWide, int subId) {
        PhoneState state = getState(subId);
        if (state == null) {
            return;
        }
        state.mMobileVisible = statusIcon.visible && !mBlockMobile;
        state.mMobileStrengthId = statusIcon.icon;
        state.mMobileTypeId = statusType;
        state.mMobileDescription = statusIcon.contentDescription;
        state.mMobileTypeDescription = typeContentDescription;
        state.mIsMobileTypeIconWide = statusType != 0 && isWide;
        state.mNetworkIcon = networkType;
        state.mVolteIcon = volteIcon;

        /// M: Add for plugin features. @ {
        state.mDataActivityIn = activityIn;
        state.mDataActivityOut = activityOut;
        /// @ }

        //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
        setDataActivityMTK(activityIn, activityOut, subId);
        //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END

        apply();
    }

    @Override
    public void setEthernetIndicators(IconState state) {
        mEthernetVisible = state.visible && !mBlockEthernet;
        mEthernetIconId = state.icon;
        mEthernetDescription = state.contentDescription;

        apply();
    }

    @Override
    public void setNoSims(boolean show) {
        mNoSimsVisible = show && !mBlockMobile;
        // M: Bug fix ALPS02302143, in case UI need to be refreshed.
        // MR1 also add this patch
        apply();
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
        if (hasCorrectSubs(subs)) {
            return;
        }
        // Clear out all old subIds.
        for (PhoneState state : mPhoneStates) {
            if (state.mMobile != null) {
                state.maybeStopAnimatableDrawable(state.mMobile);
            }
            if (state.mMobileDark != null) {
                state.maybeStopAnimatableDrawable(state.mMobileDark);
            }
        }
        mPhoneStates.clear();
        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.removeAllViews();
        }
        final int n = subs.size();
        for (int i = 0; i < n; i++) {
            inflatePhoneState(subs.get(i).getSubscriptionId());
        }
        if (isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> subs) {
        final int N = subs.size();
        if (N != mPhoneStates.size()) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            if (mPhoneStates.get(i).mSubId != subs.get(i).getSubscriptionId()) {
                return false;
            }
        }
        return true;
    }

    private PhoneState getState(int subId) {
        for (PhoneState state : mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        Log.e(TAG, "Unexpected subscription " + subId);
        return null;
    }

    private PhoneState inflatePhoneState(int subId) {
        PhoneState state = new PhoneState(subId, mContext);
        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.addView(state.mMobileGroup);
        }
        mPhoneStates.add(state);
        return state;
    }

    @Override
    public void setIsAirplaneMode(IconState icon) {
        mIsAirplaneMode = icon.visible && !mBlockAirplane;
        mAirplaneIconId = icon.icon;
        mAirplaneContentDescription = icon.contentDescription;

        apply();
    }

    @Override
    public void setMobileDataEnabled(boolean enabled) {
        // Don't care.
    }

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mEthernetVisible && mEthernetGroup != null &&
                mEthernetGroup.getContentDescription() != null)
            event.getText().add(mEthernetGroup.getContentDescription());
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        for (PhoneState state : mPhoneStates) {
            state.populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mEthernet != null) {
            mEthernet.setImageDrawable(null);
            mEthernetDark.setImageDrawable(null);
            mLastEthernetIconId = -1;
        }

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
            mWifiDark.setImageDrawable(null);
            mLastWifiStrengthId = -1;
        }

        for (PhoneState state : mPhoneStates) {
            if (state.mMobile != null) {
                state.maybeStopAnimatableDrawable(state.mMobile);
                state.mMobile.setImageDrawable(null);
                state.mLastMobileStrengthId = -1;
            }
            if (state.mMobileDark != null) {
                state.maybeStopAnimatableDrawable(state.mMobileDark);
                state.mMobileDark.setImageDrawable(null);
                state.mLastMobileStrengthId = -1;
            }
            if (state.mMobileType != null) {
                state.mMobileType.setImageDrawable(null);
                state.mLastMobileTypeId = -1;
            }
        }

        if (mAirplane != null) {
            mAirplane.setImageDrawable(null);
            mLastAirplaneIconId = -1;
        }

        apply();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        mVpn.setVisibility(mVpnVisible ? View.VISIBLE : View.GONE);
        if (DEBUG) Log.d(TAG, String.format("vpn: %s", mVpnVisible ? "VISIBLE" : "GONE"));

        if (mEthernetVisible) {
            if (mLastEthernetIconId != mEthernetIconId) {
                setIconForView(mEthernet, mEthernetIconId);
                setIconForView(mEthernetDark, mEthernetIconId);
                mLastEthernetIconId = mEthernetIconId;
            }
            mEthernetGroup.setContentDescription(mEthernetDescription);
            mEthernetGroup.setVisibility(View.VISIBLE);
        } else {
            mEthernetGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("ethernet: %s",
                    (mEthernetVisible ? "VISIBLE" : "GONE")));

        if (mWifiVisible) {
            if (mWifiStrengthId != mLastWifiStrengthId) {
                setIconForView(mWifi, mWifiStrengthId);
                setIconForView(mWifiDark, mWifiStrengthId);
                mLastWifiStrengthId = mWifiStrengthId;
            }
            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("wifi: %s sig=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId));

        boolean anyMobileVisible = false;
        /// M: Support for [Network Type on Statusbar]
        /// A spacer is set between networktype and WIFI icon @ {
        if (FeatureOptions.MTK_CTA_SET) {
            anyMobileVisible = true;
        }
        /// @ }
        int firstMobileTypeId = 0;
        for (PhoneState state : mPhoneStates) {
            if (state.apply(anyMobileVisible)) {
                if (!anyMobileVisible) {
                    firstMobileTypeId = state.mMobileTypeId;
                    anyMobileVisible = true;
                }
            }
        }

        if (mIsAirplaneMode) {
            if (mLastAirplaneIconId != mAirplaneIconId) {
                setIconForView(mAirplane, mAirplaneIconId);
                mLastAirplaneIconId = mAirplaneIconId;
            }
            mAirplane.setContentDescription(mAirplaneContentDescription);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode && mWifiVisible) {
            mWifiAirplaneSpacer.setVisibility(View.VISIBLE);
        } else {
            mWifiAirplaneSpacer.setVisibility(View.GONE);
        }

        if (((anyMobileVisible && firstMobileTypeId != 0) || mNoSimsVisible) && mWifiVisible) {
            mWifiSignalSpacer.setVisibility(View.VISIBLE);
        } else {
            mWifiSignalSpacer.setVisibility(View.GONE);
        }

        mNoSimsCombo.setVisibility(mNoSimsVisible ? View.VISIBLE : View.GONE);
        /// M: Add for Plugin feature @ {
        mStatusBarExt.setCustomizedNoSimsVisible(mNoSimsVisible);
        mStatusBarExt.setCustomizedAirplaneView(mNoSimsCombo, mIsAirplaneMode);
        /// @ }

        boolean anythingVisible = mNoSimsVisible || mWifiVisible || mIsAirplaneMode
                || anyMobileVisible || mVpnVisible || mEthernetVisible;
        setPaddingRelative(0, 0, anythingVisible ? mEndPadding : mEndPaddingNothingVisible, 0);
    }
    
    //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
    public void setTintColor(int subId, int tintColor){
        PhoneState state = getOrInflateState(subId);
        if (state != null) {
            state.mSimCardTintColor = tintColor;
        }
    }
    //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END

    /**
     * Sets the given drawable id on the view. This method will also scale the icon by
     * {@link #mIconScaleFactor} if appropriate.
     */
    private void setIconForView(ImageView imageView, @DrawableRes int iconId) {
        // Using the imageView's context to retrieve the Drawable so that theme is preserved.
        Drawable icon = imageView.getContext().getDrawable(iconId);

        if (mIconScaleFactor == 1.f) {
            imageView.setImageDrawable(icon);
        } else {
            imageView.setImageDrawable(new ScalingDrawableWrapper(icon, mIconScaleFactor));
        }
    }

    public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
        boolean changed = tint != mIconTint || darkIntensity != mDarkIntensity
                || !mTintArea.equals(tintArea);
        mIconTint = tint;
        mDarkIntensity = darkIntensity;
        mTintArea.set(tintArea);
        if (changed && isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private void applyIconTint() {
        setTint(mVpn, StatusBarIconController.getTint(mTintArea, mVpn, mIconTint));
        setTint(mAirplane, StatusBarIconController.getTint(mTintArea, mAirplane, mIconTint));
        applyDarkIntensity(
                StatusBarIconController.getDarkIntensity(mTintArea, mNoSims, mDarkIntensity),
                mNoSims, mNoSimsDark);
        applyDarkIntensity(
                StatusBarIconController.getDarkIntensity(mTintArea, mWifi, mDarkIntensity),
                mWifi, mWifiDark);
        applyDarkIntensity(
                StatusBarIconController.getDarkIntensity(mTintArea, mEthernet, mDarkIntensity),
                mEthernet, mEthernetDark);
        for (int i = 0; i < mPhoneStates.size(); i++) {
            mPhoneStates.get(i).setIconTint(mIconTint, mDarkIntensity, mTintArea);
        }
    }

    private void applyDarkIntensity(float darkIntensity, View lightIcon, View darkIcon) {
        lightIcon.setAlpha(1 - darkIntensity);
        darkIcon.setAlpha(darkIntensity);
    }

    private void setTint(ImageView v, int tint) {
        v.setImageTintList(ColorStateList.valueOf(tint));
    }

    //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
    public void setDataActivityMTK(boolean in, boolean out, int subId) {
        Log.d("yangheng", "setDataActivityMTK(in= " + in + "), out= " + out);


        int imgDataActivityID=0;
        if(in&&out){
            imgDataActivityID=DATA_ACTIVITY[2];

        }else if(out){
            imgDataActivityID=DATA_ACTIVITY[1];

        }else if(in){
            imgDataActivityID=DATA_ACTIVITY[0];

        }else {
            imgDataActivityID=0;

        }
        PhoneState state = getState(subId);
        state.mDataActivityId = imgDataActivityID;
    }
    //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END

    private class PhoneState {
        private final int mSubId;
        private boolean mMobileVisible = false;
        private int mMobileStrengthId = 0, mMobileTypeId = 0, mNetworkIcon = 0;
        private int mVolteIcon = 0;
        private int mLastMobileStrengthId = -1;
        private int mLastMobileTypeId = -1;
        private boolean mIsMobileTypeIconWide;
        private String mMobileDescription, mMobileTypeDescription;

        private ViewGroup mMobileGroup;

        private ImageView mMobile, mMobileDark, mMobileType;

        /// M: Add for new features @ {
        // Add for [Network Type and volte on Statusbar]
        private ImageView mNetworkType;
        private ImageView mVolteType;
        private boolean mIsWfcCase;
        /// @ }

        /// M: Add for plugin features. @ {
        private boolean mDataActivityIn, mDataActivityOut;
        private ISystemUIStatusBarExt mPhoneStateExt;
        /// @ }
        
        //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
        private int mSimCardTintColor = 0;
        //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END

        //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
        private ImageView mDataActivityInOut;
        private int mDataActivityId = 0;
        //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END


        public PhoneState(int subId, Context context) {
            ViewGroup root = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.mobile_signal_group_ext, null);

            /// M: Add data group for plugin feature. @ {
            mPhoneStateExt = PluginManager.getSystemUIStatusBarExt(context);
            mPhoneStateExt.addCustomizedView(subId, context, root);
            /// @ }

            setViews(root);
            mSubId = subId;
        }

        public void setViews(ViewGroup root) {
            mMobileGroup    = root;
            mMobile         = (ImageView) root.findViewById(R.id.mobile_signal);
            mMobileDark     = (ImageView) root.findViewById(R.id.mobile_signal_dark);
            mMobileType     = (ImageView) root.findViewById(R.id.mobile_type);
            mNetworkType    = (ImageView) root.findViewById(R.id.network_type);
            mVolteType      = (ImageView) root.findViewById(R.id.volte_indicator_ext);

            //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
            if (FeatureOption.BIRD_DATA_IN_OUT_ACTIVITY) {
                mDataActivityInOut = (ImageView) root.findViewById(R.id.data_inout);
                mDataActivityInOut.setVisibility(View.VISIBLE);
            }
            //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END

        }

        public boolean apply(boolean isSecondaryIcon) {
            if (mMobileVisible && !mIsAirplaneMode) {
                //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
                if (FeatureOption.BIRD_SIM_ICON_COLORFUL && mSimCardTintColor != 0 && mMobileStrengthId != 0) {
                    mMobile.setImageResource(mMobileStrengthId);
                    mMobile.setColorFilter(mSimCardTintColor);
                } else {
                    if (mLastMobileStrengthId != mMobileStrengthId) {
                        updateAnimatableIcon(mMobile, mMobileStrengthId);
                        updateAnimatableIcon(mMobileDark, mMobileStrengthId);
                        mLastMobileStrengthId = mMobileStrengthId;
                    }
                }
                //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END

                //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
                if (FeatureOption.BIRD_SIM_ICON_COLORFUL && mSimCardTintColor != 0 && mMobileTypeId != 0) {
                    mMobileTypeDrawable = mContext.getResources().getDrawable(mMobileTypeId);
                    mMobileTypeDrawable.setColorFilter(mSimCardTintColor, PorterDuff.Mode.MULTIPLY);
                    mMobileType.setImageDrawable(mMobileTypeDrawable);
                    mMobileTypeDrawable = null;
                    Log.i(TAG,"BIRD, DATA View: mMobileType, mSimCardTintColor" + mSimCardTintColor);
                } else {
                    if (mLastMobileTypeId != mMobileTypeId) {
                        mMobileType.setImageResource(mMobileTypeId);
                        mLastMobileTypeId = mMobileTypeId;
                    }
                }
                //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END
                
                //[BIRD][BIRD_SIM_ICON_SCALE_SMALLER][网络标识 4G/3G/E改小][yangheng][20170615] BEGIN
                if (FeatureOption.BIRD_SIM_ICON_SCALE_SMALLER) {
                    mMobileType.setScaleX(0.6f);
                    mMobileType.setScaleY(0.6f);
                }
                //[BIRD][BIRD_SIM_ICON_SCALE_SMALLER][网络标识 4G/3G/E改小][yangheng][20170615] END
                
                mMobileGroup.setContentDescription(mMobileTypeDescription
                        + " " + mMobileDescription);
                mMobileGroup.setVisibility(View.VISIBLE);
                showViewInWfcCase();
            } else {
                if (mIsAirplaneMode && (mIsWfcEnable && mVolteIcon != 0)) {
                    /// M:Bug fix for show vowifi icon in flight mode
                    mMobileGroup.setVisibility(View.VISIBLE);
                    hideViewInWfcCase();
                } else {
                    mMobileGroup.setVisibility(View.GONE);
                }
            }

            /// M: Set all added or customised view. @ {
            setCustomizeViewProperty();
            /// @ }

            // When this isn't next to wifi, give it some extra padding between the signals.
            mMobileGroup.setPaddingRelative(isSecondaryIcon ? mSecondaryTelephonyPadding : 0,
                    0, 0, 0);
            mMobile.setPaddingRelative(
                    mIsMobileTypeIconWide ? mWideTypeIconStartPadding : mMobileDataIconStartPadding,
                    0, 0, 0);
            mMobileDark.setPaddingRelative(
                    mIsMobileTypeIconWide ? mWideTypeIconStartPadding : mMobileDataIconStartPadding,
                    0, 0, 0);

            if (DEBUG) Log.d(TAG, String.format("mobile: %s sig=%d typ=%d",
                        (mMobileVisible ? "VISIBLE" : "GONE"), mMobileStrengthId, mMobileTypeId));
            /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]BEGIN */
            if (FeatureOption.BIRD_AIS_NETWORKICON_TASK) {
                if (mWifiVisible || mTelecomManager.isInCall()) {
                    mMobileType.setVisibility(View.GONE);
                } else {
                    mMobileType.setVisibility(mMobileTypeId != 0 ? View.VISIBLE : View.GONE);
                }
            } else {
                mMobileType.setVisibility(mMobileTypeId != 0 ? View.VISIBLE : View.GONE);
            }
            /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]END */

            //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
            if (FeatureOption.BIRD_DATA_IN_OUT_ACTIVITY) {
                if (mDataActivityId!=0) {
                    mDataActivityInOut.setImageResource(mDataActivityId);
                    mDataActivityInOut.setColorFilter(R.color.bird_single_color, PorterDuff.Mode.MULTIPLY);
                    mDataActivityInOut.setVisibility(View.VISIBLE);
                } else {
                    mDataActivityInOut.setImageDrawable(null);
                    mDataActivityInOut.setVisibility(View.GONE);
                }
            }
            //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END

            /// M: Add for support plugin featurs. @ {
            setCustomizedOpViews();
            /// @ }

            return mMobileVisible;
        }

        private void updateAnimatableIcon(ImageView view, int resId) {
            maybeStopAnimatableDrawable(view);
            setIconForView(view, resId);
            maybeStartAnimatableDrawable(view);
        }

        private void maybeStopAnimatableDrawable(ImageView view) {
            Drawable drawable = view.getDrawable();

            // Check if the icon has been scaled. If it has retrieve the actual drawable out of the
            // wrapper.
            if (drawable instanceof ScalingDrawableWrapper) {
                drawable = ((ScalingDrawableWrapper) drawable).getDrawable();
            }

            if (drawable instanceof Animatable) {
                Animatable ad = (Animatable) drawable;
                if (ad.isRunning()) {
                    ad.stop();
                }
            }
        }

        private void maybeStartAnimatableDrawable(ImageView view) {
            Drawable drawable = view.getDrawable();

            // Check if the icon has been scaled. If it has retrieve the actual drawable out of the
            // wrapper.
            if (drawable instanceof ScalingDrawableWrapper) {
                drawable = ((ScalingDrawableWrapper) drawable).getDrawable();
            }

            if (drawable instanceof Animatable) {
                Animatable ad = (Animatable) drawable;
                if (ad instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) ad).forceAnimationOnUI();
                }
                if (!ad.isRunning()) {
                    ad.start();
                }
            }
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (mMobileVisible && mMobileGroup != null
                    && mMobileGroup.getContentDescription() != null) {
                event.getText().add(mMobileGroup.getContentDescription());
            }
        }

        public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
            applyDarkIntensity(
                    StatusBarIconController.getDarkIntensity(tintArea, mMobile, darkIntensity),
                    mMobile, mMobileDark);
            setTint(mMobileType, StatusBarIconController.getTint(tintArea, mMobileType, tint));
            setTint(mNetworkType, StatusBarIconController.getTint(tintArea, mNetworkType, tint));
            setTint(mVolteType, StatusBarIconController.getTint(tintArea, mVolteType, tint));
        }

        /// M: Set all added or customised view. @ {
        private void setCustomizeViewProperty() {
            // Add for [Network Type on Statusbar], the place to set network type icon.
            setNetworkIcon();
            /// M: Add for volte icon.
            setVolteIcon();
        }

        /// M: Add for volte icon on Statusbar @{
        private void setVolteIcon() {
            if (mVolteIcon == 0) {
                mVolteType.setVisibility(View.GONE);
            } else {
                mVolteType.setImageResource(mVolteIcon);
                mVolteType.setVisibility(View.VISIBLE);
            }
            /// M: customize VoLTE icon. @{
            mStatusBarExt.setCustomizedVolteView(mVolteIcon, mVolteType);
            /// M: customize VoLTE icon. @}
        }
        ///@}

        /// M : Add for [Network Type on Statusbar]
        private void setNetworkIcon() {
            // Network type is CTA feature, so non CTA project should not set this.
            if (!FeatureOptions.MTK_CTA_SET) {
                //return;//[BIRD][BIRD_REMOVE_NETWORK_ICON][去除信号图标上的网络类型图标(G/3G/4G)][pangmeizhou][20160603]
            }
            //[BIRD][BIRD_REMOVE_NETWORK_ICON][去除信号图标上的网络类型图标(G/3G/4G)][pangmeizhou][20160603]begin
			if(FeatureOption.BIRD_REMOVE_NETWORK_ICON) {
				return;
			}
			//[BIRD][BIRD_REMOVE_NETWORK_ICON][去除信号图标上的网络类型图标(G/3G/4G)][pangmeizhou][20160603]end
            if (mNetworkIcon == 0) {
                mNetworkType.setVisibility(View.GONE);
            } else {
                int networkIcon = mNetworkIcon;
                 //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
                if (FeatureOption.BIRD_SIM_ICON_COLORFUL && mSimCardTintColor != 0 && networkIcon != 0) {
                    mSignalNetworkTypeDrawable = mContext.getResources().getDrawable(networkIcon);
                    mSignalNetworkTypeDrawable.setColorFilter(mSimCardTintColor, PorterDuff.Mode.MULTIPLY);
                    mNetworkType.setImageDrawable(mSignalNetworkTypeDrawable);
                    mSignalNetworkTypeDrawable = null;
                    Log.i(TAG,"BIRD, View: mSignalNetworkType, mSimCardTintColor" + mSimCardTintColor);
                } else {
                    mNetworkType.setImageResource(networkIcon);
                }
                //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END
                //[BIRD][BIRD_SIM_ICON_SCALE_SMALLER][网络标识 4G/3G/E改小][yangheng][20170615] BEGIN
                if (FeatureOption.BIRD_SIM_ICON_SCALE_SMALLER) {
                    mNetworkType.setScaleX(0.6f);
                    mNetworkType.setScaleY(0.6f);
                }
                //[BIRD][BIRD_SIM_ICON_SCALE_SMALLER][网络标识 4G/3G/E改小][yangheng][20170615] END
                
                /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]BEGIN */
                if (FeatureOption.BIRD_AIS_NETWORKICON_TASK) {
                    if (mWifiVisible || mTelecomManager.isInCall()) {
                        mNetworkType.setVisibility(View.GONE);
                        return;
                    }
                }
                /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]END */
                mNetworkType.setVisibility(View.VISIBLE);
            }
        }

        /// M: Add for plugin features. @ {
        private void setCustomizedOpViews() {
            if (mMobileVisible && !mIsAirplaneMode) {
                mPhoneStateExt.getServiceStateForCustomizedView(mSubId);

                mPhoneStateExt.setCustomizedAirplaneView(
                    mNoSimsCombo, mIsAirplaneMode);
                mPhoneStateExt.setCustomizedNetworkTypeView(
                    mSubId, mNetworkIcon, mNetworkType);
                mPhoneStateExt.setCustomizedDataTypeView(
                    mSubId, mMobileTypeId,
                    mDataActivityIn, mDataActivityOut);
                mPhoneStateExt.setCustomizedSignalStrengthView(
                    mSubId, mMobileStrengthId, mMobile);
                mPhoneStateExt.setCustomizedSignalStrengthView(
                    mSubId, mMobileStrengthId, mMobileDark);
                mPhoneStateExt.setCustomizedMobileTypeView(
                    mSubId, mMobileType);
                mPhoneStateExt.setCustomizedView(mSubId);
            }
        }
        /// @ }

        private void hideViewInWfcCase() {
            Log.d(TAG, "hideViewInWfcCase, isWfcEnabled = " + mIsWfcEnable + " mSubId =" + mSubId);
            mMobile.setVisibility(View.GONE);
            mMobileDark.setVisibility(View.GONE);
            mMobileType.setVisibility(View.GONE);
            mNetworkType.setVisibility(View.GONE);
            mIsWfcCase = true;
        }

        private void showViewInWfcCase() {
            Log.d(TAG, "showViewInWfcCase: mSubId = " + mSubId + ", mIsWfcCase=" + mIsWfcCase);
            if (mIsWfcCase) {
                mMobile.setVisibility(View.VISIBLE);
                mMobileDark.setVisibility(View.VISIBLE);
                mMobileType.setVisibility(View.VISIBLE);
                mNetworkType.setVisibility(View.VISIBLE);
                mIsWfcCase = false;
            }
        }
    }
    private PhoneState getOrInflateState(int subId) {
        for (PhoneState state : mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        return inflatePhoneState(subId);
    }

}

