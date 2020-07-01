package com.bird.smt_sw;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
//import com.mediatek.common.featureoption.FeatureOption;
import android.content.res.TypedArray; 
import android.util.Log;  
import java.util.ArrayList;

public class MMIKeyBoardTest extends MMINewActivity {

    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private  ImageView imageView[];
    private  int position=0;
    private  boolean flag0 = true;
    private  boolean flag1 = true;//true;//只测霍尔开关 跟上、下侧键  //change false to true by zhangaman 20150717
    private  boolean flag2 = true;
    private  boolean flag3 = true;
    private  boolean flag4 = true;
    private  boolean flag5 = true;//true;//只测霍尔开关 跟上、下侧键 //change false to true by zhangaman 20150717
    private  boolean flag6 = true;//true;//只测霍尔开关 跟上、下侧键 //change false to true by zhangaman 20150717
    private  boolean flag7 = true;
    private  boolean flag8 = true;
    private  boolean flag9 = true;
    private  boolean flag10 = true;    
    private  boolean flag11 = true; 
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
	  
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageView = new ImageView[12];
        setContentView(R.layout.keyboard_test);
        setTitle(R.string.keyboard);

        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();

        imageView[0] = (ImageView) findViewById(R.id.showkey1);
        imageView[1] = (ImageView) findViewById(R.id.showkey2);
        imageView[2] = (ImageView) findViewById(R.id.showkey3);
        imageView[3] = (ImageView) findViewById(R.id.showkey4);
        imageView[4] = (ImageView) findViewById(R.id.showkey5);
        imageView[5] = (ImageView) findViewById(R.id.showkey6);
        imageView[6] = (ImageView) findViewById(R.id.showkey7);
        imageView[7] = (ImageView) findViewById(R.id.showkey8);
        imageView[8] = (ImageView) findViewById(R.id.showkey9);
        imageView[9] = (ImageView) findViewById(R.id.showkey10);
        imageView[10] = (ImageView) findViewById(R.id.showkey11);            
        imageView[11] = (ImageView) findViewById(R.id.showkey12);   
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		//[BIRD][BIRD_MMI_TEST_CUSTOM_KEYIMAGE][工厂贴片测试按键值和相应图片自定义][pangmeizhou][20180516]begin
		if(FeatureOption.BIRD_MMI_TEST_CUSTOM_KEYIMAGE){
			if(testKeyCode(keyCode)){
				return true;
			}
		}
		//[BIRD][BIRD_MMI_TEST_CUSTOM_KEYIMAGE][工厂贴片测试按键值和相应图片自定义][pangmeizhou][20180516]end
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                if(flag0){
                    imageView[position++].setImageResource(R.drawable.camera);
                    flag0 =false;
                    return true;
                }	
                break;
            case KeyEvent.KEYCODE_MENU:
                if(flag1 && !FeatureOption.BIRD_SMT_SW_SMALL_VERSION_KEYBOARD){
                    imageView[position++].setImageResource(R.drawable.menu);
                    flag1 =false;
                    return true;
                }	
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(FeatureOption.BIRD_SMT_SW_SMALL_VERSION_KEYBOARD) {
                    if(false/*FeatureOption.BIRD_SMT_SW_HALL_SENSOR_SUPPORT*/) {
                        if(flag2 && !flag8){
                            imageView[position++].setImageResource(R.drawable.decr);
                            flag2 =false;
                            mEditor.putInt(MMIKeyBoardTest.this.getString(R.string.keyboard), Test_launcherActivity.PASS);
                            mEditor.commit();
                            finish();
                            return true;
                        }
                    } else {
                        if(flag2){
                            imageView[position++].setImageResource(R.drawable.decr);
                            flag2 =false;
                            mEditor.putInt(MMIKeyBoardTest.this.getString(R.string.keyboard), Test_launcherActivity.PASS);
                            mEditor.commit();
                            finish();
                            return true;
                        }
                    }
                } else {

                    if(false/*FeatureOption.BIRD_SMT_SW_HALL_SENSOR_SUPPORT*/) {
                        if(!flag1 && flag2 && !flag3 && !flag5 && !flag6 && !flag8){
                            imageView[position++].setImageResource(R.drawable.decr);
                            flag2 =false;
                            mEditor.putInt(MMIKeyBoardTest.this.getString(R.string.keyboard), Test_launcherActivity.PASS);
                            mEditor.commit();
                            finish();
                            return true;
                        } else if (flag2) {
                            //如果是VOLUME_DOWN,以上都没显示，就应该显示为基本的音量下键
                            imageView[position++].setImageResource(R.drawable.decr);
                            flag2 =false;
                            return true;
                        }
                    } else {
                        if(!flag1 && flag2 && !flag3 && !flag5 && !flag6){
                            imageView[position++].setImageResource(R.drawable.decr);
                            flag2 =false;
                            mEditor.putInt(MMIKeyBoardTest.this.getString(R.string.keyboard), Test_launcherActivity.PASS);
                            mEditor.commit();
                            finish();
                            return true;
                        }
                    }

                }
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(flag3 && !FeatureOption.BIRD_SMT_SW_SMALL_VERSION_KEYBOARD){
                    imageView[position++].setImageResource(R.drawable.incre);
                    flag3 =false;
                    return true;
                }	
                break;
            case KeyEvent.KEYCODE_SEARCH:
                if(flag4){
                    imageView[position++].setImageResource(R.drawable.search);
                    flag4 =false;
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if(flag5 && !FeatureOption.BIRD_SMT_SW_SMALL_VERSION_KEYBOARD){
                    imageView[position++].setImageResource(R.drawable.back);
                    flag5 =false;
                    return true;
                }	
                break;
            case KeyEvent.KEYCODE_HOME:
                if(flag6 && !FeatureOption.BIRD_SMT_SW_SMALL_VERSION_KEYBOARD){
                    imageView[position++].setImageResource(R.drawable.home);
                    flag6 =false;
                    return true;
                }	
                break;
            case KeyEvent.KEYCODE_MUTE:
                if(flag7){
                    imageView[position++].setImageResource(R.drawable.mute);
                    flag7 =false;
                    return true;
                }	
                break;
            case KeyEvent.KEYCODE_F2:
            case KeyEvent.KEYCODE_F1:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                if(false/*FeatureOption.BIRD_SMT_SW_HALL_SENSOR_SUPPORT*/) {
                    if(flag8){
                        imageView[position++].setImageResource(R.drawable.hall);
                        flag8 =false;
                        return true;
                    }	
                }
                break;
            case KeyEvent.KEYCODE_F5:
                if(flag9){
                    imageView[position++].setImageResource(R.drawable.ptt);
                    flag9 =false;
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_F6:
                if(flag10){
                    imageView[position++].setImageResource(R.drawable.f6);
                    flag10 =false;
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_F7:
                if(flag11){
                    imageView[position++].setImageResource(R.drawable.f7);
                    flag11 =false;
                    return true;
                }
                break;
            default:
                break;
        }
        return true;
    }

	ArrayList mTestedKeyValue = new ArrayList();
	int getKeycodeImage(int keyCode){
		int[] keycodeValue = getResources().getIntArray(R.array.keycode_value);
		TypedArray mTypedArray = getResources().obtainTypedArray(R.array.keycode_image);
		Log.i("MMIKeyBoardTest","getKeycodeImage keyCode = "+keyCode);
		for(int i=0;i<keycodeValue.length;i++){
			if(keycodeValue[i] == keyCode){
				int imageId=mTypedArray.getResourceId(i, 0);
				return imageId;
			}
		}
		return -1;
	}

	boolean testKeyCode(int keyCode){
		int keyImageId = getKeycodeImage(keyCode);
		if(mTestedKeyValue.contains(keyCode)) {//按键已测试过，就不重复测试
			return true;
		}
		if(keyImageId != -1) {
			imageView[position++].setImageResource(keyImageId);
			mTestedKeyValue.add(keyCode);
			return true;
		}
		return false;
	}

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {

			mEditor.putInt(MMIKeyBoardTest.this.getString(R.string.keyboard), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIKeyBoardTest.this.getString(R.string.keyboard), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIKeyBoardTest.this, MMIKeyBoardTest.class);
			intent.setComponent(comp);

			startActivity(intent);
			finish();
		}
	};
	
}
