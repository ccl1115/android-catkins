package com.bbsimon.android.demo.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.bbsimon.android.demo.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Stack;

/**
 * 实现层叠显示内容的控件
 * @author ccl
 */
public class StackLayout extends ViewGroup {


    private static final String TAG = "StackLayout";

    // for debug
    private static final boolean DEBUG = true;
    private long mSingleAnimationTime = 0;
    private long mSingleAnimationCount = 0;

    private static final int MSG_ENTER_ANIMATION = -1000;
    private static final int MSG_EXIT_ANIMATION = -1001;

    private static final int TRANSLATE_VELOCITY = 1000;
    private static final int ALPHA_VELOCITY = 200;
    private static final int DECELERATION_THRESHOLD = 50;
    private static final int ANIMATION_FRAME_DURATION = 1000 / 60;

    private List<View> mViewStack;
    private View mTop;
    private View mPreviousTop;

    private Paint mShadePaint = new Paint();
    private Paint mDrawPaint = new Paint();
    private AnimationHandler mAnimationHandler = new AnimationHandler();

    private int mLeft;
    private int mAlpha;
    private int mWidth;

    private boolean mStopAnimatingAlpha = false;

    private boolean mAnimating;

    private OnStackAnimationListener mOnStackAnimationListener;

    private int mDecelerationThreshold;
    private int mTranslateVelocity;
    private int mAlphaVelocity;
    private float mAnimatingTranslateVelocity;
    private float mAnimationPosition;
    private int mAnimatingAlphaVelocity;
    private float mAnimationAlpha;
    private long mAnimationLastTime;
    private long mCurrentAnimationTime;
    private int mHeight;
    private Bitmap mTopCache;
    private Bitmap mPTopCache;

    public StackLayout(Context context) {
        this(context, null, 0);
    }

    public StackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mViewStack = new Stack<View>();
        setBackgroundColor(0xFFFFFFFF);
        mShadePaint.setColor(0x00000000);

        final float density = getResources().getDisplayMetrics().density;
        mTranslateVelocity = (int) (density * TRANSLATE_VELOCITY + 0.5);
        mAlphaVelocity = (int) (density * ALPHA_VELOCITY + 0.5);
        mDecelerationThreshold = (int) (density * DECELERATION_THRESHOLD + 0.5);


