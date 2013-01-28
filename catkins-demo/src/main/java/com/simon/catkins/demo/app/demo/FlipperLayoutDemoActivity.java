/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.demo.app.demo;

import android.os.Bundle;
import com.simon.catkins.app.BaseActivity;
import com.simon.catkins.demo.R;
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
