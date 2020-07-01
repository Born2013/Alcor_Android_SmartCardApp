package com.sensortek.stkhealthcare2; 

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;

import com.sensortek.stkhealthcare2.R;

import java.util.ArrayList;
/*[MTK_THEMEMANAGER_APP] songwei 20160927 begin*/
import com.sensortek.stkhealthcare2.FeatureOption;
/*[MTK_THEMEMANAGER_APP] songwei 20160927 end*/

public class PopMenu extends Dialog {

    private ArrayList<ItemMenuPair<Integer, ItemHolder>> mItems = new ArrayList<ItemMenuPair<Integer, ItemHolder>>();
    private static final int TRANSLATE_DURATION = 200;
    private static final int TRANSLATE_DURATION_NA = 1;
    private View mContentView;
    private int mGravity;
    private Animation mPopMenuAnimation;
    private Animation mPopMenuNoAnimation;
    private OnPopMenuListener mItemClickListener;
    private ItemAdapter mItemAdapter;
    private boolean mIsDismiss;
    private Integer mCurrItemId = null;

    public class ItemMenuPair<F, S> {
        public F first;
        public S second;

        public ItemMenuPair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }

    public class ItemHolder {
        public int mId;
        public String mName;
        public boolean mEnable = true;
    }

    public interface OnPopMenuListener {
        public void onMenuItemClick(int what);
    }

    public PopMenu(Context context) {
        super(context);
        init();
    }

    public PopMenu(Context context, int theme) {
        super(context, theme);
        init();
    }

    public void setOnIemClickListener(OnPopMenuListener l) {
        mItemClickListener = l;
    }

    @Override
    public void show() {
        super.show();
        mIsDismiss = false;
        if (mItemAdapter != null) {
            mItemAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            toggleHideBar();
        }
    }

    @Override
    public void dismiss() {
        if (mContentView.getAnimation() != null && mContentView.getAnimation().hasStarted()) {
            return;
        }
        mIsDismiss = true;
        mContentView.startAnimation(mPopMenuAnimation);
    }

    private void dismissNoAnimation() {
        if (mContentView.getAnimation() != null && mContentView.getAnimation().hasStarted()) {
            return;
        }
        mIsDismiss = true;
        mContentView.startAnimation(mPopMenuNoAnimation);
    }

    public void immediateDismiss() {
        super.dismiss();
    }

    public void clean() {
        mItems.clear();
        mItemAdapter.notifyDataSetChanged();
    }

    public void addItem(int itemId, String itemName) {
        ItemHolder holder = new ItemHolder();
        holder.mId = itemId;
        holder.mName = itemName;
        mItems.add(new ItemMenuPair<Integer, ItemHolder>(itemId, holder));
    }

    public void removeItem(int itemId) {
        for (int i = 0; i < mItems.size(); i++) {
            Integer id = mItems.get(i).first;
            if (id != null && id == itemId) {
                mItems.remove(i);
                mItemAdapter.notifyDataSetChanged();
                return;
            }
        }
    }

    public void toggleHideBar() {
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        //[BIRD][chengshujiang]20160419
        /*[| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN]*/
        
    }

    public void updateItem(int itemId, String itemName) {
        for (int i = 0; i < mItems.size(); i++) {
            Integer id = mItems.get(i).first;
            if (id != null && id == itemId) {
                mItems.get(i).second.mName = itemName;
                return;
            }
        }
    }

    public void setEnable(int itemId, boolean enable) {
        for (int i = 0; i < mItems.size(); i++) {
            Integer id = mItems.get(i).first;
            if (id != null && id == itemId) {
                mItems.get(i).second.mEnable = enable;
                return;
            }
        }
    }

    private void init() {
        mContentView = LayoutInflater.from(getContext()).inflate(R.layout.pop_menu, null);
        ListView list = (ListView) mContentView.findViewById(R.id.item_list);
        mItemAdapter = new ItemAdapter(getContext());
        /*[KC_MTK_THEMEMANAGER_APP] songwei 20160924 begin*/
        /*if (FeatureOption.KC_MTK_THEMEMANAGER_APP) {
            list.setDivider(getContext().getResources().getDrawable(com.birdroid.R.drawable.theme_hw_list_divider)); // or some other color int
            list.setDividerHeight(1);
        }*/
        /*[KC_MTK_THEMEMANAGER_APP] songwei 20160924 end*/
        list.setAdapter(mItemAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemHolder holder = mItemAdapter.getItem(position);
                if (!holder.mEnable || mIsDismiss) {
                    return;
                }
                mCurrItemId = mItems.get(position).first;
                /*[BIRD][bug110305][chengshujiang]20160911 begin*/
                //dismissNoAnimation();
                dismiss();
                /*[BIRD][bug110305][chengshujiang]20160911 end*/
            }
        });
        mPopMenuAnimation = createTranslationOutAnimation(true);
        mPopMenuNoAnimation = createTranslationOutAnimation(false);
        setContentView(mContentView);
    }
    /*[BIRD][BIRD_QINGYU_CONTACTS]chengshujiang 20160413 begin*/
    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        return statusBarHeight;
    }

    //get title layout height
    public static  int getTitleLayoutHeight(Context context) {
        int titleLayoutHeight = 0;
        titleLayoutHeight = context.getResources().getDimensionPixelSize(R.dimen.actionbar_height);
        return titleLayoutHeight;
    }
    /*[BIRD][BIRD_QINGYU_CONTACTS]chengshujiang 20160413 end*/

    public static PopMenu build(Context context, int gravity) {
        int theme = R.style.DialogWithoutTitle;
        PopMenu dialog = new PopMenu(context, theme);
        dialog.setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams localLayoutParams = dialog.getWindow().getAttributes();
        dialog.mGravity = gravity;
        localLayoutParams.gravity = gravity;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        localLayoutParams.x = dm.widthPixels / 2;
        localLayoutParams.y = getStatusBarHeight(context) + getTitleLayoutHeight(context);
        localLayoutParams.width = dm.widthPixels / 2;
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        dialog.getWindow().setAttributes(localLayoutParams);
        return dialog;
    }

    private Animation createTranslationOutAnimation(boolean animation) {
        int type = TranslateAnimation.RELATIVE_TO_SELF;
        TranslateAnimation an = null;
        if (mGravity == Gravity.BOTTOM) {
            an = new TranslateAnimation(type, 0, type, 0, type, 0, type, 1);
        } else {
            an = new TranslateAnimation(type, 0, type, 0, type, 0, type, -1);
        }
        an.setDuration(animation ? TRANSLATE_DURATION : TRANSLATE_DURATION_NA);
        an.setFillAfter(true);
        an.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                immediateDismiss();
                if (mItemClickListener != null && mCurrItemId != null) {
                    mItemClickListener.onMenuItemClick(mCurrItemId);
                    mCurrItemId = null;
                }
            }
        });
        return an;
    }

    public class ItemAdapter extends BaseAdapter {
        private LayoutInflater mInflate;

        public ItemAdapter(Context context) {
            mInflate = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public ItemHolder getItem(int position) {
            return mItems.get(position).second;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflate.inflate(R.layout.pop_menu_item, parent, false);
            }
            TextView itemContent = (TextView) convertView.findViewById(R.id.item_content);
            itemContent.setText(mItems.get(position).second.mName);
            itemContent.setEnabled(mItems.get(position).second.mEnable);
            return convertView;
        }
    }
}
