/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.demo.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.simon.catkins.app.BaseActivity;
import com.simon.catkins.demo.R;
import com.simon.catkins.views.Flip3DLayout;

/**
 * @author bb.simon.yu@gmail.com
 */
public class Flip3DLayoutDemoActivity extends BaseActivity
    implements View.OnClickListener {

  private static final String TAG = "Flip3DLayoutDemoActivity";

  private Flip3DLayout mFlip3DLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getTitleBar().setTitle(TAG);
    setContentView(R.layout.flip_3d_layout);

    Button button = (Button) findViewById(R.id.flip);
    button.setOnClickListener(this);
    button = (Button) findViewById(R.id.flip_back);
    button.setOnClickListener(this);

    mFlip3DLayout = (Flip3DLayout) findViewById(R.id.flip_3d_layout);
  }


  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.flip:
        mFlip3DLayout.startFlip();
        break;
      case R.id.flip_back:
        mFlip3DLayout.startReverseFlip();
        break;
    }
  }
}
