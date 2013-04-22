package com.simon.catkins.test.views;

import android.test.AndroidTestCase;
import com.simon.catkins.views.AnimationConfig;

/**
 * @author bb.simon.yu@gmail.com
 */
public class AnimationConfigTest extends AndroidTestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    assertNotNull(getContext());
  }

  public void testGetVelocitySmall() throws Exception {
    final int expected = (int) (AnimationConfig.VELOCITY_SMALL *
            getContext().getResources().getDisplayMetrics().density + 0.5f);
    assertEquals(expected, AnimationConfig.getVelocitySmall(getContext()));
  }

  public void testGetVelocityMedium() throws Exception {
    final int expected = (int) (AnimationConfig.VELOCITY_MEDIUM *
        getContext().getResources().getDisplayMetrics().density + 0.5f);
    assertEquals(expected, AnimationConfig.getVelocityMedium(getContext()));
  }

  public void testGetVelocityLarge() throws Exception {
    final int expected = (int) (AnimationConfig.VELOCITY_LARGE *
        getContext().getResources().getDisplayMetrics().density + 0.5f);
    assertEquals(expected, AnimationConfig.getVelocityLarge(getContext()));
  }

  public void testGetTouchEventMoveThresholdSmall() throws Exception {
    final int expected = (int) (AnimationConfig.TOUCH_EVENT_MOVE_THRESHOLD_SMALL *
        getContext().getResources().getDisplayMetrics().density + 0.5f);
    assertEquals(expected, AnimationConfig.getTouchEventMoveThresholdSmall(getContext()));
  }

  public void testGetTouchEventMoveThresholdMedium() throws Exception {
    final int expected = (int) (AnimationConfig.TOUCH_EVENT_MOVE_THRESHOLD_MEDIUM *
        getContext().getResources().getDisplayMetrics().density + 0.5f);
    assertEquals(expected, AnimationConfig.getTouchEventMoveThresholdMedium(getContext()));
  }

  public void testGetTouchEventMoveThresholdLarge() throws Exception {
    final int expected = (int) (AnimationConfig.TOUCH_EVENT_MOVE_THRESHOLD_LARGE *
        getContext().getResources().getDisplayMetrics().density + 0.5f);
    assertEquals(expected, AnimationConfig.getTouchEventMoveThresholdLarge(getContext()));
  }
}
