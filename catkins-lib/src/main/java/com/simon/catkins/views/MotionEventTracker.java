package com.simon.catkins.views;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * To handle touch event movement.
 *
 * @author Simon Yu
 */
class MotionEventTracker {

    private int mLastDownX;
    private int mLastDownY;

    private int mLastMoveX;
    private int mLastMoveY;

    private int mMovedX;
    private int mMovedY;

    private final OnMoveListener mOnMoveListener;

    private final int mScaledMoveSlop;

    private VelocityTracker mVelocityTracker;

    private final int mMaxVelocity;

    public MotionEventTracker(Context context, OnMoveListener listener) {
        mScaledMoveSlop = ViewConfig.getTouchEventMoveSlopMedium(context);
        mOnMoveListener = listener;
        mMaxVelocity = ViewConfig.getVelocityLarge(context);

    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        final int x = (int) MotionEventCompat.getX(event, 0);
        final int y = (int) MotionEventCompat.getY(event, 0);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastDownX = x;
                mLastDownY = y;
                initVelocityTracker();
                break;
            case MotionEvent.ACTION_MOVE:
                if (x - mLastDownX > mScaledMoveSlop
                        || y - mLastDownY > mScaledMoveSlop) {
                    mLastDownX = x;
                    mLastDownY = y;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();
                break;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        final int x = (int) MotionEventCompat.getX(event, 0);
        final int y = (int) MotionEventCompat.getY(event, 0);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMoveX = x;
                mLastMoveY = y;
                initVelocityTracker();
                break;
            case MotionEvent.ACTION_MOVE:
                final int dx = x - mLastMoveX;
                final int dy = y - mLastMoveY;
                mLastMoveX = x;
                mLastMoveY = y;
                mMovedX = x - mLastDownX;
                mMovedY = y - mLastDownY;
                mVelocityTracker.addMovement(event);
                return mOnMoveListener.onMove(this, dx, dy,
                        mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
            case MotionEvent.ACTION_UP:
                recycleVelocityTracker();
                break;
        }

        return false;
    }

    public int getMovedX() {
        return mMovedX;
    }


    public int getMovedY() {
        return mMovedY;
    }

    public int getScaledMoveSlop() {
        return mScaledMoveSlop;
    }

    public interface OnMoveListener {
        public boolean onMove(MotionEventTracker tracker, int dx, int dy, float vx, float vy);
    }

    private void initVelocityTracker() {
        recycleVelocityTracker();
        mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.computeCurrentVelocity(ViewConfig.VELOCITY_UNIT, mMaxVelocity);
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }
}
