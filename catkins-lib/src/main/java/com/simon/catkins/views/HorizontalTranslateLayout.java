package com.simon.catkins.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.FrameLayout;
import com.simon.catkins.R;
import de.akquinet.android.androlog.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of HorizontalTranslateLayout interface and the interceptions for host view methods.
 *
 * <b>This layout doesn't support using drawable as background. Only color supported</b>
 */
public class HorizontalTranslateLayout extends FrameLayout implements IHorizontalTranslate {
  private static final String TAG = "HorizontalTranslateLayout";

  private enum TrackDirection {left, right, horizontal, none}

  private final static int MSG_ANIMATE_LEFT = -100;
  private final static int MSG_ANIMATE_RIGHT = -101;
  private final static int MSG_ANIMATE_LEFT_OPEN = -104;
  private final static int MSG_ANIMATE_RIGHT_OPEN = -105;

  private final static int TAP_THRESHOLD = 35;

  private float mLeftOffset;
  private float mRightOffset;

  private int mLeftTranslate;

  private final int mTouchThreshold;

  private boolean mLeftTapBack;
  private boolean mRightTapBack;

  private TrackDirection mTrackDirection;

  private int mPositionState;

  private final Rect mLeftFrameForTap = new Rect();
  private final Rect mRightFrameForTap = new Rect();
  private final Paint mBackgroundPaint;

  private int mLastDownX;
  private int mLastDownY;
  private int mLastMoveX;
  private boolean mLastMoveXBeenSet;

  private final AnimationHandler mHandler;
  private final Animator mAnimator;
  private final Tracker mTracker;

  private OnLeftAnimationListener mOnLeftAnimationListener;
  private OnRightAnimationListener mOnRightAnimationListener;
  private final List<OnOpenAnimationListener> mOnOpenAnimationListener =
      new ArrayList<OnOpenAnimationListener>();
  private OnLeftTrackListener mOnLeftTrackListener;
  private OnRightTrackListener mOnRightTrackListener;
  private OnHorizontalTrackListener mOnHorizontalTrackListener;

  public HorizontalTranslateLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    Log.init(context);
    mHandler = new AnimationHandler();
    mAnimator = new Animator();
    mTracker = new Tracker();
    mPositionState = STATE_EXPAND;

    Resources res = getResources();

    mBackgroundPaint = new Paint();
    mBackgroundPaint.setColor(0xFFFFFFFF);

    float density = res.getDisplayMetrics().density;
    mTouchThreshold = (int) (TAP_THRESHOLD * density + 0.5);

