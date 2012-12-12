package com.bbsimon.android.demo.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.views.refresh.IRefreshIndicator;
import com.bbsimon.android.demo.views.refresh.IRefreshable;
import com.bbsimon.android.demo.views.refresh.OnRefreshListener;
import de.akquinet.android.androlog.Log;

import java.util.ArrayList;
import java.util.List;

import static android.widget.AbsListView.OnScrollListener;

/**
 */
@SuppressWarnings("unused")
public class PagesCoverWrappedListView extends ViewGroup
    implements OnScrollListener, IRefreshable {
  private static final String TAG = "CoverWrappedListView";

  private static final int MSG_ANIMATE_PULL_BACK = 1000;
  private static final int MOVE_THRESHOLD = 20;
  private static final int OFF_SCREEN_PAGE_LIMIT = 3;

  private static final int REFRESHER_HEADER_HEIGHT = 250;
  private static final int REFRESHER_COVER_HEIGHT = 350;
  private static final int REFRESHER_THRESHOLD = 250;

  private final List<OnScrollListener> mOnScrollListeners =
      new ArrayList<OnScrollListener>();
  private final int[] mTempLocation = new int[2];
  private final int kHeaderHeight;
  private final int kCoverHeight;
  private final int kRefreshThreshold;
  private final int kMaxYOffset;
  private final int kMoveThreshold;

  private final AnimateHandler mHandler;
  private final Animator mAnimator;

  private ImageView mCoverImage;
  private ImageView mShadow;
  private Header mHeader;
  private ViewPager mTop;
  private ListView mListView;

  private int mLastDownY;
  private int mYOffset;
  private int mAbsY;
  private int mLastMoveY;

  private boolean mEnable = true;
  private boolean mDispatchedToHeader;

  private final Rect mHeaderFrame = new Rect();

  private OnRefreshListener mOnRefreshListener;

  private Drawable mScrollBar;

  private RefreshAsyncTask mRefreshAsyncTask;

  public PagesCoverWrappedListView(final Context context) {
    this(context, null, 0);
  }

  public PagesCoverWrappedListView(final Context context,
                                   final AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PagesCoverWrappedListView(final Context context,
                                   final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
    Log.init(context);
    mHandler = new AnimateHandler();
    mAnimator = new Animator();
    Resources res = getResources();
    final float density = res.getDisplayMetrics().density;
    kHeaderHeight = (int) (0.5f + REFRESHER_HEADER_HEIGHT * density);
    kCoverHeight = (int) (0.5f + REFRESHER_COVER_HEIGHT * density);
    kRefreshThreshold = (int) (0.5f + REFRESHER_THRESHOLD * density);
    kMoveThreshold = (int) (0.5f + density * MOVE_THRESHOLD);
    kMaxYOffset = kCoverHeight - kHeaderHeight;


    mListView = new ListView(context);
    addView(mListView, new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT));

    mListView.setOnScrollListener(this);
    mListView.setCacheColorHint(0x00FFFFFF);

    mHeader = new Header(context);
    mHeader.setLayoutParams(
        new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
            kHeaderHeight));

    mListView.addHeaderView(mHeader);

    final OnScrollListener innerScrollListener = new OnScrollListener() {
      @Override
      public void onScrollStateChanged(final AbsListView view,
                                       final int scrollState) {
      }

      @Override
      public void onScroll(final AbsListView view, final int firstVisibleItem,
                           final int visibleItemCount,
                           final int totalItemCount) {
        View childAt = getChildAt(0);
        if (childAt != null) {
          childAt.getLocationOnScreen(mTempLocation);
        }
      }
    };
    addOnScrollListener(innerScrollListener);

    mScrollBar = new ShapeDrawable(new RectShape());

    mListView.setClickable(true);
  }

  public void addOnScrollListener(final OnScrollListener listener) {
    mOnScrollListeners.add(listener);
  }

  //  --------------------- Interface OnScrollListener ---------------------
  @Override
  public final void onScrollStateChanged(final AbsListView view,
                                         final int scrollState) {
    for (final OnScrollListener listener : mOnScrollListeners) {
      listener.onScrollStateChanged(view, scrollState);
    }
  }

  @Override
  public final void onScroll(final AbsListView view,
                             final int firstVisibleItem,
                             final int visibleItemCount,
                             final int totalItemCount) {
    for (final OnScrollListener listener : mOnScrollListeners) {
      listener.onScroll(view, firstVisibleItem,
          visibleItemCount, totalItemCount);
    }
  }

  public void clearOnScrollListener() {
    mOnScrollListeners.clear();
  }

  @Override
  protected final void dispatchDraw(final Canvas canvas) {
    final long drawingTime = getDrawingTime();

    canvas.save();
    canvas.translate(0, mYOffset >> 1);
    drawChild(canvas, mListView, drawingTime);
    canvas.restore();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (mAnimator.animating) {
      return true;
    }

    final int x = (int) ev.getX();
    final int y = (int) ev.getY();
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mLastDownY = y;
        mLastMoveY = y;

        // need offset to dispatch to exact region
        mHeader.getLocationOnScreen(mTempLocation);
        mHeader.getHitRect(mHeaderFrame);
        mHeaderFrame.offset(0, mTempLocation[1]);


        if (mHeaderFrame.contains(x, y)) {
          mDispatchedToHeader = true;
          ev.offsetLocation(0, kCoverHeight - kHeaderHeight);
          return mHeader.dispatchTouchEvent(ev);
        } else {
          mDispatchedToHeader = false;
          return super.dispatchTouchEvent(ev);
        }
      case MotionEvent.ACTION_MOVE:
        if (mDispatchedToHeader) {
          return mHeader.dispatchTouchEvent(ev);
        } else if (mEnable) {
          mHeader.getLocationOnScreen(mTempLocation);
          int threshold = mAbsY + kHeaderHeight - kCoverHeight + 1;

          if ((mTempLocation[1] == threshold) && (y > mLastDownY)) {
            mYOffset += y - mLastMoveY;
            mYOffset = Math.max(0, Math.min(mYOffset, 2 * kMaxYOffset));
            mLastMoveY = y;
            Log.d(TAG, "@onInterceptTouchEvent");
            invalidate();
            return true;
          } else {
            mYOffset = 0;
            return super.dispatchTouchEvent(ev);
          }
        }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (mDispatchedToHeader) {
          return mHeader.dispatchTouchEvent(ev);
        } else {
          if ((mHeader.getTop() == getTop()) && (y > mLastDownY)) {
            if (y - mLastDownY >= kRefreshThreshold) {
              if ((mRefreshAsyncTask == null)
                  || (mRefreshAsyncTask.getStatus()
                  != AsyncTask.Status.RUNNING)) {
                mRefreshAsyncTask = new RefreshAsyncTask();
                mRefreshAsyncTask.execute((Void[]) null);
              }
            }
          }

          mAnimator.animatePullBack();
        }
      default:
        if (mDispatchedToHeader) {
          return mHeader.dispatchTouchEvent(ev);
        } else {
          return super.dispatchTouchEvent(ev);
        }
    }
  }

  //@Override
  //public final boolean onInterceptTouchEvent(final MotionEvent event) {
  //  Logger.log(TAG, Log.DEBUG, "@onInterceptTouchEvent");
  //
  //  if (!mEnable) {
  //    return false;
  //  }
  //
  //  final int action = event.getAction() & MotionEvent.ACTION_MASK;
  //  final int y = (int) event.getY();
  //
  //  switch (action) {
  //    case MotionEvent.ACTION_DOWN:
  //      mLastDownY = y;
  //      mLastMoveY = y;
  //      break;
  //
  //    case MotionEvent.ACTION_MOVE:
  //
  //      mHeader.getLocationOnScreen(mTempLocation);
  //      int threshold = mAbsY + kHeaderHeight - kCoverHeight + 1;
  //
  //      if ((mTempLocation[1] == threshold) && (y > mLastDownY)) {
  //        mYOffset += y - mLastMoveY;
  //        mYOffset = Math.min(mYOffset, 2 * kMaxYOffset);
  //        mLastMoveY = y;
  //        Logger.log(TAG, Log.DEBUG, "@onInterceptTouchEvent");
  //        return true;
  //      } else {
  //        mYOffset = 0;
  //        return false;
  //      }
  //    default:
  //      break;
  //  }
  //
  //  invalidate();
  //  return false;
  //}

  //@Override
  //public boolean onTouchEvent(MotionEvent event) {
  //  final int y = (int) event.getY();
  //  final int action = event.getAction() & MotionEvent.ACTION_MASK;
  //
  //  switch (action) {
  //    case MotionEvent.ACTION_MOVE:
  //      Logger.log(TAG, Log.DEBUG, "@onTouchEvent");
  //      if (y > mLastDownY) {
  //        mYOffset += y - mLastMoveY;
  //        mYOffset = Math.max(0, Math.min(mYOffset, kMaxYOffset << 1));
  //        mLastMoveY = y;
  //      } else {
  //        mYOffset = 0;
  //      }
  //      break;
  //    case MotionEvent.ACTION_CANCEL:
  //    case MotionEvent.ACTION_UP:
  //      if ((mHeader.getTop() == getTop()) && (y > mLastDownY)) {
  //        if (y - mLastDownY >= kRefreshThreshold) {
  //          if ((mRefreshAsyncTask == null)
  //              || (mRefreshAsyncTask.getStatus()
  //              != AsyncTask.Status.RUNNING)) {
  //            mRefreshAsyncTask = new RefreshAsyncTask();
  //            mRefreshAsyncTask.execute((Void[]) null);
  //          }
  //        }
  //      }
  //
  //      mAnimator.animatePullBack();
  //
  //      break;
  //  }
  //
  //  invalidate();
  //  return true;
  //}

  @Override
  protected final void onMeasure(final int widthMeasureSpec,
                                 final int heightMeasureSpec) {
    final int heightSize = heightMeasureSpec & ~(0x3 << 30);

    measureChild(mListView, widthMeasureSpec,
        (heightSize - kHeaderHeight + kCoverHeight) + MeasureSpec.EXACTLY);
    setMeasuredDimension(widthMeasureSpec & ~(0x3 << 30),
        heightSize);
  }

  @Override
  protected final void onLayout(final boolean changed, final int l,
                                final int t, final int r, final int b) {
    final int w = r - l;
    final int h = b - t;

    mListView.layout(0, kHeaderHeight - kCoverHeight, w, h);

    getLocationOnScreen(mTempLocation);
    mAbsY = mTempLocation[1];

    mScrollBar.setBounds(r - 30, 0, r - 15, 100);
  }

  /**
   * @return this view instance
   */
  public final ListView getListView() {
    return mListView;
  }

  public final void removeOnScrollListener(final OnScrollListener listener) {
    mOnScrollListeners.remove(listener);
  }

  public final void setCoverImageBitmap(final Bitmap bitmap) {
    mCoverImage.setImageBitmap(bitmap);
  }

  public final void setCoverImageDrawable(final Drawable drawable) {
    mCoverImage.setImageDrawable(drawable);
  }

  public final void setCoverImageResource(final int resource) {
    mCoverImage.setImageResource(resource);
  }

  @Override
  public void setOnRefreshListener(OnRefreshListener listener) {
  }

  @Override
  public void setEnable(boolean enable) {
    mEnable = enable;
  }

  @Override
  public boolean getEnable() {
    return mEnable;
  }

  @Override
  public void refresh() {
    if (mRefreshAsyncTask == null ||
        mRefreshAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
      mRefreshAsyncTask = new RefreshAsyncTask();
      mRefreshAsyncTask.execute((Void[]) null);
    }
  }

  @Override
  public void showEmptyView() {
    //TODO No empty view yet.
  }

  @Override
  public void hideEmptyView() {
    //TODO No empty view yet.
  }

  @Override
  public void setRefreshIndicator(IRefreshIndicator indicator) {
  }

  public void setTopPagerAdapter(PagerAdapter adapter) {
    mTop.setAdapter(adapter);
  }

  private class AnimateHandler extends Handler {
    @Override
    public void handleMessage(final Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_PULL_BACK:
          mAnimator.computePullBack();
          break;
        default:
          break;
      }
    }
  }

  private class Animator {
    private boolean animating;
    private long lastAnimationTime;
    private long currentAnimatingTime;
    private int animatingVelocity;
    private int animatingPosition;
    private int animationDistance;

    void computePullBack() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / Facade.ONE_SECOND;

      animatingPosition += animatingVelocity * t;

      if (animatingPosition >= animationDistance) {
        mYOffset = 0;
        animating = false;
      } else {
        mYOffset = (int) (animationDistance
            * (1 - Facade.sInterpolator.getInterpolation(animatingPosition
            / (float) animationDistance)));
        lastAnimationTime = now;
        currentAnimatingTime = now + Facade.FRAME_ANIMATION_DURATION;
        mHandler.removeMessages(MSG_ANIMATE_PULL_BACK);
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_PULL_BACK,
            currentAnimatingTime);
      }

      invalidate();
      mHeader.invalidate();
    }

    void animatePullBack() {
      final long now = SystemClock.uptimeMillis();

      lastAnimationTime = now;
      currentAnimatingTime = now + Facade.FRAME_ANIMATION_DURATION;
      animationDistance = mYOffset;
      animating = true;
      animatingPosition = 0;
      animatingVelocity = mYOffset << 1;
      mHandler.removeMessages(MSG_ANIMATE_PULL_BACK);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_PULL_BACK,
          currentAnimatingTime);
    }

  }


  private class Header extends ViewGroup {
    public Header(final Context context) {
      super(context);
      mCoverImage = new ImageView(context);
      mCoverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
      mCoverImage.setImageResource(R.drawable.icon);
      addView(mCoverImage, new LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT));
      mShadow = new ImageView(context);
      mShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
      //TODO set shadow drawable.
      addView(mShadow, new LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT));

      mTop = new ViewPager(context);
      mTop.setOffscreenPageLimit(OFF_SCREEN_PAGE_LIMIT);
      addView(mTop, new LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT));
      mTop.setClickable(true);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec,
                             final int heightMeasureSpec) {
      final int widthSize = widthMeasureSpec & ~(0x3 << 30);
      final int heightSize = heightMeasureSpec & ~(0x3 << 30);

      measureChild(mCoverImage, widthMeasureSpec,
          kCoverHeight + MeasureSpec.EXACTLY);

      measureChild(mShadow, widthMeasureSpec,
          kCoverHeight + MeasureSpec.EXACTLY);

      if (mTop != null) {
        measureChild(mTop, widthSize + MeasureSpec.EXACTLY,
            kCoverHeight + MeasureSpec.EXACTLY);
      }

      setMeasuredDimension(widthSize, kCoverHeight);
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t,
                            final int r, final int b) {
      final int w = r - l;
      final int h = b - t;

      mTop.layout(0, h - mTop.getMeasuredHeight(), w, h);
      mCoverImage.layout(0, h - mCoverImage.getMeasuredHeight(), w, h);
      mShadow.layout(0, h - mShadow.getMeasuredHeight(), w, h);
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
      final long drawingTime = getDrawingTime();
      drawChild(canvas, mCoverImage, drawingTime);
      drawChild(canvas, mShadow, drawingTime);
      drawChild(canvas, mTop, drawingTime);
    }
  }


  private class RefreshAsyncTask extends AsyncTask<Void, Void, Void> {
    private final OnRefreshListener mListener;

    RefreshAsyncTask() {
      mListener = mOnRefreshListener;
    }

    @Override
    protected void onPreExecute() {
      if (mListener != null) {
        mListener.onPreRefresh();
      }
    }

    @Override
    protected Void doInBackground(final Void... params) {
      if (mListener != null) {
        mListener.onRefreshData();
      }

      return null;
    }

    @Override
    protected void onPostExecute(final Void aVoid) {
      if (mListener != null) {
        mListener.onRefreshUI();
      }
    }
  }
}
