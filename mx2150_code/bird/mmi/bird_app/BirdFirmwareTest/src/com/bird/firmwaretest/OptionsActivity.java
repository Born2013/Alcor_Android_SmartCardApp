package com.bird.firmwaretest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
/*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]begin*/
import android.content.SharedPreferences;
import com.bird.reboottest.RebootTestActivity;
import android.text.TextUtils;
import android.widget.Toast;
/*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]end*/

public class OptionsActivity extends Activity implements View.OnClickListener {

    private ListView mListView;
    private FirmwareTestAdapter mAdapter;
    private static String[] mTestNameList;
    private ArrayList<MyTestItem> mAllList = new ArrayList<MyTestItem>();

    private EditText mEditDuration;
    private Button mStartButton;

    private AlertDialog mDialog;
    private Button mSelectAllBtn;
    private Button mSelectNoneBtn;
    private Spinner mSpinner;

    private  String mTimeFormat;
    
    /*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]begin*/
    private Button mStartTestBtn;
    private EditText mTestEditText;
    private SharedPreferences preference;
    private String PREFS_NAME = "com.bird.reboot.test";
    private int testCount = 0;
    /*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]end*/
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options_activity);
        android.util.Log.d("shujiang","onCreate");

        initTestView();
        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new FirmwareTestAdapter(this, mAllList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mAdapter == null) {
                    return;
                }
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
                boolean checked = !checkBox.isChecked();
                checkBox.setChecked(checked);
                mAdapter.setChecked(position, checked);
            }
        });

        mEditDuration = (EditText) findViewById(R.id.edit_time);
        mStartButton = (Button) findViewById(R.id.start_btn);
        mStartButton.setOnClickListener(this);

        mSelectAllBtn = (Button) findViewById(R.id.selectAll);
        mSelectNoneBtn = (Button) findViewById(R.id.selectNone);
        mSelectAllBtn.setOnClickListener(this);
        mSelectNoneBtn.setOnClickListener(this);

        mTimeFormat = getString(R.string.hour_format);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] timeFormats = getResources().getStringArray(R.array.time_format);
                mTimeFormat = timeFormats[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        /*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]begin*/
        if (FeatureOption.BIRD_ADD_REBOOT_TEST) {
            findViewById(R.id.reboot_title).setVisibility(View.VISIBLE);
            findViewById(R.id.reboot_layout).setVisibility(View.VISIBLE);
            initRebootView();
        }
        /*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]end*/

    }
    
    /*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]begin*/
    private void initRebootView(){
         mStartTestBtn = (Button)findViewById(R.id.btn_start_test);
        mTestEditText =  (EditText)findViewById(R.id.et_input_count);

        preference = getSharedPreferences(PREFS_NAME,0);
        try {
            testCount = preference.getInt("testCount", -1);
        } catch (Exception e){
            e.printStackTrace();
        }

        mStartTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SharedPreferences.Editor editor = preference.edit();
                String countStr = mTestEditText.getText().toString().trim();
                if (TextUtils.isEmpty(countStr)){
                    Toast.makeText(getApplicationContext(),getString(R.string.error_test_num),Toast.LENGTH_SHORT).show();
                    return;
                }
                testCount = Integer.valueOf(countStr);
                 if (!(testCount>0&&testCount<=100)){
                    Toast.makeText(getApplicationContext(),getString(R.string.error_test_num),Toast.LENGTH_SHORT).show();
                    return;
                }
                editor.putInt("testCount", testCount);
                editor.putInt("usedCount", 0);
                editor.putInt("unusedCount", testCount);
                editor.putString("lastTestTime","");
                editor.putString("recentTestTime","");
                editor.commit();

                Intent ootStartIntent=new Intent(OptionsActivity.this,RebootTestActivity.class);
                startActivity(ootStartIntent);
            }
        });
    }
    /*[BIRD][BIRD_ADD_REBOOT_TEST][老化测试添加重启测试][luye][20170821]end*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.about_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(OptionsActivity.this, AboutActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        android.util.Log.d("shujiang","onRestart");
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void initTestView() {
        mTestNameList = getResources().getStringArray(R.array.all_test_options);
        mAllList.clear();
        for (int i=0; i<mTestNameList.length; i++) {
            MyTestItem testView = new MyTestItem(mTestNameList[i], i, true);
            mAllList.add(testView);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_btn:
                initialTest();
                if (mCheckedListName.size()<=0 || mSelectTime<=0) {
                    showTipsDialog();
                    return;
                }
                showStartTestDialog();
                break;
            case R.id.selectAll:
                setAllChecked(true);
                break;
            case R.id.selectNone:
                setAllChecked(false);
                break;
        }
    }

    private void showStartTestDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle(R.string.start_test_title);
        View view = LayoutInflater.from(this).inflate(R.layout.set_tests_and_times,null);
        TextView mSelectView = (TextView) view.findViewById(R.id.selected_tests);
        TextView mSetTimeView = (TextView) view.findViewById(R.id.set_times);


        mSelectView.setText(mSelectName);
        mSetTimeView.setText(mEditDuration.getText()+mTimeFormat);
        mBuilder.setView(view);
        final ArrayList<String> selectNameList = mCheckedListName;
        final int selectTime = mSelectTime;
        mBuilder.setPositiveButton(getString(R.string.ok_title), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Intent intent = new Intent(OptionsActivity.this, TestActivity.class);
                intent.putStringArrayListExtra(Util.SELECT_TEST_OPTIONS, selectNameList);
                intent.putExtra(Util.SELECT_TEST_TIMES, selectTime);
                intent.putExtra(Util.TIME_FORMAT, mTimeFormat);
                startActivity(intent);
            }
        });
        mBuilder.setNegativeButton(getString(R.string.cancle_title), null);
        mDialog = mBuilder.create();
        mDialog.show();
    }
    private static String mSelectName;
    private static ArrayList<String> mCheckedListName = new ArrayList<String>();
    private static int mSelectTime;

    private void initialTest() {
        ArrayList<MyTestItem> mCheckedList = new ArrayList<MyTestItem>();
        mCheckedList.clear();
        mCheckedListName.clear();
        mCheckedList = mAdapter.getCheckedItems();
        android.util.Log.d("shujiang","initialTest:mCheckedList.size:"+mCheckedList.size());
        mSelectName = "";
        for (int i=0; i<mCheckedList.size(); i++) {
            mSelectName += mCheckedList.get(i).getName()+"/ ";
            mCheckedListName.add(mCheckedList.get(i).getName());
        }
        mSelectTime = mEditDuration.getText().toString().isEmpty()? 0 : Integer.parseInt(mEditDuration.getText().toString());
    }

    private void showTipsDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle(R.string.warning);
        mBuilder.setMessage(R.string.show_tips);
        mBuilder.setPositiveButton(getString(R.string.ok_title),null);
        mBuilder.create().show();
    }

    private void setAllChecked(boolean isCheck) {
        if (mAdapter != null) {
            for (int i = 0; i < mAllList.size(); i++) {
                // mAllList.get(i).setChecked(isCheck);
                mAdapter.setChecked(i, isCheck);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

}
