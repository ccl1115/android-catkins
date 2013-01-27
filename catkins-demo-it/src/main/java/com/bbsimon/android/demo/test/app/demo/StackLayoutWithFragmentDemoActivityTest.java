package com.bbsimon.android.demo.test.app.demo;

import android.test.ActivityInstrumentationTestCase2;
import com.bbsimon.android.demo.app.demo.StackLayoutWithFragmentDemoActivity;

/**
 * @author bb.simon.yu@gmail.com
 */
public class StackLayoutWithFragmentDemoActivityTest
    extends ActivityInstrumentationTestCase2<StackLayoutWithFragmentDemoActivity> {

  public StackLayoutWithFragmentDemoActivityTest() {
    super(StackLayoutWithFragmentDemoActivity.class);
  }

  public void testActivity() {
    StackLayoutWithFragmentDemoActivity activity = getActivity();
    assertNotNull(activity);
  }
}
