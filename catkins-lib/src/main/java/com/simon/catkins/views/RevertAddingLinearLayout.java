package com.simon.catkins.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * @author bb.simon.yu@gmail.com
 *
 * <b>This only work on vertical orientation</b>
 */
public class RevertAddingLinearLayout extends LinearLayout {
  private static final String TAG = "RevertAddingLinearLayout";

  private static final int MSG_ADD_VIEW = 1000;
  private static final int MSG_REMOVE_VIEW = 1001;

  private int mAddingIndex = -1;
  private int mRemovingIndex = -1;

  private Rect mIndexFrame = new Rect();

  private final TransitionAnimator mAnimator;

  public RevertAddingLinearLayout(Context context) {
    this(context, null, 0);
  }

  public RevertAddingLinearLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RevertAddingLinearLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mAnimator = new DefaultTransitionAnimation();
  }

  @Override
  public void addView(View child) {
    Log.d(TAG, "@addView");
    if (getChildCount() == 0) {
      super.addView(child);
      return;
    }

    super.addView(child, getChildCount() - 1);

    final int measuredWidth = getMeasuredWidth();
    measureChild(child, measuredWidth + MeasureSpec.UNSPECIFIED,
        getMeasuredHeight() + MeasureSpec.UNSPECIFIED);
    final int childMeasuredHeight = child.getMeasuredHeight();
    final int childMeasuredWidth = child.getMeasuredWidth();
    final View pre = getChildAt(getChildCount() - 3);
    if (pre == null) {
      child.layout((measuredWidth - childMeasuredWidth) >> 1, getTop(),
          (measuredWidth + childMeasuredWidth) >> 1, childMeasuredHeight);
    } else {
      child.layout(0, pre.getBottom(), measuredWidth, pre.getBottom() + childMeasuredHeight);
    }

    child.getHitRect(mIndexFrame);
    Log.d(TAG, "@addView " + mIndexFrame);
    mAddingIndex = getChildCount() - 2;
    mAnimator.animate(MSG_ADD_VIEW);
  }

  @Override
  public void removeView(View view) {
    view.getHitRect(mIndexFrame);
    Log.d(TAG, "@removeView " + mIndexFrame);
    mRemovingIndex = indexOfChild(view);
    mAnimator.animate(MSG_REMOVE_VIEW);
  }

  private void postRemoveView(View view) {
    super.removeView(view);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (mAnimator.isAnimating()) {
      mAnimator.draw(canvas);
    } else {
      super.dispatchDraw(canvas);
    }
  }

  private class DefaultTransitionAnimation implements TransitionAnimator {
    private final float mProportionVelocity;
    private boolean mAnimating;

    private float mAnimatingVelocity;
    private float mAnimatingProportion;
    private long mCurrentAnimatingTime;
    private long mLastAnimationTime;

    private float mInterpolatedPropotion;

    private Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case MSG_ADD_VIEW:
            computeAdding();
            break;
          case MSG_REMOVE_VIEW:
            computeRemoving();
            break;
        }
      }
    };


    public DefaultTransitionAnimation() {
      final float densitiy = getResources().getDisplayMetrics().density;
      mProportionVelocity = densitiy * Facade.PROPORTION_VELOCITY_SMALL;
    }

    @Override
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
    }

    @Override
    public void layout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public void draw(Canvas canvas) {
      final long drawingTime = getDrawingTime();

      if (mAddingIndex != -1) {
        drawInternal(canvas, mAddingIndex, drawingTime);
      } else if (mRemovingIndex != -1) {
        drawInternal(canvas, mRemovingIndex, drawingTime);
      }
    }

    private void drawInternal(Canvas canvas, int index, long drawingTime) {
      for (int i = 0; i < index; i++) {
        drawChild(canvas, getChildAt(i), drawingTime);
      }

      final View child = getChildAt(index);
      int count = canvas.save();
      canvas.scale(1, mInterpolatedPropotion, 0, mIndexFrame.top);
      drawChild(canvas, child, drawingTime);
      canvas.restoreToCount(count);

      count = canvas.save();
      canvas.translate(0, - mIndexFrame.height() + mIndexFrame.height() * mInterpolatedPropotion);
      for (int i = index + 1; i < getChildCount(); i++) {
        drawChild(canvas, getChildAt(i), drawingTime);
      }
      canvas.restoreToCount(count);

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
      switch (msg) {
        case MSG_ADD_VIEW: {
          final long now = SystemClock.uptimeMillis();
          mLastAnimationTime = now;
          mCurrentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;

          mAnimatingProportion = 0f;
          mAnimating = true;
          mAnimatingVelocity = mProportionVelocity;
          mInterpolatedPropotion = mAnimatingProportion;
          mHandler.removeMessages(MSG_ADD_VIEW);
          mHandler.removeMessages(MSG_REMOVE_VIEW);
          mHandler.sendEmptyMessageAtTime(MSG_ADD_VIEW, mCurrentAnimatingTime);
          break;
        }
        case MSG_REMOVE_VIEW: {
          final long now = SystemClock.uptimeMillis();
          mLastAnimationTime = now;
          mCurrentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;

          mAnimatingProportion = 1f;
          mAnimating = true;
          mAnimatingVelocity = -mProportionVelocity;
          mInterpolatedPropotion = mAnimatingProportion;
          mHandler.removeMessages(MSG_ADD_VIEW);
          mHandler.removeMessages(MSG_REMOVE_VIEW);
          mHandler.sendEmptyMessageAtTime(MSG_REMOVE_VIEW, mCurrentAnimatingTime);
          break;
        }
      }
    }

    private void computeAdding() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - mLastAnimationTime) / Facade.ONE_SECOND_FLOAT;

      mAnimatingProportion += mAnimatingVelocity * t;

      if (mAnimatingProportion >= 1f) {
        mAnimatingProportion = 1f;
        mInterpolatedPropotion = 1f;
        mAnimating = false;
        mAddingIndex = -1;
      } else {
        mInterpolatedPropotion = Facade.sInterpolator.getInterpolation(mAnimatingProportion);
        mLastAnimationTime = now;
        mCurrentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
        mHandler.sendEmptyMessageAtTime(MSG_ADD_VIEW, mCurrentAnimatingTime);
      }
      invalidate();
    }

    private void computeRemoving() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - mLastAnimationTime) / Facade.ONE_SECOND_FLOAT;

      mAnimatingProportion += mAnimatingVelocity * t;

      if (mAnimatingProportion <= 0f) {
        mAnimatingProportion = 0f;
        mInterpolatedPropotion = 0f;
        mAnimating = false;
        postRemoveView(getChildAt(mRemovingIndex));
        mRemovingIndex = -1;
      } else {
        mInterpolatedPropotion = Facade.sReverseInterpolator.getInterpolation(mAnimatingProportion);
        mLastAnimationTime = now;
        mCurrentAnimatingTime += Facade.ANIMATION_FRAME_DURATION;
        mHandler.sendEmptyMessageAtTime(MSG_REMOVE_VIEW, mCurrentAnimatingTime);
      }
      invalidate();
    }

    @Override
    public boolean isAnimating() {
      return mAnimating;
    }
  }
}
