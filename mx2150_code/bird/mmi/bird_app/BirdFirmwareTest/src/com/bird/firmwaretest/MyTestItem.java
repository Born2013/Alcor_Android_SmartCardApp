package com.bird.firmwaretest;

import android.view.View;
import android.widget.CheckBox;

/**
 * Created by root on 17-3-9.
 */
public class MyTestItem {
    private String name;
    private int tagId;
    private boolean isChecked;


    public MyTestItem(String name, int id, boolean isChecked) {
        this.name = name;
        this.tagId = id;
        this.isChecked = isChecked;
    }

    public String getName() {
        return name;
    }

    public int getTagId() {
        return tagId;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

}