        setClickable(true);
    }

    public View top() {
        if (mViewStack.size() == 0) {
            return null;
        }
        return mViewStack.get(mViewStack.size() - 1);
    }

    public int size() {
        return mViewStack.size();
    }

    public void clear() {
        mViewStack.clear();
        removeAllViews();
    }

    public void setOnStackAnimationListener(OnStackAnimationListener listener) {
        mOnStackAnimationListener = listener;
    }

    public boolean isAnimating() {
        return mAnimating;
    }

    @Override
    public void addView(View child) {
        Log.d(TAG, "@addView");
        mPreviousTop = mTop;
        mTop = child;
        mViewStack.add(child);
        super.addView(child);

        prepareContent();

        if (mViewStack.size() == 1) {
            return;
        }

        if (DEBUG) {
            mSingleAnimationTime = SystemClock.uptimeMillis();
            mSingleAnimationCount = 0;
        }

        // Skip the animation when added
        assert child != null;
        Boolean b = (Boolean) child.getTag(R.id.use_animation);
        if (b != null && b) {
            return;
        }

        mAlpha = 0;
        mShadePaint.setAlpha(mAlpha);
        mLeft = mWidth;
        mStopAnimatingAlpha = true;
        mAnimationPosition = mLeft;
        mAnimationAlpha = mAlpha;
        mAnimatingAlphaVelocity = mAlphaVelocity;
        mAnimatingTranslateVelocity = -mTranslateVelocity;

        mAnimationHandler.removeMessages(MSG_ENTER_ANIMATION);
        mAnimationHandler.removeMessages(MSG_EXIT_ANIMATION);
        long now = SystemClock.uptimeMillis();
        mAnimationLastTime = now;
        mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
        mAnimating = true;
        mAnimationHandler.sendMessageAtTime(mAnimationHandler.obtainMessage(MSG_ENTER_ANIMATION),
                mCurrentAnimationTime);

        if (mOnStackAnimationListener != null) {
            final OnStackAnimationListener listener = mOnStackAnimationListener;
            listener.onStackPushAnimationStart();
        }
    }

    @Override
    public void removeView(View view) {
        Log.d(TAG, "@removeView");

        if (view != null) {
            if (mViewStack.size() == 1) {
                // won't do animation
                mViewStack.remove(mTop);
                mPreviousTop = null;
                mTop = null;
                super.removeView(mTop);
                invalidate();
                return;
            } else if (mViewStack.size() > 2 && view.equals(mViewStack.get(1))) {
                mViewStack.remove(view);
                super.removeView(view);
                invalidate();
                return;
            } else if(!view.equals(mTop)) {
                return;
            }
        }


        assert view != null;
        Object useAnimation = view.getTag(R.id.use_animation);
        if (useAnimation != null && useAnimation instanceof Boolean && !(Boolean) useAnimation) {
            postRemoveView();
            return;
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            postRemoveView();
            return;
        }

        prepareContent();

        mAlpha = 150;
        mShadePaint.setAlpha(150);
        mLeft = 0;
        mAnimationPosition = mLeft;
        mAnimationAlpha = mAlpha;
        mAnimatingAlphaVelocity = (int) (-mAlphaVelocity * 1.5f);
        mAnimatingTranslateVelocity = mTranslateVelocity;

        mAnimationHandler.removeMessages(MSG_EXIT_ANIMATION);
        mAnimationHandler.removeMessages(MSG_ENTER_ANIMATION);
        long now = SystemClock.uptimeMillis();
        mAnimationLastTime = now;
        mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
        mAnimating = true;
        mAnimationHandler.sendMessageAtTime(mAnimationHandler.obtainMessage(MSG_EXIT_ANIMATION),
                mCurrentAnimationTime);

        if (mOnStackAnimationListener != null) {
            mOnStackAnimationListener.onStackPopAnimationStart();
        }
    }

    private void postRemoveView() {
        mViewStack.remove(mTop);
        super.removeView(mTop);
        mTop = top();
        if (mViewStack.size() >= 2) {
            mPreviousTop = mViewStack.get(mViewStack.size() - 2);
        } else {
            mPreviousTop = null;
        }
        mLeft = 0;
        mAlpha = 0;
        mShadePaint.setAlpha(mAlpha);

        if (mOnStackAnimationListener != null) {
            mOnStackAnimationListener.onStackPopAnimationEnd();
        }
        invalidate();
    }

    @Override
    public void removeAllViews() {
        //Log.d(TAG, "@removeAllViews");
        mViewStack.clear();
        mPreviousTop = null;
        mTop = null;
        super.removeAllViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.d(TAG, "@onMeasure");
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // loop children for measure
        if (mTop != null) {
            if (mTop.getLayoutParams() == null) {
                mTop.setLayoutParams(new LayoutParams(widthSize, heightSize));
            }
            measureChild(mTop,
                    MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //Log.d(TAG, "@onLayout");
        mWidth = r - l;
        mHeight = b - t;

        // loop children for layout
        if (changed) {
            for (View v : mViewStack) {
                v.layout(0, 0, mWidth, mHeight);
            }
        } else if (mTop != null) {
            mTop.layout(0, 0, mWidth, mHeight);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "@dispatchKeyEvent");
        if (mAnimating) {
            return true;
        }
        final View top = mTop;
        if (top != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return top.dispatchKeyEvent(event);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (mTop != null) {
            mTop.setVisibility(visibility);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        final long drawingTime = getDrawingTime();
        if (DEBUG) {
            mSingleAnimationCount += 1;
        }
        if (mTop != null) {
            if (mAnimating) {
                if (mPreviousTop != null) {
                    if (mPTopCache != null && !mPTopCache.isRecycled()) {
                        canvas.drawBitmap(mPTopCache, 0, 0, mDrawPaint);
                    } else {
                        drawChild(canvas, mPreviousTop, drawingTime);
                    }
                }
                canvas.drawRect(0, 0, mLeft, mHeight, mShadePaint);
                if (mTopCache != null && !mTopCache.isRecycled()) {
                    canvas.drawBitmap(mTopCache, mLeft, 0, mDrawPaint);
                } else {
                    canvas.save();
                    canvas.translate(mLeft, 0);
                    drawChild(canvas, mTop, drawingTime);
                    canvas.restore();
                }
            } else {
                drawChild(canvas, mTop, drawingTime);
            }
        }
        //Log.d(TAG, "@dispatchDraw drawingTime " + (SystemClock.uptimeMillis() - now));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "@dispatchTouchEvent");
        if (mTop != null) {
            return mTop.dispatchTouchEvent(ev);
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }

    private class AnimationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ENTER_ANIMATION:
                    doEntering();
                    break;
                case MSG_EXIT_ANIMATION:
                    doExiting();
                    break;
            }
        }
    }

    private void prepareContent() {
        //Log.d(TAG, "@prepareContent");
        if (mTop == null) {
            return;
        }

        if(mTop.isLayoutRequested()) {
            mTop.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
            mTop.layout(0, 0, mWidth, mHeight);
        }
        try {
            Method m = mTop.getClass().getMethod("isHardwareAccelerated");
            Boolean b = (Boolean) m.invoke(mTop, (Object[]) null);
            if (!b) {
                //Log.d(TAG, "@prepareContent cached");
                mTop.buildDrawingCache();
            } else {
                //Log.d(TAG, "@prepareContent accelerated");
            }
        } catch (NoSuchMethodException e) {
            //Log.d(TAG, "@prepareContent cached");
            mTop.buildDrawingCache();
        } catch (InvocationTargetException ignored) {
        } catch (IllegalAccessException ignored) {
        } finally {
            mTopCache = mTop.getDrawingCache();
        }

        if (mPreviousTop == null) {
            return;
        }

        if(mPreviousTop.isLayoutRequested()) {
            mPreviousTop.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
            mPreviousTop.layout(0, 0, mWidth, mHeight);
        }
        try {
            Method m = mPreviousTop.getClass().getMethod("isHardwareAccelerated");
            Boolean b = (Boolean) m.invoke(mPreviousTop, (Object[]) null);
            if (!b) {
                //Log.d(TAG, "@prepareContent cached");
                mPreviousTop.buildDrawingCache();
            } else {
                //Log.d(TAG, "@prepareContent accelerated");
            }
        } catch (NoSuchMethodException e) {
            //Log.d(TAG, "@prepareContent cached");
            mPreviousTop.buildDrawingCache();
        } catch (InvocationTargetException ignored) {
        } catch (IllegalAccessException ignored) {
        } finally {
            mPTopCache = mPreviousTop.getDrawingCache();
        }
    }


    private void doExiting() {
        //Log.d(TAG, "@doExiting");
        if (mAnimating) {
            computingExitAnimation();
            if (mAlpha <= 0 && !mStopAnimatingAlpha) {
                mAnimatingAlphaVelocity = 0;
                mShadePaint.setAlpha(0);
                mStopAnimatingAlpha = true;
            }
            if (mLeft >= mWidth) {
                //Log.d(TAG, "@doExiting\n==> exiting finished");
                mAnimating = false;
                mLeft = mWidth;
                mShadePaint.setAlpha(0);
                mStopAnimatingAlpha = false;
                postRemoveView();
            } else {
                mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
                mAnimationHandler.sendMessageAtTime(mAnimationHandler.obtainMessage(
                        MSG_EXIT_ANIMATION), mCurrentAnimationTime);
            }
            invalidate();
        }
    }


    private void doEntering() {
        //Log.d(TAG, "@doEntering");
        if (mAnimating) {
            computingEnterAnimation();
            mStopAnimatingAlpha = mLeft > mWidth / 2;
            if (mAlpha >= 200 && !mStopAnimatingAlpha) {
                mAnimatingAlphaVelocity = 0;
                mShadePaint.setAlpha(200);
                mStopAnimatingAlpha = true;
            }
            if (mLeft <= 0) {
                //Log.d(TAG, "@doEntering\n==> entering finished");
                if (DEBUG) {
                    Log.d(TAG,
                            "@doEntering animation fps " + (1000f * mSingleAnimationCount /
                                    (SystemClock.uptimeMillis() - mSingleAnimationTime)));
                }
                mAnimating = false;
                mLeft = 0;
                mStopAnimatingAlpha = false;

                if (mOnStackAnimationListener != null) {
                    mOnStackAnimationListener.onStackPushAnimationEnd();
                }
            } else {
                if (mLeft < mDecelerationThreshold) {
                    mAnimatingTranslateVelocity = mAnimatingTranslateVelocity * 0.9f;
                }
                mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
                mAnimationHandler.sendMessageAtTime(mAnimationHandler.obtainMessage(
                        MSG_ENTER_ANIMATION), mCurrentAnimationTime);
            }
            invalidate();
        }
    }

    private void computingExitAnimation() {
        //Log.d(TAG, "@computingExitAnimation");
        long now = SystemClock.uptimeMillis();
        float t = (now - mAnimationLastTime) / 1000f;
        final float p = mAnimationPosition;
        final float v = mAnimatingTranslateVelocity;
        // compute position
        mAnimationPosition = p + (v * t);
        mLeft = (int) mAnimationPosition;

        // compute shade
        if (!mStopAnimatingAlpha) {
            final float alpha = mAnimationAlpha;
            mAnimationAlpha = alpha + (mAnimatingAlphaVelocity * t);
            mAlpha = (int) mAnimationAlpha;
            mShadePaint.setAlpha(mAlpha);
        }

        mAnimationLastTime = now;
    }

    private void computingEnterAnimation() {  //代码冗余
        //Log.d(TAG, "@computingEnterAnimation");
        long now = SystemClock.uptimeMillis();
        float t = (now - mAnimationLastTime) / 1000f;
        final float p = mAnimationPosition;
        final float v = mAnimatingTranslateVelocity;
        // compute position
        mAnimationPosition = p + (v * t);
        mLeft = (int) mAnimationPosition;

        // compute shade
        if (!mStopAnimatingAlpha) {
            final float alpha = mAnimationAlpha;
            mAnimationAlpha = alpha + (mAnimatingAlphaVelocity * t);
            mAlpha = (int) mAnimationAlpha;
            mShadePaint.setAlpha(mAlpha);
        }

        mAnimationLastTime = now;
    }

    public interface OnStackAnimationListener {
        public void onStackPushAnimationEnd();

        public void onStackPushAnimationStart();

        public void onStackPopAnimationEnd();

        public void onStackPopAnimationStart();
    }
}
