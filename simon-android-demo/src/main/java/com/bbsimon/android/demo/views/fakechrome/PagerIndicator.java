package com.bbsimon.android.demo.views.fakechrome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.util.AttributeSet;
import android.view.View;
import com.common.R;

class PagerIndicator extends View {
    private static final String TAG = "PagerIndicator";

    private static ShapeDrawable DEFAULT_INDICATOR;
    private static ShapeDrawable DEFAULT_SELECTOR;
    private final static int DEFAULT_WIDTH  = 10;
    private final static int DEFAULT_HEIGHT = 10;

    private static void buildDefault() {
        DEFAULT_INDICATOR = new ShapeDrawable(new ArcShape(0, 360));
        DEFAULT_SELECTOR = new ShapeDrawable(new ArcShape(0, 360));
    }

    private final int mIndicatorWidth;
    private final int mIndicatorHeight;
    private final int mSelectorWidth;
    private final int mSelectorHeight;

    private int      mCount;
    private float    mPosition;
    private float    mSpace;
    private Drawable mDrawable;
    private Drawable mSelector;


    private Rect mFrame;

    public PagerIndicator(Context context) {
        this(context, null, 0);
    }

    public PagerIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotCacheDrawing(true);

        mFrame = new Rect();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FakeChromeIndicator);

        mDrawable = ta.getDrawable(R.styleable.FakeChromeIndicator_drawable);
        mSelector = ta.getDrawable(R.styleable.FakeChromeIndicator_selector);
        mSpace = ta.getDimension(R.styleable.FakeChromeIndicator_space, 0);

        ta.recycle();

        if (mDrawable == null || mSelector == null) {
            buildDefault();
            mDrawable = DEFAULT_INDICATOR;
            mSelector = DEFAULT_SELECTOR;

            final float density = getResources().getDisplayMetrics().density;
            mIndicatorWidth = (int) (DEFAULT_WIDTH * density + 0.5f);
            mIndicatorHeight = (int) (DEFAULT_HEIGHT * density + 0.5f);

            mSelectorWidth = (int) (DEFAULT_WIDTH * density + 0.5f);
            mSelectorHeight = (int) (DEFAULT_HEIGHT * density + 0.5f);
        } else {
            mIndicatorWidth = mDrawable.getMinimumWidth();
            mIndicatorHeight = mDrawable.getMinimumHeight();

            mSelectorWidth = mSelector.getMinimumWidth();
            mSelectorHeight = mSelector.getMinimumHeight();

        }
    }

    public void setSelectorPosition(float position) {
        if (position < 0 || ((int) position) > mCount - 1) {
            return;
        }
        mPosition = position;
        invalidate();
    }

    public int getCount() {
        return mCount;
    }

    public void setCount(int count) {
        mCount = count;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCount == 0) {
            setMeasuredDimension(0, 0);
        }
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int widthMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int width = 0;

        if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(widthSize, (int) (mCount * mIndicatorWidth + (mCount - 1) * mSpace));
        } else if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        }
        //Log.d(TAG, "@measureWidth " + width);
        return width;
    }

    private int measureHeight(int heightMeasureSpec) {
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int height = 0;

        if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.max(mIndicatorHeight, mSelector.getMinimumHeight());
            height = Math.min(heightSize, height);
        } else if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        }
        //Log.d(TAG, "@measureHeight " + height);
        return height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCount == 0) {
            return;
        }
        mFrame.set(0, 0, mIndicatorWidth, mIndicatorHeight);
        for (int i = 0; i < mCount; i++) {
            mDrawable.setBounds(mFrame);
            mDrawable.draw(canvas);
            mFrame.offset((int) (mSpace + mIndicatorWidth + 0.5f), 0);
        }

        mFrame.set(0, 0, mSelectorWidth, mSelectorHeight);

        mFrame.offset((int) (mPosition * (mIndicatorWidth + mSpace) + 0.5f), 0);
        mSelector.setBounds(mFrame);
        mSelector.draw(canvas);
    }
}
