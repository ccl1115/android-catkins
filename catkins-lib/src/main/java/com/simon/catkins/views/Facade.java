package com.simon.catkins.views;

import android.view.animation.Interpolator;

/**
 */
public abstract class Facade {
  public static final float ONE_SECOND_FLOAT = 1000f;
  public static final int ANIMATION_FRAME_DURATION = 16; // equals 1000 / 60
  public static final int VELOCITY_SMALL = 500;
  public static final int VELOCITY_MEDIUM = 800;
  public static final int VELOCITY_LARGE = 1100;
  public static final float PROPORTION_VELOCITY_SMALL = 0.01f;
  public static final float PROPORTION_VELOCITY_MEDIUM = 0.02f;
  public static final float PROPORTION_VELOCITY_LARGE = 0.03f;
  public static final int TOUCH_EVENT_MOVE_THRESHOLD_SMALL = 25;
  public static final int TOUCH_EVENT_MOVE_THRESHOLD_MEDIUM = 50;
  public static final int TOUCH_EVENT_MOVE_THRESHOLD_LARGE = 75;

  public static final Interpolator sInterpolator = new Interpolator() {
    public float getInterpolation(float t) {
      t -= 1.0f;

      return t * t * t * t * t + 1.0f;
    }
  };

  /**
   * Deceleration interpolator computation
   * @param distance the total length
   * @param position the current length
   * @param reverse true if from distance value to 0
   * @return the interpolated length
   */
  public static int computeInterpolator(float distance, float position, boolean reverse) {
    if (reverse) {
      float proportion = sInterpolator.getInterpolation(position / (position - distance));
      return (int) (distance - distance * proportion);
    } else {
      float proportion = sInterpolator.getInterpolation(position / distance);
      return (int) (distance * proportion);
    }
  }
}
