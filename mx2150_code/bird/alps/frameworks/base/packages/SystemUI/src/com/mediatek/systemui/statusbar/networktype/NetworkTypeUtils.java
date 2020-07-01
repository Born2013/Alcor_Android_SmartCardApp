
package com.mediatek.systemui.statusbar.networktype;

import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;

import java.util.HashMap;
import java.util.Map;
//[BIRD][BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE][更换vowifi和volte图标][yangheng][20170605] BEGIN
import com.android.systemui.FeatureOption;
//[BIRD][BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE][更换vowifi和volte图标][yangheng][20170605] END


/**
 * An utility class to access network type.
 */
public class NetworkTypeUtils {
    private static final String TAG = "NetworkTypeUtils";

    //[BIRD][BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE][更换vowifi和volte图标][yangheng][20170605] BEGIN
    public static final int VOLTE_ICON = FeatureOption.BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE ? R.drawable.stat_sys_volte_bird : R.drawable.stat_sys_volte;
    public static final int WFC_ICON = FeatureOption.BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE ? R.drawable.stat_sys_wfc_bird : R.drawable.stat_sys_wfc;
    //[BIRD][BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE][更换vowifi和volte图标][yangheng][20170605] END


    static final Map<Integer, Integer> sNetworkTypeIcons = new HashMap<Integer, Integer>() {
        {
            // For CDMA 3G
            put(TelephonyManager.NETWORK_TYPE_EVDO_0, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_EVDO_A, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_EVDO_B, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_EHRPD, R.drawable.stat_sys_network_type_3g);
            // For CDMA 1x
            put(TelephonyManager.NETWORK_TYPE_CDMA, R.drawable.stat_sys_network_type_1x);
            put(TelephonyManager.NETWORK_TYPE_1xRTT, R.drawable.stat_sys_network_type_1x);
            // Edge
            put(TelephonyManager.NETWORK_TYPE_EDGE, R.drawable.stat_sys_network_type_e);
            // 3G
            put(TelephonyManager.NETWORK_TYPE_UMTS, R.drawable.stat_sys_network_type_3g);
            // For 4G
            //[BIRD][BIRD_SHOW_LTE_FOR_4G][4G网络情况下网络类型图标和数据链接图标都显示为lte而不是4G][pangmeizhou][20161130]begin
            put(TelephonyManager.NETWORK_TYPE_LTE, FeatureOption.BIRD_SHOW_LTE_FOR_4G ? 
                              R.drawable.stat_sys_network_type_lte : R.drawable.stat_sys_network_type_4g);
            //[BIRD][BIRD_SHOW_LTE_FOR_4G][4G网络情况下网络类型图标和数据链接图标都显示为lte而不是4G][pangmeizhou][20161130]end
            // 3G
            put(TelephonyManager.NETWORK_TYPE_HSDPA, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_HSUPA, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_HSPA, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_HSPAP, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_IWLAN, 0);
        }
    };

    /**
     * Map the network type into the related icons.
     * @param serviceState ServiceState to get current network type.
     * @param config Config passed in.
     * @param hasService true for in service.
     * @return Network type's icon.
     */
    public static int getNetworkTypeIcon(ServiceState serviceState, Config config,
            boolean hasService) {
        if (!hasService) {
            // Not in service, no network type.
            return 0;
        }
        int tempNetworkType = getNetworkType(serviceState);

        Integer iconId = sNetworkTypeIcons.get(tempNetworkType);
        if (iconId == null) {
            iconId = tempNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN ? 0 :
                     config.showAtLeast3G ? R.drawable.stat_sys_network_type_3g :
                                            R.drawable.stat_sys_network_type_g;
        }
        Log.d(TAG, "getNetworkTypeIcon iconId = " + iconId);
        return iconId.intValue();
    }

    private static int getNetworkType(ServiceState serviceState) {
        int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (serviceState != null) {
            type = serviceState.getDataNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN ?
                    serviceState.getDataNetworkType() : serviceState.getVoiceNetworkType();
        }
        Log.d(TAG, "getNetworkType: type=" + type);
        return type;
    }

    /// M: Support 4G+ icon" @{
    public static int getDataNetTypeFromServiceState(int srcDataNetType, ServiceState sState){
        int destDataNetType = srcDataNetType;
        if (destDataNetType == TelephonyManager.NETWORK_TYPE_LTE
               || destDataNetType == TelephonyManager.NETWORK_TYPE_LTEA) {
            if (sState != null){
                destDataNetType = (sState.getProprietaryDataRadioTechnology() == 0 ?
                    TelephonyManager.NETWORK_TYPE_LTE : TelephonyManager.NETWORK_TYPE_LTEA);
            }
        }

        Log.d(TAG, "getDataNetTypeFromServiceState:srcDataNetType = "
            + srcDataNetType + ", destDataNetType " + destDataNetType);

        return destDataNetType;
    }
    ///@}
}
