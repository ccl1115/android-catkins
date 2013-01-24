package com.bbsimon.android.demo.views;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.animation.Interpolator;

/**
 */
public class Facade {
  public static final float ONE_SECOND = 1000f;
  public static final int ANIMATION_FRAME_DURATION = 16; // equals 1000 / 60

  public static final Interpolator sInterpolator = new Interpolator() {
    public float getInterpolation(float t) {
      t -= 1.0f;

      return t * t * t * t * t + 1.0f;
    }
  };
}
