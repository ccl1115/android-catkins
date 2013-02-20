package com.simon.catkins.views.refresh;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.simon.catkins.R;
import com.simon.catkins.views.Facade;
import com.simon.catkins.views.TransitionAnimator;
import de.akquinet.android.androlog.Log;

/**
 */
@SuppressWarnings("unused")
public class RefresherView extends ViewGroup implements IRefreshable {
  private static final String TAG = "RefresherView";

  private static final String DIRECTION_TOP = "top";
  private static final String DIRECTION_SIDE = "side";

  private static final int MSG_ANIMATE_BACK = 1000;
  private static final int MSG_ANIMATE_DOWN = 1001;

  private static final int DEFAULT_THRESHOLD_HEIGHT = 200; // dips
  private static final int DEFAULT_MAX_HEIGHT = 400; // dips

  private static final int MIN_VELOCITY = 100;
  private static final int VELOCITY = 500;

  private final int kMinVelocity;
  private final int kVelocity;

  private int mThresholdHeight;
  private int mMaxHeight;

  private int mRefresherContentId;
  private int mRefresherHeaderId;
  private int mEmptyViewId;

  private View mRefresherContent;
  private View mRefresherHeader;
  private View mEmptyView;
  private boolean mEnable = true;
  private boolean mRefreshing;
  private int mLastDownY;
  private int mLastDownX;
  private final int[] mContentLocation = new int[2];
  private final int[] mTempLocation = new int[2];
  private int mAbsY;
  private int mAbsX;
  private int mYOffset;
  private int mXOffset;
  private int mBackPosition;
  private Animator mAnimator;
  private AnimatorHandler mHandler;
  private OnRefreshListener mOnRefreshListener;
  private RefreshAsyncTask mRefreshAsyncTask;

  private TransitionAnimator mTransitionAnimator;

  private State mState = State.idle;

  public RefresherView(Context context) {
    this(context, null, 0);
  }

  public RefresherView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RefresherView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    Log.init(context);
    mAnimator = new Animator();
    mHandler = new AnimatorHandler();

    final Resources r = getResources();
    final float density = r.getDisplayMetrics().density;