    loadAttrs(attrs);
  }

  /**
   * Load xml attributes
   *
   * @param attrs the attributes set from xml
   */
  private void loadAttrs(AttributeSet attrs) {
    TypedArray a =
        getContext().obtainStyledAttributes(attrs, R.styleable.HorizontalTranslateLayout);

    mLeftOffset = a.getDimension(R.styleable.HorizontalTranslateLayout_left_offset, -1f);
    mRightOffset = a.getDimension(R.styleable.HorizontalTranslateLayout_right_offset, -1f);

    final String track = a.getString(R.styleable.HorizontalTranslateLayout_track);
    if (track != null && track.length() > 0) {
      if (mLeftOffset != -1 && mRightOffset != -1 && HORIZONTAL.equals(track)) {
        Log.d(TAG, "@parseTrack horizontal");
        mTrackDirection = TrackDirection.horizontal;
      } else if ((mRightOffset != -1) && RIGHT.equals(track)) {
        Log.d(TAG, "@parseTrack right");
        mTrackDirection = TrackDirection.right;
      } else if ((mLeftOffset != -1) && LEFT.equals(track)) {
        Log.d(TAG, "@parseTrack left");
        mTrackDirection = TrackDirection.left;
      } else {
        mTrackDirection = TrackDirection.none;
        Log.d(TAG, "@loadAttrs no direction");
      }
    }

    final String tapBackArea =
        a.getString(R.styleable.HorizontalTranslateLayout_tap_back_area);
    if (tapBackArea != null && tapBackArea.length() > 0) {
      final String[] taps = tapBackArea.split("\\|");
      for (String s : taps) {
        Log.d(TAG, "@loadAttrs tap area " + s);
        if (LEFT.equals(s) && mLeftOffset != -1) {
          mLeftTapBack = true;
        } else if (RIGHT.equals(s) && mRightOffset != -1) {
          mRightTapBack = true;
        } else {
          Log.d(TAG, "@loadAttrs tap_back_area value illegal");
        }
      }
    }

    a.recycle();

    setClickable(true);
  }

  @Override
  public void setBackgroundColor(int color) {
    mBackgroundPaint.setColor(color);
    invalidate();
  }

  /**
   * @param proportion the proportion while the range is [-1, 1]
   */
  @Override
  public void setProportion(float proportion) {
    if (proportion < -1f || proportion > 1f) {
      return;
    }
    if (proportion < 0f) {
      mLeftTranslate = (int) ((mLeftOffset - getMeasuredWidth()) * -proportion);
    } else if (proportion > 0f) {
      mLeftTranslate = (int) ((getMeasuredWidth() - mRightOffset) * proportion);
    } else if (proportion == 0f) {
      mLeftTranslate = 0;
      mPositionState = STATE_EXPAND;
    } else if (proportion == -1f) {
      mLeftOffset = mLeftOffset - getMeasuredWidth();
      mPositionState = STATE_COLLAPSE_LEFT;
    } else if (proportion == 1f) {
      mLeftOffset = getMeasuredWidth() - mRightOffset;
      mPositionState = STATE_COLLAPSE_RIGHT;
    }
    invalidate();
  }

  /**
   * The offset value when flip to left.
   *
   * @return the mLeft offset
   */
  @Override
  public int getLeftOffset() {
    return (int) mLeftOffset;
  }

  /**
   * The offset value when flip to right.
   *
   * @return the mTop offset
   */
  @Override
  public int getRightOffset() {
    return (int) mRightOffset;
  }

  /**
   * tap left offset area to flip back.
   *
   * @return true if allow tap back
   */
  @Override
  public boolean isLeftTapBack() {
    return mLeftTapBack;
  }

  /**
   * tap right offset area to flip back.
   *
   * @return true if allow tap back
   */
  @Override
  public boolean isRightTapBack() {
    return mRightTapBack;
  }

  /**
   * Set left offset area could tap back.
   *
   * @param tapBack tap back
   */
  @Override
  public void setLeftTapBack(boolean tapBack) {
    mLeftTapBack = tapBack;
  }

  /**
   * Set right offset area could tap back.
   *
   * @param tapBack tap back
   */
  @Override
  public void setRightTapBack(boolean tapBack) {
    mRightTapBack = tapBack;
  }

  /**
   * Flip left immediately, without animation.
   */
  @Override
  public void left() {
    mLeftTranslate = (int) (mLeftOffset - getMeasuredWidth());
    mPositionState = STATE_COLLAPSE_LEFT;
    invalidate();
  }

  /**
   * Flip right immediately, without animation.
   */
  @Override
  public void right() {
    mLeftTranslate = (int) (getMeasuredWidth() - mRightOffset);
    mPositionState = STATE_COLLAPSE_RIGHT;
    invalidate();
  }

  /**
   * Open host view when flipped.
   */
  @Override
  public void open() {
    mLeftTranslate = 0;
    mPositionState = STATE_EXPAND;
    invalidate();
  }

  /**
   * Animation version of flipping
   */
  @Override
  public void animateLeft() {
    if (canLeft()) {
      mAnimator.animateLeft(-mAnimator.kVelocity);
    }
  }

  /**
   * Animation version of flipping
   */
  @Override
  public void animateRight() {
    if (canRight()) {
      mAnimator.animateRight(mAnimator.kVelocity);
    }
  }

  /**
   * Animation version of flipping
   */
  @Override
  public void animateOpen() {
    switch (mPositionState) {
      case STATE_COLLAPSE_LEFT:
        mAnimator.animateLeftOpen(mAnimator.kVelocity);
        break;
      case STATE_COLLAPSE_RIGHT:
        mAnimator.animateRightOpen(-mAnimator.kVelocity);
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
  @Override
  public int getState() {
    return mPositionState;
  }

  @Override
  public void setLeftAnimationListener(OnLeftAnimationListener listener) {
    mOnLeftAnimationListener = listener;
  }

  @Override
  public void setRightAnimationListener(OnRightAnimationListener listener) {
    mOnRightAnimationListener = listener;
  }

  @Override
  public void addOpenAnimationListener(OnOpenAnimationListener listener) {
    mOnOpenAnimationListener.add(listener);
  }

  @Override
  public void removeOpenAnimationListener(OnOpenAnimationListener listener) {
    mOnOpenAnimationListener.remove(listener);
  }

  @Override
  public void setLeftTrackListener(OnLeftTrackListener listener) {
    mOnLeftTrackListener = listener;
  }

  @Override
  public void setRightTrackListener(OnRightTrackListener listener) {
    mOnRightTrackListener = listener;
  }

  @Override
  public void setHorizontalTrackListener(OnHorizontalTrackListener listener) {
    mOnHorizontalTrackListener = listener;
  }

  private boolean canLeft() {
    return mLeftOffset != -1 && mPositionState == STATE_EXPAND;
  }

  private boolean canRight() {
    return mRightOffset != -1 && mPositionState == STATE_EXPAND;
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    canvas.save();
    canvas.translate(mLeftTranslate, 0);
    Log.d(TAG, "@dispatchDraw " + mLeftTranslate);

    // draw background here
    canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mBackgroundPaint);

    super.dispatchDraw(canvas);
    canvas.restore();
  }

  public int getLeftTranslate() {
    return mLeftTranslate;
  }

  public boolean isAnimating() {
    return mAnimator.iAnimating;
  }

  @Override
  @SuppressWarnings("all")
  public boolean dispatchTouchEvent(MotionEvent ev) {
    // 保证无论子View是否可点击，都能够将后续的事件分发给onInterceptTouchEvent方法。
    return super.dispatchTouchEvent(ev) || true;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mTrackDirection == TrackDirection.none) {
      // None track direction so do not intercept touch event
      return false;
    }

    ev.offsetLocation(-mLeftTranslate, 0);
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;
    final int x = (int) ev.getX();
    final int y = (int) ev.getY();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mLastDownX = x;
        mLastDownY = y;

        // 停止所有的动画
        mHandler.removeMessages(MSG_ANIMATE_LEFT);
        mHandler.removeMessages(MSG_ANIMATE_LEFT_OPEN);
        mHandler.removeMessages(MSG_ANIMATE_RIGHT);
        mHandler.removeMessages(MSG_ANIMATE_RIGHT_OPEN);
        break;
      case MotionEvent.ACTION_MOVE:
        Log.d(TAG, "@interceptInterceptTouchEvent");

        if (mPositionState == STATE_EXPAND) {
          if (y < mLastDownY - mTouchThreshold || y > mLastDownY + mTouchThreshold) {
            return false;
          }

          if ((x < mLastDownX - mTouchThreshold || x > mLastDownX + mTouchThreshold)) {
            switch (mTrackDirection) {
              case left:
                return mTracker.prepareLeftTrack();
              case right:
                return mTracker.prepareRightTrack();
              case horizontal:
                return mTracker.prepareHorizontalTrack(x - mLastDownX);
              default:
                break;
            }
          }
          return false;
        } else {
          switch (mTrackDirection) {
            case left:
              return mTracker.prepareLeftTrack();
            case right:
              return mTracker.prepareRightTrack();
            case horizontal:
              return mTracker.prepareHorizontalTrack(x - mLastDownX);
          }
        }
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (mLastDownX - mTouchThreshold < x && x < mLastDownX + mTouchThreshold) {
          if (mLeftTapBack && mLeftFrameForTap.contains(x, y)
              && mPositionState == STATE_COLLAPSE_LEFT) {
            mAnimator.animateLeftOpen(mAnimator.kVelocity);
          } else if (mRightTapBack && mRightFrameForTap.contains(x, y)
              && mPositionState == STATE_COLLAPSE_RIGHT) {
            mAnimator.animateRightOpen(-mAnimator.kVelocity);
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
    //Log.d(TAG, String.format("@interceptTouch x %d, y %d", x, y));
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    switch (action) {
      case MotionEvent.ACTION_MOVE:
        if (mTracker.tracking) {
          if (!mLastMoveXBeenSet) {
            if (mPositionState != STATE_EXPAND) {
              mLastMoveX = mLastDownX + mLeftTranslate;
              mLastMoveXBeenSet = true;
            } else if (x > mLastDownX) {
              mLastMoveX = mLastDownX + mTouchThreshold;
              mLastMoveXBeenSet = true;
            } else {
              mLastMoveX = mLastDownX - mTouchThreshold;
              mLastMoveXBeenSet = true;
            }
          }

          mTracker.move(mLastMoveX - x);
          mLastMoveX = x;
          //ev.offsetLocation(mLeftTranslate, 0);
          mTracker.velocityTracker.addMovement(ev);
        }
        break;
      case MotionEvent.ACTION_UP:
        Log.d(TAG, "@onTouchEvent up");
        // 当不在展开的状态下的时候，我们要判断是否可以通过单击侧边区域做展开动画。
        // 只有在侧边区域的点击才能进行计算。
        mLastMoveXBeenSet = false;
        if (mPositionState != STATE_EXPAND) {
          if (mLeftTapBack && mPositionState == STATE_COLLAPSE_LEFT
              && mLeftFrameForTap.contains(x, y)) {
            mTracker.stopTracking();
            Log.d(TAG, "@onTouchEvent left open");
            mAnimator.animateLeftOpen(mAnimator.kVelocity);
            return true;
          } else if (mRightTapBack && mPositionState == STATE_COLLAPSE_RIGHT
              && mRightFrameForTap.contains(x, y)) {
            mTracker.stopTracking();
            Log.d(TAG, "@onTouchEvent right open");
            mAnimator.animateRightOpen(-mAnimator.kVelocity);
            return true;
          }
        }

        if (mTracker.tracking) {
          Log.d(TAG, "@onTouchEvent tracking");
          mTracker.stopTracking();
          mTracker.fling();
        }
        return true;
      default:
        return false;
    }
    return true;
  }

  /**
   */
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    final int width = r - l;
    final int height = b - t;

    if (changed) {
      if (mLeftOffset != -1) {
        mLeftFrameForTap.set((int) (r - mLeftOffset), t, r, b);
      }

      if (mRightOffset != -1) {
        mRightFrameForTap.set(l, t, (int) (l + mRightOffset), b);
      }
    }

    if (!mAnimator.iAnimating && !mTracker.tracking) {
      offset();
    }
  }

  /**
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    final int widthSize = widthMeasureSpec & ~(0x3 << 30);

    //check the offsets' sizes are not larger than the view's dimension
    assert widthSize >= mLeftOffset :
        "left offset should not be larger than the view's width";
    assert widthSize >= mRightOffset :
        "right offset should not be larger than the view's width";
  }

  /**
   * The offset will be reset after every re-layout, so we do an additional offset when layout based on
   * the position state.
   */
  private void offset() {
    switch (mPositionState) {
      case STATE_EXPAND:
        mLeftTranslate = 0;
        invalidate();
        break;
      case STATE_COLLAPSE_LEFT:
        mLeftTranslate = (int) (mLeftOffset - getMeasuredWidth());
        invalidate();
        break;
      case STATE_COLLAPSE_RIGHT:
        mLeftTranslate = (int) (getMeasuredWidth() - mRightOffset);
        invalidate();
        break;
    }
  }


  private class AnimationHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      if (!mAnimator.iAnimating) {
        return;
      }
      switch (msg.what) {
        case MSG_ANIMATE_LEFT:
          mAnimator.computeLeftAnimation();
          break;
        case MSG_ANIMATE_RIGHT:
          mAnimator.computeRightAnimation();
          break;
        case MSG_ANIMATE_LEFT_OPEN:
          mAnimator.computeLeftOpenAnimation();
          break;
        case MSG_ANIMATE_RIGHT_OPEN:
          mAnimator.computeRightOpenAnimation();
          break;
        default:
          break;
      }
    }
  }

  /**
   * Tracker can handle the dragging of the view.
   */
  private class Tracker {
    static final int VELOCITY_UNIT = 200;
    static final float MIN_VELOCITY = 500;

    VelocityTracker velocityTracker;

    boolean tracking;
    TrackDirection direction;
    final int velocityUnit;
    final int minVelocity;

    Tracker() {
      float density = getContext().getResources().getDisplayMetrics().density;
      velocityUnit = (int) (VELOCITY_UNIT * density + 0.5f);
      minVelocity = (int) (MIN_VELOCITY * density + 0.5f);
    }

    void prepareTracking() {
      velocityTracker = VelocityTracker.obtain();
      tracking = true;
    }

    void stopTracking() {
      tracking = false;
    }

    boolean prepareLeftTrack() {
      if (mPositionState != STATE_EXPAND &&
          mPositionState != STATE_COLLAPSE_LEFT) {
        return false;
      }
      prepareTracking();
      direction = TrackDirection.left;
      return true;
    }

    boolean prepareRightTrack() {
      if (mPositionState != STATE_EXPAND &&
          mPositionState != STATE_COLLAPSE_RIGHT) {
        return false;
      }
      prepareTracking();
      direction = TrackDirection.right;
      return true;
    }

    boolean prepareHorizontalTrack(int d) {
      prepareTracking();
      direction = TrackDirection.horizontal;
      if (mOnHorizontalTrackListener != null) {
        final OnHorizontalTrackListener listener = mOnHorizontalTrackListener;
        listener.onHorizontalTrackListener(d);
      }
      return true;
    }

    void move(int xOffset) {
      if (!tracking) {
        return;
      }
      final int left = mLeftTranslate - xOffset;
      switch (direction) {
        case left:
          Log.d(TAG, "@move left");
          if (left > mLeftOffset - getMeasuredWidth() && left < 0) {
            mLeftTranslate -= xOffset;
            invalidate();
          }
          break;
        case right:
          Log.d(TAG, "@move right");
          if (left < getMeasuredWidth() - mRightOffset && left > 0) {
            mLeftTranslate -= xOffset;
            invalidate();
          }
          break;
        case horizontal:
          Log.d(TAG, "@move horizontal");
          if (left >= mLeftOffset - getMeasuredWidth()
              && left <= getMeasuredWidth() - mRightOffset) {
            mLeftTranslate -= xOffset;
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

      Log.d(TAG, "@fling x " + xVelocity);

      if (xVelocity < 0) {
        xVelocity = Math.min(xVelocity, -minVelocity);
      } else {
        xVelocity = Math.max(xVelocity, minVelocity);
      }

      switch (direction) {
        case horizontal:
          horizontalFling(xVelocity);
          break;
        case left:
          leftFling(xVelocity);
          break;
        case right:
          rightFling(xVelocity);
          break;
        default:
          break;
      }

      velocityTracker.recycle();
      velocityTracker = null;
    }

    private void horizontalFling(float velocity) {
      Log.d(TAG, "@horizontalFling");
      final int left = mLeftTranslate;
      if (left <= 0 && left >= mLeftOffset - getMeasuredWidth()) {
        if (velocity < 0) {
          mAnimator.animateLeft(velocity);
        } else {
          mAnimator.animateLeftOpen(velocity);
        }
      } else if (left >= 0 && left <= getMeasuredWidth() - mRightOffset) {
        if (velocity < 0) {
          mAnimator.animateRightOpen(velocity);
        } else {
          mAnimator.animateRight(velocity);
        }
      }
    }

    private void leftFling(float velocity) {
      Log.d(TAG, "@leftFling");
      if (velocity < 0) {
        Log.d(TAG, "@leftFling animateLeft " + velocity);
        mAnimator.animateLeft(velocity);
      } else {
        Log.d(TAG, "@leftFling animateLeftOpen " + velocity);
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
  }

  private class Animator {
    static final String TAG = "IAwesomeImpl$Animator";

    static final int VELOCITY = 600;
    static final int MIN_VELOCITY = 300;

    final float kVelocity;
    final float kMinVelocity;

    float iAnimatingPosition;
    float iAnimatingVelocity;
    float iAnimationDistance;
    float iAnimationStart;
    long iLastAnimationTime;
    long iCurrentAnimationTime;

    boolean iAnimating;

    Animator() {
      final float density =
          getContext().getResources().getDisplayMetrics().density;
      kVelocity = VELOCITY * density;
      kMinVelocity = MIN_VELOCITY * density;
    }

    private void compute() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - iLastAnimationTime) / 1000f;
      iAnimatingPosition += iAnimatingVelocity * t;
      iLastAnimationTime = now;
      iCurrentAnimationTime += AnimationConfig.ANIMATION_FRAME_DURATION;
    }

    void computeLeftAnimation() {
      compute();
      if (iAnimatingPosition <= iAnimationDistance) {
        final OnLeftAnimationListener listener = mOnLeftAnimationListener;
        if (listener != null) {
          listener.onLeftAnimationEnd();
        }
        iAnimating = false;
        mPositionState = STATE_COLLAPSE_LEFT;
        offset();
      } else {
        float offset = AnimationConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
        Log.d(TAG, "@computeLeftAnimation " + offset);
        mLeftTranslate = (int) (offset + iAnimationStart);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT, iCurrentAnimationTime);
      }
    }

    void computeRightAnimation() {
      compute();
      if (iAnimatingPosition >= iAnimationDistance) {
        final OnRightAnimationListener listener = mOnRightAnimationListener;
        if (listener != null) {
          listener.onRightAnimationEnd();
        }
        iAnimating = false;
        mPositionState = STATE_COLLAPSE_RIGHT;
        offset();
      } else {
        float offset = AnimationConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
        mLeftTranslate = (int) (offset + iAnimationStart);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT, iCurrentAnimationTime);
      }
    }

    void computeLeftOpenAnimation() {
      compute();
      if (iAnimatingPosition >= iAnimationDistance) {
        for (final OnOpenAnimationListener listener : mOnOpenAnimationListener) {
          if (listener != null) {
            listener.onOpenAnimationEnd();
          }
        }
        iAnimating = false;
        mPositionState = STATE_EXPAND;
        offset();
      } else {
        float offset = AnimationConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
        mLeftTranslate = (int) (offset + iAnimationStart);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT_OPEN, iCurrentAnimationTime);
      }
    }

    void computeRightOpenAnimation() {
      compute();
      if (iAnimatingPosition <= iAnimationDistance) {
        for (final OnOpenAnimationListener listener : mOnOpenAnimationListener) {
          if (listener != null) {
            listener.onOpenAnimationEnd();
          }
        }
        iAnimating = false;
        mPositionState = STATE_EXPAND;
        offset();
      } else {
        float offset = AnimationConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
        mLeftTranslate = (int) (offset + iAnimationStart);
        invalidate();
        mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT_OPEN, iCurrentAnimationTime);
      }
    }

    void animateLeftOpen(float velocity) {
      for (final OnOpenAnimationListener listener : mOnOpenAnimationListener) {
        if (listener != null) {
          listener.onOpenAnimationStart();
        }
      }
      iAnimating = true;
      final long now = SystemClock.uptimeMillis();
      iLastAnimationTime = now;
      iCurrentAnimationTime = now + AnimationConfig.ANIMATION_FRAME_DURATION;
      iAnimatingVelocity = velocity;
      iAnimatingPosition = 0;
      iAnimationDistance = 0 - mLeftTranslate;
      iAnimationStart = mLeftTranslate;
      mHandler.removeMessages(MSG_ANIMATE_LEFT_OPEN);
      Log.d(TAG, "@animateLeftOpen " + iAnimationDistance);
      Log.d(TAG, "@animateLeftOpen " + velocity);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT_OPEN, iCurrentAnimationTime);
    }

    void animateRightOpen(float velocity) {
      for (final OnOpenAnimationListener listener : mOnOpenAnimationListener) {
        if (listener != null) {
          listener.onOpenAnimationStart();
        }
      }
      iAnimating = true;
      final long now = SystemClock.uptimeMillis();
      iLastAnimationTime = now;
      iCurrentAnimationTime = now + AnimationConfig.ANIMATION_FRAME_DURATION;
      iAnimatingVelocity = velocity;
      iAnimatingPosition = 0;
      iAnimationDistance = 0 - mLeftTranslate;
      iAnimationStart = mLeftTranslate;
      Log.d(TAG, "@animateRightOpen " + iAnimationDistance);
      Log.d(TAG, "@animateRightOpen " + velocity);
      mHandler.removeMessages(MSG_ANIMATE_RIGHT_OPEN);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT_OPEN, iCurrentAnimationTime);
    }

    void animateLeft(float velocity) {
      final OnLeftAnimationListener listener = mOnLeftAnimationListener;
      if (listener != null) {
        listener.onLeftAnimationStart();
      }
      iAnimating = true;
      final long now = SystemClock.uptimeMillis();
      iLastAnimationTime = now;
      iCurrentAnimationTime = now + AnimationConfig.ANIMATION_FRAME_DURATION;
      iAnimatingVelocity = velocity;
      iAnimatingPosition = 0;
      iAnimationDistance = -getMeasuredWidth() + mLeftOffset - mLeftTranslate;
      iAnimationStart = mLeftTranslate;
      Log.d(TAG, "@animateLeft " + iAnimationDistance);
      Log.d(TAG, "@animateLeft " + velocity);
      mHandler.removeMessages(MSG_ANIMATE_LEFT);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_LEFT, iCurrentAnimationTime);
    }

    void animateRight(float velocity) {
      final OnRightAnimationListener listener = mOnRightAnimationListener;
      if (listener != null) {
        listener.onRightAnimationStart();
      }
      iAnimating = true;
      final long now = SystemClock.uptimeMillis();
      iLastAnimationTime = now;
      iCurrentAnimationTime = now + AnimationConfig.ANIMATION_FRAME_DURATION;
      iAnimatingVelocity = velocity;
      iAnimatingPosition = 0;
      iAnimationDistance = (getMeasuredWidth() - mRightOffset) - mLeftTranslate;
      iAnimationStart = mLeftTranslate;
      Log.d(TAG, "@animateRight " + iAnimationDistance);
      Log.d(TAG, "@animateRight " + velocity);
      mHandler.removeMessages(MSG_ANIMATE_RIGHT);
      mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT, iCurrentAnimationTime);
    }
  }

  public void dump() {
    Log.d(TAG, "@dump left offset " + mLeftOffset);
    Log.d(TAG, "@dump right offset " + mRightOffset);
    Log.d(TAG, "@dump track " + mTrackDirection);
    Log.d(TAG, "@dump left tap " + mLeftTapBack);
    Log.d(TAG, "@dump right tap " + mRightOffset);
  }
}
