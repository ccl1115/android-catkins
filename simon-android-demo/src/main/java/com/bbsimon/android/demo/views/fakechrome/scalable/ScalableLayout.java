package com.bbsimon.android.demo.views.fakechrome.scalable;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.FrameLayout;

@SuppressWarnings("unused")
public class ScalableLayout extends FrameLayout implements IScalable {

  public static final float SCALE = 1f;

  private static final String TAG = "IScalableImpl";

  private static GradientDrawable SHADE;

  private static final int SHADE_HEIGHT = 20; // dips
  private static final int DEGREE = 15;
  private static final int DEPTH_CONSTANT = 25;
  private static final int HORIZONTAL_OFFSET = 15;

  private static final int MSG_EXPAND = 0xF9;
  private static final int MSG_SCALE = 0xFA;

  private static final int FRAME_ANIMATION_DURATION = 1000 / 60;

  private int mState;
  private int mType;
  private float mScale;

  private final int mDepthConstant;
  private final int mHorizontalOffset;

  private final Animator mAnimator;
  private final AnimatorHandler mHandler;
  private final Camera mCamera;
  private final Matrix mMatrix;

  private long mCurrentAnimationTime;
  private long mLastAnimationTime;

  private int mCenterX;
  private int mCenterY;

  private OnScaleListener mOnScaleListener;
  private OnExpandListener mOnExpandListener;

  public ScalableLayout(Context context) {
    this(context, null, 0);
  }

  public ScalableLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ScalableLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mState = STATE_EXPAND;
    mType = NO_ROTATE;
    mScale = 0;
    mAnimator = new Animator();
    mHandler = new AnimatorHandler();
    mCamera = new Camera();
    mMatrix = new Matrix();

    final float density = getResources().getDisplayMetrics().density;
    mDepthConstant = (int) (density * DEPTH_CONSTANT + 0.5f);
    mHorizontalOffset = (int) (density * HORIZONTAL_OFFSET + 0.5f);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    mCenterX = getMeasuredWidth() / 2;
    mCenterY = getMeasuredHeight() / 2;

    // construction of shade drawable.
    if (SHADE == null) {
      SHADE = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
          new int[]{0x66000000, 0x00000000});
      final float density = getResources().getDisplayMetrics().density;
      final int height = (int) (SHADE_HEIGHT * density + 0.5f);
      SHADE.setBounds(0, 0, getMeasuredWidth(), height);
    }
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    final int centerX = mCenterX;
    final int centerY = mCenterY;
    canvas.save();
    mCamera.save();
    switch (mType) {
      case LEFT_ROTATE:
        mCamera.rotateY(Math.min(0, (0.7f - mScale) * DEGREE));
        mCamera.translate(-mScale * mHorizontalOffset,
            0f, Math.min(mDepthConstant, mScale * mDepthConstant));
        break;
      case NO_ROTATE:
        mCamera.translate(0.0f, 0.0f,
            Math.min(mDepthConstant, mScale * mDepthConstant));
        break;
      case RIGHT_ROTATE:
        mCamera.rotateY(Math.max(0, (-0.7f + mScale) * DEGREE));
        mCamera.translate(mScale * mHorizontalOffset,
            0f, Math.min(mDepthConstant, mScale * mDepthConstant));
        break;
      default:
        break;
    }
    mCamera.getMatrix(mMatrix);
    mMatrix.preTranslate(-centerX, -centerY);
    mMatrix.postTranslate(centerX, centerY);
    mCamera.restore();
    canvas.concat(mMatrix);
  }

  @Override
  public void setTransformType(int type) {
    mType = type;
  }

  @Override
  public void startScale() {
    if (mState != STATE_SCALED && !mAnimator.animating) {
      mAnimator.animateScale(0f);
    }
  }

  @Override
  public void startExpand() {
    if (!mAnimator.animating) {
      mAnimator.animateExpand(mScale);
    }
  }

  @Override
  public void transform(float scale) {
    if (scale <= SCALE && scale >= 0f) {
      mScale = scale;
    } else if (scale > SCALE) {
      mScale = SCALE;
    } else if (scale < 0f) {
      mScale = 0;
    }
    invalidate();
  }

  @Override
  public float getScale() {
    return mScale;
  }

  @Override
  public void setOnScaleListener(OnScaleListener listener) {
  }

  @Override
  public void setOnExpandListener(OnExpandListener listener) {
  }

  private class AnimatorHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_EXPAND:
          mAnimator.computeExpand();
          break;
        case MSG_SCALE:
          mAnimator.computeScale();
          break;
        default:
          break;
      }
    }
  }

  private class Animator {
    static final String TAG = "IScalableImpl$Animator";

    static final float VELOCITY = 3.8f;

    float velocity;
    float animatingPosition;
    float animatingVelocity;
    boolean animating;

    Animator() {
      velocity = VELOCITY;
    }

    void compute() {
      final long now = SystemClock.uptimeMillis();
      final float t = (now - mLastAnimationTime) / 1000f;
      mLastAnimationTime = now;
      mCurrentAnimationTime = mLastAnimationTime;
      animatingPosition += animatingVelocity * t;
    }

    void computeScale() {
      compute();
      if (animatingPosition >= SCALE) {
        mScale = SCALE;
        mState = STATE_SCALED;
        animating = false;
        mType = NO_ROTATE;

        OnScaleListener listener = mOnScaleListener;
        if (listener != null) {
          listener.onScaled();
        }
      } else {
        mScale = animatingPosition;
        mCurrentAnimationTime += FRAME_ANIMATION_DURATION;
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_SCALE), mCurrentAnimationTime);
      }
      invalidate();
    }

    void computeExpand() {
      compute();
      if (animatingPosition <= 0) {
        mScale = 0;
        mState = STATE_EXPAND;
        animating = false;
        mType = NO_ROTATE;

        OnExpandListener listener = mOnExpandListener;
        if (listener != null) {
          listener.onExpanded();
        }
      } else {
        mScale = animatingPosition;
        mCurrentAnimationTime += FRAME_ANIMATION_DURATION;
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_EXPAND), mCurrentAnimationTime);
      }
      invalidate();
    }

    void animateScale(float scale) {
      animatingPosition = scale;
      animatingVelocity = velocity;
      animating = true;
      final long now = SystemClock.uptimeMillis();
      mLastAnimationTime = now;
      mCurrentAnimationTime = now + FRAME_ANIMATION_DURATION;
      mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_SCALE), mCurrentAnimationTime);
    }

    void animateExpand(float scale) {
      animatingPosition = scale;
      animatingVelocity = -velocity;
      animating = true;
      final long now = SystemClock.uptimeMillis();
      mLastAnimationTime = now;
      mCurrentAnimationTime = now + FRAME_ANIMATION_DURATION;
      mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_EXPAND), mCurrentAnimationTime);
    }
  }
}
