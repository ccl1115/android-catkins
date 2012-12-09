package com.bbsimon.android.demo.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import com.bbsimon.android.demo.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 滑动控件
 *
 * @author simon
 */
@SuppressWarnings("unused")
public class FlipperLayout extends ViewGroup {

  private static final String TAG = "FlipperLayout";

  public static final int SHADOW_WIDTH = 4;
  public static final int RIGHT_OFFSET = 270;
  public static final int LEFT_OFFSET = 270;
  public static final int STATE_COLLAPSED_LEFT = -1000;
  public static final int STATE_EXPANDED = -1001;
  public static final int STATE_COLLAPSED_RIGHT = -1002;

  private static final int TAP_THRESHOLD = 15;
  private static final float MAXIMUM_TAP_VELOCITY = 200f;
  private static final float MAXIMUM_MINOR_VELOCITY = 900f;
  private static final float MAXIMUM_MAJOR_VELOCITY = 1000f;
  private static final float BOUNCE_ACCELERATION = -15000f;
  private static final int VELOCITY_UNITS = 350;
  private static final int MSG_FLIP_RIGHT_ANIMATE = 1000;
  private static final int MSG_BOUNCE_ANIMATE = 1001;
  private static final int MSG_FLIP_LEFT_ANIMATE = 1002;
  private static final int ANIMATION_FRAME_DURATION = 1000 / 60;
  private static final int HANDLER_WIDTH = 60;
  private static final int HANDLER_OVER_SCROLL_WIDTH = 40;

  private static final int EXPANDED_FULL_OPEN = -10001;
  private static final int COLLAPSED_FULL_RIGHT_CLOSED = -10002;
  private static final int COLLAPSED_FULL_LEFT_CLOSED = -10003;

  /**
   * the right offset
   */
  private int mRightOffset;
  private int mLeftOffset;

  private int mContentId;
  private int mHeadId;
  private int mHandleId;

  private View mHead;
  private View mHandle;
  private View mContent;
  private final Rect mFrame = new Rect();
  private final Rect mFrame2 = new Rect();
  private Rect mIndicator = new Rect();

  private VelocityTracker mVelocityTracker;

  private int mState = STATE_EXPANDED;
  private boolean mTracking;
  private boolean mAnimating = false;
  private boolean mLocked;

  private List<OnOpenListener> mOnOpenListeners = new ArrayList<OnOpenListener>();
  private OnRightListener mOnRightListener;
  private OnLeftListener mOnLeftListener;
  private OnScrollListener mOnScrollListener;
  private OnSizeChanged mOnSizeChangedListener;

  private final Handler mHandler = new SlidingHandler();

  /**
   * DPI relative
   */
  private final int mTapThreshold;
  private final int mMaximumTapVelocity;
  private final int mMaximumMinorVelocity;
  private final int mMaximumMajorVelocity;
  private final int mBounceAcceleration;
  private final int mVelocityUnits;
  private final int mShadowWidth;
  private final int mHandlerWidth;
  private final int mHandlerOverScrollWidth;

  private int mTouchDelta = 0;
  private Paint mIndicatorPaint;
  private Drawable mShadow;

  private float mAnimatedVelocity;
  private float mAnimationPosition;
  private long mAnimationLastTime;
  private long mCurrentAnimationTime;
  private int mHeadLeft;
  private boolean mDispatchedToContent;

  private int mFlipperLeftOffset;
  private int mFlipperRightOffset;

  public FlipperLayout(Context context) {
    this(context, null, 0);
  }

  public FlipperLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FlipperLayout(Context context, AttributeSet attrs, int style) {
    super(context, attrs, style);
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlipperLayout, style, 0);

    int contentId = a.getResourceId(R.styleable.FlipperLayout_content, 0);
    if (contentId == 0) {
      throw new IllegalArgumentException("The content must be set. It's required.");
    }

    int headId = a.getResourceId(R.styleable.FlipperLayout_head, 0);
    if (headId == 0) {
      throw new IllegalArgumentException("The head must be set. It's required.");
    }

    int handleId = a.getResourceId(R.styleable.FlipperLayout_handle, 0);
    if (handleId == 0) {
      throw new IllegalArgumentException("The handle must be set. It's required.");
    }

    mHeadId = headId;
    mHandleId = handleId;
    mContentId = contentId;

