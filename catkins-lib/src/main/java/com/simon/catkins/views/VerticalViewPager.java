package com.simon.catkins.views;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;

/**
 * @author bb.simon.yu@gmail.com
 */
public class VerticalViewPager extends AbsListView {

  private int mCur = 0;

  private View mCView; // Current page view
  private View mPView; // Previous page view
  private View mNView; // Next page view

  private boolean mHasCur;
  private boolean mHasPre;
  private boolean mHasNext;

  private ListAdapter mAdapter;

  private boolean mInLayout;

  private DataSetObserver mDataSetObserver = new DataSetObserver() {
    @Override
    public void onChanged() {
      populate();
    }
  };

  private class RecycleBin {

  }

  /**
   * Width of last layout
   */
  private int mLayoutWidth;

  /**
   * Height of last layout
   */
  private int mLayoutHeight;

  private View mConvertedView;

  private int mItemCount = 0;

  public VerticalViewPager(Context context) {
    this(context, null, 0);
  }

  public VerticalViewPager(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public VerticalViewPager(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public ListAdapter getAdapter() {
    return mAdapter;
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    mLayoutWidth = getWidth();
    mLayoutHeight = getHeight();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (mItemCount == 0) {
      setMeasuredDimension(0, 0);
    }
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
  public void setAdapter(ListAdapter adapter) {
    if (adapter == null) {
      return;
    }

    // Clear previous adapter
    if (mAdapter != null) {
      mAdapter.unregisterDataSetObserver(mDataSetObserver);
      mConvertedView = null;
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

    mCView = mAdapter.getView(mCur, mConvertedView, this);
    mHasCur = mCView != null;

    if (mCur <= 0) {
      mHasPre = false;
    } else {
      mPView  = mAdapter.getView(mCur - 1, mConvertedView, this);
      mHasPre = mPView != null;
    }

    if (mCur == mItemCount - 1) {
      mHasNext = false;
    } else {
      mNView = mAdapter.getView(mCur + 1, mConvertedView, this);
      mHasNext = mPView != null;
    }
  }
}
