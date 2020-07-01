/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.res.Resources;
import com.android.internal.telephony.*;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;

import android.os.*;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.telephony.Rlog;

import static com.android.internal.telephony.CommandsInterface.*;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_UT_CFU_NOTIFICATION_MODE;
import static com.android.internal.telephony.TelephonyProperties.UT_CFU_NOTIFICATION_MODE_DISABLED;
import static com.android.internal.telephony.TelephonyProperties.UT_CFU_NOTIFICATION_MODE_OFF;
import static com.android.internal.telephony.TelephonyProperties.UT_CFU_NOTIFICATION_MODE_ON;

import com.android.internal.telephony.gsm.SsData;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
//[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
import java.util.Locale;
import android.os.SystemProperties;
import android.util.Log;
//[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END


/**
 * The motto for this file is:
 *
 * "NOTE:    By using the # as a separator, most cases are expected to be unambiguous."
 *   -- TS 22.030 6.5.2
 *
 * {@hide}
 *
 */
public final class GsmMmiCode extends Handler implements MmiCode {
    static final String LOG_TAG = "GsmMmiCode";

    //***** Constants

    // Max Size of the Short Code (aka Short String from TS 22.030 6.5.2)
    static final int MAX_LENGTH_SHORT_CODE = 2;

    // TS 22.030 6.5.2 Every Short String USSD command will end with #-key
    // (known as #-String)
    static final char END_OF_USSD_COMMAND = '#';

    // From TS 22.030 6.5.2
    static final String ACTION_ACTIVATE = "*";
    static final String ACTION_DEACTIVATE = "#";
    static final String ACTION_INTERROGATE = "*#";
    static final String ACTION_REGISTER = "**";
    static final String ACTION_ERASURE = "##";

    // Supp Service codes from TS 22.030 Annex B

    //Called line presentation
    static final String SC_CLIP    = "30";
    static final String SC_CLIR    = "31";

    // Call Forwarding
    static final String SC_CFU     = "21";
    static final String SC_CFB     = "67";
    static final String SC_CFNRy   = "61";
    static final String SC_CFNR    = "62";

    static final String SC_CF_All = "002";
    static final String SC_CF_All_Conditional = "004";

    // Call Waiting
    static final String SC_WAIT     = "43";

    // Call Barring
    static final String SC_BAOC         = "33";
    static final String SC_BAOIC        = "331";
    static final String SC_BAOICxH      = "332";
    static final String SC_BAIC         = "35";
    static final String SC_BAICr        = "351";

    static final String SC_BA_ALL       = "330";
    static final String SC_BA_MO        = "333";
    static final String SC_BA_MT        = "353";

    // Supp Service Password registration
    static final String SC_PWD          = "03";

    // PIN/PIN2/PUK/PUK2
    static final String SC_PIN          = "04";
    static final String SC_PIN2         = "042";
    static final String SC_PUK          = "05";
    static final String SC_PUK2         = "052";

    ///M:For query CNAP
    static final String SC_CNAP         = "300";

    //Connected line presentation //mtk00732
    static final String SC_COLP    = "76";
    static final String SC_COLR    = "77";
    // mtk00732 add for COLP and COLR
    static final int EVENT_GET_COLR_COMPLETE    = 8;
    static final int EVENT_GET_COLP_COMPLETE    = 9;

    //***** Event Constants

    static final int EVENT_SET_COMPLETE         = 1;
    static final int EVENT_GET_CLIR_COMPLETE    = 2;
    static final int EVENT_QUERY_CF_COMPLETE    = 3;
    static final int EVENT_USSD_COMPLETE        = 4;
    static final int EVENT_QUERY_COMPLETE       = 5;
    static final int EVENT_SET_CFF_COMPLETE     = 6;
    static final int EVENT_USSD_CANCEL_COMPLETE = 7;

    /* M: SS part */
    /// M: [mtk04070][111125][ALPS00093395]MTK added. @{
    static final String PROPERTY_RIL_SIM_PIN1 =  "gsm.sim.retry.pin1";
    static final String PROPERTY_RIL_SIM_PUK1 =  "gsm.sim.retry.puk1";
    static final String PROPERTY_RIL_SIM_PIN2 =  "gsm.sim.retry.pin2";
    static final String PROPERTY_RIL_SIM_PUK2 =  "gsm.sim.retry.puk2";
    static final String PROPERTY_RIL_SIM2_PIN1 =  "gsm.sim.retry.pin1.2";
    static final String PROPERTY_RIL_SIM2_PUK1 =  "gsm.sim.retry.puk1.2";
    static final String PROPERTY_RIL_SIM2_PIN2 =  "gsm.sim.retry.pin2.2";
    static final String PROPERTY_RIL_SIM2_PUK2 =  "gsm.sim.retry.puk2.2";
    static final String RETRY_BLOCKED = "0";

    static final String USSD_HANDLED_BY_STK = "stk";
    /// @}
    /* M: SS part end */

    //***** Instance Variables

    GsmCdmaPhone mPhone;
    Context mContext;
    UiccCardApplication mUiccApplication;
    IccRecords mIccRecords;
    /// M: SS Ut part @{
    private SSRequestDecisionMaker mSSReqDecisionMaker;
    /// @}

    String mAction;              // One of ACTION_*
    String mSc;                  // Service Code
    String mSia, mSib, mSic;       // Service Info a,b,c
    String mPoundString;         // Entire MMI string up to and including #
    public String mDialingNumber;
    String mPwd;                 // For password registration

    /** Set to true in processCode, not at newFromDialString time */
    private boolean mIsPendingUSSD;

    private boolean mIsUssdRequest;

    private boolean mIsCallFwdReg;
    State mState = State.PENDING;
    CharSequence mMessage;
    private boolean mIsSsInfo = false;

    //For ALPS01471897
    private boolean mUserInitiatedMMI = false;
    private int mOrigUtCfuMode = 0;
    //end

    //***** Class Variables


    // See TS 22.030 6.5.2 "Structure of the MMI"

