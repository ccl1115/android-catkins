package com.simon.catkins.views;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;

import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * @author bb.simon.yu@gmail.com
 */
public class VerticalViewPager extends AdapterView {
    private static final String TAG = "VerticalViewPager";

    private int mCur = 0;

    private static final int CURRENT_VIEW_INDEX = 0;
    private static final int PREVIOUS_VIEW_INDEX = 1;
    private static final int NEXT_VIEW_INDEX = 2;

    private boolean mHasCur;
    private boolean mHasPre;
    private boolean mHasNext;

    private Adapter mAdapter;

    private boolean mInLayout;

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            populate();
        }
    };

    /**
     * Width of last layout
     */
    private int mLayoutWidth;

    /**
     * Height of last layout
     */
    private int mLayoutHeight;

    private int mItemCount = 0;

    private View mCView; // Current page view
    private View mPView; // Previous page view
    private View mNView; // Next page view

    private ViewGroupInjector mViewGroupInjector;

    private ArrayList<View> mScrapViews = Lists.newArrayList();

    public VerticalViewPager(Context context) {
        this(context, null, 0);
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mViewGroupInjector = new ScaleViewGroupInjector();
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        return mCView;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mViewGroupInjector.layout(changed, l, t, r, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewGroupInjector.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mViewGroupInjector.draw(canvas);
    }

    @Override
    public void setSelection(int position) {
        if (mAdapter == null || position < 0 || position > mAdapter.getCount() - 1) {
            return;
        }
        mCur = position;
        populate();
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter == null) {
            return;
        }

        // Clear previous adapter
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mItemCount = 0;
            mAdapter = null;
        }

        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mDataSetObserver);
        mItemCount = mAdapter.getCount();

        populate();
    }


    private void populate() {
        if (mCur > mItemCount - 1) {
            mCur = mItemCount - 1;
        }

        mCView = mAdapter.getView(mCur, getScrapView(mCur), this);
        mHasCur = mCView != null;

        if (mHasCur && mCView.getParent() == null) {
            attachViewToParent(mCView, 0, generateLayoutParams(mCur));
            layoutChild(mCView);
        }

        if (mCur < 1) {
            mHasPre = false;
        } else {
            mPView = mAdapter.getView(mCur - 1, getScrapView(mCur - 1), this);
            mHasPre = mPView != null;
        }

        if (mHasPre && mPView.getParent() == null) {
            attachViewToParent(mPView, 0, generateLayoutParams(mCur - 1));
            layoutChild(mPView);
        }

        if (mCur == mItemCount - 1) {
            mHasNext = false;
        } else {
            mNView = mAdapter.getView(mCur + 1, getScrapView(mCur + 1), this);
            mHasNext = mNView != null;
        }

        if (mHasNext && mNView.getParent() == null) {
            attachViewToParent(mNView, 0, generateLayoutParams(mCur + 1));
            layoutChild(mNView);
        }
    }

    private void next() {
        if (mCur == mItemCount - 1) {
            return;
        }

        mCur += 1;
        if (mHasPre) {
            addToScrapViews(mPView);
        }
        mPView = mCView;

        if (mHasNext) {
            mCView = mNView;
        }

        mNView =
                mCur == mItemCount - 1 ? null : mAdapter.getView(mCur + 1, getScrapView(mCur + 1), this);

        assert mNView != null;

        if (mNView.getParent() == null) {
            attachViewToParent(mNView, 0, generateLayoutParams(mCur + 1));
            layoutChild(mNView);
        }
    }

    private void previous() {
        if (mCur == 0) {
            return;
        }

        mCur -= 1;

        if (mHasNext) {
            addToScrapViews(mNView);
        }
        mNView = mCView;

        if (mHasPre) {
            mCView = mPView;
        }

        mPView = mCur == 0 ? null : mAdapter.getView(mCur - 1, getScrapView(mCur - 1), this);

        assert mPView != null;

        if (mPView.getParent() == null) {
            attachViewToParent(mPView, PREVIOUS_VIEW_INDEX, generateLayoutParams(mCur - 1));
            layoutChild(mPView);
        }
    }

    private void layoutChild(View view) {
        if (view.isLayoutRequested()) {
            view.measure(mLayoutWidth + MeasureSpec.EXACTLY, mLayoutHeight + MeasureSpec.EXACTLY);
            final int childW = view.getMeasuredWidth();
            final int childH = view.getMeasuredHeight();
            view.layout((mLayoutWidth - childW) >> 1, (mLayoutHeight - childH) >> 1,
                    (mLayoutWidth + childW) >> 1, (mLayoutHeight + childH) >> 1);
            Log.d(TAG, "@layoutChild width " + childW);
            Log.d(TAG, "@layoutChild height " + childH);
        }
    }

    private View getScrapView(int position) {
        return retrieveFromScrap(mScrapViews, position);
    }

    private void addToScrapViews(View view) {
        mScrapViews.add(view);

    }

    static View retrieveFromScrap(ArrayList<View> scrapViews, int position) {
        int size = scrapViews.size();
        if (size > 0) {
            // See if we still have a view for this position.
            for (int i = 0; i < size; i++) {
                View view = scrapViews.get(i);
                if (((VerticalViewPager.LayoutParams) view.getLayoutParams())
                        .scrappedFromPosition == position) {
                    scrapViews.remove(i);
                    return view;
                }
            }
            return scrapViews.remove(size - 1);
        } else {
            return null;
        }
    }

    private static class LayoutParams extends ViewGroup.LayoutParams {

        int scrappedFromPosition;

        LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        LayoutParams(int width, int height) {
            super(width, height);
        }

        LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        LayoutParams(int width, int height, int scrappedFromPosition) {
            super(width, height);
            this.scrappedFromPosition = scrappedFromPosition;
        }
    }

    public LayoutParams generateLayoutParams(int scrappedFromPosition) {
        return new LayoutParams(mLayoutWidth, mLayoutHeight, scrappedFromPosition);
    }

    private class ScaleViewGroupInjector implements ViewGroupInjector {

        public static final int MSG_ANIMATE_NEXT = 1000;
        public static final int MSG_ANIMATE_BACK = 1001;
        public static final int MSG_ANIMATE_PREVIOUS = 1002;

        private int mLastDownX;
        private int mLastDownY;
        private int mLastMoveX;
        private int mLastMoveY;

        private final int mMoveThreshold;
        private int mAnimationVelocity;
        private int mAnimatingPosition;
        private long mLastAnimationTime;
        private long mCurrentAnimatingTime;

        private int mTopTranslate;

        private boolean mAnimating;

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ANIMATE_BACK:
                        break;
                    case MSG_ANIMATE_NEXT:
                        break;
                    case MSG_ANIMATE_PREVIOUS:
                        break;
                }
            }
        };


        private ScaleViewGroupInjector() {
            final float density = getResources().getDisplayMetrics().density;

            mMoveThreshold = (int) (density * ViewConfig.TOUCH_EVENT_MOVE_SLOP_SMALL + 0.5);
        }

        @Override
        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            VerticalViewPager.super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            if (mItemCount == 0) {
                setMeasuredDimension(0, 0);
            } else {
                setMeasuredDimension(widthMeasureSpec & ~(0x3 << 30), heightMeasureSpec & ~(0x3 << 30));
            }
        }

        @Override
        public void layout(boolean changed, int l, int t, int r, int b) {
            mInLayout = true;

            mLayoutWidth = getWidth();
            mLayoutHeight = getHeight();
            Log.d(TAG, "@onLayout height " + mLayoutHeight);
            Log.d(TAG, "@onLayout width " + mLayoutWidth);

            populate();
            mInLayout = false;
        }

        @Override
        public void draw(Canvas canvas) {
            final long drawingTime = getDrawingTime();

            if (mHasPre) {
                drawChild(canvas, mPView, drawingTime);
            }

            if (mHasCur) {
                drawChild(canvas, mCView, drawingTime);
            }

            if (mHasNext) {
                drawChild(canvas, mNView, drawingTime);
            }

        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            VerticalViewPager.super.dispatchTouchEvent(event);
            return true;
        }

        @Override
        public boolean interceptionTouchEvent(MotionEvent event) {
            final int action = MotionEventCompat.getActionMasked(event);
            final int x = (int) MotionEventCompat.getX(event, 0);
            final int y = (int) MotionEventCompat.getY(event, 0);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
            }
            return false;
        }

        @Override
        public boolean touchEvent(MotionEvent event) {
            return false;
        }

        @Override
        public void animate(int msg) {
            switch (msg) {
                case MSG_ANIMATE_BACK:
                    break;
                case MSG_ANIMATE_PREVIOUS:
                    break;
                case MSG_ANIMATE_NEXT:
                    break;
            }
        }

        @Override
        public boolean isAnimating() {
            return mAnimating;
        }

        private void animateBack() {
            final long now = SystemClock.uptimeMillis();
            mLastAnimationTime = now;
            mCurrentAnimatingTime = now + ViewConfig.ANIMATION_FRAME_DURATION;

            mAnimationVelocity =
                    ViewConfig.computeVelocityByPx(VerticalViewPager.this.getContext(), mTopTranslate);
            mAnimatingPosition = mTopTranslate;

        }

        private void animateNext() {

        }

        private void animatePre() {

        }
    }
}
