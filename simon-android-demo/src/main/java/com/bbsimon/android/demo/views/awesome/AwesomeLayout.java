/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.views.awesome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.FrameLayout;
import com.bbsimon.android.demo.R;
import de.akquinet.android.androlog.Log;

@SuppressWarnings("unused")
public class AwesomeLayout extends FrameLayout {

  public static final int STATE_COLLAPSE_LEFT = 10000;
  public static final int STATE_COLLAPSE_RIGHT = 10001;
  public static final int STATE_COLLAPSE_TOP = 10002;
  public static final int STATE_COLLAPSE_BOTTOM = 10003;
  public static final int STATE_EXPAND = 10004;
  public static final String LEFT = "left";
  public static final String RIGHT = "right";
  public static final String TOP = "top";
  public static final String BOTTOM = "bottom";
  public static final String HORIZONTAL = "horizontal";
  public static final String VERTICAL = "vertical";
  private static final String TAG = "AwesomeLayout";

  private final static int MSG_ANIMATE_LEFT = -100;
  private final static int MSG_ANIMATE_RIGHT = -101;
  private final static int MSG_ANIMATE_TOP = -102;
  private final static int MSG_ANIMATE_BOTTOM = -103;
  private final static int MSG_ANIMATE_LEFT_OPEN = -104;
  private final static int MSG_ANIMATE_RIGHT_OPEN = -105;
  private final static int MSG_ANIMATE_TOP_OPEN = -106;
  private final static int MSG_ANIMATE_BOTTOM_OPEN = -107;

  private final static int TRACK_DIRECTION_LEFT_MASK = 0x10 << 2;
  private final static int TRACK_DIRECTION_RIGHT_MASK = 0x10 << 1;
  private final static int TRACK_DIRECTION_HORIZONTAL_MASK = 0x10;
  private final static int TRACK_DIRECTION_TOP_MASK = 0x01 << 2;
  private final static int TRACK_DIRECTION_BOTTOM_MASK = 0x01 << 1;
  private final static int TRACK_DIRECTION_VERTICAL_MASK = 0x01;

  private final static int TAP_THRESHOLD = 25;

  private Context mContext;

  private float mLeftOffset;
  private float mRightOffset;
  private float mTopOffset;
  private float mBottomOffset;

  private final int mTouchEventThreshold;

  private boolean mLeftTapBack;
  private boolean mRightTapBack;
  private boolean mTopTapBack;
  private boolean mBottomTapBack;

  private int mTrackDirection = 0x00;

  private int mState;

  private Rect mLeftFrameForTap = new Rect();
  private Rect mTopFrameForTap = new Rect();
  private Rect mRightFrameForTap = new Rect();
  private Rect mBottomFrameForTap = new Rect();

  private int mLastDownX;
  private int mLastDownY;
  private int mLastMoveX;
  private int mLastMoveY;
  private boolean mLastMoveXBeenSet;
  private boolean mLastMoveYBeenSet;

  private AnimationHandler mHandler;
  private Animator mAnimator;
  private Tracker mTracker;

  private OnLeftAnimationListener mOnLeftAnimationListener;
  private OnRightAnimationListener mOnRightAnimationListener;
  private OnTopAnimationListener mOnTopAnimationListener;
  private OnBottomAnimationListener mOnBottomAnimationListener;
  private OnOpenAnimationListener mOnOpenAnimationListener;
  private int mMeasuredWidth;
  private int mMeasuredHeight;

  public AwesomeLayout(Context context) {
    this(context, null, 0);
  }

  public AwesomeLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AwesomeLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    Log.init(context);
    mContext = getContext();
    mHandler = new AnimationHandler();
    mAnimator = new Animator();
    mTracker = new Tracker();
    mState = STATE_EXPAND;

    float density = mContext.getResources().getDisplayMetrics().density;
    mTouchEventThreshold = (int) (TAP_THRESHOLD * density + 0.5);

