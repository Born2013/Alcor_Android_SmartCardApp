package com.bird;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.music.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.util.DisplayMetrics; //wht
/*[bug-null][gaowei][20170704]add*/
public class PopMenu extends Dialog {
    private Context mContext;
    private View mCustomView;
    private ArrayList<HashMap<String, Object>> mAdapterData = new ArrayList<HashMap<String, Object>>();
    private ArrayList<DialogListItem> mListItems = new ArrayList<DialogListItem> ();
    private DialogListClickListener mDialogListClickListener;
    private String mTitle;
    private String mMessage;
    private boolean mIsContentScroll = false;
    private boolean mIsContentLink = false;
    private String mPositiveButtonText;
    private DialogInterface.OnClickListener mPositiveButtonListener;
    private String mNegativeButtonText;
    private int mNegativeButtonColor;
    private DialogInterface.OnClickListener mNegativeButtonListener;
    private static final int DEFAULT_COLOR = Color.parseColor("#01cc99");

    public enum DisplayMode {
        ACTIONSHEET_TOP_WINDOW_SPECIAL,
    }
    private DisplayMode mDisplayMode;
    // modify more menu popup animation wht
    private int mHeadLayoutHeight;
    private boolean mIsShowDialogPartial;

    public interface DialogListClickListener {
        void onClick(int which);
    }

    private class DialogListItem {
        public int mId;
        public String mTitle;
        public boolean mEnable = true;

        public DialogListItem(int id, String title) {
            mId = id;
            mTitle = title;
        }
    }

    private class MyURLSpan extends ClickableSpan {
        private String mUrl;

        MyURLSpan(String url) {
            mUrl = url;
        }

        @Override
        public void onClick(View widget) {
            //Uri uri = UriUtils.parseUriOrNull(mUrl);
            //MessageActionManager.showDialogByUri(mContext, uri);
        }
    }

    private class DialogListAdapter extends SimpleAdapter {

        public DialogListAdapter(Context context,
                List<? extends Map<String, ?>> data, int resource,
                String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            if (!mListItems.get(position).mEnable) {
                TextView title = (TextView)view.findViewById(R.id.title);
                title.setEnabled(false);
            }

            view.setTag(mListItems.get(position));

            return view;
        }

    }

    public PopMenu(Context context, DisplayMode mode) {
        super(context, R.style.MmsDialogWithoutTitle);

        mContext = context;
        mDisplayMode = mode;
        setCanceledOnTouchOutside(true);
    }

    // modify more menu popup animation wht
    public PopMenu(Context context, DisplayMode mode, int headHeight, boolean isShowPartial) {
        this(context, mode);
        mHeadLayoutHeight = headHeight;
        mIsShowDialogPartial = isShowPartial;
    }

    public void addItem(int id, String title) {
        mListItems.add(new DialogListItem(id, title));
    }

    public int getItemCount() {
        return mListItems.size();
    }

    public void setEnable(int id, boolean isEnable) {
        for (DialogListItem item : mListItems) {
            if (id == item.mId) {
                item.mEnable = isEnable;
                break;
            }
        }
    }

