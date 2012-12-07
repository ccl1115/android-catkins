package com.bbsimon.android.demo.views.awesome;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class AwesomeFrameLayout extends FrameLayout implements IAwesome {
    private static final String TAG = "AwesomeFrameLayout";

    private IAwesome mAwesomeDelegate;
    private AwesomeInterceptor mInterceptor;

    public AwesomeFrameLayout(Context context) {
        this(context, null, 0);
    }

    public AwesomeFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AwesomeFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAwesomeDelegate = new IAwesomeImpl(this);
        loadAttrs(attrs);

        setAlwaysDrawnWithCacheEnabled(false);
        mInterceptor = (AwesomeInterceptor) mAwesomeDelegate;
    }

    @Override
    public void loadAttrs(AttributeSet attrs) {
        mAwesomeDelegate.loadAttrs(attrs);
    }

    @Override
    public int getLeftOffset() {
        return mAwesomeDelegate.getLeftOffset();
    }

    @Override
    public int getRightOffset() {
        return mAwesomeDelegate.getRightOffset();
    }

    @Override
    public int getTopOffset() {
        return mAwesomeDelegate.getTopOffset();
    }

    @Override
    public int getBottomOffset() {
        return mAwesomeDelegate.getBottomOffset();
    }

    @Override
    public boolean getLeftTrackable() {
        return mAwesomeDelegate.getLeftTrackable();
    }

    @Override
    public boolean getRightTrackable() {
        return mAwesomeDelegate.getRightTrackable();
    }

    @Override
    public boolean getTopTrackable() {
        return mAwesomeDelegate.getTopTrackable();
    }

    @Override
    public boolean getBottomTrackable() {
        return mAwesomeDelegate.getBottomTrackable();
    }

    @Override
    public void setLeftTrackable(boolean trackable) {
        mAwesomeDelegate.setLeftTrackable(trackable);
    }

    @Override
    public void setRightTrackable(boolean trackable) {
        mAwesomeDelegate.setRightTrackable(trackable);
    }

    @Override
    public void setTopTrackable(boolean trackable) {
        mAwesomeDelegate.setTopTrackable(trackable);
    }

    @Override
    public void setBottomTrackable(boolean trackable) {
        mAwesomeDelegate.setBottomTrackable(trackable);
    }

    @Override
    public boolean getLeftTapBack() {
        return mAwesomeDelegate.getLeftTapBack();
    }

    @Override
    public boolean getRightTapBack() {
        return mAwesomeDelegate.getRightTapBack();
    }

    @Override
    public boolean getTopTapBack() {
        return mAwesomeDelegate.getTopTapBack();
    }

    @Override
    public boolean getBottomTapBack() {
        return mAwesomeDelegate.getBottomTapBack();
    }

    @Override
    public void setLeftTapBack(boolean tapBack) {
        mAwesomeDelegate.setLeftTapBack(tapBack);
    }

    @Override
    public void setRightTapBack(boolean tapBack) {
        mAwesomeDelegate.setRightTapBack(tapBack);
    }

    @Override
    public void setTopTapBack(boolean tapBack) {
        mAwesomeDelegate.setTopTapBack(tapBack);
    }

    @Override
    public void setBottomTapBack(boolean tapBack) {
        mAwesomeDelegate.setBottomTapBack(tapBack);
    }

    @Override
    public void left() {
    }

    @Override
    public void right() {
    }

    @Override
    public void top() {
    }

    @Override
    public void bottom() {
    }

    @Override
    public void open() {
    }

    @Override
    public void animateLeft() {
        mAwesomeDelegate.animateLeft();
    }

    @Override
    public void animateRight() {
        mAwesomeDelegate.animateRight();
    }

    @Override
    public void animateTop() {
        mAwesomeDelegate.animateTop();
    }

    @Override
    public void animateBottom() {
        mAwesomeDelegate.animateBottom();
    }

    @Override
    public void animateOpen() {
        mAwesomeDelegate.animateOpen();
    }

    @Override
    public int getState() {
        return mAwesomeDelegate.getState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mInterceptor.postInterceptMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mInterceptor.postInterceptLayout(changed, l, t, r, b);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mInterceptor.interceptDispatchDraw(canvas);
        super.dispatchDraw(canvas);
        mInterceptor.postInterceptDispatchDraw(canvas);
    }
}
