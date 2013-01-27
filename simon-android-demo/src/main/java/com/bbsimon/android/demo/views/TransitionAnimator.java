/*
 * Copyright (c) 2013. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.views;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 */
public interface TransitionAnimator {
  void measure(int widthMeasureSpec, int heightMeasureSpec);

  void layout(boolean changed, int l, int t, int r, int b);

  void draw(Canvas canvas);

  boolean dispatchTouchEvent(MotionEvent event);

  boolean interceptionTouchEvent(MotionEvent event);

  boolean touchEvent(MotionEvent event);

  void animate(int msg);
}
