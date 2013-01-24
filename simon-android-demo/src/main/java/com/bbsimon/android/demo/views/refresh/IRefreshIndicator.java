package com.bbsimon.android.demo.views.refresh;

import android.graphics.drawable.Drawable;

/**
 */
public interface IRefreshIndicator {
  Drawable getPullDownDrawable();

  Drawable getReleaseUpDrawable();

  Drawable getRefreshingDrawable();
}
