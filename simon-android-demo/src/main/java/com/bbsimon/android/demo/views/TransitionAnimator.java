package com.bbsimon.android.demo.views;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 */
public interface TransitionAnimator {
  void measure(int widthMeasureSpec, int heightMeasureSpec);

  void layout(boolean changed, int l, int t, int r, int b);

  void draw(Canvas canvas);

  void dispatchTouchEvent(MotionEvent event);

  void interceptionTouchEvent(MotionEvent event);

  void touchEvent(MotionEvent event);

  void animate(int msg);
}
