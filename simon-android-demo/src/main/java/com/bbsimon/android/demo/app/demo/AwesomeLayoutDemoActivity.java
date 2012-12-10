/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;

/**
 * @author bb.simon.yu@gmail.com
 */
public class AwesomeLayoutDemoActivity extends BaseActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getTitleBar().setTitle("AwesomeLayoutDemoActivity");

    setContentView(R.layout.awesome_layout_demo);
  }
}