    /** Compute density relative values */
    final float density = getResources().getDisplayMetrics().density;
    mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
    mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
    mMaximumTapVelocity = (int) (MAXIMUM_TAP_VELOCITY * density + 0.5f);
    mTapThreshold = (int) (TAP_THRESHOLD * density + 0.5f);
    mVelocityUnits = (int) (VELOCITY_UNITS * density + 0.5f);
    mShadowWidth = (int) (SHADOW_WIDTH * density + 0.5f);
    mBounceAcceleration = (int) (BOUNCE_ACCELERATION * density + 0.5);
    mHandlerWidth = (int) (HANDLER_WIDTH * density + 0.5);
    mHandlerOverScrollWidth = (int) (HANDLER_OVER_SCROLL_WIDTH * density + 0.5);
    mFlipperLeftOffset = (int) (LEFT_OFFSET * density + 0.5);
    mFlipperRightOffset = (int) (RIGHT_OFFSET * density + 0.5);

    a.recycle();

    mIndicatorPaint = new Paint();
    mIndicatorPaint.setColor(0xFFFFFFFF);

    mShadow = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
        new int[]{0xFF000000, 0x00FFFFFF});

    setAlwaysDrawnWithCacheEnabled(false);

  }

  public int getState() {
    return mState;
  }

  public boolean isAnimating() {
    return mAnimating;
  }


  public void animateLeft() {
    if (mAnimating) {
      return;
    }
    prepareContent();
    if (mOnLeftListener != null) {
      mOnLeftListener.onLeftStart();
    }

    animateLeft(mHead.getLeft());
    sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
  }

  /**
   * Close with animation
   *
   * @see #close()
   * @see #open()
   * @see #animateOpen()
   */
  public void animateClose() {
    if (mAnimating) {
      return;
    }
    prepareContent();
    final OnScrollListener listener = mOnScrollListener;
    if (listener != null) {
      listener.onFlipperScrollStarted();
    }

    animateRight(mHead.getLeft());
    sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
  }

  /**
   * Open with animation
   *
   * @see #close()
   * @see #open()
   * @see #animateClose()
   */
  public void animateOpen() {
    if (mAnimating) {
      return;
    }
    prepareContent();
    if (mOnOpenListeners != null && !mOnOpenListeners.isEmpty()) {
      for (OnOpenListener listener : mOnOpenListeners) {
        listener.onOpenStart();
      }
    }
    animateOpen(mHead.getLeft());
    sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
  }

  /**
   * Close immediately
   *
   * @see #open()
   * @see #animateClose()
   * @see #animateOpen()
   */
  public void close() {
    flipperRight();
  }

  /** public methods and interfaces for external invoke */

  /**
   * Open immediately
   *
   * @see #close()
   * @see #animateClose()
   * @see #animateOpen()
   */
  public void open() {
    flipperOpen();
  }

  /**
   * Lock Flipper
   */
  public void lock() {
    mLocked = true;
  }

  /**
   * Unlock it
   */
  public void unlock() {
    mLocked = false;
  }

  /**
   * get the toggle view instance
   *
   * @return the view
   */
  public View getHandle() {
    return mHandle;
  }

  public View getContent() {
    return mContent;
  }

  public void animateToggle() {
    if (mState == STATE_EXPANDED) {
      animateClose();
    } else {
      animateOpen();
    }
  }


  /**
   * Close Flipper at a position with animation
   *
   * @param position start position
   */
  private void animateRight(int position) {
    prepareTracking(position);
    performRightFling(position, mAnimatedVelocity, true);
  }

  /**
   * Open Flipper at a position with animation
   *
   * @param position start position
   */
  private void animateOpen(int position) {
    if (mState == STATE_COLLAPSED_RIGHT) {
      prepareTracking(position);
      performRightFling(position, mAnimatedVelocity, true);
    } else if (mState == STATE_COLLAPSED_LEFT) {
      performLeftFling(position, mAnimatedVelocity, true);
    }
  }

  private void animateLeft(int position) {
    performLeftFling(position, mAnimatedVelocity, true);
  }

  /**
   * Prepare tracking at a position
   *
   * @param position at position
   */
  private void prepareTracking(int position) {
    mTracking = true;
    mVelocityTracker = VelocityTracker.obtain();
    final int opening = mState;
    if (opening == STATE_EXPANDED) {
      mAnimatedVelocity = mMaximumMajorVelocity;
      mAnimationPosition = 0;
      moveHead((int) mAnimationPosition, false);
    } else {
      if (mAnimating) {
        mAnimating = false;
      }
      if (opening == STATE_COLLAPSED_LEFT) {
        mAnimatedVelocity = mMaximumMajorVelocity;
      } else {
        mAnimatedVelocity = -mMaximumMajorVelocity;
      }
      moveHead(position, false);
    }
    mHandler.removeMessages(MSG_FLIP_RIGHT_ANIMATE);
    mHandler.removeMessages(MSG_FLIP_LEFT_ANIMATE);
    long now = SystemClock.currentThreadTimeMillis();
    mAnimationLastTime = now;
    mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
    mAnimating = true;
  }

  private void stopTracking() {
    mTracking = false;

    if (mOnScrollListener != null) {
      mOnScrollListener.onFlipperScrollEnded();
    }

    if (mVelocityTracker != null) {
      mVelocityTracker.recycle();
      mVelocityTracker = null;
    }
  }

  /**
   * prepareContent
   */
  private void prepareContent() {

    if (mAnimating) {
      return;
    }

    final View content = mContent;
    if (content.isLayoutRequested()) {
      final int headHeight = mHead.getMeasuredHeight();
      measureChild(content,
          MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(getHeight() - headHeight, MeasureSpec.EXACTLY));
      content.layout(0,
          headHeight,
          content.getMeasuredWidth(),
          headHeight + content.getMeasuredHeight());
    }

    //This snippet will work on multiple platform with drawing cache and hardware accelerated
    try {
      Method m = content.getClass().getMethod("isHardwareAccelerated");
      Boolean b = (Boolean) m.invoke(content, (Object[]) null);
      if (!b) {
        Log.d(TAG, "@prepareContent cached");
        content.buildDrawingCache();
      } else {
        Log.d(TAG, "@prepareContent accelerated");
      }
    } catch (NoSuchMethodException e) {
      Log.d(TAG, "@prepareContent cached");
      content.buildDrawingCache();
    } catch (InvocationTargetException ignored) {
    } catch (IllegalAccessException ignored) {
    }
  }

  /**
   * Fling Flipper to right with a initial velocity
   *
   * @param position where to start
   * @param velocity initial speed
   * @param always   ignore threshold
   */
  private void performRightFling(int position, float velocity, boolean always) {
    mAnimationPosition = position;
    mAnimatedVelocity = velocity;

    if (position > getWidth() - mRightOffset) {
      long now = SystemClock.uptimeMillis();
      mAnimatedVelocity = 0;
      mAnimationLastTime = now;
      mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
      mAnimating = true;
      mHandler.removeMessages(MSG_BOUNCE_ANIMATE);
      mHandler.sendMessage(mHandler.obtainMessage(MSG_BOUNCE_ANIMATE));
      stopTracking();
      return;
    }

    if (mState == STATE_EXPANDED) {
      if (always || velocity > -mMaximumTapVelocity ||
          position > (getWidth() - mRightOffset) / 4.0) {
        //force right
        if (mAnimatedVelocity < 0) {
          mAnimatedVelocity = -mMaximumMajorVelocity;
        }
      } else {
        //back left
        if (mAnimatedVelocity > 0) {
          mAnimatedVelocity = -mMaximumMajorVelocity;
        }
      }
    } else if (mState == STATE_COLLAPSED_RIGHT) {
      if (!always && (velocity > mMaximumTapVelocity ||
          (position > getWidth() - mRightOffset))) {
        //back right
        if (mAnimatedVelocity < 0) {
          mAnimatedVelocity = -mAnimatedVelocity;
        }
      } else {
        //force left
        if (mAnimatedVelocity > 0) {
          mAnimatedVelocity = -mMaximumMajorVelocity;
        }
      }
    } else if (mState == STATE_COLLAPSED_LEFT) {
      return;
    }

    long now = SystemClock.uptimeMillis();
    mAnimationLastTime = now;
    mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
    mAnimating = true;
    mHandler.removeMessages(MSG_FLIP_RIGHT_ANIMATE);
    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_FLIP_RIGHT_ANIMATE), mCurrentAnimationTime);
    stopTracking();
  }

  private void performLeftFling(int position, float velocity, boolean always) {
    mAnimationPosition = position;
    mAnimatedVelocity = velocity;

    if (mState == STATE_EXPANDED) {
      if (always) {
        //force left
        if (mAnimatedVelocity > 0) {
          mAnimatedVelocity = -mMaximumMajorVelocity;
        }
      } else {
        //back right
        if (mAnimatedVelocity < 0) {
          mAnimatedVelocity = mMaximumMajorVelocity;
        }
      }
    } else if (mState == STATE_COLLAPSED_LEFT) {
      if (!always) {
        //back left
        if (mAnimatedVelocity > 0) {
          mAnimatedVelocity = -mMaximumMajorVelocity;
        }
      } else {
        //force right
        if (mAnimatedVelocity < 0) {
          mAnimatedVelocity = mMaximumMajorVelocity;
        }
      }
    } else if (mState == STATE_COLLAPSED_RIGHT) {
      return;
    }

    long now = SystemClock.uptimeMillis();
    mAnimationLastTime = now;
    mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
    mAnimating = true;
    mHandler.removeMessages(MSG_FLIP_LEFT_ANIMATE);
    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_FLIP_LEFT_ANIMATE), mCurrentAnimationTime);
  }

  /**
   * Move Flipper at a position immediately
   *
   * @param position the position
   */
  private void moveHead(int position, boolean toLeft) {
    final View head = mHead;

    if (position == COLLAPSED_FULL_RIGHT_CLOSED) {
      head.offsetLeftAndRight(getWidth() - head.getLeft() - mRightOffset);
      invalidate();
    } else if (position == EXPANDED_FULL_OPEN) {
      head.offsetLeftAndRight(-head.getLeft());
      invalidate();
    } else if (position == COLLAPSED_FULL_LEFT_CLOSED) {
      head.offsetLeftAndRight(-getWidth() - head.getLeft() + mLeftOffset);
      invalidate();
    } else {
      final int left = head.getLeft();
      int deltaX = position - left;
      if (!toLeft && position < 0) {
        deltaX = 0;
      } else if (position > getWidth() - mRightOffset + mHandlerOverScrollWidth) {
        deltaX = 0;
      }

      if (deltaX != 0) {
        head.offsetLeftAndRight(deltaX);
        invalidate();
      }
    }
  }

  /**
   * collapse to left immediately
   */
  private void flipperLeft() {
    moveHead(COLLAPSED_FULL_LEFT_CLOSED, false);

    if (mOnLeftListener != null) {
      mOnLeftListener.onLeftEnd();
    }

    if (mState == STATE_COLLAPSED_LEFT) {
      return;
    }

    mState = STATE_COLLAPSED_LEFT;
  }

  /**
   * close immediately
   */
  private void flipperRight() {
    moveHead(COLLAPSED_FULL_RIGHT_CLOSED, false);

    if (mOnRightListener != null) {
      mOnRightListener.onRightEnd();
    }

    if (mState == STATE_COLLAPSED_RIGHT) {
      return;
    }

    mState = STATE_COLLAPSED_RIGHT;
  }

  /**
   * open immediately
   */
  private void flipperOpen() {
    moveHead(EXPANDED_FULL_OPEN, false);

    final List<OnOpenListener> listeners = mOnOpenListeners;
    if (listeners != null && !listeners.isEmpty()) {
      for (OnOpenListener listener : listeners) {
        listener.onOpenEnd();
      }
    }

    if (mState == STATE_EXPANDED) {
      return;
    }

    mState = STATE_EXPANDED;
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    final long drawingTime = getDrawingTime();
    final View content = mContent;
    final View head = mHead;

    drawChild(canvas, head, drawingTime);


    canvas.save();
    canvas.translate(head.getLeft(), 0);
    if (mTracking || mAnimating) {
      final Bitmap cache = content.getDrawingCache();
      if (cache != null) {
        canvas.drawRect(0, content.getTop(), getWidth(), getHeight(), mIndicatorPaint);
        canvas.drawBitmap(cache, 0, head.getBottom(), null);
      } else {
        drawChild(canvas, content, drawingTime);
      }
    } else {
      drawChild(canvas, content, drawingTime);
    }
    canvas.save();
    canvas.translate(-mShadowWidth, 0);
    mShadow.draw(canvas);
    canvas.restore();


  }

  /**
   * doRightAnimation
   */
  private void doRightAnimation() {
    if (mAnimating) {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - mAnimationLastTime) / 1000.0f;
      mAnimationPosition = mAnimationPosition + (mAnimatedVelocity * t);
      mAnimationLastTime = now;
      if (mAnimationPosition >= getWidth() - mRightOffset) {
        mAnimating = false;
        flipperRight();
      } else if (mAnimationPosition < 0) {
        mAnimating = false;
        flipperOpen();
      } else {
        moveHead((int) mAnimationPosition, false);
        mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_FLIP_RIGHT_ANIMATE),
            mCurrentAnimationTime);
      }
    }
  }

  /**
   * doLeftAnimation
   */
  private void doLeftAnimation() {
    if (mAnimating) {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - mAnimationLastTime) / 1000.0f;
      mAnimationPosition = mAnimationPosition + (mAnimatedVelocity * t);
      mAnimationLastTime = now;
      if (mAnimationPosition <= -getWidth() + mLeftOffset) {
        mAnimating = false;
        flipperLeft();
      } else if (mAnimationPosition >= 0) {
        mAnimating = false;
        flipperOpen();
      } else {
        moveHead((int) mAnimationPosition, true);
        mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_FLIP_LEFT_ANIMATE), mCurrentAnimationTime);
      }
    }
  }

  private void doBounceAnimation() {
    if (mAnimating) {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - mAnimationLastTime) / 1000f;
      final float a = mBounceAcceleration;
      final float v = mAnimatedVelocity + a * t;
      mAnimatedVelocity = v;
      mAnimationPosition = mAnimationPosition + v * t;
      mAnimationLastTime = now;
      if (mAnimationPosition < getWidth() - mRightOffset) {
        mAnimating = false;
        flipperRight();
      } else {
        moveHead((int) mAnimationPosition, false);
        mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_BOUNCE_ANIMATE), mCurrentAnimationTime);
      }
    }
  }

  @Override
  protected void onFinishInflate() {
    mHead = findViewById(mHeadId);
    if (mHead == null) {
      throw new IllegalArgumentException("The view that head id referred doesn't exist.");
    }
    mContent = findViewById(mContentId);

    if (mContent == null) {
      throw new IllegalArgumentException("the view that content id referred doesn't exist.");
    }
    mHandle = findViewById(mHandleId);

    if (mHandle == null) {
      throw new IllegalArgumentException("The view that handle id referred doesn't exist.");
    }

    mHandle.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        animateToggle();
      }
    });
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int width = r - l;

    mRightOffset = width - mFlipperLeftOffset;
    mLeftOffset = width - mFlipperRightOffset;

    final View head = mHead;

    int childLeft;
    int childTop;

    final int headHeight = head.getMeasuredHeight();
    childLeft = head.getLeft();
    childTop = 0;
    head.layout(childLeft, childTop, childLeft + width, headHeight);
    final View content = mContent;

    childTop += headHeight;
    content.layout(0, childTop, r, b);

    mShadow.setBounds(l, t, l + mShadowWidth, b);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (mOnSizeChangedListener != null) {
      mOnSizeChangedListener.onSizeChanged(w, h);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

    int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

    /** mHead */
    measureChild(mHead,
        MeasureSpec.makeMeasureSpec(widthSpecSize, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.AT_MOST));

    /** mContent */
    measureChild(mContent,
        MeasureSpec.makeMeasureSpec(widthSpecSize, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(heightSpecSize - mHead.getMeasuredHeight(),
            MeasureSpec.EXACTLY));

    setMeasuredDimension(widthSpecSize, heightSpecSize);
  }

  @Override
  public boolean isOpaque() {
    return true;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    final int x = (int) event.getX();
    final int y = (int) event.getY();
    final int action = event.getAction() & MotionEvent.ACTION_MASK;
    final int left = mHead.getLeft();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        if (mState == STATE_COLLAPSED_RIGHT) {
          mFrame2.set(left, mHead.getBottom(), getWidth(), getHeight());
          mHandle.getHitRect(mFrame);
          mFrame.offset(getWidth() - mRightOffset, 0);
        } else {
          mFrame2.set(left, mHead.getBottom(), left + mHandlerWidth, getHeight());
          mHead.getHitRect(mFrame);
        }
        if (!mFrame.contains(x, y) && !mFrame2.contains(x, y)) {
          mDispatchedToContent = true;
          event.offsetLocation(0, -mHead.getHeight());
          return mContent.dispatchTouchEvent(event);
        } else {
          mDispatchedToContent = false;
          return super.dispatchTouchEvent(event);
        }
      default:
        if (mDispatchedToContent) {
          mDispatchedToContent = true;
          event.offsetLocation(0, -mHead.getHeight());
          return mContent.dispatchTouchEvent(event);
        } else {
          mDispatchedToContent = false;
          return super.dispatchTouchEvent(event);
        }
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mLocked) {
      return false;
    }

    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    final float x = ev.getX();
    final float y = ev.getY();

    final View head = mHead;
    final Rect frame = mFrame;
    final int left = head.getLeft();

    head.getHitRect(frame);
    final int top = head.getBottom();
    final int bottom = getBottom();
    if (mState == STATE_COLLAPSED_RIGHT) {
      mIndicator.set(left, top, getWidth(), bottom);
    } else if (mState == STATE_EXPANDED) {
      mIndicator.set(0, top, mHandlerWidth, bottom);
    } else if (mState == STATE_COLLAPSED_LEFT) {
      mIndicator.set(head.getRight() - mLeftOffset / 2, top, head.getRight(), bottom);
    }
    if (!mTracking && !frame.contains((int) x, (int) y) &&
        !mIndicator.contains((int) x, (int) y)) {
      // not in the interception area, skip interception checking and dispatch to content view
      return false;
    }


    if ((mState != STATE_EXPANDED && mIndicator.contains((int) x, (int) y)) ||
        action == MotionEvent.ACTION_MOVE &&
            Math.abs(x - mHeadLeft - mTouchDelta) > mTapThreshold) {
      // intercepting until there is enough movement
      if (mOnScrollListener != null) {
        mOnScrollListener.onFlipperScrollStarted();
      }
      mTracking = true;
      prepareContent();
      prepareTracking(left);
      mVelocityTracker.addMovement(ev);
      mTouchDelta = (int) x - left;
      mHeadLeft = left;
      return true;
    }

    if (mAnimating) {
      return false;
    }

    if (action == MotionEvent.ACTION_DOWN) {
      mTouchDelta = (int) x - left;
      mHeadLeft = left;

    }
    return false;
  }


  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mLocked) {
      return false;
    }

    if (mTracking) {
      if (mVelocityTracker == null) {
        mVelocityTracker = VelocityTracker.obtain();
      }
      mVelocityTracker.addMovement(event);
      final int action = event.getAction();
      switch (action) {
        case MotionEvent.ACTION_MOVE:
          moveHead((int) event.getX() - mTouchDelta, false);
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          if (!(mHead.getLeft() != mHeadLeft) && mState == STATE_COLLAPSED_RIGHT) {
            mAnimating = false;
            animateOpen();
            return true;
          }
          final VelocityTracker velocityTracker = mVelocityTracker;
          velocityTracker.computeCurrentVelocity(mVelocityUnits);

          float velocity = velocityTracker.getXVelocity();
          boolean negative = false;
          if (velocity < 0) {
            negative = true;
          }

          if (Math.abs(velocity) < mMaximumMinorVelocity) {
            velocity = negative ? -mMaximumMinorVelocity : mMaximumMinorVelocity;
          }

          final int left = mHead.getLeft();

          if (Math.abs(velocity) > mMaximumMinorVelocity) {
            performRightFling(left, velocity, false);
          } else {
            performRightFling(left, velocity, false);
          }
          break;

      }
    }

    return mTracking || mAnimating;
  }


  public void setOnRightListener(OnRightListener listener) {
    mOnRightListener = listener;
  }

  public void addOnOpenListener(OnOpenListener listener) {
    mOnOpenListeners.add(listener);
  }

  public void removeOnOpenListener(OnOpenListener listener) {
    mOnOpenListeners.remove(listener);
  }

  public void setOnLeftListener(OnLeftListener listener) {
    mOnLeftListener = listener;
  }

  public void setOnFlipperScrollListener(OnScrollListener listener) {
    mOnScrollListener = listener;
  }

  public void setOnSizeChanged(OnSizeChanged listener) {
    mOnSizeChangedListener = listener;
  }

  public int getRightOffset() {
    return mRightOffset;
  }

  private class SlidingHandler extends Handler {
    public void handleMessage(Message m) {
      switch (m.what) {
        case MSG_FLIP_RIGHT_ANIMATE:
          doRightAnimation();
          break;
        case MSG_FLIP_LEFT_ANIMATE:
          doLeftAnimation();
          break;
        case MSG_BOUNCE_ANIMATE:
          doBounceAnimation();
          break;
      }
    }
  }


  /**
   * Call when flipper open
   */
  public interface OnOpenListener {
    void onOpenStart();

    void onOpenEnd();
  }


  /**
   * Call when flip right
   */
  public interface OnRightListener {
    void onRightEnd();
  }

  /**
   * Call when flip left
   */
  public interface OnLeftListener {
    void onLeftStart();

    void onLeftEnd();
  }

  /**
   * Call when scroll started or ended
   */
  public interface OnScrollListener {
    void onFlipperScrollStarted();

    void onFlipperScrollEnded();
  }

  public interface OnSizeChanged {
    void onSizeChanged(int width, int height);
  }
}
