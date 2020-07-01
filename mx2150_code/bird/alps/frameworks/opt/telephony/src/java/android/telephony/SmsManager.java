/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.telephony;

import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
// MTK-START
import android.os.SystemProperties;
// MTK-END
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.IMms;
import com.android.internal.telephony.uicc.IccConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// MTK-START
import android.app.PendingIntent.CanceledException;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.MPlugin;
// For 4G data only, this class will check 4G data only and prompt to user
import com.mediatek.common.sms.IDataOnlySmsFwkExt;
// MTK_ONLY_OWNER_SIM_SUPPORT
import com.mediatek.common.telephony.IOnlyOwnerSimSupport;

import com.mediatek.internal.telephony.IccSmsStorageStatus;
// Cell broadcast new interface
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.mediatek.internal.telephony.SmsCbConfigInfo;
import com.android.internal.telephony.SmsConstants;
// MTK-END
/*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]begin*/
import android.os.SystemProperties;
import android.database.Cursor;
/*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]end*/
/*
 * TODO(code review): Curious question... Why are a lot of these
 * methods not declared as static, since they do not seem to require
 * any local object state?  Presumably this cannot be changed without
 * interfering with the API...
 */

/**
 * Manages SMS operations such as sending data, text, and pdu SMS messages.
 * Get this object by calling the static method {@link #getDefault()}.
 *
 * <p>For information about how to behave as the default SMS app on Android 4.4 (API level 19)
 * and higher, see {@link android.provider.Telephony}.
 */
public final class SmsManager {
    private static final String TAG = "SmsManager";
    /**
     * A psuedo-subId that represents the default subId at any given time. The actual subId it
     * represents changes as the default subId is changed.
     */
    private static final int DEFAULT_SUBSCRIPTION_ID = -1002;

    /** Singleton object constructed during class initialization. */
    private static final SmsManager sInstance = new SmsManager(DEFAULT_SUBSCRIPTION_ID);
    private static final Object sLockObject = new Object();

    /** @hide */
    public static final int CELL_BROADCAST_RAN_TYPE_GSM = 0;
    /** @hide */
    public static final int CELL_BROADCAST_RAN_TYPE_CDMA = 1;

    private static final Map<Integer, SmsManager> sSubInstances =
            new ArrayMap<Integer, SmsManager>();

    /** A concrete subscription id, or the pseudo DEFAULT_SUBSCRIPTION_ID */
    private int mSubId;

    /*
     * Key for the various carrier-dependent configuration values.
     * Some of the values are used by the system in processing SMS or MMS messages. Others
     * are provided for the convenience of SMS applications.
     */

    /**
     * Whether to append transaction id to MMS WAP Push M-Notification.ind's content location URI
     * when constructing the download URL of a new MMS (boolean type)
     */
    public static final String MMS_CONFIG_APPEND_TRANSACTION_ID =
            CarrierConfigManager.KEY_MMS_APPEND_TRANSACTION_ID_BOOL;
    /**
     * Whether MMS is enabled for the current carrier (boolean type)
     */
    public static final String
        MMS_CONFIG_MMS_ENABLED = CarrierConfigManager.KEY_MMS_MMS_ENABLED_BOOL;
    /**
     * Whether group MMS is enabled for the current carrier (boolean type)
     */
    public static final String
            MMS_CONFIG_GROUP_MMS_ENABLED = CarrierConfigManager.KEY_MMS_GROUP_MMS_ENABLED_BOOL;
    /**
     * If this is enabled, M-NotifyResp.ind should be sent to the WAP Push content location instead
     * of the default MMSC (boolean type)
     */
    public static final String MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED =
            CarrierConfigManager.KEY_MMS_NOTIFY_WAP_MMSC_ENABLED_BOOL;
    /**
     * Whether alias is enabled (boolean type)
     */
    public static final String
            MMS_CONFIG_ALIAS_ENABLED = CarrierConfigManager.KEY_MMS_ALIAS_ENABLED_BOOL;
    /**
     * Whether audio is allowed to be attached for MMS messages (boolean type)
     */
    public static final String
            MMS_CONFIG_ALLOW_ATTACH_AUDIO = CarrierConfigManager.KEY_MMS_ALLOW_ATTACH_AUDIO_BOOL;
    /**
     * Whether multipart SMS is enabled (boolean type)
     */
    public static final String MMS_CONFIG_MULTIPART_SMS_ENABLED =
            CarrierConfigManager.KEY_MMS_MULTIPART_SMS_ENABLED_BOOL;
    /**
     * Whether SMS delivery report is enabled (boolean type)
     */
    public static final String MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED =
            CarrierConfigManager.KEY_MMS_SMS_DELIVERY_REPORT_ENABLED_BOOL;
    /**
     * Whether content-disposition field should be expected in an MMS PDU (boolean type)
     */
    public static final String MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION =
            CarrierConfigManager.KEY_MMS_SUPPORT_MMS_CONTENT_DISPOSITION_BOOL;
    /**
     * Whether multipart SMS should be sent as separate messages
     */
    public static final String MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES =
            CarrierConfigManager.KEY_MMS_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES_BOOL;
    /**
     * Whether MMS read report is enabled (boolean type)
     */
    public static final String MMS_CONFIG_MMS_READ_REPORT_ENABLED =
            CarrierConfigManager.KEY_MMS_MMS_READ_REPORT_ENABLED_BOOL;
    /**
     * Whether MMS delivery report is enabled (boolean type)
     */
    public static final String MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED =
            CarrierConfigManager.KEY_MMS_MMS_DELIVERY_REPORT_ENABLED_BOOL;
    /**
     * Max MMS message size in bytes (int type)
     */
    public static final String
            MMS_CONFIG_MAX_MESSAGE_SIZE = CarrierConfigManager.KEY_MMS_MAX_MESSAGE_SIZE_INT;
    /**
     * Max MMS image width (int type)
     */
    public static final String
            MMS_CONFIG_MAX_IMAGE_WIDTH = CarrierConfigManager.KEY_MMS_MAX_IMAGE_WIDTH_INT;
    /**
     * Max MMS image height (int type)
     */
    public static final String
            MMS_CONFIG_MAX_IMAGE_HEIGHT = CarrierConfigManager.KEY_MMS_MAX_IMAGE_HEIGHT_INT;
    /**
     * Limit of recipients of MMS messages (int type)
     */
    public static final String
            MMS_CONFIG_RECIPIENT_LIMIT = CarrierConfigManager.KEY_MMS_RECIPIENT_LIMIT_INT;
    /**
     * Min alias character count (int type)
     */
    public static final String
            MMS_CONFIG_ALIAS_MIN_CHARS = CarrierConfigManager.KEY_MMS_ALIAS_MIN_CHARS_INT;
    /**
     * Max alias character count (int type)
     */
    public static final String
            MMS_CONFIG_ALIAS_MAX_CHARS = CarrierConfigManager.KEY_MMS_ALIAS_MAX_CHARS_INT;
    /**
     * When the number of parts of a multipart SMS reaches this threshold, it should be converted
     * into an MMS (int type)
     */
    public static final String MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD =
            CarrierConfigManager.KEY_MMS_SMS_TO_MMS_TEXT_THRESHOLD_INT;
    /**
     * Some carriers require SMS to be converted into MMS when text length reaches this threshold
     * (int type)
     */
    public static final String MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD =
            CarrierConfigManager.KEY_MMS_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD_INT;
    /**
     * Max message text size (int type)
     */
    public static final String MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE =
            CarrierConfigManager.KEY_MMS_MESSAGE_TEXT_MAX_SIZE_INT;
    /**
     * Max message subject length (int type)
     */
    public static final String
            MMS_CONFIG_SUBJECT_MAX_LENGTH = CarrierConfigManager.KEY_MMS_SUBJECT_MAX_LENGTH_INT;
    /**
     * MMS HTTP socket timeout in milliseconds (int type)
     */
    public static final String
            MMS_CONFIG_HTTP_SOCKET_TIMEOUT = CarrierConfigManager.KEY_MMS_HTTP_SOCKET_TIMEOUT_INT;
    /**
     * The name of the UA Prof URL HTTP header for MMS HTTP request (String type)
     */
    public static final String
            MMS_CONFIG_UA_PROF_TAG_NAME = CarrierConfigManager.KEY_MMS_UA_PROF_TAG_NAME_STRING;
    /**
     * The User-Agent header value for MMS HTTP request (String type)
     */
    public static final String
            MMS_CONFIG_USER_AGENT = CarrierConfigManager.KEY_MMS_USER_AGENT_STRING;
    /**
     * The UA Profile URL header value for MMS HTTP request (String type)
     */
    public static final String
            MMS_CONFIG_UA_PROF_URL = CarrierConfigManager.KEY_MMS_UA_PROF_URL_STRING;
    /**
     * A list of HTTP headers to add to MMS HTTP request, separated by "|" (String type)
     */
    public static final String
            MMS_CONFIG_HTTP_PARAMS = CarrierConfigManager.KEY_MMS_HTTP_PARAMS_STRING;
    /**
     * Email gateway number (String type)
     */
    public static final String MMS_CONFIG_EMAIL_GATEWAY_NUMBER =
            CarrierConfigManager.KEY_MMS_EMAIL_GATEWAY_NUMBER_STRING;
    /**
     * The suffix to append to the NAI header value for MMS HTTP request (String type)
     */
    public static final String
            MMS_CONFIG_NAI_SUFFIX = CarrierConfigManager.KEY_MMS_NAI_SUFFIX_STRING;
    /**
     * If true, show the cell broadcast (amber alert) in the SMS settings. Some carriers don't want
     * this shown. (Boolean type)
     */
    public static final String MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS =
            CarrierConfigManager.KEY_MMS_SHOW_CELL_BROADCAST_APP_LINKS_BOOL;
    /**
     * Whether the carrier MMSC supports charset field in Content-Type header. If this is false,
     * then we don't add "charset" to "Content-Type"
     */
    public static final String MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER =
            CarrierConfigManager.KEY_MMS_SUPPORT_HTTP_CHARSET_HEADER_BOOL;
    /*
     * Forwarded constants from SimDialogActivity.
     */
    private static String DIALOG_TYPE_KEY = "dialog_type";
    private static final int SMS_PICK = 2;

