/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;
import de.akquinet.android.androlog.Log;

/**
 * @author bb.simon.yu@gmail.com
 */
public class FlipperLayoutDemoActivity extends BaseActivity {
  private static final String TAG = "FlipperLayoutDemoActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.init(this);
    Log.v(TAG, "@onCreate");
    getTitleBar().setTitle(TAG);
    setContentView(R.layout.flipper_layout);
  }
}
