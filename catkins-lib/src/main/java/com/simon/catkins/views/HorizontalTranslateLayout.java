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

import java.util.ArrayList;
import java.util.List;

import de.akquinet.android.androlog.Log;


/**
 * HorizontalTranslateLayout
 * <p/>
 * <b>This layout doesn't support using drawable as background. Only color supported</b>
 */
public class HorizontalTranslateLayout extends FrameLayout {
    public static final int STATE_COLLAPSE_LEFT = 10000;
    public static final int STATE_COLLAPSE_RIGHT = 10001;
    public static final int STATE_EXPAND = 10004;

    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String HORIZONTAL = "horizontal";

    private static final String TAG = "HorizontalTranslateLayout";

    private int mMeasuredWidth;
    private int mMeasuredHeight;

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
    private int mLastMoveY;
    private boolean mLastMoveXBeenSet;
    private boolean mLastMoveYBeenSet;

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
        super(context, attrs, 0);
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

        mLeftOffset = a.getDimension(R.styleable.HorizontalTranslateLayout_leftOffset, -1f);
        mRightOffset = a.getDimension(R.styleable.HorizontalTranslateLayout_rightOffset, -1f);

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
                a.getString(R.styleable.HorizontalTranslateLayout_tapBack);
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

        mBackgroundPaint.setColor(a.getColor(R.styleable.HorizontalTranslateLayout_background, 0));

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
    public void setProportion(float proportion) {
        if (proportion < -1f || proportion > 1f) {
            return;
        }
        if (proportion < 0f) {
            mLeftTranslate = (int) ((mLeftOffset - mMeasuredWidth) * -proportion);
        } else if (proportion > 0f) {
            mLeftTranslate = (int) ((mMeasuredWidth - mRightOffset) * proportion);
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
     * The offset value when flip to top.
     *
     * @return the left offset
     */
    public int getLeftOffset() {
        return (int) mLeftOffset;
    }

    /**
     * The offset value when flip to bottom.
     *
     * @return the mTop offset
     */
    public int getRightOffset() {
        return (int) mRightOffset;
    }

    /**
     * tap top offset area to flip back.
     *
     * @return true if allow tap back
     */
    public boolean isLeftTapBack() {
        return mLeftTapBack;
    }

    /**
     * tap bottom offset area to flip back.
     *
     * @return true if allow tap back
     */
    public boolean isRightTapBack() {
        return mRightTapBack;
    }

    /**
     * Set top offset area could tap back.
     *
     * @param tapBack tap back
     */
    public void setLeftTapBack(boolean tapBack) {
        mLeftTapBack = tapBack;
    }

    /**
     * Set bottom offset area could tap back.
     *
     * @param tapBack tap back
     */
    public void setRightTapBack(boolean tapBack) {
        mRightTapBack = tapBack;
    }

    /**
     * Flip top immediately, without animation.
     */
    public void left() {
        mLeftTranslate = (int) (mLeftOffset - getMeasuredWidth());
        mPositionState = STATE_COLLAPSE_LEFT;
        invalidate();
    }

    /**
     * Flip bottom immediately, without animation.
     */
    public void right() {
        mLeftTranslate = (int) (getMeasuredWidth() - mRightOffset);
        mPositionState = STATE_COLLAPSE_RIGHT;
        invalidate();
    }

    /**
     * Open host view when flipped.
     */
    public void open() {
        mLeftTranslate = 0;
        mPositionState = STATE_EXPAND;
        invalidate();
    }

    /**
     * Animation version of flipping
     */
    public void animateLeft() {
        if (canLeft()) {
            mAnimator.animateLeft(-mAnimator.kVelocity);
        }
    }

    /**
     * Animation version of flipping
     */
    public void animateRight() {
        if (canRight()) {
            mAnimator.animateRight(mAnimator.kVelocity);
        }
    }

    /**
     * Animation version of flipping
     */
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
    public int getState() {
        return mPositionState;
    }

    public void setLeftAnimationListener(OnLeftAnimationListener listener) {
        mOnLeftAnimationListener = listener;
    }

    public void setRightAnimationListener(OnRightAnimationListener listener) {
        mOnRightAnimationListener = listener;
    }

    public void addOpenAnimationListener(OnOpenAnimationListener listener) {
        mOnOpenAnimationListener.add(listener);
    }

    public void removeOpenAnimationListener(OnOpenAnimationListener listener) {
        mOnOpenAnimationListener.remove(listener);
    }

    public void setLeftTrackListener(OnLeftTrackListener listener) {
        mOnLeftTrackListener = listener;
    }

    public void setRightTrackListener(OnRightTrackListener listener) {
        mOnRightTrackListener = listener;
    }

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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mTrackDirection == TrackDirection.none) {
            // None track direction so do not intercept touch event
            return false;
        }

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (mPositionState == STATE_EXPAND) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mLastDownX = x;
                    mLastDownY = y;

                    // 停止所有的动画
                    mHandler.removeMessages(MSG_ANIMATE_LEFT);
                    mHandler.removeMessages(MSG_ANIMATE_LEFT_OPEN);
                    mHandler.removeMessages(MSG_ANIMATE_RIGHT);
                    mHandler.removeMessages(MSG_ANIMATE_RIGHT_OPEN);
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "@interceptInterceptTouchEvent");
                    ev.offsetLocation(-mLeftTranslate, 0);
                    return prepareTracking(x, y);
                default:
                    break;
            }
        } else {
            // In collapsed position, intercept even directly.
            // Because children should not receive any event when drawer collapsed.
            Log.d(TAG, "Intercepted to onTouch()");
            return true;
        }
        return false;
    }

    private boolean prepareTracking(int x, int y) {
        // We don't intercept event instantly, for children like ListView want to hold the event chain.
        // In most cases return true.
        return !(y < mLastDownY - mTouchThreshold || y > mLastDownY + mTouchThreshold)
                && (x < mLastDownX - mTouchThreshold || x > mLastDownX + mTouchThreshold)
                && mTracker.prepareTracking(x - mLastDownX);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        if (mPositionState == STATE_EXPAND) {
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    if (mTracker.tracking) {
                        if (!mLastMoveXBeenSet) {
                            if (x > mLastDownX) {
                                mLastMoveX = mLastDownX + mTouchThreshold;
                                mLastMoveXBeenSet = true;
                            } else {
                                mLastMoveX = mLastDownX - mTouchThreshold;
                                mLastMoveXBeenSet = true;
                            }
                        }

                        mTracker.move(mLastMoveX - x);
                        mLastMoveX = x;
                        mTracker.velocityTracker.addMovement(ev);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "@onTouchEvent up");
                    mLastMoveXBeenSet = false;

                    if (mTracker.tracking) {
                        Log.d(TAG, "@onTouchEvent tracking");
                        mTracker.stopTracking();
                        mTracker.fling();
                    }
                    return true;
                default:
                    return false;
            }
        } else {
            Log.d(TAG, String.format("collapse x=%d, y=%d", x, y));
            Log.d(TAG, "left tap back frame = " + mLeftFrameForTap);
            Log.d(TAG, "right tap back frame = " + mRightFrameForTap);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (((mPositionState == STATE_COLLAPSE_LEFT) && mLeftFrameForTap.contains(x, y))
                            || ((mPositionState == STATE_COLLAPSE_RIGHT) && mRightFrameForTap.contains(x, y))) {
                        if (!mTracker.tracking) {
                            mLastMoveX = x;
                            mTracker.prepareTracking(x);
                        }
                    } else {
                        // Do not consume this event, dispatch to its siblings.
                        return false;
                    }
                case MotionEvent.ACTION_MOVE:
                    if (mTracker.tracking) {
                        mTracker.move(mLastMoveX - x);
                        mLastMoveX = x;
                        mTracker.velocityTracker.addMovement(ev);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (mTracker.tracking) {
                        mTracker.stopTracking();
                        mTracker.fling();
                    }
                    break;
            }
        }
        return true;
    }

    /**
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed) {
            if (mLeftOffset != -1) {
                mLeftFrameForTap.set(l, t, (int) (l + mLeftOffset), b);
            }

            if (mRightOffset != -1) {
                mRightFrameForTap.set((int) (r - mRightOffset), t, r, b);
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
                "top offset should not be larger than the view's width";
        assert widthSize >= mRightOffset :
                "bottom offset should not be larger than the view's width";

        // cache dimension
        mMeasuredWidth = getMeasuredWidth();
        mMeasuredHeight = getMeasuredHeight();
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
        final int velocityUnit;
        final int minVelocity;

        Tracker() {
            float density = getContext().getResources().getDisplayMetrics().density;
            velocityUnit = (int) (VELOCITY_UNIT * density + 0.5f);
            minVelocity = (int) (MIN_VELOCITY * density + 0.5f);
        }

        boolean prepareTracking(int start) {
            switch (mTrackDirection) {
                case left:
                    if (mPositionState != STATE_EXPAND &&
                            mPositionState != STATE_COLLAPSE_LEFT) {
                        return false;
                    }
                    break;
                case right:
                    if (mPositionState != STATE_EXPAND &&
                            mPositionState != STATE_COLLAPSE_RIGHT) {
                        return false;
                    }
                    break;
                case horizontal:
                    if (mOnHorizontalTrackListener != null) {
                        final OnHorizontalTrackListener listener = mOnHorizontalTrackListener;
                        listener.onStartHorizontalTrack(start);
                    }
            }
            velocityTracker = VelocityTracker.obtain();
            tracking = true;
            return true;
        }

        void stopTracking() {
            tracking = false;
        }

        void move(int xOffset) {
            if (!tracking) {
                return;
            }
            final int left = mLeftTranslate - xOffset;
            switch (mTrackDirection) {
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

            switch (mTrackDirection) {
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
    }

    private class Animator {
        static final String TAG = "Animator";

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
            iCurrentAnimationTime += ViewConfig.ANIMATION_FRAME_DURATION;
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            iAnimatingVelocity = velocity;
            iAnimatingPosition = 0;
            iAnimationDistance = 0 - mLeftTranslate;
            iAnimationStart = mLeftTranslate;
            mHandler.removeMessages(MSG_ANIMATE_LEFT_OPEN);
            Log.d(TAG, "@animateTopOpen " + iAnimationDistance);
            Log.d(TAG, "@animateTopOpen " + velocity);
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            iAnimatingVelocity = velocity;
            iAnimatingPosition = 0;
            iAnimationDistance = 0 - mLeftTranslate;
            iAnimationStart = mLeftTranslate;
            Log.d(TAG, "@animateBottomOpen " + iAnimationDistance);
            Log.d(TAG, "@animateBottomOpen " + velocity);
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            iAnimatingVelocity = velocity;
            iAnimatingPosition = 0;
            iAnimationDistance = -getMeasuredWidth() + mLeftOffset - mLeftTranslate;
            iAnimationStart = mLeftTranslate;
            Log.d(TAG, "@animateTop " + iAnimationDistance);
            Log.d(TAG, "@animateTop " + velocity);
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
            iAnimatingVelocity = velocity;
            iAnimatingPosition = 0;
            iAnimationDistance = (getMeasuredWidth() - mRightOffset) - mLeftTranslate;
            iAnimationStart = mLeftTranslate;
            Log.d(TAG, "@animateBottom " + iAnimationDistance);
            Log.d(TAG, "@animateBottom " + velocity);
            mHandler.removeMessages(MSG_ANIMATE_RIGHT);
            mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_RIGHT, iCurrentAnimationTime);
        }
    }

    public void dump() {
        Log.d(TAG, "@dump top offset " + mLeftOffset);
        Log.d(TAG, "@dump bottom offset " + mRightOffset);
        Log.d(TAG, "@dump track " + mTrackDirection);
        Log.d(TAG, "@dump top tap " + mLeftTapBack);
        Log.d(TAG, "@dump bottom tap " + mRightOffset);
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
         * fire when top animation starts.
         */
        void onLeftAnimationStart();

        /**
         * fire when top animation ends.
         */
        void onLeftAnimationEnd();
    }

    interface OnRightAnimationListener {
        /**
         * fire when bottom animation starts.
         */
        void onRightAnimationStart();

        /**
         * fire when bottom animation ends.
         */
        void onRightAnimationEnd();
    }

    interface OnLeftTrackListener {
        void onLeftTrackStart();
    }

    interface OnRightTrackListener {
        void onRightTrackStart();
    }

    interface OnHorizontalTrackListener {
        void onStartHorizontalTrack(int direction);
    }
}
