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
  public static final int FRAME_RATE = 60;

  public static final int FRAME_ANIMATION_DURATION =
      (int) (ONE_SECOND / FRAME_RATE);

  public static final Interpolator sInterpolator = new Interpolator() {
    public float getInterpolation(float t) {
      t -= 1.0f;

      return t * t * t * t * t + 1.0f;
    }
  };

  public static boolean measureBitmapDrawable(BitmapDrawable d, int w, int h,
                                              Matrix outMatrix) {
    if (d == null) {
      return false;
    }
    final int dW = d.getIntrinsicWidth();
    final int dH = d.getIntrinsicHeight();

    d.setBounds(0, 0, dW, dH);

    if (dW < 1 || dH < 1) {
      return false;
    }

    final float s = Math.max(w / dW, h / dH);
    outMatrix.reset();
    outMatrix.postScale(s, s);
    outMatrix.postTranslate((w - dW * s) / 2, (h - dH * s) / 2);
    return true;
  }

  public static boolean measureText(String text, int w, Paint p, Rect outRect) {
    if (text == null || text.length() == 0) {
      return false;
    }
    int count = p.breakText(text, true, w, null);
    if (count > 0 && count < text.length()) {
      text = text.substring(0, count - 1).concat("...");
    }
    p.getTextBounds(text, 0, text.length(), outRect);
    return true;
  }
}
