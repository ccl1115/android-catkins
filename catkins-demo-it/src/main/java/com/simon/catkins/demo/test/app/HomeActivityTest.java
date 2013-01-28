package com.simon.catkins.demo.test.app;

import android.test.ActivityInstrumentationTestCase2;
import com.bbsimon.android.demo.app.HomeActivity;

public class HomeActivityTest extends ActivityInstrumentationTestCase2<HomeActivity> {

  public HomeActivityTest() {
    super(HomeActivity.class);
  }

  public void testActivity() {
    HomeActivity activity = getActivity();
    assertNotNull(activity);
  }
}

