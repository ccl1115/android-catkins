package com.simon.catkins.demo.test.app;

import android.test.ActivityInstrumentationTestCase2;
import com.simon.catkins.demo.app.StackLayoutWithFragmentDemoActivity;

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
