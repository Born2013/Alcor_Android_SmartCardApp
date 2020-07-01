//[BIRD][BIRD_CLEAN_RECENT_APP_WOTU][喔图 每小时自动清理recent app][yangheng][20170508] BEGIN
package com.android.systemui.recents;

import android.content.BroadcastReceiver;


import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.model.Task;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import com.android.systemui.FeatureOption;

public class BootCompeleteBroadcastReceiver extends BroadcastReceiver {
    
    private Context mContext;
    private String TAG = "BootCompeleteBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent arg1) {
        // TODO Auto-generated method stub

        mContext = context;
        String action = arg1.getAction();
        Log.i(TAG,"action = "+action);
        if (FeatureOption.BIRD_CLEAN_RECENT_APP_WOTU) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)){
                reloadStackView();
                Intent intent = new Intent(context, com.android.systemui.recents.BirdCleanService.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startService(intent);
            } else if (action.equals("com.android.systemui.recent.cleanallapp")) {
                reloadStackView();
            }
        }
    }
    
   public void reloadStackView() {
        // If the Recents component has preloaded a load plan, then use that to prevent
        // reconstructing the task stack
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan loadPlan = RecentsImpl.consumeInstanceLoadPlan();
        if (loadPlan == null) {
            loadPlan = loader.createLoadPlan(mContext);
        }

        // Start loading tasks according to the load plan
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        if (!loadPlan.hasTasks()) {
            loader.preloadTasks(loadPlan, launchState.launchedToTaskId,
                    !launchState.launchedFromHome);
        }

        RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
        loadOpts.runningTaskId = launchState.launchedToTaskId;
        loadOpts.numVisibleTasks = launchState.launchedNumVisibleTasks;
        loadOpts.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
        loader.loadTasks(mContext, loadPlan, loadOpts);
        TaskStack mStack = loadPlan.getTaskStack();
        ArrayList<Task> tasks = new ArrayList<>(mStack.getStackTasks());

        Log.i(TAG, "getTopActivity = " + getTopActivity(mContext));

        // Remove all tasks and delete the task data for all tasks
        mStack.removeAllTasks();
        int start;
        if (getTopActivity(mContext).equals("com.android.systemui.recents.RecentsActivity")) {
            return;
        }
        String topActivityPackageName = getTopActivity(mContext);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).topActivity != null) {
                Log.i(TAG, "tasks.get(i).topActivity.getPackageName() = " + tasks.get(i).topActivity.getPackageName());
                if (tasks.get(i).topActivity.getPackageName().equals("com.alltuu.android")) {
                    continue;
                }
                 if (!topActivityPackageName.equals("com.android.launcher3")) {
                    if (tasks.get(i).topActivity.getPackageName().equals(topActivityPackageName)) {
                        continue;
                    }
                 }
            }
            EventBus.getDefault().send(new DeleteTaskDataEvent(tasks.get(i)));
        }
    }

    private String getTopActivity(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
        if (runningTaskInfos != null) {
            ComponentName info = runningTaskInfos.get(0).topActivity;
            return info.getPackageName();
        } else {
            return null;
        }
    }
}
//[BIRD][BIRD_CLEAN_RECENT_APP_WOTU][喔图 每小时自动清理recent app][yangheng][20170508] END
