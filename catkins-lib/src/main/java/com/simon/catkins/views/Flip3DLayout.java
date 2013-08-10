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

    private static final int MSG_HORIZONTAL = 0x00;
    private static final int MSG_VERTICAL = 0xF0;

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
        mInjector.animate(MSG_ANIMATION_FLIP | mMSGOrientation);
    }

    public void startReverseFlip() {
        mInjector.animate(MSG_ANIMATION_RFLIP | mMSGOrientation);
    }

    private int mMSGOrientation = MSG_VERTICAL;

    public void setTransition(int transition) {
        if (transition == TRANSITION_VERTICAL) {
            mMSGOrientation = MSG_VERTICAL;
        } else {
            mMSGOrientation = MSG_HORIZONTAL;
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

    public int getState() {
        return mState;
    }

    public interface OnAnimationEndListener {
        void onFlipAnimationEnd();

        void onFlipBackAnimationEnd();
    }


    private class FlipInjector implements ViewGroupInjector {
        private static final int VELOCITY = 360; // degree/s

        private final int velocity; // degree/s

        boolean animating;

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

        // All variants for animation calculation
        private long lastAnimationTime;
        private long currentAnimatingTime;
        float animatingDegree;
        float animatingDegreeInterpolated;
        float animatingDepth;
        float animatingVelocity; // degree/s

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


        FlipInjector() {
            velocity = VELOCITY;

            mMatrix = new Matrix();
            mCamera = new Camera();

            mHandler = new AnimatorHandler();
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
            final float t = (now - lastAnimationTime) / 1000f;
            animatingDegree += animatingVelocity * t;
            animatingDegreeInterpolated =
                    180f * ViewConfig.sInterpolator.getInterpolation(animatingDegree / 180f);
            final float degree = Math.abs(animatingDegreeInterpolated);
            if (degree > 0 && degree <= 90) {
                animatingDepth = mWidth / 180f * degree;
            } else {
                animatingDepth = -(mWidth / 180f) * degree + mWidth;
            }
            lastAnimationTime = now;
            currentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            if (animatingDegree>= 180) {
                animating = false;
                mDegree = 180;
                mDepth = 0;
                mState = STATE_FLIPPED;

                final OnAnimationEndListener listener = mOnAnimationEndListenerListener;
                if (listener != null) {
                    listener.onFlipAnimationEnd();
                }
            } else {
                mDegree = (int) (animatingDegreeInterpolated + 0.5f);
                mDepth = (int) animatingDepth;
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATION_FLIP, currentAnimatingTime);
            }
            invalidate();
        }

        private void computeRFlip() {
            final long now = SystemClock.uptimeMillis();
            final float t = (now - lastAnimationTime) / 1000f;
            animatingDegree += animatingVelocity * t;
            animatingDegreeInterpolated =
                    -180f * ViewConfig.sInterpolator.getInterpolation(animatingDegree / -180f);
            final float degree = Math.abs(animatingDegreeInterpolated);
            if (degree > 0 && degree <= 90) {
                animatingDepth = mWidth / 180f * degree;
            } else {
                animatingDepth = -(mWidth / 180f) * degree + mWidth;
            }
            lastAnimationTime = now;
            currentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            if (animatingDegree <= -180) {
                animating = false;
                mDegree = -180;
                mDepth = 0;
                mState = STATE_INITIAL;

                final OnAnimationEndListener listener = mOnAnimationEndListenerListener;
                if (listener != null) {
                    listener.onFlipBackAnimationEnd();
                }
            } else {
                mDegree = (int) (animatingDegreeInterpolated - 0.5f);
                Log.d(TAG, "degree = " + mDegree);
                mDepth = (int) animatingDepth;
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATION_RFLIP, currentAnimatingTime);
            }
            invalidate();
        }

        private void animateFlip() {
            animating = true;
            animatingVelocity = velocity;
            animatingDegree = 0;
            animatingDepth = 0;
            prepare();
            final long now = SystemClock.uptimeMillis();
            lastAnimationTime = now;
            currentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_FLIP), currentAnimatingTime);
        }

        private void animateRFlip() {
            animating = true;
            animatingVelocity = -velocity;
            animatingDegree = 0;
            animatingDepth = 0;
            prepare();
            final long now = SystemClock.uptimeMillis();
            lastAnimationTime = now;
            currentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_RFLIP), currentAnimatingTime);
        }

        @Override
        public void layout(boolean changed, int l, int t, int r, int b) {

        }

        @Override
        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            Flip3DLayout.super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            mCenterX = getMeasuredWidth() / 2;
            mCenterY = getMeasuredHeight() / 2;
            mWidth = getMeasuredWidth() + mDepthConstant;
        }

        @Override
        public void draw(Canvas canvas) {
            final long drawingTime = getDrawingTime();
            if (animating) {
                mCamera.save();
                mCamera.translate(0, 0, mDepth);
                canvas.save();
                if (mDegree >= 0 && mDegree <= 90) {
                    if (mTransition == MSG_HORIZONTAL) mCamera.rotateX(mDegree);
                    else if (mTransition == MSG_VERTICAL) mCamera.rotateY(mDegree);
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
                    if (mTransition == MSG_HORIZONTAL) mCamera.rotateX(mDegree - 180);
                    else if (mTransition == MSG_VERTICAL) mCamera.rotateY(mDegree - 180);
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
                    if (mTransition == MSG_HORIZONTAL) mCamera.rotateX(mDegree);
                    else if (mTransition == MSG_VERTICAL) mCamera.rotateY(mDegree);
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
                    if (mTransition == MSG_HORIZONTAL) mCamera.rotateX(mDegree + 180);
                    else if (mTransition == MSG_VERTICAL) mCamera.rotateY(mDegree + 180);
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
            return false;
        }

        @Override
        public boolean touchEvent(MotionEvent event) {
            return false;
        }

        @Override
        public void animate(int msg) {
            final int direction = msg & DIRECTION_MASK;
            mTransition = msg & TRANSITION_MASK;
            switch(direction) {
                case MSG_ANIMATION_FLIP:
                    if (!animating && mState != STATE_FLIPPED) {
                        animateFlip();
                    }
                    break;
                case MSG_ANIMATION_RFLIP:
                    if (!animating && mState != STATE_INITIAL) {
                        animateRFlip();
                    }
                    break;
            }
        }

        @Override
        public boolean isAnimating() {
            return animating;
        }
    }

}
