package com.bird.firmwaretest;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by root on 17-3-16.
 */
public class FirmwareTestAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<MyTestItem> mItems = new ArrayList<MyTestItem>();
    private ArrayList<MyTestItem> mCheckedList = new ArrayList<MyTestItem>();
    public FirmwareTestAdapter(Context context , ArrayList<MyTestItem> list) {
        mContext = context;
        mItems = list;
        setTestItems(mItems);
    }

    @Override
    public int getCount() {
        if (mItems == null) {
            return 0;
        }
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        if (mItems == null) {
            return null;
        }
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (mItems == null) {
            return -1;
        }
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SelectViewHolder holder;
        View view = null;
        if (convertView == null) {
            holder = new SelectViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.select_test_item, parent, false);
            holder.mTitle = (TextView) view.findViewById(R.id.text);
            holder.mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (SelectViewHolder) view.getTag();
        }

        MyTestItem mTestView = mItems.get(position);
        holder.mTitle.setText(mTestView.getName());
        holder.mCheckBox.setChecked(mTestView.isChecked());
        return view;
    }

    public void setTestItems(final ArrayList<MyTestItem> items) {
        if (items == null) {
            return;
        }
        mItems = copyArrayList(items);
        mCheckedList.clear();
        mCheckedList = copyArrayList(items);
        Log.d("shujiang","mCheckedList.size:"+mCheckedList.size());
    }

    public ArrayList<MyTestItem> getCheckedItems() {
        if (mItems == null) {
            return null;
        }
        return mCheckedList;
    }

    public void setChecked(int position, boolean isChecked) {
        MyTestItem item = mItems.get(position);
        item.setChecked(isChecked);
        if (isChecked) {
            if (mCheckedList.indexOf(item) < 0) {
                mCheckedList.add(item);
            }
        } else {
            if (mCheckedList.indexOf(item) >= 0) {
                mCheckedList.remove(item);
            }
        }
        Log.d("shujiang","setChecked:mCheckedList.size:"+mCheckedList.size());
    }

    private ArrayList<MyTestItem> copyArrayList(ArrayList<MyTestItem> orig) {
        ArrayList<MyTestItem> tmpItems = new ArrayList<MyTestItem>();
        if (orig != null) {
            for (MyTestItem testListItem : orig) {
                tmpItems.add(testListItem);
            }
        }
        return tmpItems;
    }

    private static class SelectViewHolder {
        TextView mTitle;
        CheckBox mCheckBox;
    }
}
