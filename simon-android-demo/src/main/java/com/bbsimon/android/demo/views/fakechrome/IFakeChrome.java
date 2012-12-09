/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.views.fakechrome;

public interface IFakeChrome {
  void switchMode();

  void setAdapter(FakeChromeAdapter adapter);

  FakeChromeAdapter getAdapter();

  int getCurrentIndex();
}
