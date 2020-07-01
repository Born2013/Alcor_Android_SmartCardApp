/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.screenshot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.systemui.R;

public class BirdScreenshotHelpMain extends Activity {

    private final static String TAG = "BirdScreenshotHelpMainTAG";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bird_screenshot_help_main);
        Button fullGuide= (Button) findViewById(R.id.bird_guide_full);
        fullGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startHelpActivity(1);
            }
        });

        Button regionalGuide= (Button) findViewById(R.id.bird_guide_regional);
        regionalGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startHelpActivity(2);
            }
        });

        Button longGuide= (Button) findViewById(R.id.bird_guide_long);
        longGuide.setOnClickListener(new View.OnClickListener() {
        @Override
            public void onClick(View v) {
                startHelpActivity(3);
            }
        });
    }

    private void startHelpActivity(int whichGuide) {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName("com.android.systemui",
            "com.android.systemui.screenshot.BirdScreenshotHelpActivity");
        intent.putExtra("bird_which_guide", whichGuide);
        intent.setComponent(componentName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(intent);
        //finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
