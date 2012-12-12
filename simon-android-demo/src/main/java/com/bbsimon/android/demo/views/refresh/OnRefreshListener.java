package com.bbsimon.android.demo.views.refresh;

public interface OnRefreshListener {
    void onPreRefresh();

    void onRefreshData();

    void onRefreshUI();
}
