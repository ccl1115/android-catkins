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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.simon.catkins.R;

public class Flip3DLayout extends FrameLayout {
  private static final String TAG = "Flip3DLayout";

  public static final int STATE_INITIAL = 1000;
  public static final int STATE_FLIPPED = 1001;

  public static final int MSG_ANIMATION_FLIP = 10000;
  public static final int MSG_ANIMATION_RFLIP = 10001;

  private static final int DEPTH_CONSTANT = 120; // dips

  private int mDepthConstant;

  private boolean mTrackable;
  private int mFromId;
  private int mToId;

  private View mFrom;
  private View mTo;


  private Animator mAnimator;
  private AnimatorHandler mHandler;

  private OnAnimationEndListener mOnAnimationEndListenerListener;

  private int mDegree;
  private int mDepth;
  private int mState;
  private int mCenterX;
  private int mCenterY;

  // This value is used to calculate depth value.
  private int mWidth;

  private Camera mCamera;
  private Matrix mMatrix;

  public Flip3DLayout(Context context) {
    this(context, null, 0);
  }

  public Flip3DLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public Flip3DLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mAnimator = new Animator();
    mHandler = new AnimatorHandler();
    mMatrix = new Matrix();
    mCamera = new Camera();

    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.Flip3DLayout, defStyle, 0);

    mFromId = a.getResourceId(R.styleable.Flip3DLayout_from, 0);

    if (mFromId == 0) {
      throw new Error("the from id is illegal");
    }

    mToId = a.getResourceId(R.styleable.Flip3DLayout_to, 0);

    if (mToId == 0) {
      throw new Error("the to id is illegal");
    }

    a.recycle();

    mState = STATE_INITIAL;

    final float density = getResources().getDisplayMetrics().density;
    mDepthConstant = (int) (DEPTH_CONSTANT * density + 0.5f);
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
    if (mState == STATE_INITIAL && !mAnimator.animating) {
      mAnimator.animateFlip();
    }
  }

  public void startReverseFlip() {
    if (mState == STATE_FLIPPED && !mAnimator.animating) {
      mAnimator.animateRFlip();
    }
  }

  public void setDepthOffset(int depthOffset) {
    mDepthConstant = depthOffset;
  }

  private class AnimatorHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATION_FLIP:
          mAnimator.computeFlip();
          break;
        case MSG_ANIMATION_RFLIP:
          mAnimator.computeRFlip();
          break;
      }
    }
  }

  private Bitmap mFromCache;
  private Bitmap mToCache;

  private void prepare() {
    mFrom.destroyDrawingCache();
    mFromCache = mFrom.getDrawingCache();
    mTo.destroyDrawingCache();
    mToCache = mTo.getDrawingCache();
  }

  private class Animator {
    static final int VELOCITY = 240; // degree/s

    final int velocity; // degree/s

    boolean animating;

    long lastAnimationTime;
    long currentAnimatingTime;
    float animatingDegree;
    float animatingDegreeInterpolated;
    float animatingDepth;
    float animatingVelocity; // degree/s

    Interpolator interpolator = new Interpolator() {
      @Override
      public float getInterpolation(float t) {
        t -= 1;
        return (t * t * t * t * t) + 1;
      }
    };

    Animator() {
      velocity = VELOCITY;
    }

    private void computeFlip() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      animatingDegree += animatingVelocity * t;
      animatingDegreeInterpolated =
          180f * interpolator.getInterpolation(animatingDegree / 180f);
      final float degree = Math.abs(animatingDegreeInterpolated);
      if (degree > 0 && degree <= 90) {
        animatingDepth = mWidth / 180f * degree;
      } else {
        animatingDepth = -(mWidth / 180f) * degree + mWidth;
      }
      lastAnimationTime = now;
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
      //Log.d(TAG, "@compute " + animatingDegree);
      if (animatingDegree >= 180) {
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
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_FLIP), currentAnimatingTime);
      }
      invalidate();
    }

    private void computeRFlip() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      animatingDegree += animatingVelocity * t;
      animatingDegreeInterpolated =
          -180f * interpolator.getInterpolation(animatingDegree / -180f);
      final float degree = Math.abs(animatingDegreeInterpolated);
      if (degree > 0 && degree <= 90) {
        animatingDepth = mWidth / 180f * degree;
      } else {
        animatingDepth = -(mWidth / 180f) * degree + mWidth;
      }
      lastAnimationTime = now;
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
      if (animatingDegree <= -180) {
        animating = false;
        mDegree = 0;
        mDepth = 0;
        mState = STATE_INITIAL;

        final OnAnimationEndListener listener = mOnAnimationEndListenerListener;
        if (listener != null) {
          listener.onFlipBackAnimationEnd();
        }
      } else {
        mDegree = (int) (animatingDegreeInterpolated - 0.5f);
        mDepth = (int) animatingDepth;
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_RFLIP), currentAnimatingTime);
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
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
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
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
      mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATION_RFLIP), currentAnimatingTime);
    }
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    final long drawingTime = getDrawingTime();
    if (mAnimator.animating) {
      final int centerX = mCenterX;
      final int centerY = mCenterY;
      mCamera.save();
      mCamera.translate(0, 0, mDepth);
      canvas.save();
      if (mDegree >= 0 && mDegree <= 90) {
        mCamera.rotateY(mDegree);
        mCamera.getMatrix(mMatrix);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        if (mFromCache != null) {
          canvas.drawBitmap(mFromCache, mMatrix, null);
        } else {
          drawChild(canvas, mFrom, drawingTime);
        }
      } else if (mDegree > 90 && mDegree <= 180) {
        mCamera.rotateY(mDegree - 180);
        mCamera.getMatrix(mMatrix);
        mMatrix.postTranslate(centerX, centerY);
        mMatrix.preTranslate(-centerX, -centerY);
        canvas.concat(mMatrix);
        if (mToCache != null) {
          canvas.drawBitmap(mToCache, mMatrix, null);
        } else {
          drawChild(canvas, mTo, drawingTime);
        }
      } else if (mDegree >= -90 && mDegree <= 0) {
        mCamera.rotateY(mDegree);
        mCamera.getMatrix(mMatrix);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        if (mToCache != null) {
          canvas.drawBitmap(mToCache, mMatrix, null);
        } else {
          drawChild(canvas, mTo, drawingTime);
        }
      } else if (mDegree >= -180 && mDegree < 90) {
        mCamera.rotateY(mDegree + 180);
        mCamera.getMatrix(mMatrix);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
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
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    mCenterX = getMeasuredWidth() / 2;
    mCenterY = getMeasuredHeight() / 2;
    mWidth = getMeasuredWidth() + mDepthConstant;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (mAnimator.animating) {
      return true;
    }

    if (mState == STATE_INITIAL) {
      return mFrom.dispatchTouchEvent(ev);
    } else {
      return mTo.dispatchTouchEvent(ev);
    }
  }

  public interface OnAnimationEndListener {
    void onFlipAnimationEnd();

    void onFlipBackAnimationEnd();
  }
}
