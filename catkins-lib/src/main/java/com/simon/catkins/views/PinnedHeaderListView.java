package com.simon.catkins.views;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * @author Simon
 */
public class PinnedHeaderListView extends ListView implements AbsListView.OnScrollListener {
    private static final String TAG = "PinnedHeaderListView";

    private OnScrollListener mInnerOnScrollListener;

    private View mPinnedHeaderView;
    private int mPinnedHeaderItemType = -1;
    private int mPinnedHeaderWidth;
    private int mPinnedHeaderHeight;
    private int mPinnedHeaderOffsetY;

    private int mCurrentPinnedPosition;
    private int mLastPinnedPosition = mCurrentPinnedPosition;

    private boolean mWillDrawPinnedHeader;


    public PinnedHeaderListView(Context context) {
        this(context, null, 0);
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnScrollListener(this);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof PinnedHeaderListAdapter)) {
            throw new RuntimeException("Adapter must extended from PinnedHeaderListAdapter");
        }
        super.setAdapter(adapter);
        mPinnedHeaderItemType = getAdapter().getPinnedHeaderViewType();
        mPinnedHeaderView = getAdapter().getPinnedHeaderView();
        if (mPinnedHeaderView != null) {
            setFadingEdgeLength(0);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthSize = widthMeasureSpec & ~(0x3 << 30);
        final int heightSize = heightMeasureSpec & ~(0x3 << 30);
    }

    private void measurePinnedHeader(int widthSize, int heightSize) {
        if (mPinnedHeaderView != null) {
            ViewGroup.LayoutParams lp = mPinnedHeaderView.getLayoutParams();
            if (lp == null) lp = generateDefaultLayoutParams();
            int wSpec, hSpec;
            switch (lp.width) {
                case ViewGroup.LayoutParams.WRAP_CONTENT:
                    wSpec = widthSize + MeasureSpec.AT_MOST;
                    break;
                case ViewGroup.LayoutParams.MATCH_PARENT:
                    wSpec = widthSize + MeasureSpec.EXACTLY;
                    break;
                default:
                    wSpec = lp.width + MeasureSpec.EXACTLY;
                    break;
            }

            switch (lp.height) {
                case ViewGroup.LayoutParams.WRAP_CONTENT:
                    hSpec = heightSize + MeasureSpec.AT_MOST;
                    break;
                case ViewGroup.LayoutParams.MATCH_PARENT:
                    hSpec = heightSize + MeasureSpec.EXACTLY;
                    break;
                default:
                    hSpec = lp.height + MeasureSpec.EXACTLY;
                    break;
            }
            mPinnedHeaderView.measure(wSpec, hSpec);
            mPinnedHeaderWidth = mPinnedHeaderView.getMeasuredWidth();
            mPinnedHeaderHeight = mPinnedHeaderView.getMeasuredHeight();
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        computeHeaderView();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!mWillDrawPinnedHeader) {
            return;
        }
        final long drawingTime = getDrawingTime();
        final int savedCount = canvas.save();
        canvas.translate(0, -mPinnedHeaderOffsetY);
        drawChild(canvas, mPinnedHeaderView, drawingTime);
        canvas.restoreToCount(savedCount);
    }

    private void computeHeaderView() {
        if (mPinnedHeaderView == null) {
            return;
        }
        final PinnedHeaderListAdapter adapter = getAdapter();
        final int first = getFirstVisiblePosition();
        final int next = first + 1;
        final int firstViewType = adapter.getItemViewType(first);
        final int nextViewType = adapter.getItemViewType(next);
        if (mWillDrawPinnedHeader) {
            if (firstViewType == mPinnedHeaderItemType && first != mCurrentPinnedPosition) {
                adapter.updatePinnedHeaderView(mPinnedHeaderView, first);
                measurePinnedHeader(getMeasuredWidth(), getMeasuredHeight());
                mPinnedHeaderView.layout(0, 0, mPinnedHeaderWidth, mPinnedHeaderHeight);
                invalidate(0, 0, mPinnedHeaderWidth, mPinnedHeaderHeight);
                mLastPinnedPosition = mCurrentPinnedPosition;
                Log.d(TAG, "mLastPinnedPosition=" + mLastPinnedPosition);
                mCurrentPinnedPosition = first;
            }
            if (nextViewType == mPinnedHeaderItemType) {
                final View view = getChildAt(1);
                if (view != null) {
                    mPinnedHeaderOffsetY = mPinnedHeaderHeight - view.getTop();
                    invalidate(0, 0, mPinnedHeaderWidth, mPinnedHeaderHeight);
                }
            } else {
                mPinnedHeaderOffsetY = 0;
                invalidate(0, 0, mPinnedHeaderWidth, mPinnedHeaderHeight);
            }
        } else {
            if (firstViewType == mPinnedHeaderItemType) {
                mWillDrawPinnedHeader = true;
                mPinnedHeaderOffsetY = 0;
                adapter.updatePinnedHeaderView(mPinnedHeaderView, first);
                measurePinnedHeader(getMeasuredWidth(), getMeasuredHeight());
                mPinnedHeaderView.layout(0, 0, mPinnedHeaderWidth, mPinnedHeaderHeight);
                invalidate(0, 0, mPinnedHeaderWidth, mPinnedHeaderHeight);
                mCurrentPinnedPosition = first;
                mLastPinnedPosition = first;
            }
        }
    }

    @Override
    public PinnedHeaderListAdapter getAdapter() {
        return (PinnedHeaderListAdapter) super.getAdapter();
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mInnerOnScrollListener = l;
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mInnerOnScrollListener != null) {
            mInnerOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        computeHeaderView();
        if (mInnerOnScrollListener != null) {
            mInnerOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    public static abstract class PinnedHeaderListAdapter implements ListAdapter {
        private final DataSetObservable mDataSetObservable = new DataSetObservable();

        public boolean hasStableIds() {
            return false;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.registerObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.unregisterObserver(observer);
        }

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyChanged();
        }

        public void notifyDataSetInvalidated() {
            mDataSetObservable.notifyInvalidated();
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int position) {
            return true;
        }

        public abstract int getPinnedHeaderViewType();

        public abstract View getPinnedHeaderView();

        public abstract void updatePinnedHeaderView(View header, int position);

        public boolean isEmpty() {
            return getCount() == 0;
        }
    }
}
