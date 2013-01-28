package com.simon.catkins.demo.test.app.demo;

import android.test.ActivityInstrumentationTestCase2;
import com.bbsimon.android.demo.app.demo.FlipperLayoutDemoActivity;

/**
 * @author bb.simon.yu@gmail.com
 */
public class FlipperLayoutDemoActivityTest
    extends ActivityInstrumentationTestCase2<FlipperLayoutDemoActivity> {
  public FlipperLayoutDemoActivityTest() {
    super(FlipperLayoutDemoActivity.class);
  }

  public void testActivity() {
    FlipperLayoutDemoActivity activity = getActivity();
    assertNotNull(activity);
  }
}
