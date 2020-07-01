package com.bird.studentsos;
import android.content.Context;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.app.Application;
import android.util.Log;
public class PhoneApp extends Application{
    public CallManager mCM;
    public static PhoneApp isMe;
    public PhoneApp(){
       isMe = this;
    }
    static PhoneApp getInstance(){
        return isMe;
    }
    @Override
    public void onCreate(){
        mCM = CallManager.getInstance();
        Phone  phone = PhoneFactory.getDefaultPhone();
        mCM.registerPhone(phone);
    }
}