    static Pattern sPatternSuppService = Pattern.compile(
        "((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
/*       1  2                    3          4  5       6   7         8    9     10  11             12

         1 = Full string up to and including #
         2 = action (activation/interrogation/registration/erasure)
         3 = service code
         5 = SIA
         7 = SIB
         9 = SIC
         10 = dialing number
*/

    static final int MATCH_GROUP_POUND_STRING = 1;

    static final int MATCH_GROUP_ACTION = 2;
                        //(activation/interrogation/registration/erasure)

    static final int MATCH_GROUP_SERVICE_CODE = 3;
    static final int MATCH_GROUP_SIA = 5;
    static final int MATCH_GROUP_SIB = 7;
    static final int MATCH_GROUP_SIC = 9;
    static final int MATCH_GROUP_PWD_CONFIRM = 11;
    static final int MATCH_GROUP_DIALING_NUMBER = 12;
    static private String[] sTwoDigitNumberPattern;
    //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
    private int esChange = 0;
    private int esScvalues = 0;
    //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END

    //***** Public Class methods

    /**
     * Some dial strings in GSM are defined to do non-call setup
     * things, such as modify or query supplementary service settings (eg, call
     * forwarding). These are generally referred to as "MMI codes".
     * We look to see if the dial string contains a valid MMI code (potentially
     * with a dial string at the end as well) and return info here.
     *
     * If the dial string contains no MMI code, we return an instance with
     * only "dialingNumber" set
     *
     * Please see flow chart in TS 22.030 6.5.3.2
     */

    public static GsmMmiCode
    newFromDialString(String dialString, GsmCdmaPhone phone, UiccCardApplication app) {
        Matcher m;
        GsmMmiCode ret = null;

        m = sPatternSuppService.matcher(dialString);

        // Is this formatted like a standard supplementary service code?
        if (m.matches()) {
            ret = new GsmMmiCode(phone, app);
            ret.mPoundString = makeEmptyNull(m.group(MATCH_GROUP_POUND_STRING));
            ret.mAction = makeEmptyNull(m.group(MATCH_GROUP_ACTION));
            ret.mSc = makeEmptyNull(m.group(MATCH_GROUP_SERVICE_CODE));
            ret.mSia = makeEmptyNull(m.group(MATCH_GROUP_SIA));
            ret.mSib = makeEmptyNull(m.group(MATCH_GROUP_SIB));
            ret.mSic = makeEmptyNull(m.group(MATCH_GROUP_SIC));
            ret.mPwd = makeEmptyNull(m.group(MATCH_GROUP_PWD_CONFIRM));
            ret.mDialingNumber = makeEmptyNull(m.group(MATCH_GROUP_DIALING_NUMBER));
            // According to TS 22.030 6.5.2 "Structure of the MMI",
            // the dialing number should not ending with #.
            // The dialing number ending # is treated as unique USSD,
            // eg, *400#16 digit number# to recharge the prepaid card
            // in India operator(Mumbai MTNL)
            if(ret.mDialingNumber != null &&
                    ret.mDialingNumber.endsWith("#") &&
                    dialString.endsWith("#")){
                ret = new GsmMmiCode(phone, app);
                ret.mPoundString = dialString;
            }
        } else if (dialString.endsWith("#")) {
            // TS 22.030 sec 6.5.3.2
            // "Entry of any characters defined in the 3GPP TS 23.038 [8] Default Alphabet
            // (up to the maximum defined in 3GPP TS 24.080 [10]), followed by #SEND".

            ret = new GsmMmiCode(phone, app);
            ret.mPoundString = dialString;
        } else if (isTwoDigitShortCode(phone.getContext(), dialString)) {
            //Is a country-specific exception to short codes as defined in TS 22.030, 6.5.3.2
            ret = null;
        } else if (isShortCode(dialString, phone)) {
            // this may be a short code, as defined in TS 22.030, 6.5.3.2
            ret = new GsmMmiCode(phone, app);
            ret.mDialingNumber = dialString;
        }

        return ret;
    }

    public static GsmMmiCode
    newNetworkInitiatedUssd(String ussdMessage,
                            boolean isUssdRequest, GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret;

        ret = new GsmMmiCode(phone, app);

        ret.mMessage = ussdMessage;
        ret.mIsUssdRequest = isUssdRequest;

        // If it's a request, set to PENDING so that it's cancelable.
        if (isUssdRequest) {
            ret.mIsPendingUSSD = true;
            ret.mState = State.PENDING;
        } else {
            ret.mState = State.COMPLETE;
        }

        return ret;
    }

    public static GsmMmiCode newFromUssdUserInput(String ussdMessge,
                                                  GsmCdmaPhone phone,
                                                  UiccCardApplication app) {
        GsmMmiCode ret = new GsmMmiCode(phone, app);

        ret.mMessage = ussdMessge;
        ret.mState = State.PENDING;
        ret.mIsPendingUSSD = true;

        return ret;
    }

    /** Process SS Data */
    public void
    processSsData(AsyncResult data) {
        Rlog.d(LOG_TAG, "In processSsData");

        mIsSsInfo = true;
        try {
            SsData ssData = (SsData)data.result;
            parseSsData(ssData);
        } catch (ClassCastException ex) {
            Rlog.e(LOG_TAG, "Class Cast Exception in parsing SS Data : " + ex);
        } catch (NullPointerException ex) {
            Rlog.e(LOG_TAG, "Null Pointer Exception in parsing SS Data : " + ex);
        }
    }

    void parseSsData(SsData ssData) {
        CommandException ex;

        ex = CommandException.fromRilErrno(ssData.result);
        mSc = getScStringFromScType(ssData.serviceType);
        mAction = getActionStringFromReqType(ssData.requestType);
        Rlog.d(LOG_TAG, "parseSsData msc = " + mSc + ", action = " + mAction + ", ex = " + ex);

        switch (ssData.requestType) {
            case SS_ACTIVATION:
            case SS_DEACTIVATION:
            case SS_REGISTRATION:
            case SS_ERASURE:
                if ((ssData.result == RILConstants.SUCCESS) &&
                      ssData.serviceType.isTypeUnConditional()) {
                    /*
                     * When ServiceType is SS_CFU/SS_CF_ALL and RequestType is activate/register
                     * and ServiceClass is Voice/None, set IccRecords.setVoiceCallForwardingFlag.
                     * Only CF status can be set here since number is not available.
                     */
                    boolean cffEnabled = ((ssData.requestType == SsData.RequestType.SS_ACTIVATION ||
                            ssData.requestType == SsData.RequestType.SS_REGISTRATION) &&
                            isServiceClassVoiceorNone(ssData.serviceClass));

                    Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag cffEnabled: " + cffEnabled);
                    if (mIccRecords != null) {
                        mPhone.setVoiceCallForwardingFlag(1, cffEnabled, null);
                        Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag done from SS Info.");
                    } else {
                        Rlog.e(LOG_TAG, "setVoiceCallForwardingFlag aborted. sim records is null.");
                    }
                }
                onSetComplete(null, new AsyncResult(null, ssData.cfInfo, ex));
                break;
            case SS_INTERROGATION:
                if (ssData.serviceType.isTypeClir()) {
                    Rlog.d(LOG_TAG, "CLIR INTERROGATION");
                    onGetClirComplete(new AsyncResult(null, ssData.ssInfo, ex));
                } else if (ssData.serviceType.isTypeCF()) {
                    Rlog.d(LOG_TAG, "CALL FORWARD INTERROGATION");
                    onQueryCfComplete(new AsyncResult(null, ssData.cfInfo, ex));
                } else {
                    onQueryComplete(new AsyncResult(null, ssData.ssInfo, ex));
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Invaid requestType in SSData : " + ssData.requestType);
                break;
        }
    }

    private String getScStringFromScType(SsData.ServiceType sType) {
        switch (sType) {
            case SS_CFU:
                return SC_CFU;
            case SS_CF_BUSY:
                return SC_CFB;
            case SS_CF_NO_REPLY:
                return SC_CFNRy;
            case SS_CF_NOT_REACHABLE:
                return SC_CFNR;
            case SS_CF_ALL:
                return SC_CF_All;
            case SS_CF_ALL_CONDITIONAL:
                return SC_CF_All_Conditional;
            case SS_CLIP:
                return SC_CLIP;
            case SS_CLIR:
                return SC_CLIR;
            case SS_WAIT:
                return SC_WAIT;
            case SS_BAOC:
                return SC_BAOC;
            case SS_BAOIC:
                return SC_BAOIC;
            case SS_BAOIC_EXC_HOME:
                return SC_BAOICxH;
            case SS_BAIC:
                return SC_BAIC;
            case SS_BAIC_ROAMING:
                return SC_BAICr;
            case SS_ALL_BARRING:
                return SC_BA_ALL;
            case SS_OUTGOING_BARRING:
                return SC_BA_MO;
            case SS_INCOMING_BARRING:
                return SC_BA_MT;
        }

        return "";
    }

    private String getActionStringFromReqType(SsData.RequestType rType) {
        switch (rType) {
            case SS_ACTIVATION:
                return ACTION_ACTIVATE;
            case SS_DEACTIVATION:
                return ACTION_DEACTIVATE;
            case SS_INTERROGATION:
                return ACTION_INTERROGATE;
            case SS_REGISTRATION:
                return ACTION_REGISTER;
            case SS_ERASURE:
                return ACTION_ERASURE;
        }

        return "";
    }

    private boolean isServiceClassVoiceorNone(int serviceClass) {
        return (((serviceClass & CommandsInterface.SERVICE_CLASS_VOICE) != 0) ||
                (serviceClass == CommandsInterface.SERVICE_CLASS_NONE));
    }

    //***** Private Class methods

    /** make empty strings be null.
     *  Regexp returns empty strings for empty groups
     */
    private static String
    makeEmptyNull (String s) {
        if (s != null && s.length() == 0) return null;

        return s;
    }

    /** returns true of the string is empty or null */
    private static boolean
    isEmptyOrNull(CharSequence s) {
        return s == null || (s.length() == 0);
    }


    private static int
    scToCallForwardReason(String sc) {
        if (sc == null) {
            throw new RuntimeException ("invalid call forward sc");
        }

        if (sc.equals(SC_CF_All)) {
           return CommandsInterface.CF_REASON_ALL;
        } else if (sc.equals(SC_CFU)) {
            return CommandsInterface.CF_REASON_UNCONDITIONAL;
        } else if (sc.equals(SC_CFB)) {
            return CommandsInterface.CF_REASON_BUSY;
        } else if (sc.equals(SC_CFNR)) {
            return CommandsInterface.CF_REASON_NOT_REACHABLE;
        } else if (sc.equals(SC_CFNRy)) {
            return CommandsInterface.CF_REASON_NO_REPLY;
        } else if (sc.equals(SC_CF_All_Conditional)) {
           return CommandsInterface.CF_REASON_ALL_CONDITIONAL;
        } else {
            throw new RuntimeException ("invalid call forward sc");
        }
    }

    private static int
    siToServiceClass(String si) {
        if (si == null || si.length() == 0) {
                return  SERVICE_CLASS_NONE;
        } else {
            // NumberFormatException should cause MMI fail
            int serviceCode = Integer.parseInt(si, 10);

            switch (serviceCode) {
                case 10: return SERVICE_CLASS_SMS + SERVICE_CLASS_FAX  + SERVICE_CLASS_VOICE;
                case 11: return SERVICE_CLASS_VOICE;
                case 12: return SERVICE_CLASS_SMS + SERVICE_CLASS_FAX;
                case 13: return SERVICE_CLASS_FAX;

                case 16: return SERVICE_CLASS_SMS;

                case 19: return SERVICE_CLASS_FAX + SERVICE_CLASS_VOICE;
/*
    Note for code 20:
     From TS 22.030 Annex C:
                "All GPRS bearer services" are not included in "All tele and bearer services"
                    and "All bearer services"."
....so SERVICE_CLASS_DATA, which (according to 27.007) includes GPRS
*/
                case 20: return SERVICE_CLASS_DATA_ASYNC + SERVICE_CLASS_DATA_SYNC;

                case 21: return SERVICE_CLASS_PAD + SERVICE_CLASS_DATA_ASYNC;
                case 22: return SERVICE_CLASS_PACKET + SERVICE_CLASS_DATA_SYNC;
                //case 24: return SERVICE_CLASS_DATA_SYNC;
                case 24: return SERVICE_CLASS_DATA_SYNC + SERVICE_CLASS_VIDEO;
                case 25: return SERVICE_CLASS_DATA_ASYNC;
                case 26: return SERVICE_CLASS_DATA_SYNC + SERVICE_CLASS_VOICE;
                case 99: return SERVICE_CLASS_PACKET;

                default:
                    throw new RuntimeException("unsupported MMI service code " + si);
            }
        }
    }

    private static int
    siToTime (String si) {
        if (si == null || si.length() == 0) {
            return 0;
        } else {
            // NumberFormatException should cause MMI fail
            return Integer.parseInt(si, 10);
        }
    }

    static boolean
    isServiceCodeCallForwarding(String sc) {
        return sc != null &&
                (sc.equals(SC_CFU)
                || sc.equals(SC_CFB) || sc.equals(SC_CFNRy)
                || sc.equals(SC_CFNR) || sc.equals(SC_CF_All)
                || sc.equals(SC_CF_All_Conditional));
    }

    static boolean
    isServiceCodeCallBarring(String sc) {
        Resources resource = Resources.getSystem();
        if (sc != null) {
            String[] barringMMI = resource.getStringArray(
                com.android.internal.R.array.config_callBarringMMI);
            if (barringMMI != null) {
                for (String match : barringMMI) {
                    if (sc.equals(match)) return true;
                }
            }
        }
        return false;
    }

    static String
    scToBarringFacility(String sc) {
        if (sc == null) {
            throw new RuntimeException ("invalid call barring sc");
        }

        if (sc.equals(SC_BAOC)) {
            return CommandsInterface.CB_FACILITY_BAOC;
        } else if (sc.equals(SC_BAOIC)) {
            return CommandsInterface.CB_FACILITY_BAOIC;
        } else if (sc.equals(SC_BAOICxH)) {
            return CommandsInterface.CB_FACILITY_BAOICxH;
        } else if (sc.equals(SC_BAIC)) {
            return CommandsInterface.CB_FACILITY_BAIC;
        } else if (sc.equals(SC_BAICr)) {
            return CommandsInterface.CB_FACILITY_BAICr;
        } else if (sc.equals(SC_BA_ALL)) {
            return CommandsInterface.CB_FACILITY_BA_ALL;
        } else if (sc.equals(SC_BA_MO)) {
            return CommandsInterface.CB_FACILITY_BA_MO;
        } else if (sc.equals(SC_BA_MT)) {
            return CommandsInterface.CB_FACILITY_BA_MT;
        } else {
            throw new RuntimeException ("invalid call barring sc");
        }
    }

    //***** Constructor

    public GsmMmiCode(GsmCdmaPhone phone, UiccCardApplication app) {
        // The telephony unit-test cases may create GsmMmiCode's
        // in secondary threads
        super(phone.getHandler().getLooper());
        mPhone = phone;
        mContext = phone.getContext();
        mUiccApplication = app;
        if (app != null) {
            mIccRecords = app.getIccRecords();
        }

        /// M: SS Ut part @{
        mSSReqDecisionMaker = mPhone.getSSRequestDecisionMaker();
        /// @}
    }

    //***** MmiCode implementation

    @Override
    public State
    getState() {
        return mState;
    }

    @Override
    public CharSequence
    getMessage() {
        return mMessage;
    }

    public Phone
    getPhone() {
        return ((Phone) mPhone);
    }

    //For ALPS01471897
    public void setUserInitiatedMMI(boolean userinit)
    {
       mUserInitiatedMMI = userinit;
    }

    public boolean getUserInitiatedMMI() {
       return mUserInitiatedMMI;
    }

    // inherited javadoc suffices
    @Override
    public void
    cancel() {
        mPhone.mIsNetworkInitiatedUssr = false;
        // Complete or failed cannot be cancelled
        if (mState == State.COMPLETE || mState == State.FAILED) {
            return;
        }

        mState = State.CANCELLED;

        if (mIsPendingUSSD) {
            /*
             * There can only be one pending USSD session, so tell the radio to
             * cancel it.
             */
            mPhone.mCi.cancelPendingUssd(obtainMessage(EVENT_USSD_CANCEL_COMPLETE, this));

            /*
             * Don't call phone.onMMIDone here; wait for CANCEL_COMPLETE notice
             * from RIL.
             */
        } else {
            // TODO in cases other than USSD, it would be nice to cancel
            // the pending radio operation. This requires RIL cancellation
            // support, which does not presently exist.

            mPhone.onMMIDone (this);
        }

    }

    @Override
    public boolean isCancelable() {
        /* Can only cancel pending USSD sessions. */
        return mIsPendingUSSD;
    }

    //***** Instance Methods

    /** Does this dial string contain a structured or unstructured MMI code? */
    boolean
    isMMI() {
        return mPoundString != null;
    }

    /* Is this a 1 or 2 digit "short code" as defined in TS 22.030 sec 6.5.3.2? */
    boolean
    isShortCode() {
        return mPoundString == null
                    && mDialingNumber != null && mDialingNumber.length() <= 2;

    }

    static private boolean
    isTwoDigitShortCode(Context context, String dialString) {
        Rlog.d(LOG_TAG, "isTwoDigitShortCode");

        if (dialString == null || dialString.length() > 2) return false;

        if (sTwoDigitNumberPattern == null) {
            sTwoDigitNumberPattern = context.getResources().getStringArray(
                    com.android.internal.R.array.config_twoDigitNumberPattern);
        }

        for (String dialnumber : sTwoDigitNumberPattern) {
            Rlog.d(LOG_TAG, "Two Digit Number Pattern " + dialnumber);
            if (dialString.equals(dialnumber)) {
                Rlog.d(LOG_TAG, "Two Digit Number Pattern -true");
                return true;
            }
        }
        Rlog.d(LOG_TAG, "Two Digit Number Pattern -false");
        return false;
    }

    /**
     * Helper function for newFromDialString. Returns true if dialString appears
     * to be a short code AND conditions are correct for it to be treated as
     * such.
     */
    static private boolean isShortCode(String dialString, GsmCdmaPhone phone) {
        // Refer to TS 22.030 Figure 3.5.3.2:
        if (dialString == null) {
            return false;
        }

        // Illegal dial string characters will give a ZERO length.
        // At this point we do not want to crash as any application with
        // call privileges may send a non dial string.
        // It return false as when the dialString is equal to NULL.
        if (dialString.length() == 0) {
            return false;
        }

        if (PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), dialString)) {
            return false;
        } else {
            return isShortCodeUSSD(dialString, phone);
        }
    }

