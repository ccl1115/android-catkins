package com.simon.catkins.views.pager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.simon.catkins.R;
import com.simon.catkins.views.Facade;
import com.simon.catkins.views.TransitionAnimator;

/**
 */
public interface Indicator {
  int getCount();

  void setCount(int count);

  void setPosition(float position);

  float getPosition();

  void animateTo(int position);

  void animateNext();

  void animatePre();

  boolean isAutoHide();

  void setAutoHide(boolean autoHide);

  void setDrawable(Drawable drawable);

  Drawable getDrawable();

  void setSelector(Drawable drawable);

  Drawable getSelector();

  void setSpacing(int spacing);

  int getSpacing();

  public class IndicatorView extends View implements Indicator {
    private static final String TAG = "Indicator$IndicatorView";

    private static final int DEFAULT_SPACING = 50;
    private static final int INVALID_DIMENSION = -1;

    private final int kDefaultSpacing;

    private boolean mAutoHide;

    private int mSpacing;
    private int mCount;

    private float mPosition;
    private float mTargetPosition;

    private Drawable mDrawable;
    private Drawable mSelector;

    private TransitionAnimator mTransitionAnimator = new HorizontalTransitionAnimator();

    public IndicatorView(Context context) {
      this(context, null, 0);
    }

    public IndicatorView(Context context, AttributeSet attrs) {
      this(context, attrs, 0);
    }

    public IndicatorView(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);

      final float density = getResources().getDisplayMetrics().density;
      kDefaultSpacing = (int) (DEFAULT_SPACING * density + 0.5f);

      TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Indicator);

      mSpacing = ta.getDimensionPixelOffset(R.styleable.Indicator_spacing, INVALID_DIMENSION);
      if (mSpacing == INVALID_DIMENSION) {
        mSpacing = kDefaultSpacing;
      }

      mCount = ta.getInteger(R.styleable.Indicator_count, 0);

      mDrawable = ta.getDrawable(R.styleable.Indicator_drawable);
      mSelector = ta.getDrawable(R.styleable.Indicator_selector);

      mAutoHide = ta.getBoolean(R.styleable.Indicator_autoHide, false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      mTransitionAnimator.measure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      mTransitionAnimator.draw(canvas);
    }

    @Override
    public boolean isAutoHide() {
      return mAutoHide;
    }

    @Override
    public void setAutoHide(boolean autoHide) {
      mAutoHide = autoHide;
    }

    @Override
    public void setDrawable(Drawable drawable) {
      mDrawable = drawable;
      requestLayout();
      invalidate();
    }

    @Override
    public Drawable getDrawable() {
      return mDrawable;
    }

    @Override
    public void setSelector(Drawable drawable) {
      mSelector = drawable;
      requestLayout();
      invalidate();
    }

    @Override
    public Drawable getSelector() {
      return mSelector;
    }

    @Override
    public void setSpacing(int spacing) {
      mSpacing = spacing;
      requestLayout();
      invalidate();
    }

    @Override
    public int getSpacing() {
      return mSpacing;
    }

    @Override
    public int getCount() {
      return mCount;
    }

    @Override
    public void setCount(int count) {
      mCount = count;
      requestLayout();
      invalidate();
    }

    @Override
    public void setPosition(float position) {
      mPosition = position;
      invalidate();
    }

    @Override
    public float getPosition() {
      return mPosition;
    }

    @Override
    public void animateTo(int position) {
    }

    @Override
    public void animateNext() {
    }

    @Override
    public void animatePre() {
    }

    private class HorizontalTransitionAnimator implements TransitionAnimator {
      private final int kVelocity;

      private long lastAnimationTime;
      private long currentAnimatingTime;
      private int animatingVelocity;
      private float animatingPosition;
      private float animatingDistance;
      private boolean animating;
      private AnimationHandler handler = new AnimationHandler();

      HorizontalTransitionAnimator() {
        final float density = getResources().getDisplayMetrics().density;
        kVelocity = (int) (density * 1 + 0.5f);
      }

      @Override
      public void measure(int widthMeasureSpec, int heightMeasureSpec) {
        if ((mDrawable == null || mDrawable.getBounds().isEmpty())
            || (mSelector == null || mSelector.getBounds().isEmpty())
            || mCount == 0) {
          setWillNotDraw(true);
          setMeasuredDimension(0, 0);
          return;
        }

        setWillNotDraw(false);
        int measuredWidth = measureWidth(widthMeasureSpec);
        int measuredHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);

