/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.simon.catkins.R;

public class Flip3DLayout extends FrameLayout {
    private static final String TAG = "Flip3DLayout";

    public static final int STATE_INITIAL = 1000;
    public static final int STATE_FLIPPED = 1001;

    private static final int MSG_ANIMATION_FLIP = 0x00;
    private static final int MSG_ANIMATION_RFLIP = 0x0F;

    private static final int MSG_VERTICAL = 0x00;
    private static final int MSG_HORIZONTAL = 0xF0;

    private static final int DIRECTION_MASK = 0x0F;
    private static final int TRANSITION_MASK = 0xF0;

    public static final int TRANSITION_VERTICAL = 0x1;
    public static final int TRANSITION_HORIZONTAL = 0x2;

    private static final int DEPTH_CONSTANT = 120; // dips

    private int mDepthConstant;

    private boolean mTrackable;

    private int mState;

    private int mFromId;
    private int mToId;

    private View mFrom;
    private View mTo;

    private Bitmap mFromCache;
    private Bitmap mToCache;

    private OnAnimationEndListener mOnAnimationEndListenerListener;

    private final ViewGroupInjector mInjector;

    public Flip3DLayout(Context context) {
        this(context, null, 0);
    }

    public Flip3DLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Flip3DLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Flip3DLayout, defStyle, 0);

        mFromId = a.getResourceId(R.styleable.Flip3DLayout_from, 0);

        if (mFromId == 0) {
            throw new Error("the from id is illegal");
        }

        mToId = a.getResourceId(R.styleable.Flip3DLayout_to, 0);

        if (mToId == 0) {
            throw new Error("the to id is illegal");
        }

        mTrackable = a.getBoolean(R.styleable.Flip3DLayout_trackable, false);

        a.recycle();

        mState = STATE_INITIAL;

        final float density = getResources().getDisplayMetrics().density;
        mDepthConstant = (int) (DEPTH_CONSTANT * density + 0.5f);

        mInjector = new FlipInjector();
    }

    @Override
    protected void onFinishInflate() {
        mFrom = findViewById(mFromId);
        mTo = findViewById(mToId);

        if (mFrom == null || mTo == null) {
            throw new Error("the from view or the to view is null");
        }

        if (!mFrom.getParent().equals(mTo.getParent())) {
            throw new Error("the from view and the to view are not in the same ViewGroup");
        }
    }

    public void setOnAnimationEnd(OnAnimationEndListener listener) {
        mOnAnimationEndListenerListener = listener;
    }

    public void startFlip() {
        mInjector.animate(MSG_ANIMATION_FLIP | mMSGTransition);
    }

    public void startReverseFlip() {
        mInjector.animate(MSG_ANIMATION_RFLIP | mMSGTransition);
    }

    private int mMSGTransition = MSG_HORIZONTAL;

    public void setTransition(int transition) {
        if (transition == TRANSITION_VERTICAL) {
            mMSGTransition = MSG_HORIZONTAL;
        } else {
            mMSGTransition = MSG_VERTICAL;
        }
    }

    public void setDepthOffset(int depthOffset) {
        mDepthConstant = depthOffset;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mInjector.draw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mInjector.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mInjector.interceptionTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mInjector.touchEvent(event);
    }

    public int getState() {
        return mState;
    }

    public interface OnAnimationEndListener {
        void onFlipAnimationEnd();

        void onFlipBackAnimationEnd();
    }


    private class FlipInjector implements ViewGroupInjector, MotionEventTracker.OnTrackListener {
        private static final int VELOCITY = 120; // degree/s

        private final int velocity; // degree/s

        private boolean mAnimating;

        private boolean mTracking;

        // All variants for canvas drawing
        private int mDegree;
        private int mDepth;
        private int mCenterX;
        private int mCenterY;

        private int mWidth;
        private int mTransition;

        private final Camera mCamera;
        private final Matrix mMatrix;

        private final AnimatorHandler mHandler;
        private final MotionEventTracker mTracker;

        // All variants for animation calculation
        private long mLastAnimationTime;
        private long mCurrentAnimatingTime;
        float mAnimatingDegree;
        float mAnimatingDegreeInterpolated;
        float mAnimatingDepth;
        float mAnimatingVelocity; // degree/s

        private class AnimatorHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ANIMATION_FLIP:
                        computeFlip();
                        break;
                    case MSG_ANIMATION_RFLIP:
                        computeRFlip();
                        break;
                }
            }
        }


        public FlipInjector() {
            velocity = VELOCITY;

            mMatrix = new Matrix();
            mCamera = new Camera();

            mHandler = new AnimatorHandler();
            mTracker = new MotionEventTracker(getContext(), this);
        }

        private void prepare() {
            if (mFromCache != null) {
                mFromCache.recycle();
            }
            mFrom.destroyDrawingCache();
            mFromCache = mFrom.getDrawingCache();

            if (mToCache != null) {
                mToCache.recycle();
            }
            mTo.destroyDrawingCache();
            mToCache = mTo.getDrawingCache();
        }

        private void computeFlip() {
            final long now = SystemClock.uptimeMillis();
            final float t = (now - mLastAnimationTime) / 1000f;
            mAnimatingDegree += mAnimatingVelocity * t;
            mAnimatingDegreeInterpolated =
                    180f * ViewConfig.sInterpolator.getInterpolation(mAnimatingDegree / 180f);
            final float degree = Math.abs(mAnimatingDegreeInterpolated);
            if (degree > 0 && degree <= 90) {
                mAnimatingDepth = mWidth / 180f * degree;
            } else {
                mAnimatingDepth = -(mWidth / 180f) * degree + mWidth;
            }
            mLastAnimationTime = now;
            mCurrentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            if (mAnimatingDegree >= 180) {
                mAnimating = false;
                mDegree = 0;
                mDepth = 0;
                mState = STATE_FLIPPED;

                final OnAnimationEndListener listener = mOnAnimationEndListenerListener;
                if (listener != null) {
                    listener.onFlipAnimationEnd();
                }
            } else {
                mDegree = (int) (mAnimatingDegreeInterpolated + 0.5f);
                mDepth = (int) mAnimatingDepth;
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATION_FLIP, mCurrentAnimatingTime);
            }
            invalidate();
        }

        private void computeRFlip() {
            final long now = SystemClock.uptimeMillis();
            final float t = (now - mLastAnimationTime) / 1000f;
            mAnimatingDegree += mAnimatingVelocity * t;
            mAnimatingDegreeInterpolated =
                    -180f * ViewConfig.sInterpolator.getInterpolation(mAnimatingDegree / -180f);
            final float degree = Math.abs(mAnimatingDegreeInterpolated);
            if (degree > 0 && degree <= 90) {
                mAnimatingDepth = mWidth / 180f * degree;
            } else {
                mAnimatingDepth = -(mWidth / 180f) * degree + mWidth;
            }
            mLastAnimationTime = now;
            mCurrentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            if (mAnimatingDegree <= -180) {
                mAnimating = false;
                mDegree = 0;
                mDepth = 0;
                mState = STATE_INITIAL;

                final OnAnimationEndListener listener = mOnAnimationEndListenerListener;
                if (listener != null) {
                    listener.onFlipBackAnimationEnd();
                }
            } else {
                mDegree = (int) (mAnimatingDegreeInterpolated - 0.5f);
                mDepth = (int) mAnimatingDepth;
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATION_RFLIP, mCurrentAnimatingTime);
            }
            invalidate();
        }

        private void animateFlip() {
            mAnimating = true;
            mAnimatingVelocity = velocity;
            mAnimatingDegree = mDegree;
            mAnimatingDepth = mDepth;
            prepare();
            final long now = SystemClock.uptimeMillis();
            mLastAnimationTime = now;
            mCurrentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_FLIP), mCurrentAnimatingTime);
        }

        private void animateRFlip() {
            mAnimating = true;
            mAnimatingVelocity = -velocity;
            mAnimatingDegree = mDegree;
            mAnimatingDepth = mDepth;
            prepare();
            final long now = SystemClock.uptimeMillis();
            mLastAnimationTime = now;
            mCurrentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_RFLIP), mCurrentAnimatingTime);
        }

        @Override
        public void layout(boolean changed, int l, int t, int r, int b) {

        }

        @Override
        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            Flip3DLayout.super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            mCenterX = getMeasuredWidth() >> 1;
            mCenterY = getMeasuredHeight() >> 1;
            mWidth = getMeasuredWidth() + mDepthConstant;
        }

        @Override
        public void draw(Canvas canvas) {
            final long drawingTime = getDrawingTime();
            if (mAnimating || mTracking) {
                mCamera.save();
                mCamera.translate(0, 0, mDepth);
                canvas.save();
                if (mDegree >= 0 && mDegree <= 90) {
                    if (mTransition == MSG_VERTICAL) mCamera.rotateX(mDegree);
                    else if (mTransition == MSG_HORIZONTAL) mCamera.rotateY(mDegree);
                    mCamera.getMatrix(mMatrix);
                    mMatrix.preTranslate(-mCenterX, -mCenterY);
                    mMatrix.postTranslate(mCenterX, mCenterY);
                    canvas.concat(mMatrix);
                    if (mFromCache != null) {
                        canvas.drawBitmap(mFromCache, mMatrix, null);
                    } else {
                        drawChild(canvas, mFrom, drawingTime);
                    }
                } else if (mDegree > 90 && mDegree <= 180) {
                    if (mTransition == MSG_VERTICAL) mCamera.rotateX(mDegree - 180);
                    else if (mTransition == MSG_HORIZONTAL) mCamera.rotateY(mDegree - 180);
                    mCamera.getMatrix(mMatrix);
                    mMatrix.postTranslate(mCenterX, mCenterY);
                    mMatrix.preTranslate(-mCenterX, -mCenterY);
                    canvas.concat(mMatrix);
                    if (mToCache != null) {
                        canvas.drawBitmap(mToCache, mMatrix, null);
                    } else {
                        drawChild(canvas, mTo, drawingTime);
                    }
                } else if (mDegree >= -90 && mDegree <= 0) {
                    if (mTransition == MSG_VERTICAL) mCamera.rotateX(mDegree);
                    else if (mTransition == MSG_HORIZONTAL) mCamera.rotateY(mDegree);
                    mCamera.getMatrix(mMatrix);
                    mMatrix.preTranslate(-mCenterX, -mCenterY);
                    mMatrix.postTranslate(mCenterX, mCenterY);
                    canvas.concat(mMatrix);
                    if (mToCache != null) {
                        canvas.drawBitmap(mToCache, mMatrix, null);
                    } else {
                        drawChild(canvas, mTo, drawingTime);
                    }
                } else if (mDegree >= -180 && mDegree < 90) {
                    if (mTransition == MSG_VERTICAL) mCamera.rotateX(mDegree + 180);
                    else if (mTransition == MSG_HORIZONTAL) mCamera.rotateY(mDegree + 180);
                    mCamera.getMatrix(mMatrix);
                    mMatrix.preTranslate(-mCenterX, -mCenterY);
                    mMatrix.postTranslate(mCenterX, mCenterY);
                    canvas.concat(mMatrix);
                    if (mFromCache != null) {
                        canvas.drawBitmap(mFromCache, mMatrix, null);
                    } else {
                        drawChild(canvas, mFrom, drawingTime);
                    }
                }
                mCamera.restore();
                canvas.restore();
            } else {
                if (mState == STATE_INITIAL) {
                    drawChild(canvas, mFrom, drawingTime);
                } else {
                    drawChild(canvas, mTo, drawingTime);
                }
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return false;
        }

        @Override
        public boolean interceptionTouchEvent(MotionEvent event) {
            return mTracker.onInterceptTouchEvent(event);
        }

        @Override
        public boolean touchEvent(MotionEvent event) {
            return mTracker.onTouchEvent(event);
        }

        @Override
        public boolean onMove(MotionEventTracker tracker, int dx, int dy, float vx, float vy) {
            //Log.d(TAG, "moved from last dx = " + dx + " dy = " + dy);
            //Log.d(TAG, "moved totally x = " + tracker.getMovedX() + " y = " + tracker.getMovedY());
            //Log.d(TAG, "x velocity = " + vx + " y velocity = " + vy);
            if (mMSGTransition == MSG_HORIZONTAL) {
                if (mState == STATE_INITIAL && tracker.getMovedX() > 0
                        || mState == STATE_FLIPPED && tracker.getMovedX() < 0) {
                    mDegree = (int) (180f * ((float) tracker.getMovedX() / getMeasuredWidth()));
                    invalidate();
                    return true;
                } else {
                    return false;
                }
            } else if (mMSGTransition == MSG_VERTICAL) {
                if (mState == STATE_INITIAL && tracker.getMovedY() > 0
                        || mState == STATE_FLIPPED && tracker.getMovedY() < 0) {
                    mDegree = (int) (180f * ((float) tracker.getMovedY() / getMeasuredHeight()));
                    invalidate();
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean onStartTracking(MotionEventTracker tracker, int direction) {
            Log.d(TAG, "direction = " + direction);

            mTracking = true;
            return true;
        }

        @Override
        public boolean onStopTracking(MotionEventTracker tracker) {
            mTracking = false;
            if (mMSGTransition == MSG_HORIZONTAL) {
                if (mState == STATE_INITIAL) {
                    animateFlip();
                } else if (mState == STATE_FLIPPED) {
                    animateRFlip();
                }
            } else if (mMSGTransition == MSG_VERTICAL) {
                if (mState == STATE_INITIAL) {
                    animateFlip();
                } else if (mState == STATE_FLIPPED) {
                    animateRFlip();
                }
            }
            return false;
        }

        @Override
        public void animate(int msg) {
            final int direction = msg & DIRECTION_MASK;
            mTransition = msg & TRANSITION_MASK;
            switch(direction) {
                case MSG_ANIMATION_FLIP:
                    if (!mAnimating && mState != STATE_FLIPPED) {
                        animateFlip();
                    }
                    break;
                case MSG_ANIMATION_RFLIP:
                    if (!mAnimating && mState != STATE_INITIAL) {
                        animateRFlip();
                    }
                    break;
            }
        }

        @Override
        public boolean isAnimating() {
            return mAnimating;
        }

    }

}