    kMinVelocity = (int) (MIN_VELOCITY * density + 0.5f);
    kVelocity = (int) (VELOCITY * density + 0.5f);

    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RefresherView);

    mThresholdHeight = ta.getDimensionPixelOffset(R.styleable.RefresherView_threshold_height, -1);
    if (mThresholdHeight == -1) {
      mThresholdHeight = (int) (DEFAULT_THRESHOLD_HEIGHT * density + 0.5f);
    }

    mMaxHeight = ta.getDimensionPixelOffset(R.styleable.RefresherView_max_height, -1);
    if (mMaxHeight == -1) {
      mMaxHeight = (int) (DEFAULT_MAX_HEIGHT * density + 0.5f);
    }

    String direction = ta.getString(R.styleable.RefresherView_direction);
    if (direction == null) {
      mTransitionAnimator = new TopRefreshTransitionAnimator();
    } else if (direction.equals(DIRECTION_SIDE)) {
      mTransitionAnimator = new SideRefreshTransitionAnimator();
    } else if (direction.equals(DIRECTION_TOP)) {
      mTransitionAnimator = new TopRefreshTransitionAnimator();
    }

    mRefresherContentId = ta.getResourceId(R.styleable.RefresherView_refresher_content, -1);
    mRefresherHeaderId = ta.getResourceId(R.styleable.RefresherView_refresher_head, -1);
    mEmptyViewId = ta.getResourceId(R.styleable.RefresherView_empty_view, -1);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void onFinishInflate() {
    if (mRefresherContentId == -1) {
      throw new RuntimeException("refresher content id is not set in xml, or call setRefresherContent before add it to a view tree.");
    } else {
      mRefresherContent = findViewById(mRefresherContentId);
      if (mRefresherContent == null) {
        throw new RuntimeException("refresher content not found in the view tree by the content id.");
      }
    }

    if (mRefresherHeaderId == -1) {
      throw new RuntimeException("refresher head id is not set in xml, or call setRefresherHeader before add it to a view tree.");
    } else {
      mRefresherHeader = findViewById(mRefresherHeaderId);
      if (mRefresherHeader == null) {
        throw new RuntimeException("refresher header not found in the view tree by the header id.");
      }
    }

    if (mEmptyViewId == -1) {
      throw new RuntimeException("empty view id is not set in xml, or call setEmptyView before add it to a view tree");
    } else {
      mEmptyView = findViewById(mEmptyViewId);
      if (mEmptyView == null) {
        throw new RuntimeException("empty view not found in the view tree by the empty view's id");
      }
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mTransitionAnimator.measure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    mTransitionAnimator.layout(changed, l, t, r, b);
  }

  @Override
  @SuppressWarnings("all")
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return mTransitionAnimator.dispatchTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return mTransitionAnimator.interceptionTouchEvent(ev);
  }

  @Override
  public final boolean onTouchEvent(final MotionEvent event) {
    return mTransitionAnimator.touchEvent(event);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    mTransitionAnimator.draw(canvas);
  }

  public void setOnRefreshListener(OnRefreshListener listener) {
    mOnRefreshListener = listener;
  }

  @Override
  public void setEnable(boolean enable) {
    // Can be true only when the refresher content and header are not null.
    mEnable = enable && mRefresherContent != null && mRefresherHeader != null;
  }

  @Override
  public boolean isEnabled() {
    return mEnable;
  }

  @Override
  public State getState() {
    return mState;
  }

  public void refresh() {
    if (mRefreshAsyncTask == null ||
        mRefreshAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
      mRefreshAsyncTask = new RefreshAsyncTask();
      mRefreshAsyncTask.execute((Void[]) null);
    }
  }

  @Override
  public void refreshShowingHeader() {
    if (!mRefreshing) mTransitionAnimator.animate(MSG_ANIMATE_DOWN);
  }

  private class Animator {
    private boolean animating;
    private long lastAnimationTime;
    private long currentAnimatingTime;
    private int animatingVelocity;
    private int animatingPosition;
    private int animationDistance;

    void computeBack() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;

      animatingPosition += animatingVelocity * t;

      if (animatingPosition >= animationDistance) {
        mYOffset = mBackPosition;
        animating = false;
        mState = State.idle;
        final OnRefreshListener onRefreshListener = mOnRefreshListener;
        if (onRefreshListener != null) {
          onRefreshListener.onStateChanged(State.idle);
        }

        if (mBackPosition == 0) {
          if (onRefreshListener != null) {
            onRefreshListener.onRefreshUI();
            mRefreshing = false;
          }
        }
      } else {
        mYOffset = (int)
            (mBackPosition + animationDistance * (1 - Facade.sInterpolator.getInterpolation(
                animatingPosition / (float) animationDistance)));
        lastAnimationTime = now;
        currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
        mHandler.removeMessages(MSG_ANIMATE_BACK);
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BACK, currentAnimatingTime);
      }

      invalidate();
    }

    void computeDown() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;

      animatingPosition += animatingVelocity * t;

      if (animatingPosition >= animationDistance) {
        mYOffset = mThresholdHeight;
        animating = false;
        mState = State.idle;
        final OnRefreshListener onRefreshListener = mOnRefreshListener;
        if (onRefreshListener != null) {
          onRefreshListener.onStateChanged(State.idle);
          refresh();
        }
      } else {
        mYOffset = Facade.computeInterpolator(animationDistance, animatingPosition, false);
        lastAnimationTime = now;
        currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
        mHandler.removeMessages(MSG_ANIMATE_DOWN);
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_DOWN, currentAnimatingTime);
      }

      invalidate();
    }

    void animate(final int msg) {
      Log.d(TAG, "@animate");
      final long now = SystemClock.uptimeMillis();
      final OnRefreshListener onRefreshListener;
      lastAnimationTime = now;
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
      animating = true;

      switch (msg) {
        case MSG_ANIMATE_BACK:
          animationDistance = mYOffset - mBackPosition;
          animatingPosition = 0;
          animatingVelocity = Math.max(kMinVelocity, (mYOffset - mBackPosition) * 2);
          mHandler.removeMessages(MSG_ANIMATE_BACK);
          mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BACK, currentAnimatingTime);
          break;
        case MSG_ANIMATE_DOWN:
          animationDistance = mThresholdHeight;
          animatingPosition = 0;
          animatingVelocity = kVelocity;
          mHandler.removeMessages(MSG_ANIMATE_DOWN);
          mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_DOWN, currentAnimatingTime);
          break;
      }

      mState = State.animating;
      onRefreshListener = mOnRefreshListener;
      if (onRefreshListener != null) {
        onRefreshListener.onStateChanged(State.animating);
      }
    }

  }

  @Override
  public View getRefresherContent() {
    return mRefresherContent;
  }

  @Override
  public View getRefresherHeader() {
    return mRefresherHeader;
  }

  @Override
  public View getEmptyView() {
    return mEmptyView;
  }

  @Override
  public void setRefresherContent(ViewGroup view) {
    removeView(mRefresherContent);
    mRefresherContent = view;
    if (mRefresherContent == null) {
      mEnable = false;
    } else {
      addView(mRefresherContent);
      mEnable = mRefresherHeader != null && mRefresherContent != null;
    }

  }

  @Override
  public void setRefresherHeader(View view) {
    removeView(mRefresherHeader);
    mRefresherHeader = view;
    if (mRefresherHeader == null) {
      mEnable = false;
    } else {
      addView(mRefresherHeader);
      mEnable = mRefresherHeader != null && mRefresherContent != null;
    }
  }

  @Override
  public void setEmptyView(View view) {
    removeView(mEmptyView);
    mEmptyView = view;
    if (mEmptyView != null) {
      addView(mEmptyView);
    }
  }

  private class AnimatorHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      if (msg.what == MSG_ANIMATE_BACK) {
        mAnimator.computeBack();
      } else if (msg.what == MSG_ANIMATE_DOWN) {
        mAnimator.computeDown();
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
        mBackPosition = mThresholdHeight;
        mListener.onPreRefresh();
      }
    }

    @Override
    protected Void doInBackground(final Void... params) {
      mRefreshing = true;
      if (mListener != null) {
        mListener.onRefreshData();
      }
      return null;
    }

    @Override
    protected void onPostExecute(final Void aVoid) {
      mBackPosition = 0;
      mTransitionAnimator.animate(MSG_ANIMATE_BACK);
    }
  }

  private class TopRefreshTransitionAnimator implements TransitionAnimator {

    @Override
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
      final int widthSize = widthMeasureSpec & ~(0x3 << 30);
      final int heightSize = heightMeasureSpec & ~(0x3 << 30);
      if (mRefresherContent != null) {
        measureChild(mRefresherContent, widthSize + MeasureSpec.EXACTLY,
            heightSize + MeasureSpec.EXACTLY);
      }

      if (mEmptyView != null) {
        measureChild(mEmptyView, widthSize + MeasureSpec.AT_MOST,
            heightSize + MeasureSpec.AT_MOST);
      }

      if (mRefresherHeader != null) {
        measureChild(mRefresherHeader, widthSize + MeasureSpec.EXACTLY,
            heightSize + MeasureSpec.AT_MOST);
      }

      setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public void layout(boolean changed, int l, int t, int r, int b) {
      final int width = r - l;
      final int height = b - t;
      if (mRefresherContent != null) {
        mRefresherContent.layout(0, 0, width, height);
      }

      if (mEmptyView != null) {
        mEmptyView.layout((width - mEmptyView.getMeasuredWidth()) / 2,
            (height - mEmptyView.getMeasuredHeight()) / 2,
            (width + mEmptyView.getMeasuredWidth()) / 2,
            (height + mEmptyView.getMeasuredHeight()) / 2);
      }

      if (mRefresherHeader != null) {
        mRefresherHeader.layout(0, -mRefresherHeader.getMeasuredHeight(), width, 0);
      }

      getLocationOnScreen(mTempLocation);
      mAbsY = mTempLocation[1];
    }

    @Override
    public void draw(Canvas canvas) {
      final long drawingTime = getDrawingTime();

      if (mEmptyView != null) {
        drawChild(canvas, mEmptyView, drawingTime);
      }

      canvas.save();
      canvas.translate(0, mYOffset / 2);
      drawChild(canvas, mRefresherContent, drawingTime);
      if (mYOffset > 0) {
        drawChild(canvas, mRefresherHeader, drawingTime);
      }
      canvas.restore();
    }

    @Override
    @SuppressWarnings("all")
    public boolean dispatchTouchEvent(MotionEvent event) {
      return RefresherView.super.dispatchTouchEvent(event) || true;
    }

    @Override
    public boolean interceptionTouchEvent(MotionEvent ev) {
      if (!mEnable || mRefreshing) {
        return false;
      }

      final int action = ev.getAction() & MotionEvent.ACTION_MASK;
      final int y = (int) ev.getY();

      switch (action) {
        case MotionEvent.ACTION_DOWN:
          mLastDownY = y;

          mHandler.removeMessages(MSG_ANIMATE_BACK);
          break;

        case MotionEvent.ACTION_MOVE:
          View childAt;
          if (mRefresherContent instanceof ViewGroup
              && (childAt = ((ViewGroup) mRefresherContent).getChildAt(0)) != null) {
            childAt.getLocationOnScreen(mContentLocation);
            if (mContentLocation[1] == mAbsY && (y > mLastDownY)) {
              mState = State.pulling_no_refresh;
              final OnRefreshListener onRefreshListener = mOnRefreshListener;
              if (onRefreshListener != null) {
                onRefreshListener.onStateChanged(State.pulling_no_refresh);
              }
              return true;
            }
          } else {
            // If there's no child.
            mRefresherContent.getLocationOnScreen(mContentLocation);
            if (mContentLocation[1] == mAbsY && (y > mLastDownY)) {
              mState = State.pulling_no_refresh;
              final OnRefreshListener onRefreshListener = mOnRefreshListener;
              if (onRefreshListener != null) {
                onRefreshListener.onStateChanged(State.pulling_no_refresh);
              }
              return true;
            }
          }
        default:
          break;
      }

      return false;
    }

    @Override
    public boolean touchEvent(MotionEvent event) {
      final int action = event.getAction() & MotionEvent.ACTION_MASK;
      final int y = (int) event.getY();

      switch (action) {
        case MotionEvent.ACTION_MOVE:
          mYOffset = Math.max(0, Math.min(y - mLastDownY, mMaxHeight * 2));

          if (mYOffset > mThresholdHeight && mState == State.pulling_no_refresh) {
            mState = State.pulling_refresh;
            final OnRefreshListener onRefreshListener = mOnRefreshListener;
            if (onRefreshListener != null) {
              onRefreshListener.onStateChanged(State.pulling_refresh);
            }
          } else if (mYOffset < mThresholdHeight && mState == State.pulling_refresh) {
            mState = State.pulling_no_refresh;

            final OnRefreshListener onRefreshListener = mOnRefreshListener;
            if (onRefreshListener != null) {
              onRefreshListener.onStateChanged(State.pulling_no_refresh);
            }
          }
          invalidate();
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if (mYOffset > mThresholdHeight) {
            refresh();
          } else {
            mBackPosition = 0;
          }
          animate(MSG_ANIMATE_BACK);
          break;
        default:
          break;
      }

      return true;
    }

    @Override
    public void animate(int msg) {
      mAnimator.animate(msg);
    }

    @Override
    public boolean isAnimating() {
      return mAnimator.animating;
    }
  }

  private class SideRefreshTransitionAnimator extends Handler implements TransitionAnimator {
    private static final String TAG = "RefresherView$SideRefreshTransitionAnimator";

    private final int moveThreshold;

    private boolean animating;
    private long currentAnimatingTime;
    private long lastAnimationTime;
    private float animatingPosition;
    private float animationDistance;
    private int animatingVelocity;

    public SideRefreshTransitionAnimator() {
      final float density = getResources().getDisplayMetrics().density;
      moveThreshold = (int) (Facade.TOUCH_EVENT_MOVE_THRESHOLD_LARGE * density + 0.5);
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_DOWN:
          computeDown();
          break;
        case MSG_ANIMATE_BACK:
          computeBack();
          break;
      }
    }

    @Override
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
      final int widthSize = widthMeasureSpec & ~(0x3 << 30);
      final int heightSize = heightMeasureSpec & ~(0x3 << 30);
      if (mRefresherContent != null) {
        measureChild(mRefresherContent, widthSize + MeasureSpec.EXACTLY,
            heightSize + MeasureSpec.EXACTLY);
      }

      if (mEmptyView != null) {
        measureChild(mEmptyView, widthSize + MeasureSpec.AT_MOST,
            heightSize + MeasureSpec.AT_MOST);
      }

      if (mRefresherHeader != null) {
        measureChild(mRefresherHeader, widthSize + MeasureSpec.AT_MOST,
            heightSize + MeasureSpec.EXACTLY);
      }

      setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public void layout(boolean changed, int l, int t, int r, int b) {
      final int width = r - l;
      final int height = b - t;
      if (mRefresherContent != null) {
        mRefresherContent.layout(0, 0, width, height);
      }

      if (mEmptyView != null) {
        mEmptyView.layout((width - mEmptyView.getMeasuredWidth()) / 2,
            (height - mEmptyView.getMeasuredHeight()) / 2,
            (width + mEmptyView.getMeasuredWidth()) / 2,
            (height + mEmptyView.getMeasuredHeight()) / 2);
      }

      if (mRefresherHeader != null) {
        mRefresherHeader.layout(-mRefresherHeader.getMeasuredWidth(), 0, 0, height);
      }

      getLocationOnScreen(mTempLocation);
      mAbsX = mTempLocation[0];
    }

    @Override
    public void draw(Canvas canvas) {
      final long drawingTime = getDrawingTime();

      if (mEmptyView != null) {
        drawChild(canvas, mEmptyView, drawingTime);
      }

      canvas.save();
      canvas.translate(mXOffset >> 1, 0);
      drawChild(canvas, mRefresherContent, drawingTime);
      if (mXOffset > 0) {
        drawChild(canvas, mRefresherHeader, drawingTime);
      }
      canvas.restore();
    }

    @Override
    @SuppressWarnings("all")
    public boolean dispatchTouchEvent(MotionEvent event) {
      return RefresherView.super.dispatchTouchEvent(event) || true;
    }

    @Override
    public boolean interceptionTouchEvent(MotionEvent ev) {
      if (!mEnable || mRefreshing) {
        return false;
      }

      final int action = ev.getAction() & MotionEvent.ACTION_MASK;
      final int x = (int) ev.getX();

      switch (action) {
        case MotionEvent.ACTION_DOWN:
          mLastDownX = x;

          removeMessages(MSG_ANIMATE_BACK);
          removeMessages(MSG_ANIMATE_DOWN);
          break;

        case MotionEvent.ACTION_MOVE:
          View childAt;
          if (mRefresherContent instanceof ViewGroup
              && (childAt = ((ViewGroup) mRefresherContent).getChildAt(0)) != null) {
            childAt.getLocationOnScreen(mContentLocation);
            if (mContentLocation[0] == mAbsX && (x > mLastDownX + moveThreshold)) {
              mState = State.pulling_no_refresh;
              final OnRefreshListener onRefreshListener = mOnRefreshListener;
              if (onRefreshListener != null) {
                onRefreshListener.onStateChanged(State.pulling_no_refresh);
              }
              return true;
            }
          } else {
            // If there's no child.
            mRefresherContent.getLocationOnScreen(mContentLocation);
            if (mContentLocation[0] == mAbsX && (x > mLastDownX + moveThreshold)) {
              mState = State.pulling_no_refresh;
              final OnRefreshListener onRefreshListener = mOnRefreshListener;
              if (onRefreshListener != null) {
                onRefreshListener.onStateChanged(State.pulling_no_refresh);
              }
              return true;
            }
          }
        default:
          break;
      }

      return false;
    }

    @Override
    public boolean touchEvent(MotionEvent event) {
      final int action = event.getAction() & MotionEvent.ACTION_MASK;
      final int x = (int) event.getX();

      switch (action) {
        case MotionEvent.ACTION_MOVE:
          mXOffset = Math.max(0, Math.min(x - mLastDownX - moveThreshold, mMaxHeight * 2));

          if (mXOffset > mThresholdHeight && mState == State.pulling_no_refresh) {
            mState = State.pulling_refresh;
            final OnRefreshListener onRefreshListener = mOnRefreshListener;
            if (onRefreshListener != null) {
              onRefreshListener.onStateChanged(State.pulling_refresh);
            }
          } else if (mXOffset < mThresholdHeight && mState == State.pulling_refresh) {
            mState = State.pulling_no_refresh;

            final OnRefreshListener onRefreshListener = mOnRefreshListener;
            if (onRefreshListener != null) {
              onRefreshListener.onStateChanged(State.pulling_no_refresh);
            }
          }
          invalidate();
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if (mXOffset > mThresholdHeight) {
            refresh();
          } else {
            mBackPosition = 0;
          }
          animate(MSG_ANIMATE_BACK);
          break;
        default:
          break;
      }

      return true;
    }

    @Override
    public void animate(int msg) {
      switch (msg) {
        case MSG_ANIMATE_DOWN:
          animateDown();
          break;
        case MSG_ANIMATE_BACK:
          animateBack();
          break;
      }
    }

    @Override
    public boolean isAnimating() {
      return animating;
    }

    private void animateDown() {
      final long now = SystemClock.uptimeMillis();

      lastAnimationTime = now;
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
      animating = true;
      animationDistance = mThresholdHeight;
      animatingPosition = 0;
      animatingVelocity = kVelocity;

      removeMessages(MSG_ANIMATE_DOWN);
      sendEmptyMessageAtTime(MSG_ANIMATE_DOWN, currentAnimatingTime);
    }

    private void animateBack() {
      final long now = SystemClock.uptimeMillis();

      lastAnimationTime = now;
      currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
      animating = true;
      animationDistance = mXOffset - mBackPosition;
      animatingPosition = 0;
      animatingVelocity = Math.max(kMinVelocity, (mXOffset - mBackPosition) * 2);

      removeMessages(MSG_ANIMATE_BACK);
      sendEmptyMessageAtTime(MSG_ANIMATE_BACK, currentAnimatingTime);
    }

    private void computeDown() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;

      animatingPosition += animatingVelocity * t;

      if (animatingPosition >= animationDistance) {
        mXOffset = mThresholdHeight;
        animating = false;
        mState = State.idle;
        final OnRefreshListener onRefreshListener = mOnRefreshListener;
        if (onRefreshListener != null) {
          onRefreshListener.onStateChanged(State.idle);
          refresh();
        }
      } else {
        mXOffset = Facade.computeInterpolator(animationDistance, animatingPosition, false);
        lastAnimationTime = now;
        currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
        removeMessages(MSG_ANIMATE_DOWN);
        sendEmptyMessageAtTime(MSG_ANIMATE_DOWN, currentAnimatingTime);
      }

      invalidate();
    }

    private void computeBack() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;

      animatingPosition += animatingVelocity * t;

      if (animatingPosition >= animationDistance) {
        mXOffset = mBackPosition;
        animating = false;
        mState = State.idle;
        final OnRefreshListener onRefreshListener = mOnRefreshListener;
        if (onRefreshListener != null) {
          onRefreshListener.onStateChanged(State.idle);
        }

        if (mBackPosition == 0) {
          if (onRefreshListener != null) {
            onRefreshListener.onRefreshUI();
            mRefreshing = false;
          }
        }
      } else {
        mXOffset = (int)
            (mBackPosition + animationDistance * (1 - Facade.sInterpolator.getInterpolation(
                animatingPosition / animationDistance)));
        lastAnimationTime = now;
        currentAnimatingTime = now + Facade.ANIMATION_FRAME_DURATION;
        removeMessages(MSG_ANIMATE_BACK);
        sendEmptyMessageAtTime(MSG_ANIMATE_BACK, currentAnimatingTime);
      }

      invalidate();
    }
  }
}
