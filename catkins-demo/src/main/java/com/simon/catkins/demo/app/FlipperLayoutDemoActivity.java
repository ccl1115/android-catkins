/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import com.simon.catkins.demo.R;

/**
 * @author bb.simon.yu@gmail.com
 */
public class FlipperLayoutDemoActivity extends Activity {
  private static final String TAG = "FlipperLayoutDemoActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActionBar().setTitle(TAG);
    setContentView(R.layout.flipper_layout);
  }
}
