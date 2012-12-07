package com.bbsimon.android.demo.views.fakechrome;

public interface IFakeChrome {
    void switchMode();

    void setAdapter(FakeChromeAdapter adapter);

    FakeChromeAdapter getAdapter();

    int getCurrentIndex();
}
