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

/*[BIRD][BIRD_WRITE_IMEI][wangjianping][拨号界面写入IMEI]  20120531  BEGIN*/
public class WriteIMEIGemini extends Activity {
    private static final String TAG = "WriteIMEIGemini";
    private  Phone phone = null;
    private String mImei2 = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String IMEI2CODE = (String)intent.getExtra("#7777#");
        String IMEI2=IMEI2CODE;
        mImei2 = IMEI2;
        writeIMEIGemini(IMEI2);	
    }

    public void writeIMEIGemini(String IMEI2){

        String AttachedAT2[] = {"AT+EGMR=1,10,"+"\""+IMEI2+"\"",""};

        if(FeatureOption.MTK_GEMINI_SUPPORT == true)
         {
            phone = PhoneFactory.getDefaultPhone();
            phone.invokeOemRilRequestStrings(AttachedAT2, mResponseHander.obtainMessage(2)); 
         }
     }

    private  Handler mResponseHander = new Handler() {
             public void handleMessage(Message msg){
                 AsyncResult ar;
                 ar= (AsyncResult) msg.obj;
                 switch(msg.what)
                   {
                     case 2:
                     if(ar.exception == null)
                     //"Write IMEI2 Success"
                        {
                            SystemProperties.set("persist.sys.imei2", mImei2);
                           AlertDialog.Builder alert = new AlertDialog.Builder(WriteIMEIGemini.this);
                           alert.setTitle(R.string.write_success);
                           alert.setMessage(R.string.success_content_sim2);
                           alert.setPositiveButton(R.string.ok, 
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int whichButton) {
                                           finish();
                                           }});
                           alert.setCancelable(false);
                           alert.create().show();
                        }
                     else
                     //"Write IMEI2 Fail"
                        {
                           AlertDialog.Builder alert = new AlertDialog.Builder(WriteIMEIGemini.this);
                           alert.setTitle(R.string.write_fail);
                           alert.setMessage(R.string.fail_content_sim2);
                           alert.setPositiveButton(R.string.ok, 
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int whichButton) {
                                           finish();
                                           }});
                           alert.setCancelable(false);
                           alert.create().show();
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
