package com.simon.catkins.views;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;


/**
 * To handle touch event movement.
 *
 * @author Simon Yu
 */
class MotionEventTracker {
    private static final String TAG = "MotionEventTracker";

    public static final int DIRECTION_LEFT = 0x1;
    public static final int DIRECTION_TOP = 0x2;
    public static final int DIRECTION_RIGHT = 0x4;
    public static final int DIRECTION_BOTTOM = 0x8;

    private int mLastDownX;
    private int mLastDownY;

    private int mLastMoveX;
    private int mLastMoveY;

    private int mMovedX;
    private int mMovedY;

    private final OnTrackListener mOnTrackListener;

    private final int mScaledMoveSlop;

    private VelocityTracker mVelocityTracker;

    private final int mMaxVelocity;

    private boolean mTracking;

    private int mTrackingDirection;

    public MotionEventTracker(Context context, OnTrackListener listener) {
        mScaledMoveSlop = ViewConfig.getTouchEventMoveSlopMedium(context);
        mOnTrackListener = listener;
        mMaxVelocity = ViewConfig.getVelocityLarge(context);

    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.d(TAG, "@onInterceptTouchEvent");
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
                if (x - mLastDownX > mScaledMoveSlop) {
                    mTrackingDirection = DIRECTION_RIGHT;
                    mLastDownX = x;
                    mLastDownY = y;
                    mLastMoveX = x;
                    mLastMoveY = y;
                    return true;
                } else if (x - mLastDownX < -mScaledMoveSlop) {
                    mTrackingDirection = DIRECTION_LEFT;
                    mLastDownX = x;
                    mLastDownY = y;
                    mLastMoveX = x;
                    mLastMoveY = y;
                    return true;
                } else if (y - mLastDownY > mScaledMoveSlop) {
                    mTrackingDirection = DIRECTION_BOTTOM;
                    mLastDownX = x;
                    mLastDownY = y;
                    mLastMoveX = x;
                    mLastMoveY = y;
                    return true;
                } else if (y - mLastDownY < -mScaledMoveSlop) {
                    mTrackingDirection = DIRECTION_TOP;
                    mLastDownX = x;
                    mLastDownY = y;
                    mLastMoveX = x;
                    mLastMoveY = y;
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
        Log.d(TAG, "@onTouchEvent");
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
                if (!mTracking) {
                    mTracking = true;
                    mOnTrackListener.onStartTracking(this, mTrackingDirection);
                }
                final int dx = x - mLastMoveX;
                final int dy = y - mLastMoveY;
                mLastMoveX = x;
                mLastMoveY = y;
                mMovedX = x - mLastDownX;
                mMovedY = y - mLastDownY;
                mVelocityTracker.addMovement(event);
                return mOnTrackListener.onMove(this, dx, dy,
                        mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTracking) {
                    mTracking = false;
                    mOnTrackListener.onStopTracking(this);
                }
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

    public boolean isTracking() {
        return mTracking;
    }

    public interface OnTrackListener {
        public boolean onMove(MotionEventTracker tracker, int dx, int dy, float vx, float vy);

        public boolean onStartTracking(MotionEventTracker tracker, int direction);

        public boolean onStopTracking(MotionEventTracker tracker);
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
