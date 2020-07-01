package com.mediatek.keyguard.ext;

import android.content.Context;
import android.os.SystemProperties;
import com.mediatek.common.PluginImpl ;

/**
 * Default plugin implementation.
 */
@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.ICarrierTextExt")
public class DefaultCarrierTextExt implements ICarrierTextExt {

    @Override
    public CharSequence customizeCarrierTextCapital(CharSequence carrierText) {
        if (carrierText != null) {
            //[BIRD][BIRD_SPN_SHOW_FIRST_CAP_OTHER_LOWER][SPN显示首字母大写,其余字母小写][qianliliang][20160603] BEGIN
            if(SystemProperties.get("ro.bird_spn_custom").equals("1") && carrierText.toString().length() > 1) {
                return carrierText.toString().substring(0,1).toUpperCase() + carrierText.toString().substring(1).toLowerCase();
                //[BIRD][BIRD_SPN_SHOW_ALL_CAP][SPN显示全部字母大写][chenguangxiang][20170707] BEGIN
            } else if(SystemProperties.get("ro.bird_spn_show_cap").equals("1") && carrierText.toString().length() > 1) {
                return carrierText.toString().substring(0,1).toUpperCase() + carrierText.toString().substring(1).toUpperCase();
                //[BIRD][BIRD_SPN_SHOW_ALL_CAP][SPN显示全部字母大写][chenguangxiang][20170707] END
            } else {
                return carrierText.toString().toUpperCase();
            }
            //[BIRD][BIRD_SPN_SHOW_FIRST_CAP_OTHER_LOWER][SPN显示首字母大写,其余字母小写][qianliliang][20160603] END
        }
        return null;
    }

    @Override
    public CharSequence customizeCarrierText(CharSequence carrierText, CharSequence simMessage,
            int simId) {
        return carrierText;
    }

    @Override
    public boolean showCarrierTextWhenSimMissing(boolean isSimMissing, int simId) {
        return isSimMissing;
    }

    /**
     * For CT, display "No SERVICE" when CDMA card type is locked.
     *
     * @param carrierText
     *          the carrier text before customize.
     *
     * @param context
     *          the context of the application.
     *
     * @param phoneId
     *          the phone ID of the customized carrier text.
     *
     * @param isCardLocked
     *          whether is the card is locked.
     *
     * @return the right carrier text when card is locked.
     */
    @Override
    public CharSequence customizeCarrierTextWhenCardTypeLocked(
            CharSequence carrierText, Context context, int phoneId, boolean isCardLocked) {
        return carrierText;
    }

    /**
     * The customized carrier text when SIM is missing.
     *
     * @param carrierText the current carrier text string.
     *
     * @return the customized the carrier text.
     */
    @Override
    public CharSequence customizeCarrierTextWhenSimMissing(CharSequence carrierText) {
        return carrierText;
    }

    /**
     * The customized divider of carrier text.
     *
     * @param divider the current carrier text divider string.
     *
     * @return the customized carrier text divider string.
     */
    @Override
    public String customizeCarrierTextDivider(String divider) {
        return divider;
    }
}
