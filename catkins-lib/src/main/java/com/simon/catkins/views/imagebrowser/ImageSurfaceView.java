package com.simon.catkins.views.imagebrowser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.*;
import com.simon.catkins.views.TransitionAnimator;

import static android.view.MotionEvent.*;

/**
 * @author bb.simon.yu@gmail.com
 */
public class ImageSurfaceView extends SurfaceView implements SurfaceHolder.Callback2, ScaleGestureDetector.OnScaleGestureListener {
  private static final String TAG = "ImageSurfaceView";

  private static final int BACKGROUND_COLOR = 0xFF000000;

  private static final float SCALE_MIN = 0.5f;
  private static final float SCALE_MAX = 2.f;

  private Bitmap mBitmap;
  private Paint mPaint = new Paint();

  private int mBitmapWidth;
  private int mBitmapHeight;
  private int mWidth;
  private int mHeight;
  private float mLastDownX;
  private float mLastDownY;
  private float mLastMoveX;
  private float mLastMoveY;

  private float mScale = 1.f;
  private float mXOffset;
  private float mYOffset;

  private int mActivePointerId = INVALID_POINTER_ID;

  private ScaleGestureDetector mScaleGestureDetector;


  private VelocityTracker mVelocityTracker;

  private final TransitionAnimator mTransitionAnimator = new DefaultTransactionAnimator();

  public ImageSurfaceView(Context context) {
    this(context, null, 0);
  }

  public ImageSurfaceView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ImageSurfaceView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    getHolder().addCallback(this);
    mScaleGestureDetector = new ScaleGestureDetector(context, this);
  }

  public void setBitmap(Bitmap bitmap) {
    mBitmap = bitmap;
    mBitmapWidth = mBitmap.getWidth();
    mBitmapHeight = mBitmap.getHeight();
    invalidate();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override
  public void surfaceRedrawNeeded(SurfaceHolder holder) {
    Canvas canvas = holder.lockCanvas();
    mTransitionAnimator.draw(canvas);
    holder.unlockCanvasAndPost(canvas);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mTransitionAnimator.measure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return mTransitionAnimator.touchEvent(event);
  }

  @Override
  public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
    mScale *= scaleGestureDetector.getScaleFactor();
    mScale = Math.max(SCALE_MIN, Math.min(SCALE_MAX, mScale));
    surfaceRedrawNeeded(getHolder());
    return true;
  }

  @Override
  public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
    return true;
  }

  @Override
  public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
  }

  private class DefaultTransactionAnimator implements TransitionAnimator {

    @Override
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
      final int widthSize = widthMeasureSpec & ~(0x3 << 30);
      final int heightSize = heightMeasureSpec & ~(0x3 << 30);
      mWidth = widthSize;
      mHeight = heightSize;
      setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public void layout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public void draw(Canvas canvas) {
      canvas.drawColor(BACKGROUND_COLOR);

      final int savedCount = canvas.save();

      canvas.scale(mScale, mScale, mWidth / 2, mHeight / 2);
      canvas.translate((mWidth - mBitmapWidth) / 2, (mHeight - mBitmapHeight) / 2);
      canvas.drawBitmap(mBitmap, mXOffset / mScale, mYOffset / mScale, mPaint);

      canvas.restoreToCount(savedCount);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
      return false;
    }

    @Override
    public boolean interceptionTouchEvent(MotionEvent event) {
      return false;
    }

    @Override
    public boolean touchEvent(MotionEvent event) {

      mScaleGestureDetector.onTouchEvent(event);

      final int action = MotionEventCompat.getActionMasked(event);


      switch (action) {
        case ACTION_DOWN: {
          final int pointerIndex = MotionEventCompat.getActionIndex(event);
          final float x = MotionEventCompat.getX(event, pointerIndex);
          final float y = MotionEventCompat.getY(event, pointerIndex);
          mLastMoveX = x;
          mLastMoveY = y;
          mVelocityTracker = VelocityTracker.obtain();
          mActivePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
          break;
        }
        case ACTION_MOVE: {
          final int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
          final float x = MotionEventCompat.getX(event, pointerIndex);
          final float y = MotionEventCompat.getY(event, pointerIndex);


          if (!mScaleGestureDetector.isInProgress()) {
            mXOffset += x - mLastMoveX;
            mYOffset += y - mLastMoveY;
            surfaceRedrawNeeded(getHolder());
          }

          mLastMoveX = x;
          mLastMoveY = y;

          break;

        }
        case ACTION_UP:
        case ACTION_CANCEL: {
          mActivePointerId = INVALID_POINTER_ID;
          break;
        }
        case ACTION_POINTER_UP: {
          final int pointerIndex = MotionEventCompat.getActionIndex(event);
          final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

          if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMoveX = MotionEventCompat.getX(event, newPointerIndex);
            mLastMoveY = MotionEventCompat.getY(event, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
          }
        }

      }

      return true;
    }

    @Override
    public void animate(int msg) {
    }

    @Override
    public boolean isAnimating() {
      return false;
    }
  }
}
