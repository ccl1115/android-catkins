package com.bbsimon.android.demo.views.fakechrome.scalable;

@SuppressWarnings("unused")
public interface IScalable {

    public final int STATE_SCALED = 0xF3;
    public final int STATE_EXPAND = 0xF4;

    public final int NO_ROTATE    = 0x1;
    public final int LEFT_ROTATE  = 0x2;
    public final int RIGHT_ROTATE = 0x4;

    void setTransformType(int rotate);

    void startScale();

    void startExpand();

    void transform(float scale);

    float getScale();

    void setOnScaleListener(OnScaleListener listener);

    void setOnExpandListener(OnExpandListener listener);

    interface OnScaleListener {
        void onScaled();
    }

    interface OnExpandListener {
        void onExpanded();
    }
}
