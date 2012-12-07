package com.bbsimon.android.demo.views.fakechrome;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import com.bbsimon.android.demo.views.fakechrome.scalable.IScalable;
import com.bbsimon.android.demo.R;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

@SuppressWarnings("unused")
public final class FakeChromeLayout extends ViewGroup implements IFakeChrome {
  private static final String TAG = "FakeChromeLayout";

  public static final int PAGE_INDICATOR_ID = R.id.page_indicator;

  // devices after HONEYCOMB will have fade-out effect
  private static final boolean CAN_ALPHA =
      Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB;

  public static final int MODE_PAGER = 1000;
  public static final int MODE_OVERVIEW = 1001;
  public static final int MODE_SWITCHING = 1002;

  private static final int FRAME_ANIMATION_DURATION = 1000 / 60; // ms

  // Consider as a tap if movement within this value
  private static final int TAP_THRESHOLD = 15; // dips

  private static final float SCALE_CONSTANT = 0.005f;

  // For overview fling
  private static final int MSG_ANIMATE_DECELERATION = 10;

  // For child view addition/deletion
  private static final int MSG_ANIMATE_PAGER_ADDITION = 1010;
  private static final int MSG_ANIMATE_PAGER_DELETION = 1011;
  private static final int MSG_ANIMATE_OVERVIEW_ADDITION = 1012;
  private static final int MSG_ANIMATE_OVERVIEW_DELETION = 1013;

  // For pager animator
  private static final int MSG_ANIMATE_LEFT = 1000;
  private static final int MSG_ANIMATE_RIGHT = 1001;
  private static final int MSG_ANIMATE_LEFT_BACK = 1002;
  private static final int MSG_ANIMATE_RIGHT_BACK = 1003;

  // For re-layout
  private static final int MSG_ANIMATE_LAYOUT = 100000;

  // For switch mode
  private static final int MSG_ANIMATE_OVERVIEW = 10000;
  private static final int MSG_ANIMATE_PAGER = 10001;

  // Pixel values
  private final int mTapThreshold;
  private final float mScaleConstant;

  private int mCurItem;
  private int mMode;

  private FakeChromeAdapter mAdapter;
  private TabObserver mTabObserver;

  // Animator/Tracker/Handler for UI interaction
  private final PagerTracker mPagerTracker;
  private final PagerAnimator mPagerAnimator;
  private final PagerHandler mPagerHandler;
  private final OverviewTracker mOverviewTracker;
  private final OverviewAnimator mOverviewAnimator;
  private final OverviewHandler mOverviewHandler;
  private final SwitchAnimator mSwitchAnimator;
  private final SwitchHandler mSwitchHandler;
  private final OverviewLayoutAnimator mOverviewLayoutAnimator;
  private final OverviewLayoutHandler mOverviewLayoutHandler;
  private final AdditionDeletionAnimator mAdditionDeletionAnimator;
  private final AdditionDeletionHandler mAdditionDeletionHandler;

  // Get the velocity from MotionEvent
  private VelocityTracker mVelocityTracker;

  // Population will not request layout if it happens in layout procession.
  private boolean mInLayout;

  // Skip populate if true
  private boolean mPendingPopulate;

  private boolean mEventMoved;

  // Down/move and offset down/move position
  private int mLastDownX;
  private int mLastDownY;
  private int mLastOffsetMoveX;
  private int mLastOffsetMoveY;
  private int mLastOffsetDownX;
  private int mLastOffsetDownY;

  private final Rect mOverviewTouchedChildFrame = new Rect();

  // ...
  private final ItemInfo mCur = new ItemInfo();
  private final ItemInfo mPre = new ItemInfo();
  private final ItemInfo mNext = new ItemInfo();

  // Temp variables referencing the view to be processing
  private View mCurView;
  private TabView mPreTabView;
  private View mPreView;
  private TabView mNextTabView;
  private View mNextView;
  private TabView mCurTabView;
  private View mOverviewTouchedView;
  private View mPostRemoveView;
  private View mPostAddView;

  private PagerIndicator mIndicator;

  // Populated data
  private final List<ItemInfo> mInfos = new ArrayList<ItemInfo>();


  public FakeChromeLayout(Context context) {
    this(context, null, 0);
  }

  public FakeChromeLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FakeChromeLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    setAlwaysDrawnWithCacheEnabled(false);

    mPagerTracker = new PagerTracker();
    mPagerHandler = new PagerHandler();
    mPagerAnimator = new PagerAnimator();

    mSwitchAnimator = new SwitchAnimator();
    mSwitchHandler = new SwitchHandler();

    mOverviewTracker = new OverviewTracker();
    mOverviewAnimator = new OverviewAnimator();
    mOverviewHandler = new OverviewHandler();

    mOverviewLayoutAnimator = new OverviewLayoutAnimator();
    mOverviewLayoutHandler = new OverviewLayoutHandler();

    mAdditionDeletionAnimator = new AdditionDeletionAnimator();
    mAdditionDeletionHandler = new AdditionDeletionHandler();

    mMode = MODE_PAGER;

