/*
 * Copyright (c) 2013. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.app.demo.refresher;

import android.os.Bundle;
import android.widget.TextView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;
import com.bbsimon.android.demo.views.refresh.IRefreshable;
import com.bbsimon.android.demo.views.refresh.RefresherView;

/**
 */
public class RefresherViewFromXmlActivity extends BaseActivity {
  private static final String TAG = "RefresherViewFromXmlActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getTitleBar().setTitle(TAG);
    setContentView(R.layout.refresher_demo);

    RefresherView refresherView = (RefresherView) findViewById(R.id.content);
    final TextView headerText = (TextView) findViewById(R.id.refresher_header);

    refresherView.setOnRefreshListener(new IRefreshable.OnRefreshListener() {
      @Override
      public void onStateChanged(IRefreshable.State state) {
        headerText.setText(String.valueOf(state));
      }

      @Override
      public void onPreRefresh() {
      }

      @Override
      public void onRefreshData() {
      }

      @Override
      public void onRefreshUI() {
      }
    });
  }
}