    // MTK-START
    /** Tablet Multi-user feature */
    private IOnlyOwnerSimSupport mOnlyOwnerSimSupport = null;

    /** 4G data only feature */
    private IDataOnlySmsFwkExt mDataOnlySmsFwkExt = null;
    // MTK-END

    /**
     * Send a text based SMS.
     *
     * <p class="note"><strong>Note:</strong> Using this method requires that your app has the
     * {@link android.Manifest.permission#SEND_SMS} permission.</p>
     *
     * <p class="note"><strong>Note:</strong> Beginning with Android 4.4 (API level 19), if
     * <em>and only if</em> an app is not selected as the default SMS app, the system automatically
     * writes messages sent using this method to the SMS Provider (the default SMS app is always
     * responsible for writing its sent messages to the SMS Provider). For information about
     * how to behave as the default SMS app, see {@link android.provider.Telephony}.</p>
     *
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or text are empty
     */
    public void sendTextMessage(
            String destinationAddress, String scAddress, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]begin*/
        if (SystemProperties.get("ro.bd_aimei_student").equals("1") && isForbid()) {
            android.util.Log.d("gaowei2222","=========sendTextMessage======");
            if (!isFamilyNum(destinationAddress)) {
                android.util.Log.d("gaowei2222","familynum destinationAddress = "+destinationAddress);
                return;
            }
        }
        /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]end*/
        sendTextMessageInternal(destinationAddress, scAddress, text,
            sentIntent, deliveryIntent, true /* persistMessageForCarrierApp*/);
    }

    private void sendTextMessageInternal(String destinationAddress, String scAddress,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean persistMessageForCarrierApp) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        // MTK-START
        // Support empty content
        //if (TextUtils.isEmpty(text)) {
        //    throw new IllegalArgumentException("Invalid message body");
        //}

        Rlog.d(TAG, "sendTextMessage, text=" + text + ", destinationAddress=" + destinationAddress);

        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }

        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendTextForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(),
                    destinationAddress,
                    scAddress, text, sentIntent, deliveryIntent,
                    persistMessageForCarrierApp);
        } catch (RemoteException ex) {
            // ignore it
            // MTK-START
            Rlog.d(TAG, "sendTextMessage, RemoteException!");
            // MTK-END
        }
    }

    /**
     * Send a text based SMS without writing it into the SMS Provider.
     *
     * <p>Only the carrier app can call this method.</p>
     *
     * @see #sendTextMessage(String, String, String, PendingIntent, PendingIntent)
     * @hide
     */
    public void sendTextMessageWithoutPersisting(
            String destinationAddress, String scAddress, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendTextMessageInternal(destinationAddress, scAddress, text,
            sentIntent, deliveryIntent, false /* persistMessageForCarrierApp*/);
    }

    /**
     * A variant of {@link SmsManager#sendTextMessage} that allows self to be the caller. This is
     * for internal use only.
     *
     * @hide
     */
    public void sendTextMessageWithSelfPermissions(
            String destinationAddress, String scAddress, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        // MTK-START
        // Support empty content
        //if (TextUtils.isEmpty(text)) {
        //    throw new IllegalArgumentException("Invalid message body");
        //}

        Rlog.d(TAG, "sendTextMessage, text=" + text + ", destinationAddress=" + destinationAddress);

        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }

        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendTextForSubscriberWithSelfPermissions(getSubscriptionId(),
                    ActivityThread.currentPackageName(),
                    destinationAddress,
                    scAddress, text, sentIntent, deliveryIntent);
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Inject an SMS PDU into the android application framework.
     *
     * The caller should have carrier privileges.
     * @see android.telephony.TelephonyManager#hasCarrierPrivileges
     *
     * @param pdu is the byte array of pdu to be injected into android application framework
     * @param format is the format of SMS pdu (3gpp or 3gpp2)
     * @param receivedIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully received by the
     *  android application framework, or failed. This intent is broadcasted at
     *  the same time an SMS received from radio is acknowledged back.
     *  The result code will be <code>RESULT_SMS_HANDLED</code> for success, or
     *  <code>RESULT_SMS_GENERIC_ERROR</code> for error.
     *
     * @throws IllegalArgumentException if format is not one of 3gpp and 3gpp2.
     */
    public void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        if (!format.equals(SmsMessage.FORMAT_3GPP) && !format.equals(SmsMessage.FORMAT_3GPP2)) {
            // Format must be either 3gpp or 3gpp2.
            throw new IllegalArgumentException(
                    "Invalid pdu format. format must be either 3gpp or 3gpp2");
        }
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                iccISms.injectSmsPduForSubscriber(
                        getSubscriptionId(), pdu, format, receivedIntent);
            }
        } catch (RemoteException ex) {
          // ignore it
        }
    }

    /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     *
     * @throws IllegalArgumentException if text is null
     */
    public ArrayList<String> divideMessage(String text) {
        if (null == text) {
            throw new IllegalArgumentException("text is null");
        }
        return SmsMessage.fragmentText(text);
    }

    /**
     * Send a multi-part text based SMS.  The callee should have already
     * divided the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     *
     * <p class="note"><strong>Note:</strong> Using this method requires that your app has the
     * {@link android.Manifest.permission#SEND_SMS} permission.</p>
     *
     * <p class="note"><strong>Note:</strong> Beginning with Android 4.4 (API level 19), if
     * <em>and only if</em> an app is not selected as the default SMS app, the system automatically
     * writes messages sent using this method to the SMS Provider (the default SMS app is always
     * responsible for writing its sent messages to the SMS Provider). For information about
     * how to behave as the default SMS app, see {@link android.provider.Telephony}.</p>
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK</code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applications,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public void sendMultipartTextMessage(
            String destinationAddress, String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        sendMultipartTextMessageInternal(destinationAddress, scAddress, parts,
              sentIntents, deliveryIntents, true /* persistMessageForCarrierApp*/);
    }

    private void sendMultipartTextMessageInternal(
            String destinationAddress, String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents,
            boolean persistMessageForCarrierApp) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        // MTK-START
        // Support empty content
        //if (parts == null || parts.size() < 1) {
        //    throw new IllegalArgumentException("Invalid message body");
        //}

        Rlog.d(TAG, "sendMultipartTextMessage, destinationAddress=" + destinationAddress);

        if (!isValidParameters(destinationAddress, parts, sentIntents)) {
            return;
        }

        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        if (parts.size() > 1) {
            try {
                ISms iccISms = getISmsServiceOrThrow();
                iccISms.sendMultipartTextForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(),
                        destinationAddress, scAddress, parts,
                        sentIntents, deliveryIntents, persistMessageForCarrierApp);
            } catch (RemoteException ex) {
                // ignore it
                // MTK-START
                Rlog.d(TAG, "sendMultipartTextMessage, RemoteException!");
                // MTK-END
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }
            // MTK-START
            // If content is null, pass the empty string
            String text = (parts == null || parts.size() == 0) ? "" : parts.get(0);
            sendTextMessage(destinationAddress, scAddress, text,
                    sentIntent, deliveryIntent);
            // MTK-END
        }
    }

    /**
     * Send a multi-part text based SMS without writing it into the SMS Provider.
     *
     * <p>Only the carrier app can call this method.</p>
     *
     * @see #sendMultipartTextMessage(String, String, ArrayList, ArrayList, ArrayList)
     * @hide
     **/
    public void sendMultipartTextMessageWithoutPersisting(
            String destinationAddress, String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        sendMultipartTextMessageInternal(destinationAddress, scAddress, parts,
            sentIntents, deliveryIntents, false /* persistMessageForCarrierApp*/);
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * <p class="note"><strong>Note:</strong> Using this method requires that your app has the
     * {@link android.Manifest.permission#SEND_SMS} permission.</p>
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public void sendDataMessage(
            String destinationAddress, String scAddress, short destinationPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        // MTK-START
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);

        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {
            return;
        }

        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendDataForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(),
                    destinationAddress, scAddress, destinationPort & 0xFFFF,
                    data, sentIntent, deliveryIntent);
        } catch (RemoteException ex) {
            // ignore it
            // MTK-START
            Rlog.d(TAG, "sendDataMessage, RemoteException!");
            // MTK-END
        }
    }

    /**
     * A variant of {@link SmsManager#sendDataMessage} that allows self to be the caller. This is
     * for internal use only.
     *
     * @hide
     */
    public void sendDataMessageWithSelfPermissions(
            String destinationAddress, String scAddress, short destinationPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        // MTK-START
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);

        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {
            return;
        }

        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(1);
        sentIntents.add(sentIntent);
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendDataForSubscriberWithSelfPermissions(getSubscriptionId(),
                    ActivityThread.currentPackageName(), destinationAddress, scAddress,
                    destinationPort & 0xFFFF, data, sentIntent, deliveryIntent);
        } catch (RemoteException ex) {
            // ignore it
        }
    }



    /**
     * Get the SmsManager associated with the default subscription id. The instance will always be
     * associated with the default subscription id, even if the default subscription id is changed.
     *
     * @return the SmsManager associated with the default subscription id
     */
    public static SmsManager getDefault() {
        return sInstance;
    }

    /**
     * Get the the instance of the SmsManager associated with a particular subscription id
     *
     * @param subId an SMS subscription id, typically accessed using
     *   {@link android.telephony.SubscriptionManager}
     * @return the instance of the SmsManager associated with subId
     */
    public static SmsManager getSmsManagerForSubscriptionId(int subId) {
        // TODO(shri): Add javadoc link once SubscriptionManager is made public api
        synchronized(sLockObject) {
            SmsManager smsManager = sSubInstances.get(subId);
            if (smsManager == null) {
                smsManager = new SmsManager(subId);
                sSubInstances.put(subId, smsManager);
            }
            return smsManager;
        }
    }

    private SmsManager(int subId) {
        mSubId = subId;

        // MTK-START
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            /** Tablet Multi-user feature */
            try {
                mOnlyOwnerSimSupport = MPlugin.createInstance(IOnlyOwnerSimSupport.class.getName());
                if (mOnlyOwnerSimSupport != null) {
                    String actualClassName = mOnlyOwnerSimSupport.getClass().getName();
                    Rlog.d(TAG, "initial mOnlyOwnerSimSupport done, actual class name is " +
                            actualClassName);
                } else {
                    Rlog.e(TAG, "FAIL! intial mOnlyOwnerSimSupport");
                }
            } catch (RuntimeException e) {
                Rlog.e(TAG, "FAIL! No IOnlyOwnerSimSupport");
            }

            /** 4G data only feature */
            try {
                mDataOnlySmsFwkExt = MPlugin.createInstance(IDataOnlySmsFwkExt.class.getName());
                if (mDataOnlySmsFwkExt != null) {
                    String className = mDataOnlySmsFwkExt.getClass().getName();
                    Rlog.d(TAG, "initial mDataOnlySmsFwkExt done, class name is " + className);
                } else {
                    Rlog.e(TAG, "FAIL! intial mDataOnlySmsFwkExt");
                }
            } catch (RuntimeException e) {
                Rlog.e(TAG, "FAIL! No mDataOnlySmsFwkExt");
            }
        }
        // MTK-END
    }

    /**
     * Get the associated subscription id. If the instance was returned by {@link #getDefault()},
     * then this method may return different values at different points in time (if the user
     * changes the default subscription id). It will return < 0 if the default subscription id
     * cannot be determined.
     *
     * Additionally, to support legacy applications that are not multi-SIM aware,
     * if the following are true:
     *     - We are using a multi-SIM device
     *     - A default SMS SIM has not been selected
     *     - At least one SIM subscription is available
     * then ask the user to set the default SMS SIM.
     *
     * @return associated subscription id
     */
    public int getSubscriptionId() {
        final int subId = (mSubId == DEFAULT_SUBSCRIPTION_ID)
                ? getDefaultSmsSubscriptionId() : mSubId;
        boolean isSmsSimPickActivityNeeded = false;
        final Context context = ActivityThread.currentApplication().getApplicationContext();
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                isSmsSimPickActivityNeeded = iccISms.isSmsSimPickActivityNeeded(subId);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "Exception in getSubscriptionId");
        }

        // MTK-START
        // Mark since MTK have another SIM Card selection logic
        isSmsSimPickActivityNeeded = false;
        // MTK-END
        if (isSmsSimPickActivityNeeded) {
            Log.d(TAG, "getSubscriptionId isSmsSimPickActivityNeeded is true");
            // ask the user for a default SMS SIM.
            Intent intent = new Intent();
            intent.setClassName("com.android.settings",
                    "com.android.settings.sim.SimDialogActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(DIALOG_TYPE_KEY, SMS_PICK);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException anfe) {
                // If Settings is not installed, only log the error as we do not want to break
                // legacy applications.
                Log.e(TAG, "Unable to launch Settings application.");
            }
        }

        return subId;
    }

    /**
     * Returns the ISms service, or throws an UnsupportedOperationException if
     * the service does not exist.
     */
    private static ISms getISmsServiceOrThrow() {
        ISms iccISms = getISmsService();
        if (iccISms == null) {
            throw new UnsupportedOperationException("Sms is not supported");
        }
        return iccISms;
    }

    private static ISms getISmsService() {
        return ISms.Stub.asInterface(ServiceManager.getService("isms"));
    }

    /**
     * Copy a raw SMS PDU to the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param smsc the SMSC for this message, or NULL for the default SMSC
     * @param pdu the raw PDU to store
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @return true for success
     *
     * @throws IllegalArgumentException if pdu is NULL
     * {@hide}
     */
    public boolean copyMessageToIcc(byte[] smsc, byte[] pdu,int status) {
        // MTK-START
        Rlog.d(TAG, "copyMessageToIcc");
        // MTK-END
        boolean success = false;
        // MTK-START
        SimSmsInsertStatus smsStatus = null;
        // MTK-END

        if (null == pdu) {
            throw new IllegalArgumentException("pdu is NULL");
        }

        // MTK-START
        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.copyMessageToIccEfForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(),
                        status, pdu, smsc);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Delete the specified message from the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param messageIndex is the record index of the message on ICC
     * @return true for success
     *
     * {@hide}
     */
    public boolean
    deleteMessageFromIcc(int messageIndex) {
        // MTK-START
        Rlog.d(TAG, "deleteMessageFromIcc, messageIndex=" + messageIndex);
        // MTK-END
        boolean success = false;

        // MTK-START
        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }
        // MTK-END

        byte[] pdu = new byte[IccConstants.SMS_RECORD_LENGTH-1];
        Arrays.fill(pdu, (byte)0xff);

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.updateMessageOnIccEfForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(),
                        messageIndex, STATUS_ON_ICC_FREE, pdu);
            }
        } catch (RemoteException ex) {
            // ignore it
            // MTK-START
            Rlog.d(TAG, "deleteMessageFromIcc, RemoteException!");
            // MTK-END
        }

        return success;
    }

    /**
     * Update the specified message on the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param messageIndex record index of message to update
     * @param newStatus new message status (STATUS_ON_ICC_READ,
     *                  STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *                  STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu the raw PDU to store
     * @return true for success
     *
     * {@hide}
     */
    public boolean updateMessageOnIcc(int messageIndex, int newStatus, byte[] pdu) {
        // MTK-START
        Rlog.d(TAG, "updateMessageOnIcc, messageIndex=" + messageIndex);
        // MTK-END
        boolean success = false;

        // MTK-START
        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false ;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.updateMessageOnIccEfForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(),
                        messageIndex, newStatus, pdu);
            }
        } catch (RemoteException ex) {
            // ignore it
            // MTK-START
            Rlog.d(TAG, "updateMessageOnIcc, RemoteException!");
            // MTK-END
        }

        return success;
    }

    /**
     * Retrieves all messages currently stored on ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects
     *
     * {@hide}
     */
    public ArrayList<SmsMessage> getAllMessagesFromIcc() {
        // MTK-START
        Rlog.d(TAG, "getAllMessagesFromIcc");
        // MTK-END
        List<SmsRawData> records = null;

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEfForSubscriber(
                        getSubscriptionId(),
                        ActivityThread.currentPackageName());
            }
        } catch (RemoteException ex) {
            // ignore it
            // MTK-START
            Rlog.d(TAG, "getAllMessagesFromIcc, RemoteException!");
            // MTK-END
        }

        return createMessageListFromRawRecords(records);
    }

    /**
     * Enable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier and RAN type. The RAN type specify this message ID
     * belong to 3GPP (GSM) or 3GPP2(CDMA).Note that if two different clients
     * enable the same message identifier, they must both disable it for the device to stop
     * receiving those messages. All received messages will be broadcast in an
     * intent with the action "android.provider.Telephony.SMS_CB_RECEIVED".
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param messageIdentifier Message identifier as specified in TS 23.041 (3GPP)
     * or C.R1001-G (3GPP2)
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     * @see #disableCellBroadcast(int, int)
     *
     * {@hide}
     */
    public boolean enableCellBroadcast(int messageIdentifier, int ranType) {
        boolean success = false;

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.enableCellBroadcastForSubscriber(
                        getSubscriptionId(), messageIdentifier, ranType);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Disable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier and RAN type. The RAN type specify this message ID
     * belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different clients
     * enable the same message identifier, they must both disable it for the
     * device to stop receiving those messages.
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param messageIdentifier Message identifier as specified in TS 23.041 (3GPP)
     * or C.R1001-G (3GPP2)
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     *
     * @see #enableCellBroadcast(int, int)
     *
     * {@hide}
     */
    public boolean disableCellBroadcast(int messageIdentifier, int ranType) {
        boolean success = false;

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.disableCellBroadcastForSubscriber(
                        getSubscriptionId(), messageIdentifier, ranType);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Enable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier range and RAN type. The RAN type specify this message ID
     * belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different clients enable
     * the same message identifier, they must both disable it for the device to stop
     * receiving those messages. All received messages will be broadcast in an
     * intent with the action "android.provider.Telephony.SMS_CB_RECEIVED".
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param startMessageId first message identifier as specified in TS 23.041 (3GPP)
     * or C.R1001-G (3GPP2)
     * @param endMessageId last message identifier as specified in TS 23.041 (3GPP)
     * or C.R1001-G (3GPP2)
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     * @see #disableCellBroadcastRange(int, int, int)
     *
     * @throws IllegalArgumentException if endMessageId < startMessageId
     * {@hide}
     */
    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        boolean success = false;

        if (endMessageId < startMessageId) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.enableCellBroadcastRangeForSubscriber(getSubscriptionId(),
                        startMessageId, endMessageId, ranType);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Disable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier range and RAN type. The RAN type specify this message
     * ID range belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different
     * clients enable the same message identifier, they must both disable it for
     * the device to stop receiving those messages.
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param startMessageId first message identifier as specified in TS 23.041 (3GPP)
     * or C.R1001-G (3GPP2)
     * @param endMessageId last message identifier as specified in TS 23.041 (3GPP)
     * or C.R1001-G (3GPP2)
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     *
     * @see #enableCellBroadcastRange(int, int, int)
     *
     * @throws IllegalArgumentException if endMessageId < startMessageId
     * {@hide}
     */
    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        boolean success = false;

        if (endMessageId < startMessageId) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                success = iccISms.disableCellBroadcastRangeForSubscriber(getSubscriptionId(),
                        startMessageId, endMessageId, ranType);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Create a list of <code>SmsMessage</code>s from a list of RawSmsData
     * records returned by <code>getAllMessagesFromIcc()</code>
     *
     * @param records SMS EF records, returned by
     *   <code>getAllMessagesFromIcc</code>
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects.
     */
    // MTK-START
    private ArrayList<SmsMessage> createMessageListFromRawRecords(List<SmsRawData> records) {
    // MTK-END
        ArrayList<SmsMessage> messages = new ArrayList<SmsMessage>();
        // MTK-START
        Rlog.d(TAG, "createMessageListFromRawRecords");
        // MTK-END
        if (records != null) {
            int count = records.size();
            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);
                // List contains all records, including "free" records (null)
                if (data != null) {
                    // MTK-START
                    int activePhone = TelephonyManager.getDefault().getCurrentPhoneType(mSubId);
                    String phoneType = (PhoneConstants.PHONE_TYPE_CDMA == activePhone)
                            ? SmsConstants.FORMAT_3GPP2 : SmsConstants.FORMAT_3GPP;
                    Rlog.d(TAG, "phoneType: " + phoneType);
                    SmsMessage sms = SmsMessage.createFromEfRecord(i + 1, data.getBytes(),
                            phoneType);
                    //SmsMessage sms = SmsMessage.createFromEfRecord(i+1, data.getBytes());
                    // MTK-END
                    if (sms != null) {
                        messages.add(sms);
                    }
                }
            }
            // MTK-START
            Rlog.d(TAG, "actual sms count is " + count);
            // MTK-END
        // MTK-START
        } else {
            Rlog.d(TAG, "fail to parse SIM sms, records is null");
        }
        // MTK-END

        return messages;
    }

    /**
     * SMS over IMS is supported if IMS is registered and SMS is supported
     * on IMS.
     *
     * @return true if SMS over IMS is supported, false otherwise
     *
     * @see #getImsSmsFormat()
     *
     * @hide
     */
    public boolean isImsSmsSupported() {
        boolean boSupported = false;
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                boSupported = iccISms.isImsSmsSupportedForSubscriber(getSubscriptionId());
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return boSupported;
    }

    /**
     * Gets SMS format supported on IMS.  SMS over IMS format is
     * either 3GPP or 3GPP2.
     *
     * @return SmsMessage.FORMAT_3GPP,
     *         SmsMessage.FORMAT_3GPP2
     *      or SmsMessage.FORMAT_UNKNOWN
     *
     * @see #isImsSmsSupported()
     *
     * @hide
     */
    public String getImsSmsFormat() {
        String format = com.android.internal.telephony.SmsConstants.FORMAT_UNKNOWN;
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                format = iccISms.getImsSmsFormatForSubscriber(getSubscriptionId());
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return format;
    }

    /**
     * Get default sms subscription id
     *
     * @return the default SMS subscription id
     */
    public static int getDefaultSmsSubscriptionId() {
        ISms iccISms = null;
        try {
            iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            return iccISms.getPreferredSmsSubscription();
        } catch (RemoteException ex) {
            return -1;
        } catch (NullPointerException ex) {
            return -1;
        }
    }

    /**
     * Get SMS prompt property,  enabled or not
     *
     * @return true if enabled, false otherwise
     * @hide
     */
    public boolean isSMSPromptEnabled() {
        ISms iccISms = null;
        try {
            iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            return iccISms.isSMSPromptEnabled();
        } catch (RemoteException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    // see SmsMessage.getStatusOnIcc

    /** Free space (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_FREE      = 0;

    /** Received and read (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_READ      = 1;

    /** Received and unread (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_UNREAD    = 3;

    /** Stored and sent (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_SENT      = 5;

    /** Stored and unsent (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_UNSENT    = 7;

    // SMS send failure result codes

    /** Generic failure cause */
    static public final int RESULT_ERROR_GENERIC_FAILURE    = 1;
    /** Failed because radio was explicitly turned off */
    static public final int RESULT_ERROR_RADIO_OFF          = 2;
    /** Failed because no pdu provided */
    static public final int RESULT_ERROR_NULL_PDU           = 3;
    /** Failed because service is currently unavailable */
    static public final int RESULT_ERROR_NO_SERVICE         = 4;
    /** Failed because we reached the sending queue limit.  {@hide} */
    static public final int RESULT_ERROR_LIMIT_EXCEEDED     = 5;
    /** Failed because FDN is enabled. {@hide} */
    static public final int RESULT_ERROR_FDN_CHECK_FAILURE  = 6;

    static private final String PHONE_PACKAGE_NAME = "com.android.phone";

    // MTK-START
    /**
     * Sucessful error code.
     *
     * @internal
     * @hide
     */
    static public final int RESULT_ERROR_SUCCESS = 0;
    /**
     * Failed because sim memory is full.
     *
     * @internal
     * @hide
     */
    static public final int RESULT_ERROR_SIM_MEM_FULL = 7;
    /** @hide */
    static public final int RESULT_ERROR_INVALID_ADDRESS = 8;

    // for SMS validity period feature
    /**
     * Support to change the validity period.
     * Extra parameter on bundle for validity period.
     *
     * @internal
     * @hide
     */
    public static final String EXTRA_PARAMS_VALIDITY_PERIOD = "validity_period";

    /** @hide */
    public static final String EXTRA_PARAMS_ENCODING_TYPE = "encoding_type";

    /**
     * Support to change the validity period.
     * The value of no duration.
     *
     * @internal
     * @hide
     */
    public static final int VALIDITY_PERIOD_NO_DURATION = -1;

    /**
     * Support to change the validity period.
     * The value of one hour.
     *
     * @internal
     * @hide
     */
    public static final int VALIDITY_PERIOD_ONE_HOUR = 11; // (VP + 1) * 5 = 60 Mins

    /**
     * Support to change the validity period.
     * The value of six hours.
     *
     * @internal
     * @hide
     */
    public static final int VALIDITY_PERIOD_SIX_HOURS = 71; // (VP + 1) * 5 = 6 * 60 Mins

    /**
     * Support to change the validity period.
     * The value of twelve hours.
     *
     * @internal
     * @hide
     */
    public static final int VALIDITY_PERIOD_TWELVE_HOURS = 143; // (VP + 1) * 5 = 12 * 60 Mins

    /**
     * Support to change the validity period.
     * The value of one day.
     *
     * @internal
     * @hide
     */
    public static final int VALIDITY_PERIOD_ONE_DAY = 167; // 12 + (VP - 143) * 30 Mins = 24 Hours

    /**
     * Support to change the validity period.
     * The value of maximum duration and use the network setting.
     *
     * @internal
     * @hide
     */
    public static final int VALIDITY_PERIOD_MAX_DURATION = 255; // (VP - 192) Weeks
    // MTK-END

    /**
     * Send an MMS message
     *
     * @param context application context
     * @param contentUri the content Uri from which the message pdu will be read
     * @param locationUrl the optional location url where message should be sent to
     * @param configOverrides the carrier-specific messaging configuration values to override for
     *  sending the message.
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed
     * @throws IllegalArgumentException if contentUri is empty
     */
    public void sendMultimediaMessage(Context context, Uri contentUri, String locationUrl,
            Bundle configOverrides, PendingIntent sentIntent) {
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            final IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms == null) {
                return;
            }

            iMms.sendMessage(getSubscriptionId(), ActivityThread.currentPackageName(), contentUri,
                    locationUrl, configOverrides, sentIntent);
        } catch (RemoteException e) {
            // Ignore it
        }
    }

    /**
     * Download an MMS message from carrier by a given location URL
     *
     * @param context application context
     * @param locationUrl the location URL of the MMS message to be downloaded, usually obtained
     *  from the MMS WAP push notification
     * @param contentUri the content uri to which the downloaded pdu will be written
     * @param configOverrides the carrier-specific messaging configuration values to override for
     *  downloading the message.
     * @param downloadedIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is downloaded, or the download is failed
     * @throws IllegalArgumentException if locationUrl or contentUri is empty
     */
    public void downloadMultimediaMessage(Context context, String locationUrl, Uri contentUri,
            Bundle configOverrides, PendingIntent downloadedIntent) {
        if (TextUtils.isEmpty(locationUrl)) {
            throw new IllegalArgumentException("Empty MMS location URL");
        }
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            final IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms == null) {
                return;
            }
            iMms.downloadMessage(
                    getSubscriptionId(), ActivityThread.currentPackageName(), locationUrl,
                    contentUri, configOverrides, downloadedIntent);
        } catch (RemoteException e) {
            // Ignore it
        }
    }

    // MMS send/download failure result codes
    public static final int MMS_ERROR_UNSPECIFIED = 1;
    public static final int MMS_ERROR_INVALID_APN = 2;
    public static final int MMS_ERROR_UNABLE_CONNECT_MMS = 3;
    public static final int MMS_ERROR_HTTP_FAILURE = 4;
    public static final int MMS_ERROR_IO_ERROR = 5;
    public static final int MMS_ERROR_RETRY = 6;
    public static final int MMS_ERROR_CONFIGURATION_ERROR = 7;
    public static final int MMS_ERROR_NO_DATA_NETWORK = 8;

    /** Intent extra name for MMS sending result data in byte array type */
    public static final String EXTRA_MMS_DATA = "android.telephony.extra.MMS_DATA";
    /** Intent extra name for HTTP status code for MMS HTTP failure in integer type */
    public static final String EXTRA_MMS_HTTP_STATUS = "android.telephony.extra.MMS_HTTP_STATUS";

    /**
     * Import a text message into system's SMS store
     *
     * Only default SMS apps can import SMS
     *
     * @param address the destination(source) address of the sent(received) message
     * @param type the type of the message
     * @param text the message text
     * @param timestampMillis the message timestamp in milliseconds
     * @param seen if the message is seen
     * @param read if the message is read
     * @return the message URI, null if failed
     * @hide
     */
    public Uri importTextMessage(String address, int type, String text, long timestampMillis,
            boolean seen, boolean read) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.importTextMessage(ActivityThread.currentPackageName(),
                        address, type, text, timestampMillis, seen, read);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return null;
    }

    /** Represents the received SMS message for importing {@hide} */
    public static final int SMS_TYPE_INCOMING = 0;
    /** Represents the sent SMS message for importing {@hide} */
    public static final int SMS_TYPE_OUTGOING = 1;

    /**
     * Import a multimedia message into system's MMS store. Only the following PDU type is
     * supported: Retrieve.conf, Send.req, Notification.ind, Delivery.ind, Read-Orig.ind
     *
     * Only default SMS apps can import MMS
     *
     * @param contentUri the content uri from which to read the PDU of the message to import
     * @param messageId the optional message id. Use null if not specifying
     * @param timestampSecs the optional message timestamp. Use -1 if not specifying
     * @param seen if the message is seen
     * @param read if the message is read
     * @return the message URI, null if failed
     * @throws IllegalArgumentException if pdu is empty
     * {@hide}
     */
    public Uri importMultimediaMessage(Uri contentUri, String messageId, long timestampSecs,
            boolean seen, boolean read) {
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.importMultimediaMessage(ActivityThread.currentPackageName(),
                        contentUri, messageId, timestampSecs, seen, read);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return null;
    }

    /**
     * Delete a system stored SMS or MMS message
     *
     * Only default SMS apps can delete system stored SMS and MMS messages
     *
     * @param messageUri the URI of the stored message
     * @return true if deletion is successful, false otherwise
     * @throws IllegalArgumentException if messageUri is empty
     * {@hide}
     */
    public boolean deleteStoredMessage(Uri messageUri) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.deleteStoredMessage(ActivityThread.currentPackageName(), messageUri);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return false;
    }

    /**
     * Delete a system stored SMS or MMS thread
     *
     * Only default SMS apps can delete system stored SMS and MMS conversations
     *
     * @param conversationId the ID of the message conversation
     * @return true if deletion is successful, false otherwise
     * {@hide}
     */
    public boolean deleteStoredConversation(long conversationId) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.deleteStoredConversation(
                        ActivityThread.currentPackageName(), conversationId);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return false;
    }

    /**
     * Update the status properties of a system stored SMS or MMS message, e.g.
     * the read status of a message, etc.
     *
     * @param messageUri the URI of the stored message
     * @param statusValues a list of status properties in key-value pairs to update
     * @return true if update is successful, false otherwise
     * @throws IllegalArgumentException if messageUri is empty
     * {@hide}
     */
    public boolean updateStoredMessageStatus(Uri messageUri, ContentValues statusValues) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.updateStoredMessageStatus(ActivityThread.currentPackageName(),
                        messageUri, statusValues);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return false;
    }

    /** Message status property: whether the message has been seen. 1 means seen, 0 not {@hide} */
    public static final String MESSAGE_STATUS_SEEN = "seen";
    /** Message status property: whether the message has been read. 1 means read, 0 not {@hide} */
    public static final String MESSAGE_STATUS_READ = "read";

    /**
     * Archive or unarchive a stored conversation
     *
     * @param conversationId the ID of the message conversation
     * @param archived true to archive the conversation, false to unarchive
     * @return true if update is successful, false otherwise
     * {@hide}
     */
    public boolean archiveStoredConversation(long conversationId, boolean archived) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.archiveStoredConversation(ActivityThread.currentPackageName(),
                        conversationId, archived);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return false;
    }

    /**
     * Add a text message draft to system SMS store
     *
     * Only default SMS apps can add SMS draft
     *
     * @param address the destination address of message
     * @param text the body of the message to send
     * @return the URI of the stored draft message
     * {@hide}
     */
    public Uri addTextMessageDraft(String address, String text) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.addTextMessageDraft(ActivityThread.currentPackageName(), address, text);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return null;
    }

    /**
     * Add a multimedia message draft to system MMS store
     *
     * Only default SMS apps can add MMS draft
     *
     * @param contentUri the content uri from which to read the PDU data of the draft MMS
     * @return the URI of the stored draft message
     * @throws IllegalArgumentException if pdu is empty
     * {@hide}
     */
    public Uri addMultimediaMessageDraft(Uri contentUri) {
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.addMultimediaMessageDraft(ActivityThread.currentPackageName(),
                        contentUri);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return null;
    }

    /**
     * Send a system stored text message.
     *
     * You can only send a failed text message or a draft text message.
     *
     * @param messageUri the URI of the stored message
     * @param scAddress is the service center address or null to use the current default SMSC
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if messageUri is empty
     * {@hide}
     */
    public void sendStoredTextMessage(Uri messageUri, String scAddress, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        // MTK-START

        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendStoredText(
                    getSubscriptionId(), ActivityThread.currentPackageName(), messageUri,
                    scAddress, sentIntent, deliveryIntent);
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Send a system stored multi-part text message.
     *
     * You can only send a failed text message or a draft text message.
     * The provided <code>PendingIntent</code> lists should match the part number of the
     * divided text of the stored message by using <code>divideMessage</code>
     *
     * @param messageUri the URI of the stored message
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK</code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applications,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if messageUri is empty
     * {@hide}
     */
    public void sendStoredMultipartTextMessage(Uri messageUri, String scAddress,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        // MTK-START
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendStoredMultipartText(
                    getSubscriptionId(), ActivityThread.currentPackageName(), messageUri,
                    scAddress, sentIntents, deliveryIntents);
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Send a system stored MMS message
     *
     * This is used for sending a previously sent, but failed-to-send, message or
     * for sending a text message that has been stored as a draft.
     *
     * @param messageUri the URI of the stored message
     * @param configOverrides the carrier-specific messaging configuration values to override for
     *  sending the message.
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed
     * @throws IllegalArgumentException if messageUri is empty
     * {@hide}
     */
    public void sendStoredMultimediaMessage(Uri messageUri, Bundle configOverrides,
            PendingIntent sentIntent) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                iMms.sendStoredMessage(
                        getSubscriptionId(), ActivityThread.currentPackageName(), messageUri,
                        configOverrides, sentIntent);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Turns on/off the flag to automatically write sent/received SMS/MMS messages into system
     *
     * When this flag is on, all SMS/MMS sent/received are stored by system automatically
     * When this flag is off, only SMS/MMS sent by non-default SMS apps are stored by system
     * automatically
     *
     * This flag can only be changed by default SMS apps
     *
     * @param enabled Whether to enable message auto persisting
     * {@hide}
     */
    public void setAutoPersisting(boolean enabled) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                iMms.setAutoPersisting(ActivityThread.currentPackageName(), enabled);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Get the value of the flag to automatically write sent/received SMS/MMS messages into system
     *
     * When this flag is on, all SMS/MMS sent/received are stored by system automatically
     * When this flag is off, only SMS/MMS sent by non-default SMS apps are stored by system
     * automatically
     *
     * @return the current value of the auto persist flag
     * {@hide}
     */
    public boolean getAutoPersisting() {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.getAutoPersisting();
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return false;
    }

    /**
     * Get carrier-dependent configuration values.
     *
     * @return bundle key/values pairs of configuration values
     */
    public Bundle getCarrierConfigValues() {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.getCarrierConfigValues(getSubscriptionId());
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return null;
    }

    /**
     * Filters a bundle to only contain MMS config variables.
     *
     * This is for use with bundles returned by {@link CarrierConfigManager} which contain MMS
     * config and unrelated config. It is assumed that all MMS_CONFIG_* keys are present in the
     * supplied bundle.
     *
     * @param config a Bundle that contains MMS config variables and possibly more.
     * @return a new Bundle that only contains the MMS_CONFIG_* keys defined above.
     * @hide
     */
    public static Bundle getMmsConfig(BaseBundle config) {
        Bundle filtered = new Bundle();
        filtered.putBoolean(MMS_CONFIG_APPEND_TRANSACTION_ID,
                config.getBoolean(MMS_CONFIG_APPEND_TRANSACTION_ID));
        filtered.putBoolean(MMS_CONFIG_MMS_ENABLED, config.getBoolean(MMS_CONFIG_MMS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_GROUP_MMS_ENABLED,
                config.getBoolean(MMS_CONFIG_GROUP_MMS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED,
                config.getBoolean(MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED));
        filtered.putBoolean(MMS_CONFIG_ALIAS_ENABLED, config.getBoolean(MMS_CONFIG_ALIAS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_ALLOW_ATTACH_AUDIO,
                config.getBoolean(MMS_CONFIG_ALLOW_ATTACH_AUDIO));
        filtered.putBoolean(MMS_CONFIG_MULTIPART_SMS_ENABLED,
                config.getBoolean(MMS_CONFIG_MULTIPART_SMS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED,
                config.getBoolean(MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED));
        filtered.putBoolean(MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION,
                config.getBoolean(MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION));
        filtered.putBoolean(MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES,
                config.getBoolean(MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES));
        filtered.putBoolean(MMS_CONFIG_MMS_READ_REPORT_ENABLED,
                config.getBoolean(MMS_CONFIG_MMS_READ_REPORT_ENABLED));
        filtered.putBoolean(MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED,
                config.getBoolean(MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED));
        filtered.putInt(MMS_CONFIG_MAX_MESSAGE_SIZE, config.getInt(MMS_CONFIG_MAX_MESSAGE_SIZE));
        filtered.putInt(MMS_CONFIG_MAX_IMAGE_WIDTH, config.getInt(MMS_CONFIG_MAX_IMAGE_WIDTH));
        filtered.putInt(MMS_CONFIG_MAX_IMAGE_HEIGHT, config.getInt(MMS_CONFIG_MAX_IMAGE_HEIGHT));
        filtered.putInt(MMS_CONFIG_RECIPIENT_LIMIT, config.getInt(MMS_CONFIG_RECIPIENT_LIMIT));
        filtered.putInt(MMS_CONFIG_ALIAS_MIN_CHARS, config.getInt(MMS_CONFIG_ALIAS_MIN_CHARS));
        filtered.putInt(MMS_CONFIG_ALIAS_MAX_CHARS, config.getInt(MMS_CONFIG_ALIAS_MAX_CHARS));
        filtered.putInt(MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD,
                config.getInt(MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD));
        filtered.putInt(MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD,
                config.getInt(MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD));
        filtered.putInt(MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE,
                config.getInt(MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE));
        filtered.putInt(MMS_CONFIG_SUBJECT_MAX_LENGTH,
                config.getInt(MMS_CONFIG_SUBJECT_MAX_LENGTH));
        filtered.putInt(MMS_CONFIG_HTTP_SOCKET_TIMEOUT,
                config.getInt(MMS_CONFIG_HTTP_SOCKET_TIMEOUT));
        filtered.putString(MMS_CONFIG_UA_PROF_TAG_NAME,
                config.getString(MMS_CONFIG_UA_PROF_TAG_NAME));
        filtered.putString(MMS_CONFIG_USER_AGENT, config.getString(MMS_CONFIG_USER_AGENT));
        filtered.putString(MMS_CONFIG_UA_PROF_URL, config.getString(MMS_CONFIG_UA_PROF_URL));
        filtered.putString(MMS_CONFIG_HTTP_PARAMS, config.getString(MMS_CONFIG_HTTP_PARAMS));
        filtered.putString(MMS_CONFIG_EMAIL_GATEWAY_NUMBER,
                config.getString(MMS_CONFIG_EMAIL_GATEWAY_NUMBER));
        filtered.putString(MMS_CONFIG_NAI_SUFFIX, config.getString(MMS_CONFIG_NAI_SUFFIX));
        filtered.putBoolean(MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS,
                config.getBoolean(MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS));
        filtered.putBoolean(MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER,
                config.getBoolean(MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER));
        return filtered;
    }

    // MTK-START
    /**
     * Judge if the destination address is a valid SMS address or not, and if
     * the text is null or not
     *
     * @destinationAddress the destination address to which the message be sent
     * @text the content of shorm message
     * @sentIntent will be broadcast if the address or the text is invalid
     * @return true for valid parameters
     */
    private static boolean isValidParameters(String destinationAddress, String text,
            PendingIntent sentIntent) {
        ArrayList<PendingIntent> sentIntents =
                new ArrayList<PendingIntent>();
        ArrayList<String> parts =
                new ArrayList<String>();

        sentIntents.add(sentIntent);
        parts.add(text);

        // if (TextUtils.isEmpty(text)) {
        // throw new IllegalArgumentException("Invalid message body");
        // }

        return isValidParameters(destinationAddress, parts, sentIntents);
    }

    /**
     * Judges if the destination address is a valid SMS address or not, and if
     * the text is null or not.
     *
     * @param destinationAddress The destination address to which the message be sent
     * @param parts The content of shorm message
     * @param sentIntent will be broadcast if the address or the text is invalid
     * @return True for valid parameters
     */
    private static boolean isValidParameters(String destinationAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents) {
        if (parts == null || parts.size() == 0) {
            return true;
        }

        if (!isValidSmsDestinationAddress(destinationAddress)) {
            for (int i = 0; i < sentIntents.size(); i++) {
                PendingIntent sentIntent = sentIntents.get(i);
                if (sentIntent != null) {
                    try {
                        sentIntent.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                    } catch (CanceledException ex) { }
                }
            }

            Rlog.d(TAG, "Invalid destinationAddress: " + destinationAddress);
            return false;
        }

        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (parts == null || parts.size() < 1) {
            throw new IllegalArgumentException("Invalid message body");
        }

        return true;
    }

    /**
     * judge if the input destination address is a valid SMS address or not
     *
     * @param da the input destination address
     * @return true for success
     *
     */
    private static boolean isValidSmsDestinationAddress(String da) {
        String encodeAddress = PhoneNumberUtils.extractNetworkPortion(da);
        if (encodeAddress == null)
            return true;

        int spaceCount = 0;
        for (int i = 0; i < da.length(); ++i) {
            if (da.charAt(i) == ' ' || da.charAt(i) == '-') {
                spaceCount++;
            }
        }

        return encodeAddress.length() == (da.length() - spaceCount);
    }

    /**
     * Retrieves all messages currently stored on ICC based on different mode.
     * Ex. CDMA mode or GSM mode for international cards.
     *
     * @param subId subscription identity
     * @param mode the GSM mode or CDMA mode
     *
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects
     * @hide
     */
    public ArrayList<SmsMessage> getAllMessagesFromIccEfByMode(int mode) {
        Rlog.d(TAG, "getAllMessagesFromIcc, mode=" + mode);

        List<SmsRawData> records = null;

        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEfByModeForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), mode);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException!");
        }

        int sz = 0;
        if (records != null) {
            sz = records.size();
        }
        for (int i = 0; i < sz; ++i) {
            byte[] data = null;
            SmsRawData record = records.get(i);
            if (record == null) {
                continue;
            } else {
                data = record.getBytes();
            }
            int index = i + 1;
            if ((data[0] & 0xff) == SmsManager.STATUS_ON_ICC_UNREAD) {
                Rlog.d(TAG, "index[" + index + "] is STATUS_ON_ICC_READ");
                boolean ret;
                ret = updateMessageOnIcc(index, SmsManager.STATUS_ON_ICC_READ, data);
                if (ret) {
                    Rlog.d(TAG, "update index[" + index + "] to STATUS_ON_ICC_READ");
                } else {
                    Rlog.d(TAG, "fail to update message status");
                }
            }
        }

        return createMessageListFromRawRecordsByMode(getSubscriptionId(), records, mode);
    }

    /**
     * Create a list of <code>SmsMessage</code>s from a list of RawSmsData
     * records returned by <code>getAllMessagesFromIcc()</code>.
     *
     * @param subId subscription identity
     * @param records SMS EF records, returned by
     *            <code>getAllMessagesFromIcc</code>
     *
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects.
     */
    private static ArrayList<SmsMessage> createMessageListFromRawRecordsByMode(int subId,
            List<SmsRawData> records, int mode) {
        Rlog.d(TAG, "createMessageListFromRawRecordsByMode");

        ArrayList<SmsMessage> msg = null;
        if (records != null) {
            int count = records.size();
            msg = new ArrayList<SmsMessage>();

            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);

                if (data != null) {
                    SmsMessage singleSms =
                            createFromEfRecordByMode(subId, i + 1, data.getBytes(), mode);
                    if (singleSms != null) {
                        msg.add(singleSms);
                    }
                }
            }
            Rlog.d(TAG, "actual sms count is " + msg.size());
        } else {
            Rlog.d(TAG, "fail to parse SIM sms, records is null");
        }

        return msg;
    }

    /**
     * Create an SmsMessage from an SMS EF record.
     *
     * @param index Index of SMS record. This should be index in ArrayList
     *              returned by SmsManager.getAllMessagesFromSim + 1.
     * @param data Record data.
     * @param slotId SIM card the user would like to access
     * @return An SmsMessage representing the record.
     *
     */
    private static SmsMessage createFromEfRecordByMode(int subId, int index, byte[] data,
            int mode) {
        SmsMessage sms = null;

        if (mode == PhoneConstants.PHONE_TYPE_CDMA) {
            sms = SmsMessage.createFromEfRecord(index, data, SmsConstants.FORMAT_3GPP2);
        } else {
            sms = SmsMessage.createFromEfRecord(index, data, SmsConstants.FORMAT_3GPP);
        }

        if (sms != null) {
            sms.setSubId(subId);
        }

        return sms;
    }

    /**
     * Copy a text SMS to the ICC.
     *
     * @param subId subscription identity
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @return success or not
     *
     * @internal
     * @hide
     */
    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp) {
        Rlog.d(TAG, "copyTextMessageToIccCard");
        int result = SmsManager.RESULT_ERROR_GENERIC_FAILURE;

        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return result;
        }

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.copyTextMessageToIccCardForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), scAddress, address, text, status,
                        timestamp);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException!");
        }

        return result;
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param originalPort the port to deliver the message from
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     *
     * @hide
     */
    public void sendDataMessage(String destinationAddress, String scAddress, short destinationPort,
            short originalPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {
            return;
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        // MTK-START
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                iccISms.sendDataWithOriginalPortForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), destinationAddress, scAddress,
                        destinationPort & 0xFFFF, originalPort & 0xFFFF, data, sentIntent,
                        deliveryIntent);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException!");
        }

    }

    /**
     * Send a text based SMS.
     *
     * @param subId subscription identity
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param encodingType the encoding type of message(gsm 7-bit, unicode or automatic)
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or text are empty
     * @hide
     */
    public void sendTextMessageWithEncodingType(String destAddr, String scAddr, String text,
            int encodingType, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "sendTextMessageWithEncodingType, text=" + text + ", encoding=" + encodingType);
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (!isValidParameters(destAddr, text, sentIntent)) {
            Rlog.d(TAG, "the parameters are invalid");
            return;
        }

        // MTK-START
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                iccISms.sendTextWithEncodingTypeForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), destAddr, scAddr, text, encodingType,
                        sentIntent, deliveryIntent, true /*persistMessageForCarrierApp*/);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }
    }

    /**
     * Send a multi-part text based SMS.  The callee should have already
     * divided the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     *
     * @param subId subscription identity
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param encodingType the encoding type of message(gsm 7-bit, unicode or automatic)
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applicaitons,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     *
     * @internal
     * @hide
     */
    public void sendMultipartTextMessageWithEncodingType(String destAddr, String scAddr,
            ArrayList<String> parts, int encodingType, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]begin*/
        if (SystemProperties.get("ro.bd_aimei_student").equals("1") && isForbid()) {
            android.util.Log.d("gaowei2222","=========sendMultipartTextMessageWithEncodingType======");
            if (!isFamilyNum(destAddr)) {
                android.util.Log.d("gaowei2222","familynum destAddr = "+destAddr);
                return;
            }
        }
        /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]end*/
        Rlog.d(TAG, "sendMultipartTextMessageWithEncodingType, encoding=" + encodingType);
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (!isValidParameters(destAddr, parts, sentIntents)) {
            Rlog.d(TAG, "invalid parameters for multipart message");
            return;
        }
        // MTK-START
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        if (parts.size() > 1) {
            try {
                ISms iccISms = getISmsServiceOrThrow();
                if (iccISms != null) {
                    iccISms.sendMultipartTextWithEncodingTypeForSubscriber(getSubscriptionId(),
                            ActivityThread.currentPackageName(), destAddr, scAddr, parts,
                            encodingType, sentIntents, deliveryIntents,
                            true /*persistMessageForCarrierApp*/);
                }
            } catch (RemoteException ex) {
                Rlog.d(TAG, "RemoteException");
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            Rlog.d(TAG, "get sentIntent: " + sentIntent);
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }
            Rlog.d(TAG, "send single message");
            if (parts != null) {
                Rlog.d(TAG, "parts.size = " + parts.size());
            }
            String text = (parts == null || parts.size() == 0) ? "" : parts.get(0);
            Rlog.d(TAG, "pass encoding type " + encodingType);
            sendTextMessageWithEncodingType(destAddr, scAddr, text, encodingType, sentIntent,
                    deliveryIntent);
        }
    }

    /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @param encodingType text encoding type(7-bit, 16-bit or automatic)
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     *
     * @internal
     * @hide
     */
    public ArrayList<String> divideMessage(String text, int encodingType) {
        Rlog.d(TAG, "divideMessage, encoding = " + encodingType);
        ArrayList<String> ret = SmsMessage.fragmentText(text, encodingType);
        Rlog.d(TAG, "divideMessage: size = " + ret.size());
        return ret;
    }

    /**
     * insert a text SMS to the ICC.
     *
     * @param subId subscription identity
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @return SimSmsInsertStatus
     * @hide
     */
    public SimSmsInsertStatus insertTextMessageToIccCard(String scAddress, String address,
            List<String> text, int status, long timestamp) {
        Rlog.d(TAG, "insertTextMessageToIccCard");
        SimSmsInsertStatus ret = null;

        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return null;
        }

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                ret = iccISms.insertTextMessageToIccCardForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), scAddress, address, text, status,
                        timestamp);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        Rlog.d(TAG, (ret != null) ? "insert Text " + ret.indexInIcc : "insert Text null");
        return ret;

    }

    /**
     * Copy a raw SMS PDU to the ICC.
     *
     * @param subId subscription identity
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param pdu the raw PDU to store
     * @param smsc encoded smsc service center
     * @return SimSmsInsertStatus
     * @hide
     */
    public SimSmsInsertStatus insertRawMessageToIccCard(int status, byte[] pdu, byte[] smsc) {
        Rlog.d(TAG, "insertRawMessageToIccCard");
        SimSmsInsertStatus ret = null;

        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "");
            return null;
        }

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                ret = iccISms.insertRawMessageToIccCardForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), status, pdu, smsc);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        Rlog.d(TAG, (ret != null) ? "insert Raw " + ret.indexInIcc : "insert Raw null");
        return ret;
    }

    /**
     * Send an SMS with specified encoding type.
     *
     * @param subId subscription identity
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @hide
     */
    public void sendTextMessageWithExtraParams(String destAddr, String scAddr, String text,
            Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "sendTextMessageWithExtraParams, text=" + text);
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (!isValidParameters(destAddr, text, sentIntent)) {
            return;
        }

        if (extraParams == null) {
            Rlog.d(TAG, "bundle is null");
            return;
        }

        // MTK-START
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                iccISms.sendTextWithExtraParamsForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), destAddr, scAddr, text, extraParams,
                        sentIntent, deliveryIntent, true /*persistMessageForCarrierApp*/);
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }

    }

    /**
     * Send a multi-part text based SMS with specified encoding type.
     *
     * @param subId subscription identity
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @internal
     * @hide
     */
    public void sendMultipartTextMessageWithExtraParams(String destAddr, String scAddr,
            ArrayList<String> parts, Bundle extraParams, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        Rlog.d(TAG, "sendMultipartTextMessageWithExtraParams");
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }

        if (!isValidParameters(destAddr, parts, sentIntents)) {
            return;
        }

        if (extraParams == null) {
            Rlog.d(TAG, "bundle is null");
            return;
        }
        // MTK-START
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (mDataOnlySmsFwkExt != null && mDataOnlySmsFwkExt.is4GDataOnlyMode(
                sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        // MTK-END

        if (parts.size() > 1) {
            try {
                ISms iccISms = getISmsServiceOrThrow();
                if (iccISms != null) {
                    iccISms.sendMultipartTextWithExtraParamsForSubscriber(getSubscriptionId(),
                            ActivityThread.currentPackageName(), destAddr, scAddr, parts,
                            extraParams, sentIntents, deliveryIntents,
                            true /*persistMessageForCarrierApp*/);
                }
            } catch (RemoteException e) {
                Rlog.d(TAG, "RemoteException");
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }

            String text = (parts == null || parts.size() == 0) ? "" : parts.get(0);
            sendTextMessageWithExtraParams(destAddr, scAddr, text, extraParams, sentIntent,
                    deliveryIntent);
        }
    }

    /**
     * Get SMS paramter from icc cards
     *
     * @return <code>SmsParameters</code> object for sms sim card settings.
     *
     * @hide
     */
    public SmsParameters getSmsParameters() {
        Rlog.d(TAG, "getSmsParameters");

        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return null;
        }

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                return iccISms.getSmsParametersForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName());
            } else {
                return null;
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        Rlog.d(TAG, "fail to get SmsParameters");
        return null;

    }

    /**
     * Set sms paramter icc cards.
     *
     * @param params <code>SmsParameters</code>.
     *
     * @return true set complete; false set failed.
     *
     * @hide
     */
    public boolean setSmsParameters(SmsParameters params) {
        Rlog.d(TAG, "setSmsParameters");

        /** Tablet Multi-user feature */
        if (mOnlyOwnerSimSupport != null && !mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                return iccISms.setSmsParametersForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), params);
            } else {
                return false;
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        return false;

    }

    /**
     * Copy SMS to Icc cards.
     *
     * @param smsc service message centers address
     * @param pdu sms pdu
     * @param status sms status
     *
     * @return copied index on Icc cards
     *
     * @hide
     */
    public int copySmsToIcc(byte[] smsc, byte[] pdu, int status) {
        Rlog.d(TAG, "copySmsToIcc");

        SimSmsInsertStatus smsStatus = insertRawMessageToIccCard(status, pdu, smsc);
        if (smsStatus == null) {
            return -1;
        }
        int[] index = smsStatus.getIndex();

        if (index != null && index.length > 0) {
            return index[0];
        }

        return -1;
    }

    /**
     * Update sms status on icc card.
     *
     * @param index updated index of sms on icc card
     * @param read read status
     *
     * @return true updated successful; false updated failed.
     *
     * @hide
     */
    public boolean updateSmsOnSimReadStatus(int index, boolean read) {
        Rlog.d(TAG, "updateSmsOnSimReadStatus");
        SmsRawData record = null;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                record = iccISms.getMessageFromIccEfForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName(), index);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        if (record != null) {
            byte[] rawData = record.getBytes();
            int status = rawData[0] & 0xff;
            Rlog.d(TAG, "sms status is " + status);
            if (status != SmsManager.STATUS_ON_ICC_UNREAD &&
                    status != SmsManager.STATUS_ON_ICC_READ) {
                Rlog.d(TAG, "non-delivery sms " + status);
                return false;
            } else {
                if ((status == SmsManager.STATUS_ON_ICC_UNREAD && read == false)
                        || (status == SmsManager.STATUS_ON_ICC_READ && read == true)) {
                    Rlog.d(TAG, "no need to update status");
                    return true;
                } else {
                    Rlog.d(TAG, "update sms status as " + read);
                    int newStatus = ((read == true) ? SmsManager.STATUS_ON_ICC_READ
                            : SmsManager.STATUS_ON_ICC_UNREAD);
                    return updateMessageOnIcc(index, newStatus, rawData);
                }
            }
        } // end if(record != null)

        Rlog.d(TAG, "record is null");

        return false;
    }

    /**
     * Set ETWS config to modem.
     *
     * @param mode ETWS config mode.
     *
     * @return true set ETWS config successful; false set ETWS config failed.
     *
     * @hide
     */
    public boolean setEtwsConfig(int mode) {
        Rlog.d(TAG, "setEtwsConfig, mode=" + mode);
        boolean ret = false;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                ret = iccISms.setEtwsConfigForSubscriber(getSubscriptionId(), mode);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        return ret;
    }

    /**
     * Set the memory storage status of the SMS.
     * This function is used for FTA test only.
     *
     * @param subId subscription identity
     * @param status false for storage full, true for storage available
     *
     * @internal
     * @hide
     */
    public void setSmsMemoryStatus(boolean status) {
        Rlog.d(TAG, "setSmsMemoryStatus");

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                iccISms.setSmsMemoryStatusForSubscriber(getSubscriptionId(), status);
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }
    }

    /**
     * Get SMS SIM Card memory's total and used number.
     *
     * @param subId subscription identity
     *
     * @return <code>IccSmsStorageStatus</code> object
     *
     * @internal
     * @hide
     */
    public IccSmsStorageStatus getSmsSimMemoryStatus() {
        Rlog.d(TAG, "getSmsSimMemoryStatus");

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                return iccISms.getSmsSimMemoryStatusForSubscriber(getSubscriptionId(),
                        ActivityThread.currentPackageName());
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        return null;
    }

    /**
     * @hide
     */
    private SmsBroadcastConfigInfo Convert2SmsBroadcastConfigInfo(SmsCbConfigInfo info) {
        return new SmsBroadcastConfigInfo(
                info.mFromServiceId,
                info.mToServiceId,
                info.mFromCodeScheme,
                info.mToCodeScheme,
                info.mSelected);
    }

    /**
     * @hide
     */
    private SmsCbConfigInfo Convert2SmsCbConfigInfo(SmsBroadcastConfigInfo info) {
        return new SmsCbConfigInfo(
                info.getFromServiceId(),
                info.getToServiceId(),
                info.getFromCodeScheme(),
                info.getToCodeScheme(),
                info.isSelected());
    }

    /**
     * Set cell broadcast config to icc cards.
     *
     * @return <code>SmsBroadcastConfigInfo</code>
     *
     * @internal
     * @hide
     */
    public SmsBroadcastConfigInfo[] getCellBroadcastSmsConfig() {
        Rlog.d(TAG, "getCellBroadcastSmsConfig");
        Rlog.d(TAG, "subId=" + getSubscriptionId());
        SmsCbConfigInfo[] configs = null;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                configs = iccISms.getCellBroadcastSmsConfigForSubscriber(getSubscriptionId());
            } else {
                Rlog.d(TAG, "fail to get sms service");
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException");
        }

        if (configs != null) {
            Rlog.d(TAG, "config length = " + configs.length);
            int i = 0;
            if (configs.length != 0) {
                SmsBroadcastConfigInfo[] result = new SmsBroadcastConfigInfo[configs.length];
                for (i = 0; i < configs.length; i++)
                    result[i] = Convert2SmsBroadcastConfigInfo(configs[i]);
                return result;
            }
        }

        /*
         * Exception to return null case, Even if there is no channesl,
         * it still have one config with -1
         */
        return null;
    }

    /**
     * Get cell broadcast config from icc cards.
     *
     * @param channels <code>SmsBroadcastConfigInfo</code>
     * @param languages <code>SmsBroadcastConfigInfo</code>
     *
     * @return <code>SmsBroadcastConfigInfo</code>
     *
     * @internal
     * @hide
     */
    public boolean setCellBroadcastSmsConfig(SmsBroadcastConfigInfo[] channels,
            SmsBroadcastConfigInfo[] languages) {
        Rlog.d(TAG, "setCellBroadcastSmsConfig");
        Rlog.d(TAG, "subId=" + getSubscriptionId());
        if (channels != null) {
            Rlog.d(TAG, "channel size=" + channels.length);
        } else {
            Rlog.d(TAG, "channel size=0");
        }
        if (languages != null) {
            Rlog.d(TAG, "language size=" + languages.length);
        } else {
            Rlog.d(TAG, "language size=0");
        }
        boolean result = false;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                int i = 0;
                SmsCbConfigInfo[] channelInfos = null, languageInfos = null;
                if (channels != null && channels.length != 0) {
                    channelInfos = new SmsCbConfigInfo[channels.length];
                    for (i = 0 ; i < channels.length ; i++)
                        channelInfos[i] = Convert2SmsCbConfigInfo(channels[i]);
                }
                if (languages != null && languages.length != 0) {
                    languageInfos = new SmsCbConfigInfo[languages.length];
                    for (i = 0 ; i < languages.length ; i++)
                        languageInfos[i] = Convert2SmsCbConfigInfo(languages[i]);
                }

                result = iccISms.setCellBroadcastSmsConfigForSubscriber(getSubscriptionId(),
                        channelInfos, languageInfos);
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "setCellBroadcastSmsConfig, RemoteException!");
        }

        return result;
    }

    /**
     * Query if cell broadcast activation.
     *
     * @return true activatd; false deactivated.
     *
     * @internal
     * @hide
     */
    public boolean queryCellBroadcastSmsActivation() {
        Rlog.d(TAG, "queryCellBroadcastSmsActivation");
        Rlog.d(TAG, "subId=" + getSubscriptionId());
        boolean result = false;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.queryCellBroadcastSmsActivationForSubscriber(
                        getSubscriptionId());
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoteException!");
        }

        return result;
    }

    /**
     * To activate the cell broadcast.
     *
     * @param activate true activation; false de-activation.
     *
     * @return true process successfully; false process failed.
     *
     * @internal
     * @hide
     */
    public boolean activateCellBroadcastSms(boolean activate) {
        Rlog.d(TAG, "activateCellBroadcastSms activate : " + activate + ", sub = " +
                getSubscriptionId());
        boolean result = false;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.activateCellBroadcastSmsForSubscriber(getSubscriptionId(),
                        activate);
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "fail to activate CB");
            result = false;
        }

        return result;
    }

    /**
     * Remove specified channel and serial of cb message.
     *
     * @param channelId removed channel id
     * @param serialId removed serial id
     *
     * @return true process successfully; false process failed.
     *
     * @hide
     */
    public boolean removeCellBroadcastMsg(int channelId, int serialId) {
        Rlog.d(TAG, "RemoveCellBroadcastMsg, subId=" + getSubscriptionId());
        boolean result = false;

        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.removeCellBroadcastMsgForSubscriber(getSubscriptionId(),
                        channelId, serialId);
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException ex) {
            Rlog.d(TAG, "RemoveCellBroadcastMsg, RemoteException!");
        }

        return result;
    }

    // MTK-END

    /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]begin*/
    private static final Uri mAppUri =
            Uri.parse("content://com.xycm.schoolbased.AppContentProvider/apps");
    private static final String COL_STATE = "state";
    private static final String[] COL_S = {
            COL_STATE
    };
    private static final String SELECTION = "pkgname='com.android.mms'";
    private static final int LOCK = 1;
    private boolean isForbid() {
        Cursor cursor = ActivityThread.currentApplication().getApplicationContext().getContentResolver().query(mAppUri, COL_S, SELECTION, null, null);
        android.util.Log.d("gaowei2222","=========isForbid  cursor======"+cursor);
        if (cursor != null) {
            int stateIndex = cursor.getColumnIndex(COL_STATE);
            android.util.Log.d("gaowei2222","=========isForbid  stateIndex======"+stateIndex);
            while (cursor.moveToNext()) {
                int state = cursor.getInt(stateIndex);
                android.util.Log.d("gaowei2222","=========isForbid  state======"+state);
                if (state == LOCK) {
                    return true;
                }
            }
        }
        return false;
    }
    /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20170617]end*/

    /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20180112]begin*/
    //read family_phone_number from database
    private String[] phoneNums;
    private int EMEGERCY_NUM_MAX;
    private static final String FAMILY_NAME = "familyname";
    private static final String FAMILY_NUM = "familynum";
    private static final String[] SOS_S = {
            FAMILY_NAME,
            FAMILY_NUM
    };
    private static final Uri mFamilyUri =
            Uri.parse("content://com.xycm.schoolbased.FamilyContentProvider/familynum");
    private boolean isFamilyNum(String number) {
        Cursor cursor = ActivityThread.currentApplication().getApplicationContext().getContentResolver().query(mFamilyUri, SOS_S, null, null, null);
        Log.d("gaowei2222", "count = " + cursor.getCount());
        EMEGERCY_NUM_MAX = cursor.getCount();
        phoneNums = new String[EMEGERCY_NUM_MAX];
        int index = 0;
        if (cursor != null) {
            int familyName = cursor.getColumnIndex(FAMILY_NAME);
            int familyNum = cursor.getColumnIndex(FAMILY_NUM);
            while (cursor.moveToNext()) {
                String familyNum1 = cursor.getString(familyNum);
                Log.d("gaowei2222","=====familyNum1===="+familyNum1);
                phoneNums[index] = familyNum1;
            }
        }
        for (int j = 0; j < phoneNums.length; j++) {
            if (number.equals(phoneNums[j])) {
                return true;
            }
        }
        return false;
    }
    /*[BIRD][BIRD_AIMEI_STUDENT][gaowei][20180112]end*/
}
