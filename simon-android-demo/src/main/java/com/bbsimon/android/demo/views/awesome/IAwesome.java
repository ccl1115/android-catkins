package com.bbsimon.android.demo.views.awesome;

import android.util.AttributeSet;

@SuppressWarnings("unused")
public interface IAwesome {
    public static final int STATE_COLLAPSE_LEFT = 10000;
    public static final int STATE_COLLAPSE_RIGHT = 10001;
    public static final int STATE_COLLAPSE_TOP = 10002;
    public static final int STATE_COLLAPSE_BOTTOM = 10003;
    public static final int STATE_EXPAND = 10004;

    void loadAttrs(AttributeSet attrs);

    int getLeftOffset();

    int getRightOffset();

    int getTopOffset();

    int getBottomOffset();

    boolean getLeftTrackable();

    boolean getRightTrackable();

    boolean getTopTrackable();

    boolean getBottomTrackable();

    void setLeftTrackable(boolean trackable);

    void setRightTrackable(boolean trackable);

    void setTopTrackable(boolean trackable);

    void setBottomTrackable(boolean trackable);

    boolean getLeftTapBack();

    boolean getRightTapBack();

    boolean getTopTapBack();

    boolean getBottomTapBack();

    void setLeftTapBack(boolean tapBack);

    void setRightTapBack(boolean tapBack);

    void setTopTapBack(boolean tapBack);

    void setBottomTapBack(boolean tapBack);

    void left();

    void right();

    void top();

    void bottom();

    void open();

    void animateLeft();

    void animateRight();

    void animateTop();

    void animateBottom();

    void animateOpen();

    int getState();

}
