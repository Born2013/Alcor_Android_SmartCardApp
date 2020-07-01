package com.bird.studentsos;

import com.bird.studentsos.R;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class StudentSosReceiver extends BroadcastReceiver {
    
/**  broadcast Receiver
** onReceive
**/
    public void onReceive(Context context, Intent intent) { 
        Log.d("gaowei0621", "-----------enter StudentSosReceiver-----------");
        if (intent.getAction().equals("android.intent.action.STUDENTSOS")) {
            String action_content = intent.getStringExtra("call_action"); 
            if("star_call".equals(action_content)) {
                Log.d("gaowei0621", "---------- start StudentSosService----------");
                Intent msg = new Intent(context, StudentSosService.class);
                context.startService(msg);
            } else if(action_content.equals("end_call")){
                context.stopService(new Intent(context, StudentSosService.class));
            }   
        }
     }
}
