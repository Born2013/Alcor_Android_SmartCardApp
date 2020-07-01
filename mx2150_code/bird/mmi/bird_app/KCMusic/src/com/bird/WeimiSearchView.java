/*
 * Copyright (C) 2010 The Android Open Source Project
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
/*[BIRD_WEIMI_MUSIC]wangyueyue 20150314 add */
package com.bird;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.util.Log;
import com.android.music.R;

public class WeimiSearchView extends LinearLayout {
    private final static String TAG = "WeimiSearchView";

    private OnQueryTextListener mOnQueryChangeListener;
    private OnFocusChangeListener mOnQueryTextFocusChangeListener;
    private OnCloseListener mOnCloseListener;

    private EditText mQueryTextView;
    private ImageView mCloseButton;

    private CharSequence mOldQueryText;

    private boolean mAutoExpandIME;

    public WeimiSearchView(Context context) {
        this(context, null);
    }

    public WeimiSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.bird_bbk_search_view, this, true);

        mQueryTextView = (EditText) findViewById(R.id.search_src_text);
        mQueryTextView.setHint(R.string.search_title);
        mQueryTextView.setHorizontalFadingEdgeEnabled(true);

        mQueryTextView.addTextChangedListener(mTextWatcher);
        mQueryTextView.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                // TODO Auto-generated method stub
                switch (actionId) {
                case EditorInfo.IME_ACTION_SEARCH:
                    if (mOnQueryChangeListener != null) {
                        mOnQueryChangeListener.onQueryTextSubmit(mQueryTextView
                                .getText().toString());
                    }
                    break;
                }
                return false;
            }
        });
        mQueryTextView.setOnFocusChangeListener(new OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnQueryTextFocusChangeListener != null) {
                    mOnQueryTextFocusChangeListener.onFocusChange(
                            WeimiSearchView.this, hasFocus);
                }
                if (mAutoExpandIME && hasFocus && getVisibility() == VISIBLE) {
                    showInputMethod(v);
                }
            }
        });
        mQueryTextView.setFocusable(true);
        mQueryTextView.setFocusableInTouchMode(true);
        mQueryTextView.requestFocus();

        mCloseButton = (ImageView) findViewById(R.id.search_close_btn);
        mCloseButton.setOnClickListener(mOnClickListener);
        updateCloseButton();
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    /**
     * Callback to watch the text field for empty/non-empty
     */
    private TextWatcher mTextWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int before,
                int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                int after) {
            WeimiSearchView.this.onTextChanged(s);
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private void onTextChanged(CharSequence newText) {
        updateCloseButton();
        if (mOnQueryChangeListener != null
                && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }

    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    public void setQueryHint(CharSequence hint) {
        mQueryTextView.setHint(hint);
    }

    public void setQuery(CharSequence query, boolean submit) {
        mQueryTextView.setText(query);
    }

    public CharSequence getQuery() {
        return mQueryTextView.getText();
    }

    public void setAutoExpandIME(boolean flag) {
        mAutoExpandIME = flag;
    }

    private void updateCloseButton() {
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());
        final boolean showClose = hasText;
        mCloseButton.setVisibility(showClose ? VISIBLE : GONE);
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {

        public void onClick(View v) {
            if (v == mCloseButton) {
                onCloseClicked();
            }
        }
    };

    private void onCloseClicked() {
        mQueryTextView.setText("");
        mQueryTextView.requestFocus();
        if (mOnCloseListener != null) {
            mOnCloseListener.onClose();
        }
    }

    public interface OnQueryTextListener {

        boolean onQueryTextSubmit(String query);

        boolean onQueryTextChange(String newText);
    }

    public interface OnCloseListener {

        boolean onClose();
    }
}