    final float density = context.getResources().getDisplayMetrics().density;
    mTapThreshold = (int) (TAP_THRESHOLD * density + 0.5f);
    mScaleConstant = SCALE_CONSTANT * density;
  }

  @Override
  protected void onFinishInflate() {
    mIndicator = (PagerIndicator) findViewById(PAGE_INDICATOR_ID);
  }

  @Override
  public void addView(View child) {
//        Log.d(TAG, "@addView");
    super.addView(child);

  }

  @Override
  public void removeView(View view) {
//        Log.d(TAG, "@removeView");
    mPostRemoveView = view;
    if (mMode == MODE_OVERVIEW) {
      mAdditionDeletionAnimator.animateOverviewDeletion(mPostRemoveView, OverviewAnimator.MIN_VELOCITY);
    } else if (mMode == MODE_PAGER) {
      mAdditionDeletionAnimator.animatePagerDeletion(mPostRemoveView);
    } else {
      postRemoveView();
    }
  }

  private void postRemoveView() {
//        Log.d(TAG, "@postRemoveView");
    super.removeView(mPostRemoveView);
    mPostRemoveView = null;
  }

  static class ItemInfo {
    TabFragment fragment;
    int position;
  }

  private class AdditionDeletionAnimator {
    static final int VELOCITY = 1000; // dips
    static final int MIN_CHILD_VELOCITY = 1000; // dips

    final int velocity;
    final int minChildVelocity;

    int animatingVelocity;
    long lastAnimationTime;
    long currentAnimatingTime;
    View animatingChild;

    boolean animating;

    AdditionDeletionAnimator() {
      final float density = getResources().getDisplayMetrics().density;
      velocity = (int) (VELOCITY * density + 0.5f);
      minChildVelocity = (int) (MIN_CHILD_VELOCITY * density + 0.5f);
    }

    void computeOverviewAddition() {

    }

    void animateOverviewAddition() {
    }

    void computePagerAddition() {

    }

    void animatePagerAddition() {

    }

    void computeOverviewDeletion() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      final int offset = (int) (animatingVelocity * t);
      final int left = animatingChild.getLeft();
      final int width = getWidth();

      if (left + offset > width) {
        animatingChild.offsetLeftAndRight(width - left);
        animating = false;
        animatingChild = null;

        mPendingPopulate = false;
        populate(false);

        mOverviewLayoutAnimator.animate();
      } else if (left + offset < 0) {
        if (CAN_ALPHA) {
          animatingChild.setAlpha(1f);
        }
        animatingChild.offsetLeftAndRight(-left);
        animating = false;
        animatingChild = null;
      } else {
        animatingChild.offsetLeftAndRight(offset);

        if (CAN_ALPHA) {
          animatingChild.setAlpha((float) (width - left) / width);
        }

        lastAnimationTime = now;
        currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
        mAdditionDeletionHandler.removeMessages(MSG_ANIMATE_OVERVIEW_DELETION);
        mAdditionDeletionHandler.sendMessageAtTime(
            mOverviewHandler.obtainMessage(MSG_ANIMATE_OVERVIEW_DELETION),
            currentAnimatingTime);
      }
      invalidate();
    }

    void animateOverviewDeletion(View child, int velocity) {
      animating = true;
      animatingChild = child;

      if (velocity > 0) {
        animatingVelocity = max(velocity, minChildVelocity);
      } else {
        animatingVelocity = min(velocity, -minChildVelocity);
      }

      final long now = SystemClock.uptimeMillis();
      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
      mAdditionDeletionHandler.removeMessages(MSG_ANIMATE_OVERVIEW_DELETION);
      mAdditionDeletionHandler.sendMessageAtTime(mOverviewHandler.obtainMessage(MSG_ANIMATE_OVERVIEW_DELETION),
          currentAnimatingTime);

    }

    void computePagerDeletion() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      final int offset = (int) (animatingVelocity * t);
      final int left = animatingChild.getLeft();
      final int width = getWidth();

