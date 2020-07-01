package com.bird.smt_sw;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import com.mediatek.camera.mode.gyfacebeauty.BirdDualCameraNative;

public class MMISubBackCameraTest extends MMINewActivity {

    private Camera mCamera;
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    DeviceTestCameraPreview camera_preview; 
    private Parameters parameters = null;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);
        setTitle(R.string.sub_back_camera);       
		
        camera_preview = (DeviceTestCameraPreview )findViewById(R.id.camera_surface_view);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        BirdDualCameraNative.writeBackCameraState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open(1);  /*main camera*/
        parameters = mCamera.getParameters();
        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);//����
        mCamera.setParameters(parameters);
        camera_preview.setCamera(mCamera);
    }



    @Override
    protected void onPause() {

        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
        	camera_preview.setCamera(null);
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);//�ر�
			mCamera.setParameters(parameters);
            mCamera.release();
            mCamera = null;
        }
        BirdDualCameraNative.writeFrontCameraState();
    }

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {
			mEditor.putInt(MMISubBackCameraTest.this.getString(R.string.sub_back_camera), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};

	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {
			mEditor.putInt(MMISubBackCameraTest.this.getString(R.string.sub_back_camera), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};

	

	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {
			//Intent intent = null;	
			//intent = new Intent(DeviceTestMainCamera.this,DeviceTestMainCamera.class);
			//startActivityForResult(intent, 0);
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMISubBackCameraTest.this, MMISubBackCameraTest.class);
			intent.setComponent(comp);
			
			startActivity(intent);
			finish();
		}
	};
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_HOME){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMISubBackCameraTest.this, MMISubBackCameraTest.class);
			intent.setComponent(comp);
			
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMISubBackCameraTest.this.getString(R.string.sub_back_camera), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