    /**
     * Helper function for isShortCode. Returns true if dialString appears to be
     * a short code and it is a USSD structure
     *
     * According to the 3PGG TS 22.030 specification Figure 3.5.3.2: A 1 or 2
     * digit "short code" is treated as USSD if it is entered while on a call or
     * does not satisfy the condition (exactly 2 digits && starts with '1'), there
     * are however exceptions to this rule (see below)
     *
     * Exception (1) to Call initiation is: If the user of the device is already in a call
     * and enters a Short String without any #-key at the end and the length of the Short String is
     * equal or less then the MAX_LENGTH_SHORT_CODE [constant that is equal to 2]
     *
     * The phone shall initiate a USSD/SS commands.
     */
    static private boolean isShortCodeUSSD(String dialString, GsmCdmaPhone phone) {
        if (dialString != null && dialString.length() <= MAX_LENGTH_SHORT_CODE) {
            if (phone.isInCall()) {
                return true;
            }

            if (dialString.length() != MAX_LENGTH_SHORT_CODE ||
                    dialString.charAt(0) != '1') {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the Service Code is PIN/PIN2/PUK/PUK2-related
     */
    public boolean isPinPukCommand() {
        return mSc != null && (mSc.equals(SC_PIN) || mSc.equals(SC_PIN2)
                              || mSc.equals(SC_PUK) || mSc.equals(SC_PUK2));
     }

    /**
     * See TS 22.030 Annex B.
     * In temporary mode, to suppress CLIR for a single call, enter:
     *      " * 31 # [called number] SEND "
     *  In temporary mode, to invoke CLIR for a single call enter:
     *       " # 31 # [called number] SEND "
     */
    public boolean
    isTemporaryModeCLIR() {
        return mSc != null && mSc.equals(SC_CLIR) && mDialingNumber != null
                && (isActivate() || isDeactivate());
    }

    /**
     * returns CommandsInterface.CLIR_*
     * See also isTemporaryModeCLIR()
     */
    public int
    getCLIRMode() {
        if (mSc != null && mSc.equals(SC_CLIR)) {
            if (isActivate()) {
                return CommandsInterface.CLIR_SUPPRESSION;
            } else if (isDeactivate()) {
                return CommandsInterface.CLIR_INVOCATION;
            }
        }

        return CommandsInterface.CLIR_DEFAULT;
    }

    boolean isActivate() {
        return mAction != null && mAction.equals(ACTION_ACTIVATE);
    }

    boolean isDeactivate() {
        return mAction != null && mAction.equals(ACTION_DEACTIVATE);
    }

    boolean isInterrogate() {
        return mAction != null && mAction.equals(ACTION_INTERROGATE);
    }

    boolean isRegister() {
        return mAction != null && mAction.equals(ACTION_REGISTER);
    }

    boolean isErasure() {
        return mAction != null && mAction.equals(ACTION_ERASURE);
    }

    /**
     * Returns true if this is a USSD code that's been submitted to the
     * network...eg, after processCode() is called
     */
    public boolean isPendingUSSD() {
        return mIsPendingUSSD;
    }

    @Override
    public boolean isUssdRequest() {
        return mIsUssdRequest;
    }

    public boolean isSsInfo() {
        return mIsSsInfo;
    }

    /** Process a MMI code or short code...anything that isn't a dialing number */
    public void
    processCode() throws CallStateException {
        try {
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "isShortCode");
                // These just get treated as USSD.
                sendUssd(mDialingNumber);
            } else if (mDialingNumber != null) {
                // We should have no dialing numbers here
                /* M: SS part */
                /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{
                Rlog.w(LOG_TAG, "Special USSD Support:" + mPoundString + mDialingNumber);
                sendUssd(mPoundString + mDialingNumber);
                //throw new RuntimeException ("Invalid or Unsupported MMI Code");
                /// @}
            } else if (mSc != null && mSc.equals(SC_CNAP) && isInterrogate()) {
                Rlog.d(LOG_TAG, "is CNAP");
                if (mPoundString != null) {
                    sendCNAPSS(mPoundString);
                }
                /* M: SS part end */
            } else if (mSc != null && mSc.equals(SC_CLIP)) {
                Rlog.d(LOG_TAG, "is CLIP");
                /* M: SS part  */ /*M: patch back CLIP*/
                if (isActivate()) {
                    Rlog.d(LOG_TAG, "is CLIP - isActivate");
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.setCLIP(1,
                                obtainMessage(EVENT_SET_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.setCLIP(true, obtainMessage(EVENT_SET_COMPLETE, this));
                } else if (isDeactivate()) {
                    Rlog.d(LOG_TAG, "is CLIP - isDeactivate");
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.setCLIP(0,
                                obtainMessage(EVENT_SET_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.setCLIP(false, obtainMessage(EVENT_SET_COMPLETE, this));
                } else if (isInterrogate()) {
                /* M: SS part end */
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.getCLIP(obtainMessage(EVENT_QUERY_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.queryCLIP(
                            obtainMessage(EVENT_QUERY_COMPLETE, this));
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && mSc.equals(SC_CLIR)) {
                Rlog.d(LOG_TAG, "is CLIR");
                if (isActivate()) {
                    if (mPhone.isOpTbClir()) {
                        mPhone.mCi.setCLIR(CommandsInterface.CLIR_INVOCATION,
                        obtainMessage(EVENT_SET_COMPLETE, this));
                        return;
                    }

                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.setCLIR(CommandsInterface.CLIR_INVOCATION,
                                obtainMessage(EVENT_SET_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.setCLIR(CommandsInterface.CLIR_INVOCATION,
                        obtainMessage(EVENT_SET_COMPLETE, this));
                } else if (isDeactivate()) {
                    if (mPhone.isOpTbClir()) {
                        mPhone.mCi.setCLIR(CommandsInterface.CLIR_SUPPRESSION,
                        obtainMessage(EVENT_SET_COMPLETE, this));
                        return;
                    }

                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.setCLIR(CommandsInterface.CLIR_SUPPRESSION,
                                obtainMessage(EVENT_SET_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.setCLIR(CommandsInterface.CLIR_SUPPRESSION,
                        obtainMessage(EVENT_SET_COMPLETE, this));
                } else if (isInterrogate()) {
                    if (mPhone.isOpTbClir()) {
                        mPhone.mCi.getCLIR(
                            obtainMessage(EVENT_GET_CLIR_COMPLETE, this));
                        return;
                    }

                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.getCLIR(obtainMessage(EVENT_GET_CLIR_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.getCLIR(
                        obtainMessage(EVENT_GET_CLIR_COMPLETE, this));
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            /* M: SS part */
            /// M: [mtk04070][111125][ALPS00093395]MTK added for COLP and COLR. @{
            } else if (mSc != null && mSc.equals(SC_COLP)) {
                Rlog.d(LOG_TAG, "is COLP");
                if (isInterrogate()) {
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.getCOLP(obtainMessage(EVENT_GET_COLP_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.getCOLP(obtainMessage(EVENT_GET_COLP_COMPLETE, this));
                }
                // SET COLP as *76# or #76# is not supported.
                //else if (isActivate()) {
                //    mPhone.mCM.setCOLP(true,obtainMessage(EVENT_SET_COMPLETE, this));
                //} else if (isDeactivate()) {
                //    mPhone.mCM.setCOLP(false,obtainMessage(EVENT_SET_COMPLETE, this));
                //}
                else {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && mSc.equals(SC_COLR)) {
                Rlog.d(LOG_TAG, "is COLR");
                if (isInterrogate()) {
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.getCOLR(obtainMessage(EVENT_GET_COLR_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.getCOLR(obtainMessage(EVENT_GET_COLR_COMPLETE, this));
                } else {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
            /// @}
            /* M: SS part end */
            } else if (isServiceCodeCallForwarding(mSc)) {
                Rlog.d(LOG_TAG, "is CF");

                String dialingNumber = mSia;
                int serviceClass = siToServiceClass(mSib);
                int reason = scToCallForwardReason(mSc);
                int time = siToTime(mSic);

                if (isInterrogate()) {
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.queryCallForwardStatus(reason, serviceClass,
                                dialingNumber, obtainMessage(EVENT_QUERY_CF_COMPLETE, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.queryCallForwardStatus(
                            reason, serviceClass,  dialingNumber,
                                obtainMessage(EVENT_QUERY_CF_COMPLETE, this));
                } else {
                    int cfAction;

                    if (isActivate()) {
                        // 3GPP TS 22.030 6.5.2
                        // a call forwarding request with a single * would be
                        // interpreted as registration if containing a forwarded-to
                        // number, or an activation if not
                        if (isEmptyOrNull(dialingNumber)) {
                            cfAction = CommandsInterface.CF_ACTION_ENABLE;
                            mIsCallFwdReg = false;
                        } else {
                            cfAction = CommandsInterface.CF_ACTION_REGISTRATION;
                            mIsCallFwdReg = true;
                        }
                    } else if (isDeactivate()) {
                        cfAction = CommandsInterface.CF_ACTION_DISABLE;
                    } else if (isRegister()) {
                        cfAction = CommandsInterface.CF_ACTION_REGISTRATION;
                    } else if (isErasure()) {
                        cfAction = CommandsInterface.CF_ACTION_ERASURE;
                    } else {
                        throw new RuntimeException ("invalid action");
                    }

                    int isSettingUnconditionalVoice =
                        (((reason == CommandsInterface.CF_REASON_UNCONDITIONAL) ||
                                (reason == CommandsInterface.CF_REASON_ALL)) &&
                                (((serviceClass & CommandsInterface.SERVICE_CLASS_VOICE) != 0) ||
                                 (serviceClass == CommandsInterface.SERVICE_CLASS_NONE))) ? 1 : 0;

                    int isEnableDesired =
                        ((cfAction == CommandsInterface.CF_ACTION_ENABLE) ||
                                (cfAction == CommandsInterface.CF_ACTION_REGISTRATION)) ? 1 : 0;

                    Rlog.d(LOG_TAG, "is CF setCallForward");
                    if (isSettingUnconditionalVoice == 1) {
                        mOrigUtCfuMode = 0;
                        String utCfuMode;
                        utCfuMode = mPhone.getSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_DISABLED);
                        if (UT_CFU_NOTIFICATION_MODE_ON.equals(utCfuMode)) {
                            mOrigUtCfuMode = 1;
                        } else if (UT_CFU_NOTIFICATION_MODE_OFF.equals(utCfuMode)) {
                            mOrigUtCfuMode = 2;
                        }

                        mPhone.setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_DISABLED);
                    }
                    /// M: SS Ut part @{
                    if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                            mPhone.isGsmUtSupport()) {
                        mSSReqDecisionMaker.setCallForward(cfAction, reason, serviceClass,
                            dialingNumber, time, obtainMessage(
                                    EVENT_SET_CFF_COMPLETE,
                                    isSettingUnconditionalVoice,
                                    isEnableDesired, this));
                        return;
                    }
                    /// @}
                    if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    mPhone.mCi.setCallForward(cfAction, reason, serviceClass,
                            dialingNumber, time, obtainMessage(
                                    EVENT_SET_CFF_COMPLETE,
                                    isSettingUnconditionalVoice,
                                    isEnableDesired, this));
                }
            } else if (isServiceCodeCallBarring(mSc)) {
                // sia = password
                // sib = basic service group

                String password = mSia;
                int serviceClass = siToServiceClass(mSib);
                String facility = scToBarringFacility(mSc);

                /* M: SS part */
                /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{
                if (isInterrogate()) {
                    if (password == null) {
                        /// M: SS Ut part @{
                        if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                                mPhone.isGsmUtSupport()) {
                            mSSReqDecisionMaker.queryFacilityLock(facility, password,
                                    serviceClass, obtainMessage(EVENT_QUERY_COMPLETE, this));
                            return;
                        }
                        /// @}
                        if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                            mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                        }
                    mPhone.mCi.queryFacilityLock(facility, password,
                            serviceClass, obtainMessage(EVENT_QUERY_COMPLETE, this));
                    } else {
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                } else if (isActivate() || isDeactivate()) {
                    if ((password != null) && (password.length() == 4)) {
                        /// M: SS Ut part @{
                        if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) &&
                                mPhone.isGsmUtSupport()) {
                            mSSReqDecisionMaker.setFacilityLock(facility, isActivate(), password,
                                    serviceClass, obtainMessage(EVENT_SET_COMPLETE, this));
                            return;
                        }
                        /// @}
                        if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                            mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                        }
                    mPhone.mCi.setFacilityLock(facility, isActivate(), password,
                            serviceClass, obtainMessage(EVENT_SET_COMPLETE, this));
                } else {
                        handlePasswordError(com.android.internal.R.string.passwordIncorrect);
                    }
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
                /// @}
                /* M: SS part end */
            } else if (mSc != null && mSc.equals(SC_PWD)) {
                // sia = fac
                // sib = old pwd
                // sic = new pwd
                // pwd = new pwd
                String facility;
                String oldPwd = mSib;
                String newPwd = mSic;
                if (isActivate() || isRegister()) {
                    // Even though ACTIVATE is acceptable, this is really termed a REGISTER
                    mAction = ACTION_REGISTER;

                    if (mSia == null) {
                        // If sc was not specified, treat it as BA_ALL.
                        facility = CommandsInterface.CB_FACILITY_BA_ALL;
                    } else {
                        facility = scToBarringFacility(mSia);
                    }
                    /* M: SS part */
                    /// M: Check password in network side. @{
                    if ((oldPwd != null) && (newPwd != null) && (mPwd != null)) {
                        if ((mPwd.length() != newPwd.length())
                            || (oldPwd.length() != 4) || (mPwd.length() != 4)) {
                             handlePasswordError(com.android.internal.R.string.passwordIncorrect);
                        } else {
                            if (mPhone.isDuringImsCall()) {
                                Message msg = obtainMessage(EVENT_SET_COMPLETE, this);
                                CommandException ce = new CommandException(
                                        CommandException.Error.GENERIC_FAILURE);
                                AsyncResult.forMessage(msg, null, ce);
                                msg.sendToTarget();
                            } else {
                                /* From test spec 51.010-1 31.8.1.2.3,
                                 * we shall not compare pwd here. Let pwd check in NW side.
                                 */
                                mPhone.mCi.changeBarringPassword(facility, oldPwd,
                                        newPwd, mPwd, obtainMessage(EVENT_SET_COMPLETE, this));
                            }
                        }
                    } else {
                        // password mismatch; return error
                        handlePasswordError(com.android.internal.R.string.passwordIncorrect);
                    }
                    /// @}
                    /* M: SS part end */
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }

            } else if (mSc != null && mSc.equals(SC_WAIT)) {
                // sia = basic service group
                int serviceClass = siToServiceClass(mSia);

                int tbcwMode = mPhone.getTbcwMode();

                if (isActivate() || isDeactivate()) {
                    if ((tbcwMode == GsmCdmaPhone.TBCW_OPTBCW_VOLTE_USER) &&
                            !mPhone.isOpNwCW()) {
                        mPhone.setTerminalBasedCallWaiting(isActivate(),
                                obtainMessage(EVENT_SET_COMPLETE, this));
                    } else if (tbcwMode == GsmCdmaPhone.TBCW_OPTBCW_NOT_VOLTE_USER
                            || tbcwMode == GsmCdmaPhone.TBCW_OPTBCW_WITH_CS) {
                        if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                            mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                        }
                        mPhone.mCi.setCallWaiting(isActivate(), serviceClass,
                                obtainMessage(EVENT_SET_COMPLETE,
                                    isActivate() ? 1 : 0, -1, this));
                    } else {
                        Rlog.d(LOG_TAG, "processCode  setCallWaiting");
                        if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                                && mPhone.isGsmUtSupport()) {
                            mSSReqDecisionMaker.setCallWaiting(isActivate(), serviceClass,
                                    obtainMessage(EVENT_SET_COMPLETE, this));
                        } else {
                            mPhone.mCi.setCallWaiting(isActivate(), serviceClass,
                                    obtainMessage(EVENT_SET_COMPLETE, this));
                        }
                    }
                } else if (isInterrogate()) {
                    if ((tbcwMode == GsmCdmaPhone.TBCW_OPTBCW_VOLTE_USER)
                        && !mPhone.isOpNwCW()) {
                        mPhone.getTerminalBasedCallWaiting(
                                obtainMessage(EVENT_QUERY_COMPLETE, this));
                    } else if (tbcwMode == GsmCdmaPhone.TBCW_OPTBCW_NOT_VOLTE_USER
                            || tbcwMode == GsmCdmaPhone.TBCW_OPTBCW_WITH_CS) {
                        if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                            mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                        }
                        mPhone.mCi.queryCallWaiting(serviceClass,
                                obtainMessage(EVENT_QUERY_COMPLETE, this));
                    } else {
                        if ((mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                                && mPhone.isGsmUtSupport()) {
                            mSSReqDecisionMaker.queryCallWaiting(serviceClass,
                                    obtainMessage(EVENT_QUERY_COMPLETE, this));
                        } else {
                            mPhone.mCi.queryCallWaiting(serviceClass,
                                    obtainMessage(EVENT_QUERY_COMPLETE, this));
                        }
                    }
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (isPinPukCommand()) {
                // TODO: This is the same as the code in CmdaMmiCode.java,
                // MmiCode should be an abstract or base class and this and
                // other common variables and code should be promoted.

                /* M: SS part */
               /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{
               Rlog.d(LOG_TAG, "is PIN command");

                // sia = old PIN or PUK
                // sib = new PIN
                // sic = new PIN
                String oldPinOrPuk = mSia;
                String newPinOrPuk = mSib;
                int pinLen = newPinOrPuk != null ? newPinOrPuk.length() : 0;

                // int pinLen = newPin.length();
                // int oldPinLen = oldPinOrPuk.length();
                String retryPin1;
                String retryPin2;
                String retryPuk1;
                String retryPuk2;

                int phoneId = mPhone.getPhoneId();

                //Support multi sim design.
                StringBuilder appendStr = new StringBuilder();

                if (phoneId != 0) {
                    appendStr.append(".").append(phoneId + 1);
                }

                retryPin1 = SystemProperties.get("gsm.sim.retry.pin1" + appendStr.toString(),
                    null);
                retryPin2 = SystemProperties.get("gsm.sim.retry.pin2" + appendStr.toString(),
                    null);
                retryPuk1 = SystemProperties.get("gsm.sim.retry.puk1" + appendStr.toString(),
                    null);
                retryPuk2 = SystemProperties.get("gsm.sim.retry.puk2" + appendStr.toString(),
                    null);

                Rlog.d(LOG_TAG, "retryPin1:" + retryPin1 + "\n"
                                          + "retryPin2:" + retryPin2 + "\n"
                                          + "retryPuk1:" + retryPuk1 + "\n"
                                          + "retryPuk2:" + retryPuk2 + "\n");
                if (isRegister()) {
                    if (newPinOrPuk == null || oldPinOrPuk == null) {
                        handlePasswordError(com.android.internal.R.string.mmiError);
                        return;
                    }

                    int oldPinLen = oldPinOrPuk.length();

                /// M: Solve 27.14.2 FTA fail due to
                ///    press "**04*" to change PIN code the ME popup invalid MMI code. @{
                Phone currentPhone;

                currentPhone = PhoneFactory.getPhone(mPhone.getPhoneId());

                IccCard iccCard = currentPhone.getIccCard();
                /// @}

                    if (!iccCard.hasIccCard()) {
                        handlePasswordError(com.android.internal.R.string.mmiError);
                    } else if (!newPinOrPuk.equals(mSic)) {
                        // password mismatch; return error
                        handlePasswordError(com.android.internal.R.string.mismatchPin);
                    } else if ((mSc.equals(SC_PIN) || mSc.equals(SC_PIN2))
                               && ((pinLen < 4) || (pinLen > 8)
                                    || (oldPinLen < 4) || (oldPinLen > 8))) {
                        // invalid length
                        handlePasswordError(com.mediatek.R.string.checkPwdLen);
                    } else if ((mSc.equals(SC_PUK) || mSc.equals(SC_PUK2))
                               && (pinLen < 4) || (pinLen > 8)) {
                        handlePasswordError(com.mediatek.R.string.checkPwdLen);
                    } else if (mSc.equals(SC_PIN)
                            && mUiccApplication != null
                            && mUiccApplication.getState() == AppState.APPSTATE_PUK) {
                        // Sim is puk-locked
                        handlePasswordError(com.android.internal.R.string.needPuk);
                    } else if (!isValidPin(newPinOrPuk)) {
                        handlePasswordError(com.android.internal.R.string.mmiError);
                    } else if (mUiccApplication != null) {
                        Rlog.d(LOG_TAG, "process mmi service code using UiccApp sc=" + mSc);

                        // We have an app and the pre-checks are OK
                        if (mSc.equals(SC_PIN)) {
                            if (RETRY_BLOCKED.equals(retryPin1)) {
                                // PIN1 is in PUK state.
                                handlePasswordError(com.android.internal.R.string.needPuk);
                            } else {
                                //
                                //mPhone.mCM.changeIccPin(oldPinOrPuk, newPin,
                                //    obtainMessage(EVENT_SET_COMPLETE, this));
                                // Use SimCard provided interfaces.
                            mUiccApplication.changeIccLockPassword(oldPinOrPuk, newPinOrPuk,

                                    obtainMessage(EVENT_SET_COMPLETE, this));
                            }
                        } else if (mSc.equals(SC_PIN2)) {
                            if (RETRY_BLOCKED.equals(retryPin2)) {
                                // PIN2 is in PUK state.
                                handlePasswordError(com.android.internal.R.string.needPuk2);
                            } else {
                                //
                                //mPhone.mCM.changeIccPin2(oldPinOrPuk, newPin,
                                //    obtainMessage(EVENT_SET_COMPLETE, this));
                                // Use SimCard provided interfaces.
                            mUiccApplication.changeIccFdnPassword(oldPinOrPuk, newPinOrPuk,
                                    obtainMessage(EVENT_SET_COMPLETE, this));
                            }
                        } else if (mSc.equals(SC_PUK)) {
                            if (RETRY_BLOCKED.equals(retryPuk1)) {
                                // PIN1 Dead
                                handlePasswordError(com.mediatek.R.string.puk1Blocked);
                            } else {
                                //
                                //mPhone.mCM.supplyIccPuk(oldPinOrPuk, newPin,
                                //    obtainMessage(EVENT_SET_COMPLETE, this));
                                // Use SimCard provided interfaces.
                                if (oldPinOrPuk.length() == 8) {
                            mUiccApplication.supplyPuk(oldPinOrPuk, newPinOrPuk,
                                    obtainMessage(EVENT_SET_COMPLETE, this));
                                } else {
                                    handlePasswordError(com.mediatek.R.string.invalidPuk);
                                }
                            }
                        } else if (mSc.equals(SC_PUK2)) {
                            if (RETRY_BLOCKED.equals(retryPuk2)) {
                                // PIN2 Dead
                                handlePasswordError(com.mediatek.R.string.puk2Blocked);
                            } else {
                                //
                                //mPhone.mCM.supplyIccPuk2(oldPinOrPuk, newPin,
                                //    obtainMessage(EVENT_SET_COMPLETE, this));
                                if (oldPinOrPuk.length() == 8) {
                            mUiccApplication.supplyPuk2(oldPinOrPuk, newPinOrPuk,
                                    obtainMessage(EVENT_SET_COMPLETE, this));
                                } else {
                                    handlePasswordError(com.mediatek.R.string.invalidPuk);
                                }
                            }
                        }
                    /* M: SS part end */
                    } else {
                        throw new RuntimeException("No application mUiccApplicaiton is null");
                    }
                } else {
                    throw new RuntimeException ("Ivalid register/action=" + mAction);
                }
            } else if (mPoundString != null) {
                if (mPhone.getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                    mPhone.setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                }
                sendUssd(mPoundString);
            } else {
                throw new RuntimeException ("Invalid or Unsupported MMI Code");
            }
        } catch (RuntimeException exc) {
            mState = State.FAILED;
            exc.printStackTrace();
            Rlog.d(LOG_TAG, "exc.toString() = " + exc.toString());
            Rlog.d(LOG_TAG, "procesCode: mState = FAILED");
            mMessage = mContext.getText(com.android.internal.R.string.mmiError);
            mPhone.onMMIDone(this);
        }
    }

    private void handlePasswordError(int res) {
        mState = State.FAILED;
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        sb.append(mContext.getText(res));
        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    /**
     * Called from GsmCdmaPhone
     *
     * An unsolicited USSD NOTIFY or REQUEST has come in matching
     * up with this pending USSD request
     *
     * Note: If REQUEST, this exchange is complete, but the session remains
     *       active (ie, the network expects user input).
     */
    public void
    onUssdFinished(String ussdMessage, boolean isUssdRequest) {
        if (mState == State.PENDING) {
            if (ussdMessage == null || ussdMessage.length() == 0) {
                mMessage = mContext.getText(com.android.internal.R.string.mmiComplete);
            } else {
                mMessage = ussdMessage;
            }
            mIsUssdRequest = isUssdRequest;
            // If it's a request, leave it PENDING so that it's cancelable.
            if (!isUssdRequest) {
                mState = State.COMPLETE;
            }

            mPhone.onMMIDone(this);
        }
    }

    public void
    onUssdStkHandling(String ussdMessage, boolean isUssdRequest) {
        if (mState == State.PENDING) {
            if (ussdMessage == null || ussdMessage.length() == 0) {
                mMessage = mContext.getText(com.android.internal.R.string.mmiComplete);
            } else {
                mMessage = ussdMessage;
            }
            mIsUssdRequest = isUssdRequest;
            // If it's a request, leave it PENDING so that it's cancelable.
            if (!isUssdRequest) {
                mState = State.COMPLETE;
            }
            String userObjStringStk = USSD_HANDLED_BY_STK;
            mPhone.onMMIDone(this, (Object) userObjStringStk);
        }
    }

    /**
     * Called from GsmCdmaPhone
     *
     * The radio has reset, and this is still pending
     */

    public void
    onUssdFinishedError() {
        if (mState == State.PENDING) {
            mState = State.FAILED;
            mMessage = mContext.getText(com.android.internal.R.string.mmiError);

            mPhone.onMMIDone(this);
        }
    }

    /**
     * Called from GsmCdmaPhone
     *
     * An unsolicited USSD NOTIFY or REQUEST has come in matching
     * up with this pending USSD request
     *
     * Note: If REQUEST, this exchange is complete, but the session remains
     *       active (ie, the network expects user input).
     */
    public void
    onUssdRelease() {
        if (mState == State.PENDING) {
            mState = State.COMPLETE;
            mMessage = null;

            mPhone.onMMIDone(this);
        }
    }

    public void sendUssd(String ussdMessage) {
        // Treat this as a USSD string
        mIsPendingUSSD = true;

        // Note that unlike most everything else, the USSD complete
        // response does not complete this MMI code...we wait for
        // an unsolicited USSD "Notify" or "Request".
        // The matching up of this is done in GsmCdmaPhone.

        mPhone.mCi.sendUSSD(ussdMessage,
            obtainMessage(EVENT_USSD_COMPLETE, this));
    }

    /* M: SS part */
    ///M: For query CNAP
    void sendCNAPSS(String cnapssMessage) {
        // Note that unlike most everything else, the USSD complete
        // response does not complete this MMI code...we wait for
        // an unsolicited USSD "Notify" or "Request".
        // The matching up of this is done in GsmCdmaPhone.
        Rlog.d(LOG_TAG, "sendCNAPSS");
        mPhone.mCi.sendCNAPSS(cnapssMessage, obtainMessage(EVENT_QUERY_COMPLETE, this));
    }
    ///@
    /* M: SS part end */

    /** Called from GsmCdmaPhone.handleMessage; not a Handler subclass */
    @Override
    public void
    handleMessage (Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_SET_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                /* M: SS part */
                if (mSc.equals(SC_WAIT)
                    && mPhone.getTbcwMode() == GsmCdmaPhone.TBCW_OPTBCW_WITH_CS) {
                    if (ar.exception == null) {
                        int ienable = msg.arg1;
                        boolean enable = ienable == 1 ? true : false;
                        mPhone.setTerminalBasedCallWaiting(enable, null);
                    }
                }
                /* M: SS part end */

                onSetComplete(msg, ar);
                break;

            case EVENT_SET_CFF_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                /*
                * msg.arg1 = 1 means to set unconditional voice call forwarding
                * msg.arg2 = 1 means to enable voice call forwarding
                */
                // if ((ar.exception == null) && (msg.arg1 == 1)) {
                //     boolean cffEnabled = (msg.arg2 == 1);
                //     if (mIccRecords != null) {
                //         mPhone.setVoiceCallForwardingFlag(1, cffEnabled, mDialingNumber);
                //     }
                // }

                /* M: SS part */
                if ((ar.exception == null) && (msg.arg1 == 1)) {
                    boolean cffEnabled = (msg.arg2 == 1);
                    if (mIccRecords != null) {
                        mPhone.setVoiceCallForwardingFlag(1, cffEnabled, mDialingNumber);
                        /// M: SS OP01 Ut
                        mPhone.saveTimeSlot(null);
                    }
                }

                if ((ar.exception != null) && (mOrigUtCfuMode != 0)) {
                    if (mOrigUtCfuMode == 1) {
                        mPhone.setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_ON);
                    } else {
                        mPhone.setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_OFF);
                    }
                }
                mOrigUtCfuMode = 0;
                /* M: SS part end */

                onSetComplete(msg, ar);
                break;

            case EVENT_GET_CLIR_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onGetClirComplete(ar);
            break;

            /* M: SS part */
            /// M: [mtk04070][111125][ALPS00093395]MTK added for COLP and COLR. @{
            case EVENT_GET_COLP_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onGetColpComplete(ar);
            break;

            case EVENT_GET_COLR_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onGetColrComplete(ar);
            break;
            /// @}
            /* M: SS part end */

            case EVENT_QUERY_CF_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onQueryCfComplete(ar);
            break;

            case EVENT_QUERY_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                /* M: SS part */
                if (mSc.equals(SC_WAIT)
                    && mPhone.getTbcwMode() == GsmCdmaPhone.TBCW_OPTBCW_WITH_CS) {
                    Rlog.i(LOG_TAG, "TBCW_OPTBCW_WITH_CS");
                    if (ar.exception == null) {
                        int[] cwArray = (int[]) ar.result;
                        // If cwArray[0] is = 1, then cwArray[1] must follow,
                        // with the TS 27.007 service class bit vector of services
                        // for which call waiting is enabled.
                        try {
                            Rlog.d(LOG_TAG, "EVENT_GET_CALL_WAITING_FOR_CS_TB"
                                    + " cwArray[0]:cwArray[1] = "
                                    + cwArray[0] + ":" + cwArray[1]);

                            boolean csEnable = ((cwArray[0] == 1) &&
                                ((cwArray[1] & 0x01) == SERVICE_CLASS_VOICE));
                            mPhone.setTerminalBasedCallWaiting(csEnable, null);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "EVENT_GET_CALL_WAITING_FOR_CS_TB:"
                                    + " improper result: err ="
                                    + e.getMessage());
                        }
                    }
                }
                /* M: SS part end */

                onQueryComplete(ar);
            break;

            case EVENT_USSD_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                if (ar.exception != null) {
                    mState = State.FAILED;
                    mMessage = getErrorMessage(ar);

                    mPhone.onMMIDone(this);
                }

                // Note that unlike most everything else, the USSD complete
                // response does not complete this MMI code...we wait for
                // an unsolicited USSD "Notify" or "Request".
                // The matching up of this is done in GsmCdmaPhone.

            break;

            case EVENT_USSD_CANCEL_COMPLETE:
                mPhone.onMMIDone(this);
            break;
        }
    }
    //***** Private instance methods

    private CharSequence getErrorMessage(AsyncResult ar) {

        if (ar.exception instanceof CommandException) {
            CommandException.Error err = ((CommandException)(ar.exception)).getCommandError();
            if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                Rlog.i(LOG_TAG, "FDN_CHECK_FAILURE");
                return mContext.getText(com.android.internal.R.string.mmiFdnError);
            } else if (err == CommandException.Error.USSD_MODIFIED_TO_DIAL) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_DIAL");
                return mContext.getText(com.android.internal.R.string.stk_cc_ussd_to_dial);
            } else if (err == CommandException.Error.USSD_MODIFIED_TO_SS) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_SS");
                return mContext.getText(com.android.internal.R.string.stk_cc_ussd_to_ss);
            } else if (err == CommandException.Error.USSD_MODIFIED_TO_USSD) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_USSD");
                return mContext.getText(com.android.internal.R.string.stk_cc_ussd_to_ussd);
            } else if (err == CommandException.Error.SS_MODIFIED_TO_DIAL) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_DIAL");
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_dial);
            } else if (err == CommandException.Error.SS_MODIFIED_TO_USSD) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_USSD");
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_ussd);
            } else if (err == CommandException.Error.SS_MODIFIED_TO_SS) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_SS");
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_ss);
            }
        }

        return mContext.getText(com.android.internal.R.string.mmiError);
    }

    private CharSequence getScString() {
        if (mSc != null) {
            if (isServiceCodeCallBarring(mSc)) {
                return mContext.getText(com.android.internal.R.string.BaMmi);
            } else if (isServiceCodeCallForwarding(mSc)) {
                //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
                esChange = 1;
                String language = Locale.getDefault().getLanguage();
                if (SystemProperties.get("ro.bd_spanish_call_tran").equals("1") && language != null && language.endsWith("es")) {
                    if (mSc.equals(SC_CFU)) {
                        esScvalues = 21;
                        return "Desvio de llamadas incondicional";
                    } else if (mSc.equals(SC_CFB)) {
                        esScvalues = 67;
                        return "Desvio de llamadas cuando está ocupado";
                    } else if (mSc.equals(SC_CFNRy)) {
                        esScvalues = 61;
                        return "Desvio de llamadas cuando no hay respuesta";
                    } else if (mSc.equals(SC_CFNR)) {
                        esScvalues = 62;
                        return "Desvio de llamadas cuando no está disponible";
                    }
                } else {
                    return mContext.getText(com.android.internal.R.string.CfMmi);
                }
                //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END
            } else if (mSc.equals(SC_CLIP)) {
                return mContext.getText(com.android.internal.R.string.ClipMmi);
            } else if (mSc.equals(SC_CLIR)) {
                return mContext.getText(com.android.internal.R.string.ClirMmi);
            } else if (mSc.equals(SC_PWD)) {
                return mContext.getText(com.android.internal.R.string.PwdMmi);
            } else if (mSc.equals(SC_WAIT)) {
                return mContext.getText(com.android.internal.R.string.CwMmi);
            } else if (isPinPukCommand()) {
                return mContext.getText(com.android.internal.R.string.PinMmi);
            /* M: SS part */
            } else if (mSc.equals(SC_PIN)) {
                return mContext.getText(com.android.internal.R.string.PinMmi);
            } else if (mSc.equals(SC_PIN2)) {
                return mContext.getText(com.mediatek.R.string.Pin2Mmi);
            } else if (mSc.equals(SC_PUK)) {
                return mContext.getText(com.mediatek.R.string.PukMmi);
            } else if (mSc.equals(SC_PUK2)) {
                return mContext.getText(com.mediatek.R.string.Puk2Mmi);
            }
            /// @}
            /* M: SS part end */

        }

        return "";
    }

    private void
    onSetComplete(Message msg, AsyncResult ar){
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException)(ar.exception)).getCommandError();
                if (err == CommandException.Error.PASSWORD_INCORRECT) {
                    if (isPinPukCommand()) {
                         /* M: SS part */
                        /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{

                        // look specifically for the PUK commands and adjust
                        // the message accordingly.

                        if (mSc.equals(SC_PUK)) {
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.badPuk));
                        } else if (mSc.equals(SC_PUK2)) {
                            sb.append(mContext.getText(
                                    com.mediatek.R.string.badPuk2));
                        } else if (mSc.equals(SC_PIN)) {
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.badPin));
                        } else if (mSc.equals(SC_PIN2)) {
                            sb.append(mContext.getText(
                                    com.mediatek.R.string.badPin2));
                        }
                        /* M: SS part end */

                        // if (mSc.equals(SC_PUK) || mSc.equals(SC_PUK2)) {
                        //     sb.append(mContext.getText(
                        //             com.android.internal.R.string.badPuk));
                        // } else {
                        //     sb.append(mContext.getText(
                        //             com.android.internal.R.string.badPin));
                        // }
                        // Get the No. of retries remaining to unlock PUK/PUK2

                        int attemptsRemaining = msg.arg1;
                        if (attemptsRemaining <= 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: PUK locked,"
                                    + " cancel as lock screen will handle this");
                            mState = State.CANCELLED;
                        } else if (attemptsRemaining > 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: attemptsRemaining="+attemptsRemaining);
                            sb.append(mContext.getResources().getQuantityString(
                                    com.android.internal.R.plurals.pinpuk_attempts,
                                    attemptsRemaining, attemptsRemaining));
                        }
                    } else {
                        sb.append(mContext.getText(
                                com.android.internal.R.string.passwordIncorrect));
                    }
                } else if (err == CommandException.Error.SIM_PUK2) {
                    // sb.append(mContext.getText(
                            // com.android.internal.R.string.badPin));
                    /* M: SS part  */
                    /// M: [mtk04070][111125][ALPS00093395]MTK modified.
                    sb.append(mContext.getText(com.mediatek.R.string.badPin2));
                    sb.append("\n");
                    sb.append(mContext.getText(
                            com.android.internal.R.string.needPuk2));
                } else if (err == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    if (mSc.equals(SC_PIN)) {
                        sb.append(mContext.getText(com.android.internal.R.string.enablePin));
                    }
                /// M: [mtk04070][111125][ALPS00093395]MTK added for call barred. @{
                } else if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(mContext.getText(com.mediatek.R.string.callBarringFailMmi));
                /// @}
                /* M: SS part end */
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    Rlog.i(LOG_TAG, "FDN_CHECK_FAILURE");
                    sb.append(mContext.getText(com.android.internal.R.string.mmiFdnError));
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(mContext.getText(
                        com.android.internal.R.string.mmiError));
            }
        } else if (isActivate()) {
            mState = State.COMPLETE;
            if (mIsCallFwdReg) {
                //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
                String language = Locale.getDefault().getLanguage();
                if (SystemProperties.get("ro.bd_spanish_call_tran").equals("1") && language != null && language.endsWith("es") && esChange == 1) {
                    esChange = 0;
                    sb = new StringBuilder("");
                    sb.append("\n");
                    Log.i("chenguangxiang","serviceRegistered1:"+esScvalues);
                    if (esScvalues == 21) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas incondicional se ha activado correctamente.");
                    } else if (esScvalues == 67) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas cuando está ocupado se ha activado correctamente.");
                    } else if (esScvalues == 61) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas cuando no hay respuesta se ha activado correctamente.");
                    } else if (esScvalues == 62) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas cuando no está disponible se ha activado correctamente.");
                    } else {
                        sb.append(mContext.getText(
                                com.android.internal.R.string.zzzzz_bird_serviceRegistered));
                    }
                } else {
                    sb.append(mContext.getText(
                            com.android.internal.R.string.serviceRegistered));
                }
                //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END
            } else {
                sb.append(mContext.getText(
                        com.android.internal.R.string.serviceEnabled));
            }
            // Record CLIR setting
            if (mSc.equals(SC_CLIR)) {
                mPhone.saveClirSetting(CommandsInterface.CLIR_INVOCATION);
            }
        } else if (isDeactivate()) {
            mState = State.COMPLETE;
            //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
            String language = Locale.getDefault().getLanguage();
            if (SystemProperties.get("ro.bd_spanish_call_tran").equals("1") && language != null && language.endsWith("es") && esChange == 1) {
                esChange = 0;
                sb = new StringBuilder("");
                sb.append("\n");
                Log.i("chenguangxiang","serviceDisabled1:"+esScvalues);
                if (esScvalues == 21) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas incondicional se ha desactivado correctamente.");
                } else if (esScvalues == 67) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas cuando está ocupado se ha desactivado correctamente.");
                } else if (esScvalues == 61) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas cuando no hay respuesta se ha desactivado correctamente.");
                } else if (esScvalues == 62) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas cuando no está disponible se ha desactivado correctamente.");
                } else {
                    sb.append(mContext.getText(
                            com.android.internal.R.string.zzzzz_bird_serviceDisabled));
                }
            } else {
                sb.append(mContext.getText(
                        com.android.internal.R.string.serviceDisabled));
            }
            //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END
            // Record CLIR setting
            if (mSc.equals(SC_CLIR)) {
                mPhone.saveClirSetting(CommandsInterface.CLIR_SUPPRESSION);
            }
        } else if (isRegister()) {
            mState = State.COMPLETE;
            //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
            String language = Locale.getDefault().getLanguage();
            if (SystemProperties.get("ro.bd_spanish_call_tran").equals("1") && language != null && language.endsWith("es") && esChange == 1) {
                esChange = 0;
                sb = new StringBuilder("");
                sb.append("\n");
                Log.i("chenguangxiang","serviceRegistered2:"+esScvalues);
                if (esScvalues == 21) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas incondicional se ha activado correctamente.");
                } else if (esScvalues == 67) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas cuando está ocupado se ha activado correctamente.");
                } else if (esScvalues == 61) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas cuando no hay respuesta se ha activado correctamente.");
                } else if (esScvalues == 62) {
                    esScvalues = 0;
                    sb.append("El desvio de llamadas cuando no está disponible se ha activado correctamente.");
                } else {
                    sb.append(mContext.getText(
                            com.android.internal.R.string.zzzzz_bird_serviceRegistered));
                }
            } else {
                sb.append(mContext.getText(
                        com.android.internal.R.string.serviceRegistered));
            }
            //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END
        } else if (isErasure()) {
            mState = State.COMPLETE;
            sb.append(mContext.getText(
                    com.android.internal.R.string.serviceErased));
        } else {
            mState = State.FAILED;
            sb.append(mContext.getText(
                    com.android.internal.R.string.mmiError));
        }

        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    private void
    onGetClirComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;
            // sb.append(getErrorMessage(ar));
            /* M: SS part end */
            /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{
            //sb.append(getErrorMessage(ar));
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) (ar.exception)).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(mContext.getText(com.mediatek.R.string.callBarringFailMmi));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(mContext.getText(com.mediatek.R.string.fdnFailMmi));
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
            /// @}
            /* M: SS part end */
        } else {
            int clirArgs[];

            clirArgs = (int[])ar.result;

            // the 'm' parameter from TS 27.007 7.7
            switch (clirArgs[1]) {
                case 0: // CLIR not provisioned
                    sb.append(mContext.getText(
                                com.android.internal.R.string.serviceNotProvisioned));
                    mState = State.COMPLETE;
                break;

                case 1: // CLIR provisioned in permanent mode
                    sb.append(mContext.getText(
                                com.android.internal.R.string.CLIRPermanent));
                    mState = State.COMPLETE;
                break;

                case 2: // unknown (e.g. no network, etc.)
                    sb.append(mContext.getText(
                                com.android.internal.R.string.mmiError));
                    mState = State.FAILED;
                break;

                case 3: // CLIR temporary mode presentation restricted

                    // the 'n' parameter from TS 27.007 7.7
                    switch (clirArgs[0]) {
                        default:
                        case 0: // Default
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOnNextCallOn));
                        break;
                        case 1: // CLIR invocation
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOnNextCallOn));
                        break;
                        case 2: // CLIR suppression
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOnNextCallOff));
                        break;
                    }
                    mState = State.COMPLETE;
                break;

                case 4: // CLIR temporary mode presentation allowed
                    // the 'n' parameter from TS 27.007 7.7
                    switch (clirArgs[0]) {
                        default:
                        case 0: // Default
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOffNextCallOff));
                        break;
                        case 1: // CLIR invocation
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOffNextCallOn));
                        break;
                        case 2: // CLIR suppression
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOffNextCallOff));
                        break;
                    }

                    mState = State.COMPLETE;
                break;
            }
        }

        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    /**
     * @param serviceClass 1 bit of the service class bit vectory
     * @return String to be used for call forward query MMI response text.
     *        Returns null if unrecognized
     */

    private CharSequence
    serviceClassToCFString (int serviceClass) {
        switch (serviceClass) {
            case SERVICE_CLASS_VOICE:
                return mContext.getText(com.android.internal.R.string.serviceClassVoice);
            case SERVICE_CLASS_DATA:
                return mContext.getText(com.android.internal.R.string.serviceClassData);
            case SERVICE_CLASS_FAX:
                return mContext.getText(com.android.internal.R.string.serviceClassFAX);
            case SERVICE_CLASS_SMS:
                return mContext.getText(com.android.internal.R.string.serviceClassSMS);
            case SERVICE_CLASS_DATA_SYNC:
                return mContext.getText(com.android.internal.R.string.serviceClassDataSync);
            case SERVICE_CLASS_DATA_ASYNC:
                return mContext.getText(com.android.internal.R.string.serviceClassDataAsync);
            case SERVICE_CLASS_PACKET:
                return mContext.getText(com.android.internal.R.string.serviceClassPacket);
            case SERVICE_CLASS_PAD:
                return mContext.getText(com.android.internal.R.string.serviceClassPAD);
            /* M: SS part*/
            /// M: [mtk04070][111125][ALPS00093395]MTK added for line2 and video call. @{
            case SERVICE_CLASS_LINE2:
            case SERVICE_CLASS_VIDEO:
                return mContext.getText(com.mediatek.R.string.serviceClassVideo);
            /// @}
            /* M: SS part end */
            default:
                return null;
        }
    }


    /** one CallForwardInfo + serviceClassMask -> one line of text */
    private CharSequence
    makeCFQueryResultMessage(CallForwardInfo info, int serviceClassMask) {
        CharSequence template;
        String sources[] = {"{0}", "{1}", "{2}"};
        CharSequence destinations[] = new CharSequence[3];
        boolean needTimeTemplate;

        // CF_REASON_NO_REPLY also has a time value associated with
        // it. All others don't.

        needTimeTemplate =
            (info.reason == CommandsInterface.CF_REASON_NO_REPLY);

        /* M: SS part end */
        /// M: [mtk04070][111125][ALPS00093395]Also check if info.number is not empty or null. @{
        if ((info.status == 1)  && !isEmptyOrNull(info.number)) {
            /* Number cannot be NULL when status is activated */
            if (needTimeTemplate) {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateForwardedTime);
            } else {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateForwarded);
            }
        // } else if (info.status == 0 && isEmptyOrNull(info.number)) {
        } else if (isEmptyOrNull(info.number)) {
        /// @}
        /* M: SS part end */
            template = mContext.getText(
                        com.android.internal.R.string.cfTemplateNotForwarded);
        } else { /* (info.status == 0) && !isEmptyOrNull(info.number) */
            // A call forward record that is not active but contains
            // a phone number is considered "registered"

            if (needTimeTemplate) {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateRegisteredTime);
            } else {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateRegistered);
            }
        }

        // In the template (from strings.xmls)
        //         {0} is one of "bearerServiceCode*"
        //        {1} is dialing number
        //      {2} is time in seconds

        // destinations[0] = serviceClassToCFString(info.serviceClass & serviceClassMask);
        // destinations[1] = formatLtr(
        //         PhoneNumberUtils.stringFromStringAndTOA(info.number, info.toa));
                destinations[0] = serviceClassToCFString(info.serviceClass & serviceClassMask);
        /* M: SS part end */

        destinations[1] = PhoneNumberUtils.stringFromStringAndTOA(info.number, info.toa);
        /* M: SS part end */
        destinations[2] = Integer.toString(info.timeSeconds);

        if (info.reason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                (info.serviceClass & serviceClassMask)
                        == CommandsInterface.SERVICE_CLASS_VOICE) {
            boolean cffEnabled = (info.status == 1);
            if (mIccRecords != null) {
                mPhone.setVoiceCallForwardingFlag(1, cffEnabled, info.number);
            }
        }

        return TextUtils.replace(template, sources, destinations);
    }

    /**
     * Used to format a string that should be displayed as LTR even in RTL locales
     */
    private String formatLtr(String str) {
        BidiFormatter fmt = BidiFormatter.getInstance();
        return str == null ? str : fmt.unicodeWrap(str, TextDirectionHeuristics.LTR, true);
    }

    private void
    onQueryCfComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;
            /* M: SS part end */
            /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{
            //sb.append(getErrorMessage(ar));
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) (ar.exception)).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(mContext.getText(com.mediatek.R.string.callBarringFailMmi));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(mContext.getText(com.mediatek.R.string.fdnFailMmi));
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
            /// @}
            /* M: SS part end */
        } else {
            CallForwardInfo infos[];

            infos = (CallForwardInfo[]) ar.result;

            if (infos.length == 0) {
                // Assume the default is not active
                //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
                String language = Locale.getDefault().getLanguage();
                if (SystemProperties.get("ro.bd_spanish_call_tran").equals("1") && language != null && language.endsWith("es") && esChange == 1) {
                    esChange = 0;
                    sb = new StringBuilder("");
                    sb.append("\n");
                    Log.i("chenguangxiang","serviceDisabled2:"+esScvalues);
                    if (esScvalues == 21) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas incondicional se ha desactivado correctamente.");
                    } else if (esScvalues == 67) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas cuando está ocupado se ha desactivado correctamente.");
                    } else if (esScvalues == 61) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas cuando no hay respuesta se ha desactivado correctamente.");
                    } else if (esScvalues == 62) {
                        esScvalues = 0;
                        sb.append("El desvio de llamadas cuando no está disponible se ha desactivado correctamente.");
                    } else {
                        sb.append(mContext.getText(com.android.internal.R.string.zzzzz_bird_serviceDisabled));
                    }
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));
                }
                //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END

                // Set unconditional CFF in SIM to false
                if (mIccRecords != null) {
                    mPhone.setVoiceCallForwardingFlag(1, false, null);
                }
            } else {

                SpannableStringBuilder tb = new SpannableStringBuilder();

                // Each bit in the service class gets its own result line
                // The service classes may be split up over multiple
                // CallForwardInfos. So, for each service class, find out
                // which CallForwardInfo represents it and then build
                // the response text based on that

                /* M: SS part end */
                /// M: [mtk04070][111125][ALPS00093395]MTK added. @{
                boolean isAllCfDisabled = false;
                for (int i = 0, s = infos.length; i < s ; i++) {
                    if (infos[i].serviceClass == SERVICE_CLASS_VOICE
                                               + SERVICE_CLASS_FAX
                                               + SERVICE_CLASS_SMS
                                               + SERVICE_CLASS_DATA_SYNC
                                               + SERVICE_CLASS_DATA_ASYNC) {
                        isAllCfDisabled = true;
                        break;
                    }
                }
                Rlog.d(LOG_TAG, "[GsmMmiCode] isAllCfDisabled = " + isAllCfDisabled);
                /// @}

                for (int serviceClassMask = 1
                            ; serviceClassMask <= SERVICE_CLASS_MAX
                            ; serviceClassMask <<= 1
                ) {
                    // for (int i = 0, s = infos.length; i < s ; i++) {
                    //     if ((serviceClassMask & infos[i].serviceClass) != 0) {
                    //         tb.append(makeCFQueryResultMessage(infos[i],
                    //                         serviceClassMask));
                    //         tb.append("\n");
                    //     }
                    // }
                    /// M: [mtk04070][111125][ALPS00093395]MTK added and modified. @{
                    if (serviceClassMask == SERVICE_CLASS_LINE2) continue;

                    if (isAllCfDisabled) {
                        if (serviceClassToCFString(serviceClassMask) != null) {
                            String getServiceName =
                                serviceClassToCFString(serviceClassMask).toString();
                            if (getServiceName != null)
                            {
                                    sb.append(getServiceName);
                                    sb.append(" : ");
                                    sb.append(mContext.getText(
                                        com.mediatek.R.string.cfServiceNotForwarded));
                                    sb.append("\n");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "[GsmMmiCode] "
                                + serviceClassMask + " service returns null");
                        }
                    } else {
                      for (int i = 0, s = infos.length; i < s ; i++) {
                        if ((serviceClassMask & infos[i].serviceClass) != 0) {
                            if (infos[i].status == 1) {
                               tb.append(makeCFQueryResultMessage(infos[i],
                                            serviceClassMask));
                               tb.append("\n");
                            } else {
                                    if (serviceClassToCFString(serviceClassMask) != null) {
                                       String getServiceName1 =
                                            serviceClassToCFString(serviceClassMask).toString();
                                       sb.append(getServiceName1);
                                       sb.append(" : ");
                                       sb.append(mContext.getText(
                                            com.mediatek.R.string.cfServiceNotForwarded));
                                       sb.append("\n");
                                    }
                            }
                        }
                      }
                    }
                    /// @}
                    /* M: SS part end */
                }
                sb.append(tb);
            }

            mState = State.COMPLETE;
        }

        mMessage = sb;
        mPhone.onMMIDone(this);

    }

    private void
    onQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;
            // sb.append(getErrorMessage(ar));
            /* M: SS part */
            /// M: [mtk04070][111125][ALPS00093395]MTK modified. @{
            //sb.append(getErrorMessage(ar));
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) (ar.exception)).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(mContext.getText(com.mediatek.R.string.callBarringFailMmi));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(mContext.getText(com.mediatek.R.string.fdnFailMmi));
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
            /// @}
            /* M: SS part end */
        } else {
            int[] ints = (int[])ar.result;

            if (ints.length != 0) {
                if (ints[0] == 0) {
                    //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] BEGIN
                    String language = Locale.getDefault().getLanguage();
                    if (SystemProperties.get("ro.bd_spanish_call_tran").equals("1") && language != null && language.endsWith("es") && esChange == 1) {
                        esChange = 0;
                        sb = new StringBuilder("");
                        sb.append("\n");
                        Log.i("chenguangxiang","serviceDisabled3:"+esScvalues);
                        if (esScvalues == 21) {
                            esScvalues = 0;
                            sb.append("El desvio de llamadas incondicional se ha desactivado correctamente.");
                        } else if (esScvalues == 67) {
                            esScvalues = 0;
                            sb.append("El desvio de llamadas cuando está ocupado se ha desactivado correctamente.");
                        } else if (esScvalues == 61) {
                            esScvalues = 0;
                            sb.append("El desvio de llamadas cuando no hay respuesta se ha desactivado correctamente.");
                        } else if (esScvalues == 62) {
                            esScvalues = 0;
                            sb.append("El desvio de llamadas cuando no está disponible se ha desactivado correctamente.");
                        } else {
                            sb.append(mContext.getText(com.android.internal.R.string.zzzzz_bird_serviceDisabled));
                        }
                    } else {
                        sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));
                    }
                    //[BIRD][TASK #5427][呼叫转移西班牙语翻译][chenguangxiang][20170721] END
                } else if (mSc.equals(SC_WAIT)) {
                    // Call Waiting includes additional data in the response.
                    sb.append(createQueryCallWaitingResultMessage(ints[1]));
                } else if (isServiceCodeCallBarring(mSc)) {
                    // ints[0] for Call Barring is a bit vector of services
                    sb.append(createQueryCallBarringResultMessage(ints[0]));
                } else if (mSc.equals(SC_CNAP)) {
                    ///M: For query CNAP
                    Rlog.d(LOG_TAG, "onQueryComplete_CNAP");
                    sb.append(createQueryCnapResultMessage(ints[1]));
                } else if (ints[0] == 1) {
                    // for all other services, treat it as a boolean
                    sb.append(mContext.getText(com.android.internal.R.string.serviceEnabled));
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
            mState = State.COMPLETE;
        }

        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    /* M: SS part */
    ///M: For query CNAP
    private CharSequence
    createQueryCnapResultMessage(int serviceClass) {
        Rlog.d(LOG_TAG, "createQueryCnapResultMessage_CNAP");
        StringBuilder sb =
                new StringBuilder(
                    mContext.getText(com.android.internal.R.string.serviceEnabledFor));

        for (int classMask = 1
                    ; classMask <= SERVICE_CLASS_MAX
                    ; classMask <<= 1
        ) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        Rlog.d(LOG_TAG, "CNAP_sb = " + sb);
        return sb;
    }
    /* M: SS part end */

    private CharSequence
    createQueryCallWaitingResultMessage(int serviceClass) {
        StringBuilder sb =
                new StringBuilder(mContext.getText(com.android.internal.R.string.serviceEnabledFor));

        for (int classMask = 1
                    ; classMask <= SERVICE_CLASS_MAX
                    ; classMask <<= 1
        ) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        return sb;
    }
    private CharSequence
    createQueryCallBarringResultMessage(int serviceClass)
    {
        StringBuilder sb = new StringBuilder(mContext.getText(com.android.internal.R.string.serviceEnabledFor));

        for (int classMask = 1
                    ; classMask <= SERVICE_CLASS_MAX
                    ; classMask <<= 1
        ) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        return sb;
    }

    /***
     * TODO: It would be nice to have a method here that can take in a dialstring and
     * figure out if there is an MMI code embedded within it.  This code would replace
     * some of the string parsing functionality in the Phone App's
     * SpecialCharSequenceMgr class.
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GsmMmiCode {");

        sb.append("State=" + getState());
        if (mAction != null) sb.append(" action=" + mAction);
        if (mSc != null) sb.append(" sc=" + mSc);
        if (mSia != null) sb.append(" sia=" + mSia);
        if (mSib != null) sb.append(" sib=" + mSib);
        if (mSic != null) sb.append(" sic=" + mSic);
        if (mPoundString != null) sb.append(" poundString=" + mPoundString);
        if (mDialingNumber != null) sb.append(" dialingNumber=" + mDialingNumber);
        if (mPwd != null) sb.append(" pwd=" + mPwd);
        sb.append("}");
        return sb.toString();
    }

    /* M: SS part */
    /// M: [mtk04070][111125][ALPS00093395]MTK proprietary methods. @{
    static public GsmMmiCode
    newNetworkInitiatedUssdError(String ussdMessage,
                                boolean isUssdRequest,
                                GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret;

        ret = new GsmMmiCode(phone, app);

        ret.mMessage = ret.mContext.getText(com.android.internal.R.string.mmiError);
        ret.mIsUssdRequest = isUssdRequest;

        ret.mState = State.FAILED;

        return ret;
    }

    private boolean isValidPin(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (address.charAt(i) < '0' || address.charAt(i) > '9')
                return false;
        }
        return true;
    }

    private void
    onGetColrComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) (ar.exception)).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(mContext.getText(com.mediatek.R.string.callBarringFailMmi));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(mContext.getText(com.mediatek.R.string.fdnFailMmi));
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
    }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
        } else {
            int colrArgs[];

            colrArgs = (int[]) ar.result;

            // the 'm' parameter from mtk proprietary
            switch (colrArgs[0]) {
                case 0: // COLR not provisioned
                    sb.append(mContext.getText(
                                com.android.internal.R.string.serviceNotProvisioned));
                    mState = State.COMPLETE;
                break;

                case 1: // COLR provisioned
                    sb.append(mContext.getText(
                                com.mediatek.R.string.serviceProvisioned));
                    mState = State.COMPLETE;
                break;

                case 2: // unknown (e.g. no network, etc.)
                    sb.append(mContext.getText(
                                com.android.internal.R.string.mmiError));
                    mState = State.FAILED;
                break;

            }
        }

        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    private void
    onGetColpComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) (ar.exception)).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(mContext.getText(com.mediatek.R.string.callBarringFailMmi));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(mContext.getText(com.mediatek.R.string.fdnFailMmi));
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
        } else {
            int colpArgs[];

            colpArgs = (int[]) ar.result;

            // the 'm' parameter from TS 27.007 7.8
            switch (colpArgs[1]) {
                case 0: // COLP not provisioned
                    sb.append(mContext.getText(
                                com.android.internal.R.string.serviceNotProvisioned));
                    mState = State.COMPLETE;
                break;

                case 1: // COLP provisioned
                    sb.append(mContext.getText(
                                com.mediatek.R.string.serviceProvisioned));
                    mState = State.COMPLETE;
                break;

                case 2: // unknown (e.g. no network, etc.)
                    sb.append(mContext.getText(
                                com.mediatek.R.string.serviceUnknown));
                    mState = State.COMPLETE;
                break;
            }
        }

        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    /// @}

    public static boolean
    isUtMmiCode(String dialString, GsmCdmaPhone dialPhone, UiccCardApplication iccApp) {

        GsmMmiCode mmi = GsmMmiCode.newFromDialString(dialString, dialPhone, iccApp);
        if (mmi == null || mmi.isTemporaryModeCLIR()) {
            return false;
        }

        if (mmi.isShortCode() || mmi.mDialingNumber != null) {
            return false;
        } else if (mmi.mSc != null
                && (mmi.mSc.equals(SC_CLIP)
                || mmi.mSc.equals(SC_CLIR)
                || mmi.mSc.equals(SC_COLP)
                || mmi.mSc.equals(SC_COLR)
                || isServiceCodeCallForwarding(mmi.mSc)
                || isServiceCodeCallBarring(mmi.mSc)
                || mmi.mSc.equals(SC_WAIT)
                )) {
            return true;
        }
        return false;
    }
    /* M: SS part end */
}