//            Log.d(TAG, "@computePagerDeletion " + left);

      if (left + offset > width) {
        animatingChild.offsetLeftAndRight(width - left);
        animating = false;
        animatingChild = null;

        populate(true);

        postRemoveView();
      } else {
        animatingChild.offsetLeftAndRight(offset);

        if (CAN_ALPHA) {
          animatingChild.setAlpha((float) (width - left) / width);
        }

        lastAnimationTime = now;
        currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
        mAdditionDeletionHandler.removeMessages(MSG_ANIMATE_PAGER_DELETION);
        mAdditionDeletionHandler.sendMessageAtTime(
            mOverviewHandler.obtainMessage(MSG_ANIMATE_PAGER_DELETION),
            currentAnimatingTime);
      }
      invalidate();
    }

    void animatePagerDeletion(View child) {
      if (child.equals(mCurView)) {

        animating = true;
        animatingChild = child;

        preparePagerDeletion();

        animatingVelocity = velocity;

        final long now = SystemClock.uptimeMillis();
        lastAnimationTime = now;
        currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
        mAdditionDeletionHandler.removeMessages(MSG_ANIMATE_PAGER_DELETION);
        mAdditionDeletionHandler.sendMessageAtTime(mOverviewHandler.obtainMessage(MSG_ANIMATE_PAGER_DELETION),
            currentAnimatingTime);
      } else {
        postRemoveView();
      }
    }

    private void preparePagerDeletion() {
      if (mNextView != null) {
        mNextView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
        mNextView.layout(0, 0, getWidth(), getHeight());
      } else if (mPreView != null) {
        mPreView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
        mPreView.layout(0, 0, getWidth(), getHeight());
      }

      animatingChild.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
      animatingChild.layout(0, 0, getWidth(), getHeight());
    }
  }

  private class AdditionDeletionHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_PAGER_ADDITION:
          mAdditionDeletionAnimator.computePagerAddition();
          break;
        case MSG_ANIMATE_PAGER_DELETION:
          mAdditionDeletionAnimator.computePagerDeletion();
          break;
        case MSG_ANIMATE_OVERVIEW_ADDITION:
          mAdditionDeletionAnimator.computeOverviewAddition();
          break;
        case MSG_ANIMATE_OVERVIEW_DELETION:
          mAdditionDeletionAnimator.computeOverviewDeletion();
          break;
      }
    }
  }

  private class OverviewLayoutAnimator {
    static final int VELOCITY = 500; // dips

    final int velocity;

    int animatingVelocity;
    long lastAnimationTime;
    long currentAnimatingTime;

    boolean animating;

    int height;
    int count;

    OverviewLayoutAnimator() {
      final float density = getResources().getDisplayMetrics().density;
      velocity = (int) (VELOCITY * density + 0.5f);
    }

    void compute() {
//            Log.d(TAG, "@compute");
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      int animatingOffset = (int) (animatingVelocity * t);
      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;

      boolean end = true;
      View view;
      int target, top;

      for (int i = 0; i < count; i++) {
        view = mInfos.get(i).fragment.getView();

        if (view == null) {
          continue;
        }

        target = i * height;
        top = view.getTop();
        if (top > target) {
          if (top - animatingOffset * i <= target) {
            view.offsetTopAndBottom(target - top);
          } else {
            end = false;
            view.offsetTopAndBottom(-animatingOffset * i);
          }
        } else {
          if (top + animatingOffset * i >= target) {
            view.offsetTopAndBottom(target - top);
          } else {
            end = false;
            view.offsetTopAndBottom(animatingOffset * i);
          }
        }
      }

      if (end) {
        animating = false;
        postRemoveView();
      } else {
        mOverviewLayoutHandler.removeMessages(MSG_ANIMATE_LAYOUT);
        mOverviewLayoutHandler.sendMessageAtTime(
            mOverviewLayoutHandler.obtainMessage(MSG_ANIMATE_LAYOUT),
            currentAnimatingTime);
      }
      invalidate();
    }

    void animate() {
      if (mInfos.isEmpty()) {
        postRemoveView();
        return;
      } else {
        count = mInfos.size();
        height = getHeight() / count;
      }

      animating = true;
      animatingVelocity = velocity;
      long now = SystemClock.uptimeMillis();
      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
      mOverviewLayoutHandler.removeMessages(MSG_ANIMATE_LAYOUT);
      mOverviewLayoutHandler.sendMessageAtTime(
          mOverviewLayoutHandler.obtainMessage(MSG_ANIMATE_LAYOUT),
          currentAnimatingTime);
    }
  }

  private class OverviewLayoutHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_LAYOUT:
          mOverviewLayoutAnimator.compute();
          break;
      }
    }
  }

  private class PagerTracker {
    boolean tracking;
    boolean changed;
    int lastDistance;

    void prepareTracking() {
      mVelocityTracker = VelocityTracker.obtain();
      mCurView = getChildView(mCur);
      mPreView = getChildView(mPre);
      mNextView = getChildView(mNext);
      mCurTabView = getChildTabView(mCur);
      mPreTabView = getChildTabView(mPre);
      mNextTabView = getChildTabView(mNext);
      tracking = true;
      mPendingPopulate = true;
    }

    void stopTracking() {
      if (!tracking) {
        return;
      }
      if (mCurView != null) {
        final int left = mCurView.getLeft();
        mVelocityTracker.computeCurrentVelocity(100);
        final boolean direction = mVelocityTracker.getXVelocity() > 0;
        //Log.d(TAG, "@stopVerticalTracking " + mVelocityTracker.getXVelocity());
        if (mNextView != null && mPreView != null) {
          flingWithBoth(left, direction);
        } else if (mPreView != null) {
          flingWithPre(left, direction);
        } else if (mNextView != null) {
          flingWithNext(left, direction);
        } else {
          flingWithNone();
        }
      }

      tracking = false;
      invalidate();

      if (mVelocityTracker != null) {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
      }
    }


    void move(int x) {
      if (!tracking) {
        return;
      }
      if (mCurView == null) {
        return;
      }
      final int left = mCurView.getLeft();
      final int distance = mLastOffsetDownX - x;
      changed = (lastDistance < 0 && distance >= 0) ||
          (lastDistance > 0 && distance <= 0);
      lastDistance = distance;
      final int offset = mLastOffsetMoveX - x;
      final float scale = abs(lastDistance) * mScaleConstant;
      //Log.d(TAG, "@move offset " + offset);
      //Log.d(TAG, "@move distance " + lastDistance);
      if (mPreView == null && mNextView == null) {
        if (distance > 0) {
          mCurTabView.setTransformType(IScalable.LEFT_ROTATE);
          mCurTabView.transform(scale);
        } else {
          mCurTabView.setTransformType(IScalable.RIGHT_ROTATE);
          mCurTabView.transform(scale);
        }
      } else if (mPreView == null) {
        if (distance > 0) {
          mCurTabView.setTransformType(IScalable.NO_ROTATE);
          mCurTabView.transform(scale);
          mCurView.offsetLeftAndRight(-offset);
          mNextTabView.setTransformType(IScalable.NO_ROTATE);
          mNextTabView.transform(scale);
          mNextView.offsetLeftAndRight(-offset);
        } else {
          mCurTabView.setTransformType(IScalable.RIGHT_ROTATE);
          mCurTabView.transform(scale);
          if (left != 0) {
            mCurView.offsetLeftAndRight(-mCurView.getLeft());
          }
          if (mNextView.getLeft() != getWidth()) {
            mNextView.offsetLeftAndRight(getWidth() - mNextView.getLeft());
          }
        }
      } else if (mNextView == null) {
        if (distance < 0) {
          //Log.d(TAG, "@move 111");
          mCurTabView.setTransformType(IScalable.NO_ROTATE);
          mCurTabView.transform(scale);
          mCurView.offsetLeftAndRight(-offset);
          mPreTabView.setTransformType(IScalable.NO_ROTATE);
          mPreTabView.transform(scale);
          mPreView.offsetLeftAndRight(-offset);
        } else {
          //Log.d(TAG, "@move");
          mCurTabView.setTransformType(IScalable.LEFT_ROTATE);
          mCurTabView.transform(scale);
          if (left != 0) {
            mCurView.offsetLeftAndRight(-mCurView.getLeft());
          }
          if (mPreView.getLeft() != -getWidth()) {
            mPreView.offsetLeftAndRight(-getWidth() - mPreView.getLeft());
          }
        }
      } else {
        if (changed) {
          //Log.d(TAG, "@move changed 1");
          mCurTabView.transform(0f);
          mNextTabView.transform(0f);
          mPreTabView.transform(0f);
          mCurView.offsetLeftAndRight(-left);
          mNextView.offsetLeftAndRight(getWidth() - mNextView.getLeft());
          mPreView.offsetLeftAndRight(-getWidth() - mPreView.getLeft());
        } else if (distance > 0) {
          mCurTabView.setTransformType(IScalable.NO_ROTATE);
          mCurTabView.transform(scale);
          mCurView.offsetLeftAndRight(-offset);
          mNextTabView.setTransformType(IScalable.NO_ROTATE);
          mNextTabView.transform(scale);
          mNextView.offsetLeftAndRight(-offset);
        } else {
          mCurTabView.setTransformType(IScalable.NO_ROTATE);
          mCurTabView.transform(scale);
          mCurView.offsetLeftAndRight(-offset);
          mPreTabView.setTransformType(IScalable.NO_ROTATE);
          mPreTabView.transform(scale);
          mPreView.offsetLeftAndRight(-offset);
        }
      }
      mIndicator.setSelectorPosition(
          mCurItem - mCurView.getLeft() / (float) getWidth());
      invalidate();
    }

    private void flingWithPre(int left, boolean positive) {
      if (left == 0) {
        //Log.d(TAG, "@flingWithPre");
        mCurTabView.startExpand();
        mPendingPopulate = false;
      } else {
        if (positive) {
          //Log.d(TAG, "@flingWithPre positive");
          mPagerAnimator.animate(mPagerAnimator.velocity, MSG_ANIMATE_RIGHT);
        } else {
          //Log.d(TAG, "@flingWithPre negative");
          mPagerAnimator.animate(-mPagerAnimator.velocity, MSG_ANIMATE_RIGHT_BACK);
        }
      }
    }

    private void flingWithNext(int left, boolean positive) {
      if (left == 0) {
        //Log.d(TAG, "@flingWithNext");
        mCurTabView.startExpand();
        mPendingPopulate = false;
      } else {
        if (positive) {
          //Log.d(TAG, "@flingWithNext positive");
          mPagerAnimator.animate(mPagerAnimator.velocity, MSG_ANIMATE_LEFT_BACK);
        } else {
          //Log.d(TAG, "@flingWithNext negative");
          mPagerAnimator.animate(-mPagerAnimator.velocity, MSG_ANIMATE_LEFT);
        }
      }
    }

    private void flingWithBoth(int left, boolean positive) {
      if (left > 0) {
        if (positive) {
          //Log.d(TAG, "@flingWithBoth right");
          mPagerAnimator.animate(mPagerAnimator.velocity, MSG_ANIMATE_RIGHT);
        } else {
          //Log.d(TAG, "@flingWithBoth right back");
          mPagerAnimator.animate(-mPagerAnimator.velocity, MSG_ANIMATE_RIGHT_BACK);
        }
      } else {
        if (positive) {
          //Log.d(TAG, "@flingWithBoth left back");
          mPagerAnimator.animate(mPagerAnimator.velocity, MSG_ANIMATE_LEFT_BACK);
        } else {
          //Log.d(TAG, "@flingWithBoth left");
          mPagerAnimator.animate(-mPagerAnimator.velocity, MSG_ANIMATE_LEFT);
        }
      }
    }

    private void flingWithNone() {
      //Log.d(TAG, "@flingWithNone");
      mCurTabView.startExpand();
      mPendingPopulate = false;
    }
  }

  private class PagerAnimator {
    static final int VELOCITY = 1200; // dips
    boolean animating;
    boolean expanded;
    final int velocity;

    int animatingOffset;
    int animatingVelocity;
    long currentAnimatingTime;
    long lastAnimationTime;

    PagerAnimator() {
      final float density =
          getContext().getResources().getDisplayMetrics().density;
      velocity = (int) (VELOCITY * density + 0.5f);
    }

    void compute() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      lastAnimationTime = currentAnimatingTime;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
      animatingOffset = (int) (animatingVelocity * t);
    }

    void computeLeftAnimation() {
      compute();
      final int left = mCurView.getLeft();
      final int width = getWidth();
      if (left + animatingOffset <= -width) {
        animating = false;
        mCurView.offsetLeftAndRight(-width - left);
        mNextView.offsetLeftAndRight(-mNextView.getLeft());
        mCurTabView.transform(0f);
        mNextTabView.startExpand();
        mPendingPopulate = false;
        mIndicator.setSelectorPosition(mCurItem - left / (float) width);
        next();
        clear();
      } else {
        mCurView.offsetLeftAndRight(animatingOffset);
        mNextView.offsetLeftAndRight(animatingOffset);
        mPagerHandler.removeMessages(MSG_ANIMATE_LEFT);
        mPagerHandler.sendMessageAtTime(mPagerHandler.obtainMessage(MSG_ANIMATE_LEFT),
            currentAnimatingTime);
        mIndicator.setSelectorPosition(mCurItem - left / (float) width);
      }
      invalidate();
    }

    void computeLeftBackAnimation() {
      compute();
      final int left = mCurView.getLeft();
      final int width = getWidth();
      if (left + animatingOffset >= 0) {
        animating = false;
        mCurView.offsetLeftAndRight(-left);
        mNextView.offsetLeftAndRight(width - mNextView.getLeft());
        mCurTabView.startExpand();
        mNextTabView.transform(0f);
        mPendingPopulate = false;
        mIndicator.setSelectorPosition(mCurItem - left / (float) width);
        clear();
      } else {
        mCurView.offsetLeftAndRight(animatingOffset);
        mNextView.offsetLeftAndRight(animatingOffset);
        mPagerHandler.removeMessages(MSG_ANIMATE_LEFT_BACK);
        mPagerHandler.sendMessageAtTime(mPagerHandler.obtainMessage(MSG_ANIMATE_LEFT_BACK),
            currentAnimatingTime);
        mIndicator.setSelectorPosition(mCurItem - left / (float) width);
      }
      invalidate();
    }

    void computeRightAnimation() {
      compute();
      final int left = mCurView.getLeft();
      final int width = getWidth();
      if (left + animatingOffset >= width) {
        animating = false;
        mCurView.offsetLeftAndRight(width - left);
        mPreView.offsetLeftAndRight(-mPreView.getLeft());
        mCurTabView.transform(0f);
        mPreTabView.startExpand();
        mPendingPopulate = false;
        mIndicator.setSelectorPosition(mCurItem - left / (float) width);
        previous();
        clear();
      } else {
        mCurView.offsetLeftAndRight(animatingOffset);
        mPreView.offsetLeftAndRight(animatingOffset);
        mPagerHandler.removeMessages(MSG_ANIMATE_RIGHT);
        mPagerHandler.sendMessageAtTime(mPagerHandler.obtainMessage(MSG_ANIMATE_RIGHT),
            currentAnimatingTime);
        mIndicator.setSelectorPosition(mCurItem - left / (float) width);
      }
      invalidate();
    }

    void computeRightBackAnimation() {
      compute();
      final int left = mCurView.getLeft();
      if (left + animatingOffset <= 0) {
        animating = false;
        mCurView.offsetLeftAndRight(-left);
        mPreView.offsetLeftAndRight(-getWidth() - mPreView.getLeft());
        mCurTabView.startExpand();
        mPreTabView.transform(0f);
        mPendingPopulate = false;
        mIndicator.setSelectorPosition(
            mCurItem - mCurView.getLeft() / (float) getWidth());
        clear();
      } else {
        mCurView.offsetLeftAndRight(animatingOffset);
        mPreView.offsetLeftAndRight(animatingOffset);
        mPagerHandler.removeMessages(MSG_ANIMATE_RIGHT_BACK);
        mPagerHandler.sendMessageAtTime(mPagerHandler.obtainMessage(MSG_ANIMATE_RIGHT_BACK),
            currentAnimatingTime);
        mIndicator.setSelectorPosition(
            mCurItem - mCurView.getLeft() / (float) getWidth());
      }
      invalidate();
    }

    void animate(int velocity, int msg) {
      final long now = SystemClock.uptimeMillis();
      expanded = false;
      animating = true;
      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mPagerHandler.removeMessages(msg);
      mPagerHandler.sendMessageAtTime(mPagerHandler.obtainMessage(msg),
          currentAnimatingTime);
    }

    private void clear() {
      mPreView = null;
      mNextView = null;
      mPreTabView = null;
      mNextTabView = null;
    }
  }

  private class PagerHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_LEFT:
          mPagerAnimator.computeLeftAnimation();
          break;
        case MSG_ANIMATE_RIGHT:
          mPagerAnimator.computeRightAnimation();
          break;
        case MSG_ANIMATE_LEFT_BACK:
          mPagerAnimator.computeLeftBackAnimation();
          break;
        case MSG_ANIMATE_RIGHT_BACK:
          mPagerAnimator.computeRightBackAnimation();
          break;
        default:
          break;
      }
    }
  }

  private class OverviewTracker {
    private static final int MIN_HEIGHT = 5; // dips
    private static final int HEIGHT_PADDING = 50;

    private final int minHeight;
    private int maxHeight;
    private final int mHeightPadding;

    boolean verticalTracking;
    boolean horizontalTracking;

    OverviewTracker() {
      final float density = getResources().getDisplayMetrics().density;
      minHeight = (int) (MIN_HEIGHT * density + 0.5f);
      mHeightPadding = (int) (HEIGHT_PADDING * density + 0.5f);
    }

    void prepareVerticalTracking() {
      mVelocityTracker = VelocityTracker.obtain();
      mOverviewAnimator.stopFling();

      verticalTracking = true;
      maxHeight = (getHeight() - mHeightPadding) /
          (mInfos.size() == 1 ? 1 : mInfos.size() - 1);
    }

    void prepareHorizontalTracking() {
      mVelocityTracker = VelocityTracker.obtain();
      mOverviewAnimator.stopFling();

      horizontalTracking = true;
    }

    void stopVerticalTracking() {
//            Log.d(TAG, "@stopVerticalTracking");

      if (verticalTracking) {
        mVelocityTracker.computeCurrentVelocity(250);
        mOverviewAnimator.animateFling((int) mVelocityTracker.getYVelocity());
      }

      verticalTracking = false;
      horizontalTracking = false;

      if (mVelocityTracker != null) {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
      }
    }

    void stopHorizontalTracking() {
//            Log.d(TAG, "@stopHorizontalTracking");
      if (horizontalTracking) {
        mVelocityTracker.computeCurrentVelocity(250);
        final float xVelocity = mVelocityTracker.getXVelocity();
        if (xVelocity > 0) {
          mAdapter.remove(findFragment(mOverviewTouchedView));
        } else {
          mAdditionDeletionAnimator.animateOverviewDeletion(mOverviewTouchedView, (int) xVelocity);
        }
      }

      verticalTracking = false;
      horizontalTracking = false;

      if (mVelocityTracker != null) {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
      }
    }

    void verticalMove(int y) {
      View child;
      int index, top, indexedOffset;
      int offset = (y - mLastOffsetMoveY) / 2;
      index = 0;
      for (ItemInfo ii : mInfos) {
        child = getChildView(ii);
        top = child.getTop();
        indexedOffset = offset * index;
        if (offset < 0 && top + indexedOffset < minHeight * index) {
          child.offsetTopAndBottom(minHeight * index - top);
        } else if (offset > 0 && top + indexedOffset > maxHeight * index) {
          child.offsetTopAndBottom(maxHeight * index - top);
        } else {
          child.offsetTopAndBottom(indexedOffset);
        }
        index++;
      }

      invalidate();
    }

    void horizontalMove(int x) {
      int offset = (x - mLastOffsetMoveX);

      final int left = mOverviewTouchedView.getLeft();
      if (left + offset < 0) {
        mOverviewTouchedView.offsetLeftAndRight(-left);
      } else {
        final int width = getWidth();
        if (left + offset > width) {
          if (CAN_ALPHA) {
            mOverviewTouchedView.setAlpha(1f);
          }
          mOverviewTouchedView.offsetLeftAndRight(width - left);
        } else {
          if (CAN_ALPHA) {
            mOverviewTouchedView.setAlpha((float) (width - left) / width);
          }
          mOverviewTouchedView.offsetLeftAndRight(offset);
        }
      }
      invalidate();
    }
  }

  private class OverviewAnimator {
    static final int DECELERATION = 500;
    static final int MIN_VELOCITY = 800;
    static final int MIN_CHILD_VELOCITY = 1500;

    final int deceleration;
    final int minVelocity;
    final int minChildVelocity;

    boolean animating;

    float animatingDeceleration;
    float animatingVelocity;
    long lastAnimationTime;
    long currentAnimatingTime;

    View animatingChild;

    OverviewAnimator() {
      final float density = getResources().getDisplayMetrics().density;
      deceleration = (int) (DECELERATION * density + 0.5f);
      minVelocity = (int) (MIN_VELOCITY * density + 0.5f);
      minChildVelocity = (int) (MIN_CHILD_VELOCITY * density + 0.5f);
    }

    void computeFling() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      animatingVelocity += animatingDeceleration * t;
      final int offset = (int) (animatingVelocity * t);
//            Log.d(TAG, "@computeFling " + offset);
      View child;
      boolean end = true;
      int index = 0, top;

      if (offset == 0) {
        animating = false;
        return;
      }

      for (ItemInfo ii : mInfos) {
        child = getChildView(ii);
        top = child.getTop();
        if (offset < 0 &&
            top + offset * index < mOverviewTracker.minHeight * index) {
          child.offsetTopAndBottom(mOverviewTracker.minHeight * index - top);
        } else if (offset > 0 &&
            top + offset * index > mOverviewTracker.maxHeight * index) {
          child.offsetTopAndBottom(mOverviewTracker.maxHeight * index - top);
        } else {
          child.offsetTopAndBottom(offset * index);
          end = false;
        }
        index++;
      }

      if (end) {
        animating = false;
      } else {
        lastAnimationTime = now;
        currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
        mOverviewHandler.removeMessages(MSG_ANIMATE_DECELERATION);
        mOverviewHandler.sendMessageAtTime(
            mOverviewHandler.obtainMessage(MSG_ANIMATE_DECELERATION),
            currentAnimatingTime);
      }

      invalidate();
    }

    void animateFling(int velocity) {
//            Log.d(TAG, "@animateFling " + velocity);
      animating = true;
      animatingVelocity = velocity;
      if (velocity > 0) {
        animatingDeceleration = -deceleration;
      } else {
        animatingDeceleration = deceleration;
      }
      final long now = SystemClock.uptimeMillis();
      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
      mOverviewHandler.removeMessages(MSG_ANIMATE_DECELERATION);
      mOverviewHandler.sendMessageAtTime(mOverviewHandler.obtainMessage(MSG_ANIMATE_DECELERATION),
          currentAnimatingTime);
    }


    void computeChild() {
    }

    void animateChild(View child, int velocity) {
    }

    private void stopFling() {
      animatingVelocity = 0;
      animatingDeceleration = 0;
      animatingChild = null;
      mOverviewHandler.removeMessages(MSG_ANIMATE_DECELERATION);
    }
  }

  private class OverviewHandler extends Handler {
    static final int MSG_ANIMATE_CHILD = 11;

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_DECELERATION:
          mOverviewAnimator.computeFling();
          break;
        case MSG_ANIMATE_CHILD:
          mOverviewAnimator.computeChild();
          break;
      }
    }
  }

  private class SwitchAnimator {
    static final int VELOCITY = 1000;
    static final int DELAY = 100; // ms

    final int velocity;

    boolean animating;
    long lastAnimationTime;
    long currentAnimatingTime;
    float animatingOffset;
    float animatingVelocity;
    int count;
    int height;

    SwitchAnimator() {
      final float density = getResources().getDisplayMetrics().density;
      velocity = (int) (VELOCITY * density + 0.5);
    }

    private void compute() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      animatingOffset = animatingVelocity * t;
      lastAnimationTime = now;
      currentAnimatingTime = now + FRAME_ANIMATION_DURATION;
    }

    private void computeOverview() {
      compute();
      boolean end = true;
      View view;
      int target, top;
      int offset;

      for (int i = 0; i < count; i++) {
        view = mAdapter.getFragment(i).getView();
        target = i * height;
        top = view.getTop();
        if (i <= mCurItem) {
          offset = (int) (animatingOffset * (i + 1));
          if (top + offset >= target) {
            view.offsetTopAndBottom(target - top);
          } else {
            end = false;
            view.offsetTopAndBottom(offset);
          }
        } else {
          offset = (int) -animatingOffset * (count - i + 1);
          if (top + offset <= target) {
            view.offsetTopAndBottom(target - top);
          } else {
            end = false;
            view.offsetTopAndBottom(offset);
          }
        }
      }

      if (end) {
        animating = false;
        mMode = MODE_OVERVIEW;
        populate(true);
      } else {
        mSwitchHandler.removeMessages(MSG_ANIMATE_OVERVIEW);
        mSwitchHandler.sendMessageAtTime(mSwitchHandler.obtainMessage(MSG_ANIMATE_OVERVIEW),
            currentAnimatingTime);
      }
      invalidate();
    }

    private void computePager() {
      compute();
      boolean end = true;
      View view;
      int top;
      int offset;

      for (int i = 0; i < count; i++) {
        view = mAdapter.getFragment(i).getView();
        height = getHeight();
        top = view.getTop();
        if (i <= mCurItem) {
          offset = (int) (animatingOffset * (i + 1));
          if (top + offset <= 0) {
            view.offsetTopAndBottom(-top);
          } else {
            end = false;
            view.offsetTopAndBottom(offset);
          }
        } else {
          offset = (int) -animatingOffset * (count - i + 1);
          if (top + offset >= height) {
            view.offsetTopAndBottom(height - top);
          } else {
            end = false;
            view.offsetTopAndBottom(offset);
          }
        }
      }

      if (end) {
        animating = false;
        mMode = MODE_PAGER;
        populate(true);
        TabView tabView;
        int index = 0;
        for (ItemInfo ii : mInfos) {
          if (mCurItem == index) {
            continue;
          }
          tabView = getChildTabView(ii);
          tabView.transform(0f);
          index++;
        }
      } else {
        mSwitchHandler.removeMessages(MSG_ANIMATE_PAGER);
        mSwitchHandler.sendMessageAtTime(mSwitchHandler.obtainMessage(MSG_ANIMATE_PAGER),
            currentAnimatingTime);
      }
      invalidate();
    }

    void animateOverview() {
      count = mAdapter.getCount();
      if (count == 0) {
        return;
      }
      height = getHeight() / count;

      prepareOverview();

      mMode = MODE_SWITCHING;
      animating = true;
      animatingVelocity = velocity / count;
      final long now = SystemClock.uptimeMillis();
      lastAnimationTime = now + DELAY;
      currentAnimatingTime = lastAnimationTime + FRAME_ANIMATION_DURATION;

      mSwitchHandler.removeMessages(MSG_ANIMATE_OVERVIEW);
      mSwitchHandler.sendMessageAtTime(mSwitchHandler.obtainMessage(MSG_ANIMATE_OVERVIEW),
          currentAnimatingTime);
    }

    void animatePager() {
//            Log.d(TAG, "@animatePager");
      count = mAdapter.getCount();
      if (count == 0) {
        return;
      }

      if (mCurItem >= count) {
        mCurItem = count - 1;
      }

      TabView tabview = mAdapter.getFragment(mCurItem).getTabView();
      tabview.startExpand();

      mMode = MODE_SWITCHING;
      animating = true;
      animatingVelocity = -(velocity / count);
      lastAnimationTime = SystemClock.uptimeMillis();
      currentAnimatingTime = lastAnimationTime + FRAME_ANIMATION_DURATION;

      mSwitchHandler.removeMessages(MSG_ANIMATE_PAGER);
      mSwitchHandler.sendMessageAtTime(mSwitchHandler.obtainMessage(MSG_ANIMATE_PAGER),
          currentAnimatingTime);
    }

    private void prepareOverview() {
      if (count > 0) {
        View child;
        TabView tabView;
        final int width = getMeasuredWidth(), height = getMeasuredHeight();
        int i;
        for (i = 0; i <= mCurItem; i++) {
          child = mAdapter.getFragment(i).getView();
          if (child != null) {
            child.measure(
                width + MeasureSpec.EXACTLY, height + MeasureSpec.EXACTLY);
            child.layout(0, 0, width, height);
          }
        }

        for (i = mCurItem + 1; i < count; i++) {
          child = mAdapter.getFragment(i).getView();
          if (child != null) {
            child.measure(
                width + MeasureSpec.EXACTLY, height + MeasureSpec.EXACTLY);
            child.layout(0, height, width, height + height);
          }
        }

        for (i = 0; i < count; i++) {
          tabView = mAdapter.getFragment(i).getTabView();
          if (i == mCurItem) {
            tabView.startScale();
          } else {
            tabView.transform(1f);
          }
        }
      }

    }
  }

  private class SwitchHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ANIMATE_OVERVIEW:
          mSwitchAnimator.computeOverview();
          break;
        case MSG_ANIMATE_PAGER:
          mSwitchAnimator.computePager();
          break;
      }
    }
  }

  private class TabObserver extends DataSetObserver {
    @Override
    public void onChanged() {
      dataSetChanged();
    }

    @Override
    public void onInvalidated() {
      dataSetInvalidated();
    }
  }

  private void dataSetChanged() {
    populate(true);
  }

  private void dataSetInvalidated() {
    //populate(false);
  }

  @Override
  public void switchMode() {
    if (mMode == MODE_PAGER) {
      mSwitchAnimator.animateOverview();
    } else if (mMode == MODE_OVERVIEW) {
      mSwitchAnimator.animatePager();
    }
  }

  public void setAdapter(FakeChromeAdapter adapter) {
    if (mAdapter != null) {
      mAdapter.unregisterDataSetObserver(mTabObserver);
    }
    mAdapter = adapter;
    if (mTabObserver == null) {
      mTabObserver = new TabObserver();
    }
    mAdapter.registerDataSetObserver(mTabObserver);
    mCurItem = 0;
    populate(true);
  }

  public FakeChromeAdapter getAdapter() {
    return mAdapter;
  }

  @Override
  public int getCurrentIndex() {
    return mCurItem;
  }

  private void populate(boolean requestLayout) {
    if (mAdapter == null) {
      return;
    }

    if (mPendingPopulate) {
      return;
    }

    if (getWindowToken() == null) {
      return;
    }

    mAdapter.update(this);

    if (mMode == MODE_PAGER) {
      pagerPopulate();
    } else if (mMode == MODE_OVERVIEW) {
      overviewPopulate();
    }

    if (!mInLayout && requestLayout) {
      requestLayout();
      invalidate();
    }
  }

  private void pagerPopulate() {
//        Log.d(TAG, "@pagerPopulate");
    if (mAdapter.getCount() == 0) {
      mCur.fragment = null;
      mCur.position = -1;
      mPre.fragment = null;
      mPre.position = -1;
      mNext.fragment = null;
      mNext.position = -1;
    } else {
      if (mCurItem >= mAdapter.getCount()) {
        mCurItem = mAdapter.getCount() - 1;
      }

      mCur.fragment = mAdapter.getFragment(mCurItem);
      mCur.position = mCurItem;
      mCurView = getChildView(mCur);

      if (mCurItem == 0) {
        mPre.fragment = null;
        mPre.position = -1;
        mPreView = null;
      } else {
        mPre.fragment = mAdapter.getFragment(mCurItem - 1);
        mPre.position = mCurItem - 1;
        mPreView = getChildView(mPre);
      }

      if (mCurItem == mAdapter.getCount() - 1) {
        mNext.fragment = null;
        mNext.position = -1;
        mNextView = null;
      } else {
        mNext.fragment = mAdapter.getFragment(mCurItem + 1);
        mNext.position = mCurItem + 1;
        mNextView = getChildView(mNext);
      }

      if (mIndicator != null) {
        mIndicator.setCount(mAdapter.getCount());
      }
    }
  }

  private void overviewPopulate() {
    mInfos.clear();
    ItemInfo ii;
    final int count = mAdapter.getCount();
//        Log.d(TAG, "@overviewPopulate " + count);
    for (int i = 0; i < count; i++) {
      ii = new ItemInfo();
      ii.fragment = mAdapter.getFragment(i);
      ii.position = i;
      mInfos.add(ii);
    }
  }

  private void next() {
    if (mCurItem != mAdapter.getCount() - 1) {
      mCurItem += 1;
      populate(true);
    }
  }

  private void previous() {
    if (mCurItem != 0) {
      mCurItem -= 1;
      populate(true);
    }
  }

  private TabFragment findFragment(View view) {
    for (TabFragment f : mAdapter) {
      if (view.equals(f.getView())) {
        return f;
      }
    }
    return null;
  }

  private TabView getChildTabView(ItemInfo ii) {
    if (ii.fragment != null) {
      return ii.fragment.getTabView();
    }
    return null;
  }

  private View getChildView(ItemInfo ii) {
    if (ii.fragment != null) {
      return ii.fragment.getView();
    }
    return null;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    View child;
    if (mMode == MODE_PAGER) {
      child = getChildView(mCur);
      if (child != null) {
        measureChild(child, MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
      }
      child = getChildView(mPre);
      if (child != null) {
        measureChild(child, MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
      }
      child = getChildView(mNext);
      if (child != null) {
        measureChild(child, MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
      }
      if (mIndicator != null) {
        measureChild(mIndicator, MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST));
      }
    } else if (mMode == MODE_OVERVIEW) {
      for (ItemInfo ii : mInfos) {
        child = getChildView(ii);
        if (child != null) {
          measureChild(child, MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
        }
      }
    }

    setMeasuredDimension(widthSize, heightSize);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

//        Log.d(TAG, "@onLayout");

    mInLayout = true;
    populate(true);
    mInLayout = false;

    final int width = r - l;
    final int height = b - t;

    View child;

    if (mMode == MODE_PAGER) {
      child = getChildView(mCur);
      if (child != null && child.getLayoutParams() != null) {
        child.layout(0, 0, width, height);
      }

      child = getChildView(mPre);
      if (child != null && child.getLayoutParams() != null) {
        child.layout(-width, 0, 0, height);
      }

      child = getChildView(mNext);
      if (child != null && child.getLayoutParams() != null) {
        child.layout(width, 0, 2 * width, height);
      }

      if (mIndicator != null) {
        //Log.d(TAG, "@onLayout");
        final int indicatorWidth = mIndicator.getMeasuredWidth();
        final int indicatorHeight = mIndicator.getMeasuredHeight();
        mIndicator.layout(
            (width - indicatorWidth) / 2, height - indicatorHeight,
            (width + indicatorWidth) / 2, height);
      }
    } else if (mMode == MODE_OVERVIEW) {
      final int count = mInfos.size();
      if (count == 0) {
        return;
      }
      final int perHeight = height / count;

      for (int i = 0; i < count; i++) {
        ItemInfo info = mInfos.get(i);
        child = getChildView(info);
        if (child != null && child.getLayoutParams() != null) {
          child.layout(l,
              i * perHeight, r, i * perHeight + child.getMeasuredHeight());
        }
      }
    }
  }


  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    // Skip any touch event dispatching when animating or adapter is empty
    return mAdapter == null || mAdapter.getCount() == 0 ||
        mPagerAnimator.animating ||
        mOverviewAnimator.animating || mSwitchAnimator.animating ||
        mOverviewLayoutAnimator.animating ||
        mAdditionDeletionAnimator.animating ||
        super.dispatchTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int x = (int) ev.getX();
    final int y = (int) ev.getY();
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    if (mMode == MODE_PAGER) {
      switch (action) {
        case MotionEvent.ACTION_DOWN:
          mEventMoved = false;
          mLastDownX = x;
          break;
        case MotionEvent.ACTION_MOVE:
          if (!mPagerTracker.tracking &&
              (x < mLastDownX - mTapThreshold ||
                  x > mLastDownX + mTapThreshold)) {
            // To ensure the offset only be set once.
            mLastOffsetDownX = x;
            mPagerTracker.prepareTracking();
          }
          if (mPagerTracker.tracking) {
            mVelocityTracker.addMovement(ev);
          }
          mPagerTracker.move(x);
          mLastOffsetMoveX = x;
          break;
      }
    } else if (mMode == MODE_OVERVIEW) {
      switch (action) {
        case MotionEvent.ACTION_DOWN:
          //Log.d(TAG, "@onInterceptTouchEvent");
          mEventMoved = false;
          mVelocityTracker = VelocityTracker.obtain();
          mLastDownX = x;
          mLastDownY = y;

          View child;
          final int count = mInfos.size();
          //Log.d(TAG, "@onInterceptTouchEvent " + count);
          for (int i = count - 1; i >= 0; i--) {
            //Log.d(TAG, "@onInterceptTouchEvent for child");
            child = getChildView(mInfos.get(i));
            if (child != null) {
              child.getHitRect(mOverviewTouchedChildFrame);
              if (mOverviewTouchedChildFrame.contains(x, y)) {
                mCurItem = i;
                mOverviewTouchedView = child;
                break;
              }
            }
          }
          return true;
      }
    }
    return mOverviewTracker.verticalTracking || mPagerTracker.tracking;
  }


  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    final int x = (int) ev.getX();
    final int y = (int) ev.getY();
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    if (mMode == MODE_PAGER) {
      switch (action) {
        case MotionEvent.ACTION_MOVE:
          mVelocityTracker.addMovement(ev);
          mPagerTracker.move(x);
          mLastOffsetMoveX = x;
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          mPagerTracker.stopTracking();
          break;
      }
    } else if (mMode == MODE_OVERVIEW) {
      switch (action) {
        case MotionEvent.ACTION_MOVE:
          if (!mOverviewTracker.verticalTracking &&
              !mOverviewTracker.horizontalTracking) {
            if ((y < mLastDownY - mTapThreshold ||
                y > mLastDownY + mTapThreshold)) {
              mEventMoved = true;
              mLastOffsetDownY = y;
              mOverviewTracker.prepareVerticalTracking();
            } else if ((x < mLastDownX - mTapThreshold ||
                x > mLastDownX + mTapThreshold)) {
              mLastOffsetDownX = x;
              mEventMoved = true;
              mOverviewTracker.prepareHorizontalTracking();
            }
          }

          mVelocityTracker.addMovement(ev);

          if (mOverviewTracker.verticalTracking) {
            mOverviewTracker.verticalMove(y);
          } else if (mOverviewTracker.horizontalTracking) {
            mOverviewTracker.horizontalMove(x);
          }

          mLastOffsetMoveX = x;
          mLastOffsetMoveY = y;
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if (!mEventMoved && x < mLastDownX + mTapThreshold &&
              x > mLastDownX - mTapThreshold &&
              y < mLastDownY + mTapThreshold &&
              y > mLastDownY - mTapThreshold) {
            mSwitchAnimator.animatePager();
            return true;
          }
          if (mOverviewTracker.verticalTracking) {
            mOverviewTracker.stopVerticalTracking();
            mOverviewTouchedView = null;
            return false;
          } else if (mOverviewTracker.horizontalTracking) {
            mOverviewTracker.stopHorizontalTracking();
            mOverviewTouchedView = null;
          }
      }
    }
    return true;
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    final long drawingTime = getDrawingTime();
    if (mMode == MODE_PAGER) {
      if (mPreView != null) {
        drawChild(canvas, mPreView, drawingTime);
      }
      if (mNextView != null) {
        drawChild(canvas, mNextView, drawingTime);
      }
      if (mCurView != null) {
//                Log.d(TAG, "@dispatchDraw");
        drawChild(canvas, mCurView, drawingTime);
      }
    } else {
      View child;
      for (int i = 0, count = getChildCount(); i < count; i++) {
        child = getChildAt(i);
        if (child != null) {
          if (child.equals(mIndicator)) {
            continue;
          }
          drawChild(canvas, child, drawingTime);
        }
      }
    }

    if (mIndicator != null &&
        (mPagerAnimator.animating || mPagerTracker.tracking)) {
      drawChild(canvas, mIndicator, getDrawingTime());
    }
  }
}
