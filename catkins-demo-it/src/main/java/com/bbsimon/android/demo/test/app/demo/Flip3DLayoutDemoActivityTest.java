package com.bbsimon.android.demo.test.app.demo;

import android.test.ActivityInstrumentationTestCase2;
import com.bbsimon.android.demo.app.demo.Flip3DLayoutDemoActivity;

/**
 * @author bb.simon.yu@gmail.com
 */
public class Flip3DLayoutDemoActivityTest extends
    ActivityInstrumentationTestCase2<Flip3DLayoutDemoActivity> {
  public Flip3DLayoutDemoActivityTest() {
    super(Flip3DLayoutDemoActivity.class);
  }

  public void testActivity() {
    Flip3DLayoutDemoActivity activity = getActivity();
    assertNotNull(activity);
  }
}