    loadAttrs(attrs);
  }

  /**
   * Load xml attributes
   *
   * @param attrs the attributes set from xml
   */
  public void loadAttrs(AttributeSet attrs) {
    TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.AwesomeLayout);

    mLeftOffset = a.getDimension(R.styleable.AwesomeLayout_left_offset, -1f);
    mRightOffset = a.getDimension(R.styleable.AwesomeLayout_right_offset, -1f);
    mTopOffset = a.getDimension(R.styleable.AwesomeLayout_top_offset, -1f);
    mBottomOffset = a.getDimension(R.styleable.AwesomeLayout_bottom_offset, -1f);

    parseTrack(a);
    parseTapBackArea(a);

    a.recycle();
  }

  private void parseTrack(TypedArray a) {
    Log.d(TAG, "@parseTrack");
    final String track = a.getString(R.styleable.AwesomeLayout_track);
    if (track != null && track.length() > 0) {
      final String[] tracks = track.split("\\|");
      for (String s : tracks) {
        if (mLeftOffset != -1 && mRightOffset != -1 &&
            HORIZONTAL.equals(s) && (mTrackDirection & 0xF0) == 0) {
          mTrackDirection |= TRACK_DIRECTION_HORIZONTAL_MASK;
          Log.v(TAG, "@parseTrack horizontal track");
        } else if ((mRightOffset != -1) && RIGHT.equals(s) &&
            (mTrackDirection & 0xF0) == 0) {
          mTrackDirection |= TRACK_DIRECTION_RIGHT_MASK;
          Log.v(TAG, "@parseTrack right track");
        } else if ((mLeftOffset != -1) && LEFT.equals(s) &&
            (mTrackDirection & 0xF0) == 0) {
          mTrackDirection |= TRACK_DIRECTION_LEFT_MASK;
          Log.v(TAG, "@parseTrack left track");
        } else if (mTopOffset != -1 && mBottomOffset != -1 &&
            VERTICAL.equals(s) && (mTrackDirection & 0x0F) == 0) {
          mTrackDirection |= TRACK_DIRECTION_VERTICAL_MASK;
          Log.v(TAG, "@parseTrack vertical track");
        } else if (mTopOffset != -1 && TOP.equals(s) &&
            (mTrackDirection & 0x0F) == 0) {
          mTrackDirection |= TRACK_DIRECTION_TOP_MASK;
          Log.v(TAG, "@parseTrack top track");
        } else if (mBottomOffset != -1 && BOTTOM.equals(s) &&
            (mTrackDirection & 0x0F) == 0) {
          mTrackDirection |= TRACK_DIRECTION_BOTTOM_MASK;
          Log.v(TAG, "@parseTrack bottom track");
        }
      }

    }
  }

  private void parseTapBackArea(TypedArray a) {
    Log.d(TAG, "@parseTapBackArea");
    final String tapBackArea = a.getString(R.styleable.AwesomeLayout_tap_back_area);
    if (tapBackArea != null && tapBackArea.length() > 0) {
      final String[] taps = tapBackArea.split("\\|");
      for (String s : taps) {
        if (LEFT.equals(s) && mLeftOffset != -1) {
          mLeftTapBack = true;
          Log.v(TAG, "@parseTapBackArea left tap back");
        } else if (RIGHT.equals(s) && mRightOffset != -1) {
          mRightTapBack = true;
          Log.v(TAG, "@parseTapBackArea right tap back");
        } else if (TOP.equals(s) && mTopOffset != -1) {
          mTopTapBack = true;
          Log.v(TAG, "@parseTapBackArea top tap back");
        } else if (BOTTOM.equals(s) && mBottomOffset != -1) {
          mBottomTapBack = true;
          Log.v(TAG, "@parseTapBackArea bottom tap back");
        }
      }
    }
  }

  /**
   * The offset value when flip to mLeft.
   *
   * @return the mLeft offset
   */
  public int getLeftOffset() {
    return (int) mLeftOffset;
  }

  /**
   * The offset value when flip to right.
   *
   * @return the mTop offset
   */
  public int getRightOffset() {
    return (int) mRightOffset;
  }

  /**
   * The mTop value when flip to mTop.
   *
   * @return the mTop offset
   */
  public int getTopOffset() {
    return (int) mTopOffset;
  }

  /**
   * The bottom value when flip to bottom
   *
   * @return the bottom offset
   */
  public int getBottomOffset() {
    return (int) mBottomOffset;
  }

  /**
   * tap left offset area to flip back.
   *
   * @return true if allow tap back
   */
  public boolean getLeftTapBack() {
    return mLeftTapBack;
  }

  /**
   * tap right offset area to flip back.
   *
   * @return true if allow tap back
   */
  public boolean getRightTapBack() {
    return mRightTapBack;
  }

  /**
   * tap top offset area to flip back.
   *
   * @return true if allow tap back
   */
  public boolean getTopTapBack() {
    return mTopTapBack;
  }

  /**
   * tap bottom offset area to flip back.
   *
   * @return true if allow tap back
   */
  public boolean getBottomTapBack() {
    return mBottomTapBack;
  }

  /**
   * Set left offset area could tap back.
   *
   * @param tapBack tap back
   */
  public void setLeftTapBack(boolean tapBack) {
    mLeftTapBack = tapBack;
  }

  /**
   * Set right offset area could tap back.
   *
   * @param tapBack tap back
   */
  public void setRightTapBack(boolean tapBack) {
    mRightTapBack = tapBack;
  }

  /**
   * Set top offset area could tap back.
   *
   * @param tapBack tap back
   */
  public void setTopTapBack(boolean tapBack) {
    mTopTapBack = tapBack;
  }

  /**
   * Set bottom offset area could tap back.
   *
   * @param tapBack tap back
   */
  public void setBottomTapBack(boolean tapBack) {
    mBottomTapBack = tapBack;
  }

  /**
   * Flip left immediately, without animation.
   */
  public void left() {
    if (canLeft()) {
      offsetLeftAndRight((int) (mLeftOffset - mMeasuredWidth - getLeft()));
      invalidate();
    }
  }

  /**
   * Flip right immediately, without animation.
   */
  public void right() {
    if (canRight()) {
      offsetLeftAndRight((int) (mMeasuredWidth - mRightOffset - getLeft()));
      invalidate();
    }
  }

  /**
   * Flip top immediately, without animation.
   */
  public void top() {
    if (canTop()) {
      offsetTopAndBottom((int) (mTopOffset - mMeasuredHeight - getTop()));
      invalidate();
    }
  }

  /**
   * Flip bottom immediately, without animation.
   */
  public void bottom() {
    if (canBottom()) {
      offsetTopAndBottom((int) (mMeasuredHeight - mBottomOffset - getTop()));
      invalidate();
    }
  }

  /**
   * Open host view when flipped.
   */
  public void open() {
    offsetLeftAndRight(getLeft());
    offsetTopAndBottom(getTop());
    invalidate();
  }

  /**
   * Animation version of flipping
   */
  public void animateLeft() {
    if (canLeft()) mAnimator.animateLeft(-mAnimator.velocity);
  }

  /**
   * Animation version of flipping
   */
  public void animateRight() {
    if (canRight()) mAnimator.animateRight(mAnimator.velocity);
  }

  /**
   * Animation version of flipping
   */
  public void animateTop() {
    if (canTop()) mAnimator.animateTop(-mAnimator.velocity);
  }

  /**
   * Animation version of flipping
   */
  public void animateBottom() {
    if (canBottom()) mAnimator.animateBottom(mAnimator.velocity);
  }

  /**
   * Animation version of flipping
   */
  public void animateOpen() {
    switch (mState) {
      case STATE_COLLAPSE_LEFT:
        mAnimator.animateLeftOpen(mAnimator.velocity);
        break;
      case STATE_COLLAPSE_RIGHT:
        mAnimator.animateRightOpen(-mAnimator.velocity);
        break;
      case STATE_COLLAPSE_TOP:
        mAnimator.animateTopOpen(mAnimator.velocity);
        break;
      case STATE_COLLAPSE_BOTTOM:
        mAnimator.animateBottomOpen(-mAnimator.velocity);
        break;
      default:
        break;
    }
  }

  /**
   * Get flipping state
   *
   * @return the state
   */
  public int getState() {
    return mState;
  }

  public void setLeftAnimationListener(OnLeftAnimationListener listener) {
    mOnLeftAnimationListener = listener;
  }

  public void setRightAnimationListener(OnRightAnimationListener listener) {
    mOnRightAnimationListener = listener;
  }

  public void setTopAnimationListener(OnTopAnimationListener listener) {
    mOnTopAnimationListener = listener;
  }

  public void setBottomAnimationListener(OnBottomAnimationListener listener) {
    mOnBottomAnimationListener = listener;
  }

  public void setOpenAnimationListener(OnOpenAnimationListener listener) {
    mOnOpenAnimationListener = listener;
  }

  interface OnOpenAnimationListener {
    /**
     * fire when open animation starts.
     */
    void onOpenAnimationStart();

    /**
     * fire when open animation ends;
     */
    void onOpenAnimationEnd();
  }

  interface OnLeftAnimationListener {
    /**
     * fire when left animation starts.
     */
    void onLeftAnimationStart();

    /**
     * fire when left animation ends.
     */
    void onLeftAnimationEnd();
  }

  interface OnRightAnimationListener {
    /**
     * fire when right animation starts.
     */
    void onRightAnimationStart();

    /**
     * fire when right animation ends.
     */
    void onRightAnimationEnd();
  }

  interface OnTopAnimationListener {
    /**
     * fire when top animation starts.
     */
    void onTopAnimationStart();

    /**
     * fire when top animation ends.
     */
    void onTopAnimationEnd();
  }

  interface OnBottomAnimationListener {
    /**
     * fire when bottom animation starts.
     */
    void onBottomAnimationStart();

    /**
     * fire when bottom animation ends.
     */
    void onBottomAnimationEnd();
  }


  private class AnimationHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      if (!mAnimator.animating) {
        return;
      }
      switch (msg.what) {
        case MSG_ANIMATE_LEFT:
          mAnimator.computeLeftAnimation();
          break;
        case MSG_ANIMATE_RIGHT:
          mAnimator.computeRightAnimation();
          break;
        case MSG_ANIMATE_TOP:
          mAnimator.computeTopAnimation();
          break;
        case MSG_ANIMATE_BOTTOM:
          mAnimator.computeBottomAnimation();
          break;
        case MSG_ANIMATE_LEFT_OPEN:
          mAnimator.computeLeftOpenAnimation();
          break;
        case MSG_ANIMATE_RIGHT_OPEN:
          mAnimator.computeRightOpenAnimation();
          break;
        case MSG_ANIMATE_TOP_OPEN:
          mAnimator.computeTopOpenAnimation();
          break;
        case MSG_ANIMATE_BOTTOM_OPEN:
          mAnimator.computeBottomOpenAnimation();
          break;
        default:
          break;
      }
    }
  }

  /**
   * The offset will be reset after every layout, so we do an additional offset when layout based on
   * the position state.
   */
  private void offset() {
    switch (mState) {
      case STATE_EXPAND:
        offsetLeftAndRight(-getLeft());
        offsetTopAndBottom(-getTop());
        invalidate();
        break;
      case STATE_COLLAPSE_LEFT:
        offsetLeftAndRight((int) (mLeftOffset - mMeasuredWidth - getLeft()));
        invalidate();
        break;
      case STATE_COLLAPSE_TOP:
        offsetTopAndBottom((int) (mTopOffset - mMeasuredHeight - getTop()));
        invalidate();
        break;
      case STATE_COLLAPSE_RIGHT:
        offsetLeftAndRight((int) (mMeasuredWidth - mRightOffset - getLeft()));
        invalidate();
        break;
      case STATE_COLLAPSE_BOTTOM:
        offsetTopAndBottom((int) (mMeasuredHeight - mBottomOffset - getTop()));
        invalidate();
        break;
    }

  }

  private boolean canLeft() {
    return mLeftOffset != -1 && mState == STATE_EXPAND;
  }

  private boolean canTop() {
    return mTopOffset != -1 && mState == STATE_EXPAND;
  }

  private boolean canBottom() {
    return mBottomOffset != -1 && mState == STATE_EXPAND;
  }

  private boolean canRight() {
    return mRightOffset != -1 && mState == STATE_EXPAND;
  }

  /**
   * Tracker can handle the dragging of the host view.
   */
  private class Tracker {
    static final int VELOCITY_UNIT = 500;
    static final float MIN_VELOCITY = 1000;

    VelocityTracker velocityTracker;

    boolean tracking;
    int direction;
    final int velocityUnit;
    final int minVelocity;

    Tracker() {
      float density = mContext.getResources().getDisplayMetrics().density;
      velocityUnit = (int) (VELOCITY_UNIT * density + 0.5f);
      minVelocity = (int) (MIN_VELOCITY * density + 0.5f);
    }

    void prepareTracking() {
      velocityTracker = VelocityTracker.obtain();
      tracking = true;
    }

    void stopTracking() {
      fling();
      tracking = false;
      if (velocityTracker != null) {
        velocityTracker.recycle();
        velocityTracker = null;
      }
    }

    boolean prepareLeftTrack() {
      if (mState != STATE_EXPAND &&
          mState != STATE_COLLAPSE_LEFT) return false;
      prepareTracking();
      direction = TRACK_DIRECTION_LEFT_MASK;
      return true;
    }

    boolean prepareTopTrack() {
      if (mState != STATE_EXPAND &&
          mState != STATE_COLLAPSE_TOP) return false;
      prepareTracking();
      direction = TRACK_DIRECTION_TOP_MASK;
      return true;
    }

    boolean prepareRightTrack() {
      if (mState != STATE_EXPAND &&
          mState != STATE_COLLAPSE_RIGHT) return false;
      prepareTracking();
      direction = TRACK_DIRECTION_RIGHT_MASK;
      return true;
    }

    boolean prepareBottomTrack() {
      if (mState != STATE_EXPAND &&
          mState != STATE_COLLAPSE_BOTTOM) return false;
      prepareTracking();
      direction = TRACK_DIRECTION_BOTTOM_MASK;
      return true;
    }

    boolean prepareHorizontalTrack() {
      prepareTracking();
      direction = TRACK_DIRECTION_HORIZONTAL_MASK;
      return true;
    }

    boolean prepareVerticalTrack() {
      prepareTracking();
      direction = TRACK_DIRECTION_VERTICAL_MASK;
      return true;
    }

    void move(int xOffset, int yOffset) {
      if (!tracking) return;
      final int left = getLeft() - xOffset;
      final int top = getTop() - yOffset;
      switch (direction) {
        case TRACK_DIRECTION_LEFT_MASK:
          if (left > mLeftOffset - mMeasuredWidth && left < 0) {
            offsetLeftAndRight(-xOffset);
            invalidate();
          }
          break;
        case TRACK_DIRECTION_RIGHT_MASK:
          if (left < mMeasuredWidth - mRightOffset && left > 0) {
            offsetLeftAndRight(-xOffset);
            invalidate();
          }
          break;
        case TRACK_DIRECTION_TOP_MASK:
          if (top > mTopOffset - mMeasuredHeight && top < 0) {
            offsetTopAndBottom(-yOffset);
            invalidate();
          }
          break;
        case TRACK_DIRECTION_BOTTOM_MASK:
          if (top < mMeasuredHeight - mBottomOffset && top > 0) {
            offsetTopAndBottom(-yOffset);
            invalidate();
          }
          break;
        case TRACK_DIRECTION_HORIZONTAL_MASK:
          if (left > mLeftOffset - mMeasuredWidth &&
              left < mMeasuredWidth - mRightOffset) {
            offsetLeftAndRight(-xOffset);
            invalidate();
          }
          break;
        case TRACK_DIRECTION_VERTICAL_MASK:
          if (top > mTopOffset - mMeasuredHeight &&
              top < mMeasuredHeight - mBottomOffset) {
            offsetTopAndBottom(-yOffset);
            invalidate();
          }
          break;
        default:
          break;
      }
    }

    /**
     * 拖动结束之后，我们会根据速度和位置把View动画到合适的位置。
     */
    private void fling() {
      velocityTracker.computeCurrentVelocity(velocityUnit);
      float xVelocity = velocityTracker.getXVelocity();
      float yVelocity = velocityTracker.getYVelocity();

      if (xVelocity < 0) {
        xVelocity = Math.min(xVelocity, -minVelocity);
      } else {
        xVelocity = Math.max(xVelocity, minVelocity);
      }

      if (yVelocity < 0) {
        yVelocity = Math.min(yVelocity, -minVelocity);
      } else {
        yVelocity = Math.max(yVelocity, minVelocity);
      }

      switch (direction) {
        case TRACK_DIRECTION_HORIZONTAL_MASK:
          horizontalFling(xVelocity);
          break;
        case TRACK_DIRECTION_LEFT_MASK:
          leftFling(xVelocity);
          break;
        case TRACK_DIRECTION_RIGHT_MASK:
          rightFling(xVelocity);
          break;
        case TRACK_DIRECTION_TOP_MASK:
          topFling(yVelocity);
          break;
        case TRACK_DIRECTION_BOTTOM_MASK:
          bottomFling(yVelocity);
          break;
        case TRACK_DIRECTION_VERTICAL_MASK:
          verticalFling(yVelocity);
          break;
        default:
          break;
      }
    }

    private void verticalFling(float velocity) {
      final int top = getTop();
      if (top <= 0 && top >= mTopOffset - mMeasuredHeight) {
        if (velocity < 0) {
          mAnimator.animateTop(velocity);
        } else {
          mAnimator.animateTopOpen(velocity);
        }
      } else if (top >= 0 && top <= mMeasuredHeight - mBottomOffset) {
        if (velocity < 0) {
          mAnimator.animateBottomOpen(velocity);
        } else {
          mAnimator.animateBottom(velocity);
        }
      }
    }

    private void bottomFling(float velocity) {
      if (velocity > 0) {
        mAnimator.animateBottom(velocity);
      } else {
        mAnimator.animateBottomOpen(velocity);
      }
    }

    private void horizontalFling(float velocity) {
      Log.d(TAG, "@horizontalFling");
      final int left = getLeft();
      if (left <= 0 && left >= mLeftOffset - mMeasuredWidth) {
        if (velocity < 0) {
          mAnimator.animateLeft(velocity);
        } else {
          mAnimator.animateLeftOpen(velocity);
        }
      } else if (left >= 0 && left <= mMeasuredWidth - mRightOffset) {
        if (velocity < 0) {
          mAnimator.animateRightOpen(velocity);
        } else {
          mAnimator.animateRight(velocity);
        }
      }
    }

    private void leftFling(float velocity) {
      if (velocity < 0) {
        mAnimator.animateLeft(velocity);
      } else {
        mAnimator.animateLeftOpen(velocity);
      }
    }

    private void rightFling(float velocity) {
      Log.d(TAG, "@rightFling");
      if (velocity < 0) {
        mAnimator.animateRightOpen(velocity);
      } else {
        mAnimator.animateRight(velocity);
      }
    }

    private void topFling(float velocity) {
      Log.d(TAG, "@topFling");
      if (velocity < 0) {
        mAnimator.animateTop(velocity);
      } else {
        mAnimator.animateTopOpen(velocity);
      }
    }
  }

  private class Animator {
    static final String TAG = "IAwesomeImpl$Animator";

    static final int FRAME_ANIMATION_DURATION = 1000 / 60;

    static final int VELOCITY = 1500;
    static final int END_VELOCITY = 500;
    static final int MIN_VELOCITY = 300;
    static final int ACCELERATION = 3000;

    final float velocity;
    final float minVelocity;
    final float endVelocity;

    float animatingDelta;
    float animatingVelocity;
    long lastAnimationTime;
    long currentAnimationTime;
    boolean animating;

    Animator() {
      final float density = mContext.getResources().getDisplayMetrics().density;
      velocity = VELOCITY * density;
      minVelocity = MIN_VELOCITY * density;
      endVelocity = END_VELOCITY * density;
    }

    private void compute() {
      final float v = animatingVelocity;
      final long now = SystemClock.uptimeMillis();
      final float t = (now - lastAnimationTime) / 1000f;
      animatingDelta = v * t;
      lastAnimationTime = now;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
    }

    void computeLeftAnimation() {
      compute();
      if (getLeft() + animatingDelta <= mLeftOffset - mMeasuredWidth) {
        final OnLeftAnimationListener listener = mOnLeftAnimationListener;
        if (listener != null) listener.onLeftAnimationEnd();
        animating = false;
        mState = STATE_COLLAPSE_LEFT;
        offset();
      } else {
        offsetLeftAndRight((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT, currentAnimationTime);
      }
    }

    void computeRightAnimation() {
      compute();
      if (getLeft() + animatingDelta >= mMeasuredWidth - mRightOffset) {
        final OnRightAnimationListener listener = mOnRightAnimationListener;
        if (listener != null) listener.onRightAnimationEnd();
        animating = false;
        mState = STATE_COLLAPSE_RIGHT;
        offset();
      } else {
        offsetLeftAndRight((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT,
            currentAnimationTime);
      }
    }

    void computeTopAnimation() {
      compute();
      if (getTop() + animatingDelta <= mTopOffset - mMeasuredHeight) {
        final OnTopAnimationListener listener = mOnTopAnimationListener;
        if (listener != null) listener.onTopAnimationEnd();
        animating = false;
        mState = STATE_COLLAPSE_TOP;
        offset();
      } else {
        offsetTopAndBottom((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP, currentAnimationTime);
      }
    }

    void computeBottomAnimation() {
      compute();
      if (getTop() + animatingDelta >= mMeasuredHeight - mBottomOffset) {
        final OnBottomAnimationListener listener = mOnBottomAnimationListener;
        if (listener != null) listener.onBottomAnimationEnd();
        animating = false;
        mState = STATE_COLLAPSE_BOTTOM;
        offset();
      } else {
        offsetTopAndBottom((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM,
            currentAnimationTime);
      }
    }

    void computeLeftOpenAnimation() {
      compute();
      if (getLeft() + animatingDelta >= 0) {
        final OnOpenAnimationListener listener = mOnOpenAnimationListener;
        if (listener != null) {
          listener.onOpenAnimationEnd();
        }
        animating = false;
        mState = STATE_EXPAND;
        offset();
      } else {
        offsetLeftAndRight((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT_OPEN,
            currentAnimationTime);
      }
    }

    void computeRightOpenAnimation() {
      compute();
      if (getLeft() + animatingDelta <= 0) {
        final OnOpenAnimationListener listener = mOnOpenAnimationListener;
        if (listener != null) listener.onOpenAnimationEnd();
        animating = false;
        mState = STATE_EXPAND;
        offset();
      } else {
        offsetLeftAndRight((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT_OPEN,
            currentAnimationTime);
      }
    }

    void computeTopOpenAnimation() {
      compute();
      if (getTop() + animatingDelta >= 0) {
        final OnOpenAnimationListener listener = mOnOpenAnimationListener;
        if (listener != null) listener.onOpenAnimationEnd();
        animating = false;
        mState = STATE_EXPAND;
        offset();
      } else {
        offsetTopAndBottom((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP_OPEN,
            currentAnimationTime);
      }
    }

    void computeBottomOpenAnimation() {
      compute();
      if (getTop() + animatingDelta <= 0) {
        final OnOpenAnimationListener listener = mOnOpenAnimationListener;
        if (listener != null) listener.onOpenAnimationEnd();
        animating = false;
        mState = STATE_EXPAND;
        offset();
      } else {
        offsetTopAndBottom((int) animatingDelta);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM_OPEN,
            currentAnimationTime);
      }
    }

    void animateLeftOpen(float velocity) {
      final OnOpenAnimationListener listener = mOnOpenAnimationListener;
      if (listener != null) {
        listener.onOpenAnimationStart();
      }
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.removeMessages(MSG_ANIMATE_LEFT_OPEN);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT_OPEN,
          currentAnimationTime);
    }

    void animateRightOpen(float velocity) {
      final OnOpenAnimationListener listener = mOnOpenAnimationListener;
      if (listener != null) {
        listener.onOpenAnimationStart();
      }
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.removeMessages(MSG_ANIMATE_RIGHT_OPEN);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT_OPEN,
          currentAnimationTime);
    }

    void animateTopOpen(float velocity) {
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.removeMessages(MSG_ANIMATE_TOP_OPEN);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP_OPEN,
          currentAnimationTime);
    }

    void animateBottomOpen(float velocity) {
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.removeMessages(MSG_ANIMATE_BOTTOM_OPEN);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM_OPEN,
          currentAnimationTime);
    }

    void animateLeft(float velocity) {
      final OnLeftAnimationListener listener = mOnLeftAnimationListener;
      if (listener != null) {
        listener.onLeftAnimationStart();
      }
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT, currentAnimationTime);
    }

    void animateRight(float velocity) {
      final OnRightAnimationListener listener = mOnRightAnimationListener;
      if (listener != null) {
        listener.onRightAnimationStart();
      }
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT, currentAnimationTime);
    }

    void animateTop(float velocity) {
      final OnTopAnimationListener listener = mOnTopAnimationListener;
      if (listener != null) {
        listener.onTopAnimationStart();
      }
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP, currentAnimationTime);
    }

    void animateBottom(float velocity) {
      final OnBottomAnimationListener listener = mOnBottomAnimationListener;
      if (listener != null) {
        listener.onBottomAnimationStart();
      }
      animating = true;
      currentAnimationTime = SystemClock.uptimeMillis();
      lastAnimationTime = currentAnimationTime;
      currentAnimationTime += FRAME_ANIMATION_DURATION;
      animatingVelocity = velocity;
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM, currentAnimationTime);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    mMeasuredWidth = getMeasuredWidth();
    mMeasuredHeight = getMeasuredHeight();
    assert mMeasuredWidth >= mLeftOffset :
        "mLeft offset should not be larger than the view's width";
    assert mMeasuredWidth >= mRightOffset :
        "right offset should not be larger than the view's width";
    assert mMeasuredHeight >= mTopOffset :
        "mTop offset should not be larger than the view's height";
    assert mMeasuredHeight >= mBottomOffset :
        "bottom offset should not be larger than the view's height";
  }

  /**
   * call this method at the end of the onLayout method body
   *
   * @param changed if layout changed
   * @param l       left side
   * @param t       top side
   * @param r       right side
   * @param b       bottom size
   */
  @Override
  public void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    if (changed) {
      if (mLeftOffset != -1) {
        mLeftFrameForTap.set((int) (r - mLeftOffset), t, r, b);
      }

      if (mTopOffset != -1) {
        mTopFrameForTap.set(l, (int) (b - mTopOffset), r, b);
      }

      if (mRightOffset != -1) {
        mRightFrameForTap.set(l, t, (int) (l + mRightOffset), b);
      }

      if (mBottomOffset != -1) {
        mBottomFrameForTap.set(l, t, r, (int) (t + mBottomOffset));
      }
    }

    switch (mState) {
      case STATE_COLLAPSE_BOTTOM:
        bottom();
        break;
      case STATE_COLLAPSE_LEFT:
        left();
        break;
      case STATE_COLLAPSE_TOP:
        top();
        break;
      case STATE_COLLAPSE_RIGHT:
        right();
        break;
      case STATE_EXPAND:
        open();
        break;
      default:
        break;
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    ev.offsetLocation(-getLeft(), -getTop());
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;
    final int x = (int) ev.getX();
    final int y = (int) ev.getY();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mLastDownX = x;
        mLastDownY = y;
        break;
      case MotionEvent.ACTION_MOVE:
        if ((mTrackDirection & 0xF0) > 0 &&
            (x < mLastDownX - mTouchEventThreshold || x > mLastDownX +
                mTouchEventThreshold)) {
          switch (mTrackDirection & 0xF0) {
            case TRACK_DIRECTION_LEFT_MASK:
              return mTracker.prepareLeftTrack();
            case TRACK_DIRECTION_RIGHT_MASK:
              return mTracker.prepareRightTrack();
            case TRACK_DIRECTION_HORIZONTAL_MASK:
              return mTracker.prepareHorizontalTrack();
            default:
              break;
          }
        } else if ((mTrackDirection & 0x0F) > 0 &&
            (y < mLastDownY - mTouchEventThreshold || y > mLastDownY +
                mTouchEventThreshold)) {
          switch (mTrackDirection & 0x0F) {
            case TRACK_DIRECTION_TOP_MASK:
              return mTracker.prepareTopTrack();
            case TRACK_DIRECTION_BOTTOM_MASK:
              return mTracker.prepareBottomTrack();
            case TRACK_DIRECTION_VERTICAL_MASK:
              return mTracker.prepareVerticalTrack();
            default:
              break;
          }
        }

        return false;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (mLastDownX - mTouchEventThreshold < x &&
            x < mLastDownX + mTouchEventThreshold &&
            mLastDownY - mTouchEventThreshold < y &&
            y < mLastDownY + mTouchEventThreshold) {
          if (mLeftTapBack && mLeftFrameForTap.contains(x, y) &&
              mState == STATE_COLLAPSE_LEFT) {
            mAnimator.animateLeftOpen(mAnimator.velocity);
          } else if (mTopTapBack && mTopFrameForTap.contains(x, y) &&
              mState == STATE_COLLAPSE_TOP) {
            mAnimator.animateTopOpen(mAnimator.velocity);
          } else if (mRightTapBack && mRightFrameForTap.contains(x, y) &&
              mState == STATE_COLLAPSE_RIGHT) {
            mAnimator.animateRightOpen(-mAnimator.velocity);
          } else if (mBottomTapBack && mBottomFrameForTap.contains(x, y) &&
              mState == STATE_COLLAPSE_BOTTOM) {
            mAnimator.animateBottomOpen(-mAnimator.velocity);
          } else {
            return false;
          }
          return true;
        }
      default:
        break;
    }
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    final int x = (int) ev.getX();
    final int y = (int) ev.getY();
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    switch (action) {
      case MotionEvent.ACTION_MOVE:
        if (mTracker.tracking) {
          if (!mLastMoveXBeenSet) {
            if (x > mLastDownX) {
              mLastMoveX = mLastDownX + mTouchEventThreshold;
              mLastMoveXBeenSet = true;
            } else {
              mLastMoveX = mLastDownX - mTouchEventThreshold;
              mLastMoveXBeenSet = true;
            }
          }

          if (!mLastMoveYBeenSet) {
            if (y > mLastDownY) {
              mLastMoveY = mLastDownY + mTouchEventThreshold;
              mLastMoveYBeenSet = true;
            } else {
              mLastMoveY = mLastDownY - mTouchEventThreshold;
              mLastMoveYBeenSet = true;
            }
          }

          mTracker.move(mLastMoveX - x, mLastMoveY - y);
          ev.offsetLocation(getLeft(), getTop());
          mTracker.velocityTracker.addMovement(ev);
        }
        break;
      case MotionEvent.ACTION_UP:
        if (mTracker.tracking) {
          mTracker.stopTracking();
          mLastMoveXBeenSet = false;
          mLastMoveYBeenSet = false;
          return true;
        }
      default:
        return false;
    }
    return true;
  }
}
