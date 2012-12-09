/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.views.fakechrome;

public interface FakeChromeListener {

  public static interface OnPagerDeletion {
    void onPagerDeleted();
  }

  public static interface OnOverviewDeletion {
    void onOverviewDeleeted();
  }
}
