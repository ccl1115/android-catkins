package com.simon.catkins.views.imagebrowser;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.simon.catkins.views.TransitionAnimator;

/**
 * @author bb.simon.yu@gmail.com
 */
public class ImageSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

  private Bitmap mBitmap;
  private Paint mPaint = new Paint();

  private int mBitmapWidth;
  private int mBitmapHeight;

  private float mScale = 1;
  private float mXOffset;
  private float mYOffset;

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
    Canvas c = holder.lockCanvas();

    c.drawColor(0xFF000000);

    c.drawBitmap(mBitmap,
        (width - mBitmapWidth + mXOffset) * mScale,
        (height - mBitmapHeight + mYOffset) * mScale,
        mPaint);

    holder.unlockCanvasAndPost(c);
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

  private class DefaultTransactionAnimator implements TransitionAnimator {

    @Override
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
      final int widthSize = widthMeasureSpec & ~(0x3 << 30);
      final int heightSize = heightMeasureSpec & ~(0x3 << 30);
      setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public void layout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public void draw(Canvas canvas) {
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
      final int action = event.getAction() & MotionEvent.ACTION_MASK;

      switch (action) {
        case MotionEvent.ACTION_DOWN:
          break;
        case MotionEvent.ACTION_MOVE:
          invalidate();
          break;
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