        // Maybe these codes should move to onLayout method.

        final int widthSize = widthMeasureSpec & ~(0x3 << 30);
        final int startX = (widthSize - measuredWidth) >> 1;
        final int heightSize = heightMeasureSpec & ~(0x3 << 30);
        final int startY = (heightSize - measuredHeight) >> 1;

        mSelector.getBounds().offset(startX, startY);

        final int drawableWidth = mDrawable.getIntrinsicWidth();
        final int selectorWidth = mSelector.getIntrinsicWidth();

        if (drawableWidth - selectorWidth > 0) {
          mSelector.getBounds().offset((drawableWidth - selectorWidth) >> 1, 0);
        } else {
          mDrawable.getBounds().offset((selectorWidth - drawableWidth) >> 1, 0);
        }
      }

      private int measureWidth(int widthMeasureSpec) {
        final int mode = widthMeasureSpec & (0x3 << 30);
        final int size = widthMeasureSpec & ~(0x3 << 30);

        final int suppose = mSpacing * (mCount - 1)
            + Math.max(mDrawable.getIntrinsicWidth(), mSelector.getIntrinsicHeight());
        switch (mode) {
          case MeasureSpec.AT_MOST:
            return Math.min(size, suppose);
          case MeasureSpec.EXACTLY:
            return size;
          case MeasureSpec.UNSPECIFIED:
            return suppose;
        }
        return size;
      }

      private int measureHeight(int heightMeasureSpec) {
        final int mode = heightMeasureSpec & (0x3 << 30);
        final int size = heightMeasureSpec & ~(0x3 << 30);

        final int suppose = Math.max(mDrawable.getIntrinsicHeight(), mSelector.getIntrinsicHeight());
        switch (mode) {
          case MeasureSpec.AT_MOST:
            return Math.min(size, suppose);
          case MeasureSpec.EXACTLY:
            return size;
          case MeasureSpec.UNSPECIFIED:
            return suppose;
        }
        return size;
      }

      @Override
      public void layout(boolean changed, int l, int t, int r, int b) {
      }

      @Override
      public void draw(Canvas canvas) {
        for (int i = 0; i < mCount; i++) {
          mDrawable.getBounds().offset(mSpacing * i, 0);
          mDrawable.draw(canvas);
        }

        mSelector.getBounds().offset((int) (mSpacing * mPosition), 0);
        mSelector.draw(canvas);
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
        if (mTargetPosition > mPosition) {
          animatingVelocity = kVelocity;
        } else if (mTargetPosition < mPosition) {
          animatingVelocity = -kVelocity;
        } else {
          return;
        }
        animatingDistance =  mTargetPosition - mPosition;
        animatingPosition = mPosition;

        lastAnimationTime = SystemClock.uptimeMillis();
        currentAnimatingTime = lastAnimationTime + Facade.ANIMATION_FRAME_DURATION;
        handler.removeMessages(AnimationHandler.MSG_ANIMATE);
        handler.sendEmptyMessageAtTime(AnimationHandler.MSG_ANIMATE, currentAnimatingTime);
      }

      private void compute() {
        final long now = SystemClock.uptimeMillis();
        final float t = (now - lastAnimationTime) / Facade.ONE_SECOND_FLOAT;

        animatingPosition += animatingVelocity * t;

        lastAnimationTime = now;
        currentAnimatingTime = lastAnimationTime + Facade.ANIMATION_FRAME_DURATION;

        if (animatingVelocity < 0) {
          if (animatingPosition < mTargetPosition) {
            mPosition = mTargetPosition;
            animating = false;
          } else {
            mPosition = animatingPosition;

            handler.removeMessages(AnimationHandler.MSG_ANIMATE);
            handler.sendEmptyMessageAtTime(AnimationHandler.MSG_ANIMATE, currentAnimatingTime);
          }
        } else {
          if (animatingPosition > mTargetPosition) {
            mPosition = mTargetPosition;
            animating = false;
          } else {
            mPosition = animatingPosition;

            handler.removeMessages(AnimationHandler.MSG_ANIMATE);
            handler.sendEmptyMessageAtTime(AnimationHandler.MSG_ANIMATE, currentAnimatingTime);
          }
        }

        invalidate();
      }

      private class AnimationHandler extends Handler {
        private static final int MSG_ANIMATE = 1000;

        @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
            case MSG_ANIMATE:
              compute();
              break;
          }
        }
      }
    }
  }
}
