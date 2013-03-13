/*
 * Copyright (c) 2013. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.views;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * Android自定义动画机制
 *
 * 1. 使用android.os.Handler控制动画的帧，并且将对动画的计算和动画的绘制分开。
 *    Handler是Android提供的方便操作Looper（UI循环）的工具类。由于动画的计算
 *    时间远远小于动画的绘制时间，而为了保证动画的速度的均匀，我们需要在固定的时间
 *    内（每1000/60毫秒）请求一次绘制。
 */
public interface TransitionAnimator {
  void measure(int widthMeasureSpec, int heightMeasureSpec);

  void layout(boolean changed, int l, int t, int r, int b);

  void draw(Canvas canvas);

  boolean dispatchTouchEvent(MotionEvent event);

  boolean interceptionTouchEvent(MotionEvent event);

  boolean touchEvent(MotionEvent event);

  void animate(int msg);

  boolean isAnimating();
}
