package com.simon.catkins.views;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import de.akquinet.android.androlog.Log;

/**
 * @author Simon
 */
public class PinnedHeaderListView extends ListView implements AbsListView.OnScrollListener {
    private static final String TAG = "PinnedHeaderListView";

    private int mPinnedHeaderItemType = -1;
    private OnScrollListener mInnerOnScrollListener;

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
    }

    @Override
    public PinnedHeaderListAdapter getAdapter() {
        return (PinnedHeaderListAdapter) super.getAdapter();
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        Log.d(TAG, "@setOnScrollListener");
        mInnerOnScrollListener = l;
    }

    private final int[] mHeaderLocation = new int[2];

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mPinnedHeaderView == null) {
            return;
        }
        final long drawingTime = getDrawingTime();
        final int savedCount = canvas.save();
        mPinnedHeaderView.getLocationOnScreen(mHeaderLocation);
        canvas.translate(-mHeaderLocation[0], 0);
        drawChild(canvas, mPinnedHeaderView, drawingTime);
        canvas.restoreToCount(savedCount);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    private int mLastFirstVisibleItem;
    private View mPinnedHeaderView;

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem > mLastFirstVisibleItem) {
            mLastFirstVisibleItem = firstVisibleItem;
            if (getAdapter().getItemViewType(firstVisibleItem) == mPinnedHeaderItemType) {
                mPinnedHeaderView = getChildAt(0);
            }
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

        public boolean isEmpty() {
            return getCount() == 0;
        }
    }
}
