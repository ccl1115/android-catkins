/*
 * Copyright (c) 2013. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;
import com.bbsimon.android.demo.views.refresh.IRefreshable;
import com.bbsimon.android.demo.views.refresh.RefresherView;
import de.akquinet.android.androlog.Log;

/**
 */
public class RefresherViewActivity extends BaseActivity {
  private static final String TAG = "RefresherViewActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getTitleBar().setTitle(TAG);
    setContentView(R.layout.refresher_demo);
  }
}
