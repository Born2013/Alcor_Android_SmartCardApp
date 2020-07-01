package com.android.writeimei;

import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.os.SystemProperties;

/*[BIRD][BIRD_WRITE_IMEI][wangjianping][拨号界面写入IMEI] 20120531  BEGIN*/
public class WriteIMEI extends Activity {
    private static final String TAG = "WriteIMEI";
    private  Phone phone = null;
    private String mImei1;
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String IMEI1CODE = (String)intent.getExtra("#6666#");
        String IMEI1=IMEI1CODE;
        mImei1 = IMEI1;
        writeIMEI(IMEI1);	
    }

    public void writeIMEI(String IMEI1){

        String AttachedAT1[] = {"AT+EGMR=1,7,"+"\""+IMEI1+"\"",""};
        String AttachedAT2[] = {"AT+EGMR=1,10,"+"\""+IMEI1+"\"",""};

        if(FeatureOption.MTK_GEMINI_SUPPORT == true)
         {
            phone = PhoneFactory.getDefaultPhone();
            phone.invokeOemRilRequestStrings(AttachedAT1, mResponseHander.obtainMessage(1));
            phone.invokeOemRilRequestStrings(AttachedAT2, mResponseHander.obtainMessage(2)); 
         }
        else
         {
            phone = PhoneFactory.getDefaultPhone();
            phone.invokeOemRilRequestStrings(AttachedAT1,mResponseHander.obtainMessage(1)); 
         } 
     }

    private  Handler mResponseHander = new Handler() {
             public void handleMessage(Message msg){
                 AsyncResult ar;
                 ar= (AsyncResult) msg.obj;
                 switch(msg.what)
                   {
                     case 1:
                     if(ar.exception == null)
                     //"Write IMEI1 Success"
                        {
                           SystemProperties.set("persist.sys.imei1", mImei1);
                           AlertDialog.Builder alert = new AlertDialog.Builder(WriteIMEI.this);
                           alert.setTitle(R.string.write_success);
                           alert.setMessage(R.string.success_content_sim1);
                           alert.setPositiveButton(R.string.ok, 
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int whichButton) {
                                           finish();
                                           }});
                           alert.setCancelable(false);
                           alert.create().show();
                        }
                     else
                     //"Write IMEI1 Fail"
                        {
                           AlertDialog.Builder alert = new AlertDialog.Builder(WriteIMEI.this);
                           alert.setTitle(R.string.write_fail);
                           alert.setMessage(R.string.fail_content_sim1);
                           alert.setPositiveButton(R.string.ok, 
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int whichButton) {
                                           finish();
                                           }});
                           alert.setCancelable(false);
                           alert.create().show();
                        }          
                     break;

                     case 2:
                        if (ar.exception == null) {
                            SystemProperties.set("persist.sys.imei2", mImei1);
                            Log.d(TAG, "  IMEI2 Success");
                        } else {
                            Log.d(TAG, "  IMEI2 Fail");
                        }          
                     break;

                     default:
                     Log.d(TAG, "  default");
                     break;
                   } 
               }
          };


}
/*[BIRD][BIRD_WRITE_IMEI][wangjianping][拨号界面写入IMEI]  20120531  END*/
