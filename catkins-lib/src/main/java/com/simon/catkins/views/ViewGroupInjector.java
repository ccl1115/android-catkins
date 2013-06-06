/*
 * Copyright (c) 2013. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.views;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 */
public interface ViewGroupInjector extends ViewInjector {
  void layout(boolean changed, int l, int t, int r, int b);
}