    public int[] getIDList() {
        int size = mListItems.size();
        int[] IDList = new int[size];
        DialogListItem item = null;
        for (int i = 0; i < size; i++) {
            item = mListItems.get(i);
            IDList[i] = item.mId;
        }

        return IDList;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public void enableContentScrolling(boolean isScroll) {
        mIsContentScroll = isScroll;
    }

    public void enableContentLink(boolean isLink) {
        mIsContentLink = isLink;
    }

    public void setPositiveButton(String buttonText, DialogInterface.OnClickListener listener) {
        mPositiveButtonText = buttonText;
        mPositiveButtonListener = listener;
    }

    public void setPositiveButton(String buttonText, int color, DialogInterface.OnClickListener listener) {
        mPositiveButtonText = buttonText;
        mPositiveButtonListener = listener;
    }

    public void setNegativeButton(String buttonText, DialogInterface.OnClickListener listener) {
        mNegativeButtonText = buttonText;
        TypedArray array = mContext.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.colorPrimary });
        mNegativeButtonColor = array.getColor(0, DEFAULT_COLOR);
        array.recycle();
        mNegativeButtonListener = listener;
    }

    public void setNegativeButton(String buttonText, int color, DialogInterface.OnClickListener listener) {
        mNegativeButtonText = buttonText;
        mNegativeButtonColor = color;
        mNegativeButtonListener = listener;
    }

    public void setDialogListClickListener(DialogListClickListener l) {
        mDialogListClickListener = l;
    }

    public void setCustomView(View view) {
        mCustomView = view;
    }

    public void showDialog() {
        if (mDisplayMode.equals(DisplayMode.ACTIONSHEET_TOP_WINDOW_SPECIAL)) {
            mCustomView = createActionMenuView();
        }
        setContentView(mCustomView);
        show();
    }

    private View createActionMenuView() {
        RelativeLayout parent = new RelativeLayout(mContext);

        //get resolution info
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        // modify more menu popup animation wht
        int statusAndTitleHeight = /*Utility.getStatusBarHeight((Activity)mContext)*/50 + mHeadLayoutHeight;
        if (metrics.widthPixels == 720) {
            statusAndTitleHeight = statusAndTitleHeight + 18;
        }

        RelativeLayout.LayoutParams parentParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parent.setLayoutParams(parentParams);
        parent.setBackgroundColor(mContext.getResources().getColor(R.color.background_primary));

        WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
        if (mDisplayMode.equals(DisplayMode.ACTIONSHEET_TOP_WINDOW_SPECIAL)) {
            // modify more menu popup animation wht
            if (mIsShowDialogPartial) {
                localLayoutParams.gravity = Gravity.TOP;
                localLayoutParams.x = metrics.widthPixels / 2;
                localLayoutParams.y = statusAndTitleHeight;
            } else {
                localLayoutParams.gravity = Gravity.FILL_HORIZONTAL | Gravity.TOP;
            }
            // getWindow().setWindowAnimations(R.style.head_action_menu_animstyle_special);
        }

        getWindow().setAttributes(localLayoutParams);

        ListView list = new ListView(mContext);
        ViewGroup.LayoutParams listParams = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        // modify more menu popup animation wht
        if (mIsShowDialogPartial) {
            listParams.width = metrics.widthPixels / 2;
        } else {
            // nothing to do
        }
        list.setLayoutParams(listParams);
        list.setBackgroundResource(R.drawable.dialog_list_view_bg);
        list.setSelector(R.drawable.dialog_list_item_bg);
        fillAdapterData();

        list.setAdapter(new DialogListAdapter(mContext, mAdapterData, R.layout.kusai_dialog_list_item, new String[] { "title" }, new int[] { R.id.title }));

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                DialogListItem item = (DialogListItem) view.getTag();
                if (!item.mEnable) {
                    return;
                }

                if (mDialogListClickListener != null) {
                    mDialogListClickListener.onClick(item.mId);
                }
            }

        });

        Resources resources = mContext.getResources();
        list.setDivider(mContext.getResources().getDrawable(R.drawable.menu_hw_list_divider));
        parent.addView(list);
        return parent;
    }

    private void fillAdapterData() {
        mAdapterData.clear();

        for (DialogListItem item : mListItems) {
            HashMap<String, Object> map = new HashMap<String, Object>(1);
            map.put("title", item.mTitle);
            mAdapterData.add(map);
        }
    }

    private void CreatClickSpans(TextView textView) {
        if (textView == null || textView.length() == 0) {
            return;
        }

        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            Spannable sp = (Spannable) text;

            final URLSpan[] spans = textView.getUrls();

            if (spans.length == 0) {
                return;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            builder.clearSpans();

            for (URLSpan span : spans) {
                MyURLSpan mySpan = new MyURLSpan(span.getURL());
                builder.setSpan(mySpan, sp.getSpanStart(span),
                        sp.getSpanEnd(span), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }

            textView.setText(builder);
        }
    }

    public void toggleHideBar() {
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // YUNOS BEGIN PB
                // ##modules(Mms): ##yongxing.lyx@alibaba-inc.com
                // ##BugID:(6469370) ##date:2015/09/30
                // ##description: don't hide navigation bar in action dialog.
                /*| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION*/
                // YUNOS END PB
                | (mIsShowDialogPartial? 0: View.SYSTEM_UI_FLAG_FULLSCREEN)
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mDisplayMode.equals(DisplayMode.ACTIONSHEET_TOP_WINDOW_SPECIAL) && hasFocus) {
            toggleHideBar();
        }
    }
}
