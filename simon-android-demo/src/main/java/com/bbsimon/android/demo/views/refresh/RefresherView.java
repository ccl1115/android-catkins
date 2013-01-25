package com.bbsimon.android.demo.views.refresh;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.views.Facade;

/**
 */
@SuppressWarnings("unused")
public class RefresherView<T extends ViewGroup> extends ViewGroup implements IRefreshable {
  private static final String TAG = "RefresherView";

  private static final int FRAME_ANIMATION_DURATION = 1000 / 60;
  private static final int MSG_ANIMATE = 1000;

  private static final int REFRESH_HEADER_MAX_HEIGHT = 300;
  private static final int REFRESH_HEADER_HEIGHT = 100;
  private static final int REFRESH_THRESHOLD = 200;

  private final String kPullDownToRefresh;
  private final String kReleaseToRefresh;
  private final String kRefreshing;

  private final int kRefreshThreshold;
  private final int kRefreshHeaderHeight;
  private final int kRefreshHeaderMaxHeight;

  private T mRefresherContent;
  private View mRefresherHeader;
  private TextView mText;
  private View mEmptyView;
  private boolean mHasContent;
  private boolean mShouldDrawEmptyView = true;
  private boolean mEnable;
  private int mLastDownY;
  private final int[] mContentLocation = new int[2];
  private final int[] mTempLocation = new int[2];
  private int mAbsY;
  private int mYOffset;
  private int mBackPosition;
  private Animator mAnimator;
  private AnimatorHandler mHandler;
  private OnRefreshListener mOnRefreshListener;
  private RefreshAsyncTask mRefreshAsyncTask;

  public RefresherView(Context context) {
    this(context, null, 0);
  }

  public RefresherView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RefresherView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mAnimator = new Animator();
    mHandler = new AnimatorHandler();

    final Resources r = getResources();
    final float density = r.getDisplayMetrics().density;

    kRefreshHeaderHeight = (int) (REFRESH_HEADER_HEIGHT * density + 0.5f);
    kRefreshHeaderMaxHeight =
        (int) (REFRESH_HEADER_MAX_HEIGHT * density + 0.5f);
    kRefreshThreshold = (int) (REFRESH_THRESHOLD * density + 0.5f);

    kPullDownToRefresh = r.getString(R.string.refresher_pull_down_to_refresh);
    kRefreshing = r.getString(R.string.refresher_refreshing);
    kReleaseToRefresh = r.getString(R.string.refresher_release_to_refresh);

