package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;
import com.bbsimon.android.demo.views.fakechrome.FakeChromeLayout;

/**
 */
public class FakeChromeDemoActivity extends BaseActivity {
  private static final String TAG = "FakeChromeDemoActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getTitleBar().setTitle(TAG);
    setContentView(R.layout.fake_chrome_demo);

    FakeChromeLayout fakeChromeLayout = (FakeChromeLayout) findViewById(R.id.content);
  }
}
