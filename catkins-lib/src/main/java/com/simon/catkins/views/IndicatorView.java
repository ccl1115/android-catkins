package com.simon.catkins.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.simon.catkins.R;

/**
 */
public class IndicatorView extends View {
    private static final String TAG = "IndicatorView$IndicatorView";

    private static final int DEFAULT_SPACING = 50;
    private static final int INVALID_DIMENSION = -1;

    private boolean mAutoHide;

    private int mSpacing;
    private int mCount;

    private float mPosition;
    private float mTargetPosition;

    private Drawable mDrawable;
    private Drawable mSelector;

    private final ViewInjector mViewInjector = new HorizontalViewInjector();

    public IndicatorView(Context context) {
        this(context, null, 0);
    }

    public IndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final float density = getResources().getDisplayMetrics().density;
        int defaultSpacing = (int) (DEFAULT_SPACING * density + 0.5f);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.IndicatorView);

        mSpacing = ta.getDimensionPixelSize(R.styleable.IndicatorView_spacing, INVALID_DIMENSION);
        if (mSpacing == INVALID_DIMENSION) {
            mSpacing = defaultSpacing;
        }

        mCount = ta.getInteger(R.styleable.IndicatorView_count, 0);

        mDrawable = ta.getDrawable(R.styleable.IndicatorView_drawable);

        if (mDrawable == null) {
            Log.d(TAG, "Drawable not defined in xml");
        } else {
            mDrawable.setBounds(0, 0, mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
            Log.d(TAG, "Drawable bounds=" + mDrawable.getBounds());
        }

        mSelector = ta.getDrawable(R.styleable.IndicatorView_selector);

        if (mSelector == null) {
            Log.d(TAG, "Selector not defined in xml");
        } else {
            mSelector.setBounds(0, 0, mSelector.getIntrinsicWidth(), mSelector.getIntrinsicHeight());
            Log.d(TAG, "Selector bound=" + mSelector.getBounds());
        }

        mAutoHide = ta.getBoolean(R.styleable.IndicatorView_autoHide, false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewInjector.measure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        mViewInjector.draw(canvas);
    }

    public boolean isAutoHide() {
        return mAutoHide;
    }

    public void setAutoHide(boolean autoHide) {
        mAutoHide = autoHide;
    }

    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
        requestLayout();
        invalidate();
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public void setSelector(Drawable drawable) {
        mSelector = drawable;
        requestLayout();
        invalidate();
    }

    public Drawable getSelector() {
        return mSelector;
    }

    public void setSpacing(int spacing) {
        mSpacing = spacing;
        requestLayout();
        invalidate();
    }

    public int getSpacing() {
        return mSpacing;
    }

    public int getCount() {
        return mCount;
    }

    public void setCount(int count) {
        mCount = count;
        requestLayout();
        invalidate();
    }

    public void setPosition(float position) {
        Log.d(TAG, "@setPosition");
        mPosition = position;
        invalidate();
    }

    public float getPosition() {
        return mPosition;
    }

    public void animateTo(int position) {
    }

    public void animateNext() {
    }

    public void animatePre() {

    }

    private class HorizontalViewInjector implements ViewInjector {
        private final int kVelocity;

        private long lastAnimationTime;
        private long currentAnimatingTime;
        private int animatingVelocity;
        private float animatingPosition;
        private float animatingDistance;
        private boolean animating;
        private final AnimationHandler handler = new AnimationHandler();

        HorizontalViewInjector() {
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
                Log.d(TAG, "will not draw.");
                return;
            }

            setWillNotDraw(false);
            final int measuredWidth = measureWidth(widthMeasureSpec);
            final int measuredHeight = measureHeight(heightMeasureSpec);
            Log.d(TAG, "@onMeasure width=" + measuredWidth);
            Log.d(TAG, "@onMeasure height=" + measuredHeight);
            setMeasuredDimension(measuredWidth, measuredHeight);

            // offset the select drawable to center of the normal drawable;

            final int drawableWidth = mDrawable.getIntrinsicWidth();
            final int selectorWidth = mSelector.getIntrinsicWidth();

            if (drawableWidth - selectorWidth > 0) {
                final Rect bounds = mSelector.getBounds();
                bounds.offset((drawableWidth - selectorWidth) >> 1, 0);
                mSelector.setBounds(bounds);
            } else {
                final Rect bounds = mDrawable.getBounds();
                bounds.offset((selectorWidth - drawableWidth) >> 1, 0);
                mDrawable.setBounds(bounds);
            }

            final int drawableHeight = mDrawable.getIntrinsicHeight();
            final int selectorHeight = mSelector.getIntrinsicHeight();

            if (drawableHeight > selectorHeight) {
                final Rect bounds = mSelector.getBounds();
                bounds.offset(0, (drawableHeight - selectorHeight) >> 1);
                mSelector.setBounds(bounds);
            } else {
                final Rect bounds = mDrawable.getBounds();
                bounds.offset(0, (selectorHeight - drawableHeight) >> 1);
                mSelector.setBounds(bounds);
            }
        }

        private int measureWidth(int widthMeasureSpec) {
            final int mode = widthMeasureSpec & (0x3 << 30);
            final int size = widthMeasureSpec & ~(0x3 << 30);

            Log.d(TAG, "@measureWidth size=" + size);
            final int suppose = (mSpacing * (mCount - 1)) +
                    (Math.max(mDrawable.getIntrinsicWidth(), mSelector.getIntrinsicWidth()) * mCount);
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
        public void draw(Canvas canvas) {
            final int savedCount = canvas.save();
            for (int i = 0; i < mCount; i++) {
                mDrawable.draw(canvas);
                canvas.translate(mDrawable.getIntrinsicWidth() + mSpacing, 0);
            }
            canvas.restoreToCount(savedCount);

            final int savedCount2 = canvas.save();
            canvas.translate((mSelector.getIntrinsicWidth() + mSpacing) * mPosition, 0);
            mSelector.draw(canvas);
            canvas.restoreToCount(savedCount2);
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
            animatingDistance = mTargetPosition - mPosition;
            animatingPosition = mPosition;

            lastAnimationTime = SystemClock.uptimeMillis();
            currentAnimatingTime = lastAnimationTime + ViewConfig.ANIMATION_FRAME_DURATION;
            handler.removeMessages(AnimationHandler.MSG_ANIMATE);
            handler.sendEmptyMessageAtTime(AnimationHandler.MSG_ANIMATE, currentAnimatingTime);
        }

        @Override
        public boolean isAnimating() {
            return animating;
        }

        private void compute() {
            final long now = SystemClock.uptimeMillis();
            final float t = (now - lastAnimationTime) / ViewConfig.ONE_SECOND_FLOAT;

            animatingPosition += animatingVelocity * t;

            lastAnimationTime = now;
            currentAnimatingTime = lastAnimationTime + ViewConfig.ANIMATION_FRAME_DURATION;

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
