package com.simon.catkins.views;

import android.content.Context;
import android.view.animation.Interpolator;

/**
 */
public abstract class AnimationConfig {
  public static final float ONE_SECOND_FLOAT = 1000f; //ms
  public static final int ANIMATION_FRAME_DURATION = 16; // equals 1000 / 60
  public static final int VELOCITY_SMALL = 500; //dp
  public static final int VELOCITY_MEDIUM = 800; //dp
  public static final int VELOCITY_LARGE = 1100; //dp
  public static final float PROPORTION_VELOCITY_SMALL = 1.0f;
  public static final float PROPORTION_VELOCITY_MEDIUM = 2.0f;
  public static final float PROPORTION_VELOCITY_LARGE = 4.0f;
  public static final int TOUCH_EVENT_MOVE_THRESHOLD_SMALL = 25;
  public static final int TOUCH_EVENT_MOVE_THRESHOLD_MEDIUM = 50;
  public static final int TOUCH_EVENT_MOVE_THRESHOLD_LARGE = 75;

  private static final int MIN_VELOCITY = 100; // dips
  private static final int MAX_VELOCITY = 1000; //dips

  public static final Interpolator sInterpolator = new Interpolator() {
    public float getInterpolation(float t) {
      t -= 1.0f;

      return t * t * t * t * t + 1.0f;
    }
  };

  public static final Interpolator sReverseInterpolator = new Interpolator() {
    @Override
    public float getInterpolation(float t) {
      return t * t * t * t * t;
    }
  };

  public static int getVelocitySmall(Context context) {
    return convertToPx(context, VELOCITY_SMALL);
  }

  public static int getVelocityMedium(Context context) {
    return convertToPx(context, VELOCITY_MEDIUM);
  }

  public static int getVelocityLarge(Context context) {
    return convertToPx(context, VELOCITY_LARGE);
  }

  public static int getTouchEventMoveThresholdSmall(Context context) {
    return convertToPx(context, TOUCH_EVENT_MOVE_THRESHOLD_SMALL);
  }

  public static int getTouchEventMoveThresholdMedium(Context context) {
    return convertToPx(context, TOUCH_EVENT_MOVE_THRESHOLD_MEDIUM);
  }

  public static int getTouchEventMoveThresholdLarge(Context context) {
    return convertToPx(context, TOUCH_EVENT_MOVE_THRESHOLD_LARGE);
  }

  public static int computeVelocityByDp(Context context, int distanceDp) {
    return convertToPx(context, Math.min(MAX_VELOCITY, Math.max(MIN_VELOCITY, distanceDp)));
  }

  public static int computeVelocityByPx(Context context, int distancePx) {
    return Math.min(convertToPx(context, MAX_VELOCITY), Math.max(convertToPx(context, MIN_VELOCITY), distancePx));
  }

  private static float getDensity(Context context) {
    return context.getResources().getDisplayMetrics().density;
  }

  private static int convertToPx(Context context, int lengthInDp) {
    return (int) (getDensity(context) * lengthInDp + 0.5f);
  }

  /**
   * Deceleration interpolator computation
   *
   * @param distance the total length
   * @param position the current length
   * @param reverse  true if from distance value to 0
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