    mEmptyView = new ProgressBar(context);
    addView(mEmptyView, new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT));

    mRefresherHeader = View.inflate(context, R.layout.refresher, null);
    mText = (TextView) mRefresherHeader.findViewById(R.id.text);
    addView(mRefresherHeader, new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT));
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void onFinishInflate() {
    if (mRefresherContent == null) {
      mRefresherContent = (T) findViewById(R.id.refresher_content);
    }
    if (mRefresherContent == null) {
      mHasContent = false;
      mEnable = false;
    } else {
      mHasContent = true;
      mEnable = true;
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthSize = widthMeasureSpec & ~(0x3 << 30);
    final int heightSize = heightMeasureSpec & ~(0x3 << 30);
    if (mHasContent) {
      measureChild(mRefresherContent, widthSize + MeasureSpec.EXACTLY,
          heightSize + MeasureSpec.EXACTLY);
    }

    if (mShouldDrawEmptyView) {
      measureChild(mEmptyView, widthSize + MeasureSpec.AT_MOST,
          heightSize + MeasureSpec.AT_MOST);
    }

    measureChild(mRefresherHeader, widthSize + MeasureSpec.EXACTLY,
        kRefreshHeaderHeight + MeasureSpec.EXACTLY);

    setMeasuredDimension(widthSize, heightSize);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int width = r - l;
    final int height = b - t;
    if (mHasContent) {
      mRefresherContent.layout(0, 0, width, height);
    }

    if (mShouldDrawEmptyView) {
      mEmptyView.layout((width - mEmptyView.getMeasuredWidth()) / 2,
          (height - mEmptyView.getMeasuredHeight()) / 2,
          (width + mEmptyView.getMeasuredWidth()) / 2,
          (height + mEmptyView.getMeasuredHeight()) / 2);
    }

    mRefresherHeader.layout(0, -mRefresherHeader.getMeasuredHeight(), width, 0);

    getLocationOnScreen(mTempLocation);
    mAbsY = mTempLocation[1];
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (!mEnable || mAnimator.animating) {
      return false;
    }

    final int action = ev.getAction() & MotionEvent.ACTION_MASK;
    final int y = (int) ev.getY();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mLastDownY = y;

        break;

      case MotionEvent.ACTION_MOVE:
        View childAt = mRefresherContent.getChildAt(0);
        if (childAt != null) {
          childAt.getLocationOnScreen(mContentLocation);
          if (mContentLocation[1] == mAbsY && (y > mLastDownY)) {
            mYOffset = Math.min(y - mLastDownY, kRefreshHeaderMaxHeight);
            invalidate();

            return true;
          }
        }
      default:
        break;
    }

    return false;
  }

  @Override
  public final boolean onTouchEvent(final MotionEvent event) {
    final int action = event.getAction() & MotionEvent.ACTION_MASK;
    final int y = (int) event.getY();

    switch (action) {
      case MotionEvent.ACTION_MOVE:
        mYOffset = Math.max(0, Math.min(y - mLastDownY, kRefreshHeaderMaxHeight * 2));

        if (mYOffset > kRefreshThreshold) {
          mText.setText(kReleaseToRefresh);
        } else {
          mText.setText(kPullDownToRefresh);
        }

        invalidate();

        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (mYOffset > kRefreshThreshold) {
          mBackPosition = kRefreshThreshold;
          mText.setText(kRefreshing);
          refresh();
        } else {
          mBackPosition = 0;
        }

        mAnimator.animate();

        break;
      default:
        break;
    }

    return true;
  }

  public void setOnRefreshListener(OnRefreshListener listener) {
    mOnRefreshListener = listener;
  }

  @Override
  public void setEnable(boolean enable) {
    mEnable = enable;
  }

  @Override
  public boolean getEnable() {
    return mEnable;
  }

  public void refresh() {
    if (mRefreshAsyncTask == null ||
        mRefreshAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
      mRefreshAsyncTask = new RefreshAsyncTask();
      mRefreshAsyncTask.execute((Void[]) null);
    }
  }

  @Override
  public void showEmptyView() {
    mShouldDrawEmptyView = true;
    addView(mEmptyView, new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT));
  }

  @Override
  public void hideEmptyView() {
    mShouldDrawEmptyView = false;
    removeView(mEmptyView);
  }

  @Override
  public void setRefreshIndicator(IRefreshIndicator indicator) {
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    final long drawingTime = getDrawingTime();

    if (mShouldDrawEmptyView) {
      drawChild(canvas, mEmptyView, drawingTime);
    }

    canvas.save();
    canvas.translate(0, mYOffset / 2);
    if (mYOffset > 0) {
      drawChild(canvas, mRefresherHeader, drawingTime);
      invalidate();
    }
    drawChild(canvas, mRefresherContent, drawingTime);
    canvas.restore();
  }

  private class Animator {
    final static int VELOCITY = 600;
    private boolean animating;
    private long lastAnimationTime;
    private long currentAnimatingTime;
    private final int kVelocity;
    private int animatingVelocity;
    private int animatingPosition;
    private int animationDistance;

    public Animator() {
      kVelocity =
          (int) (getResources().getDisplayMetrics().density * VELOCITY + 0.5);
    }

    void compute() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;

      animatingPosition += animatingVelocity * t;

      if (animatingPosition >= animationDistance) {
        mYOffset = mBackPosition;
        animating = false;
      } else {
        mYOffset = (int)
            (mBackPosition + animationDistance * (1 - Facade.sInterpolator.getInterpolation(
            animatingPosition / (float) animationDistance)));
        lastAnimationTime = now;
        currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
        mHandler.removeMessages(MSG_ANIMATE);
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE, currentAnimatingTime);
      }

      invalidate();
    }

    void animate() {
      final long now = SystemClock.uptimeMillis();

      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
      animationDistance = mYOffset - mBackPosition;
      Log.d(TAG, "@animatePullBack animating distance " + animationDistance);
      animating = true;
      animatingPosition = 0;
      animatingVelocity = (mYOffset - mBackPosition) * 2;
      mHandler.removeMessages(MSG_ANIMATE);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE,
          currentAnimatingTime);
    }
  }

  public T getRefresherContent() {
    return mRefresherContent;
  }

  public void setRefresherContent(T refresherContent) {
    removeView(mRefresherContent);
    mRefresherContent = refresherContent;
    if (mRefresherContent == null) {
      mHasContent = false;
      mEnable = false;
    } else {
      addView(mRefresherContent, new LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT));
      mHasContent = true;
      mEnable = true;
    }

  }

  private class AnimatorHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      if (msg.what == MSG_ANIMATE) {
        mAnimator.compute();
      }
    }
  }

  /**
   */
  private class RefreshAsyncTask extends AsyncTask<Void, Void, Void> {
    /**
     */
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
      mBackPosition = 0;
      mAnimator.animate();
    }
  }
}
