package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;

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
