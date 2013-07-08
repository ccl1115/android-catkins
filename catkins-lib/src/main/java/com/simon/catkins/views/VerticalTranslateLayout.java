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
 * VerticalTranslateLayout
 * <p/>
 * <b>This layout doesn't support using drawable as background. Only color supported</b>
 */
public class VerticalTranslateLayout extends FrameLayout {
    public static final int STATE_COLLAPSE_TOP = 10000;
    public static final int STATE_COLLAPSE_BOTTOM = 10001;
    public static final int STATE_EXPAND = 10004;

    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";
    public static final String VERTICAL = "vertical";
    private static final String TAG = "VerticalTranslateLayout";
    private int mMeasuredWidth;
    private int mMeasuredHeight;

    private enum TrackDirection {top, bottom, vertical, none}

    private final static int MSG_ANIMATE_TOP = -100;
    private final static int MSG_ANIMATE_BOTTOM = -101;
    private final static int MSG_ANIMATE_TOP_OPEN = -104;
    private final static int MSG_ANIMATE_BOTTOM_OPEN = -105;

    private final static int TAP_THRESHOLD = 35;

    private float mTopOffset;
    private float mBottomOffset;

    private int mTopTranslate;

    private final int mTouchThreshold;

    private boolean mTopTapBack;
    private boolean mBottomTapBack;

    private TrackDirection mTrackDirection;

    private int mPositionState;

    private final Rect mTopFrameForTap = new Rect();
    private final Rect mBottomFrameForTap = new Rect();
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

    private OnTopAnimationListener mOnTopAnimationListener;
    private OnBottomAnimationListener mOnBottomAnimationListener;
    private final List<OnOpenAnimationListener> mOnOpenAnimationListener =
            new ArrayList<OnOpenAnimationListener>();
    private OnTopTrackListener mOnTopTrackListener;
    private OnBottomTrackListener mOnBottomTrackListener;
    private OnVerticalTrackListener mOnVerticalTrackListener;

    public VerticalTranslateLayout(Context context, AttributeSet attrs) {
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
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.VerticalTranslateLayout);

        mTopOffset = a.getDimension(R.styleable.VerticalTranslateLayout_top_offset, -1f);
        mBottomOffset = a.getDimension(R.styleable.VerticalTranslateLayout_bottom_offset, -1f);

        final String track = a.getString(R.styleable.VerticalTranslateLayout_track);
        if (track != null && track.length() > 0) {
            if (mTopOffset != -1 && mBottomOffset != -1 && VERTICAL.equals(track)) {
                Log.d(TAG, "@parseTrack vertical");
                mTrackDirection = TrackDirection.vertical;
            } else if ((mBottomOffset != -1) && BOTTOM.equals(track)) {
                Log.d(TAG, "@parseTrack bottom");
                mTrackDirection = TrackDirection.bottom;
            } else if ((mTopOffset != -1) && TOP.equals(track)) {
                Log.d(TAG, "@parseTrack top");
                mTrackDirection = TrackDirection.top;
            } else {
                mTrackDirection = TrackDirection.none;
                Log.d(TAG, "@loadAttrs no direction");
            }
        }

        final String tapBackArea =
                a.getString(R.styleable.VerticalTranslateLayout_tap_back_area);
        if (tapBackArea != null && tapBackArea.length() > 0) {
            final String[] taps = tapBackArea.split("\\|");
            for (String s : taps) {
                Log.d(TAG, "@loadAttrs tap area " + s);
                if (TOP.equals(s) && mTopOffset != -1) {
                    mTopTapBack = true;
                } else if (BOTTOM.equals(s) && mBottomOffset != -1) {
                    mBottomTapBack = true;
                } else {
                    Log.d(TAG, "@loadAttrs tap_back_area value illegal");
                }
            }
        }

        mBackgroundPaint.setColor(a.getColor(R.styleable.VerticalTranslateLayout_background, 0));

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
            mTopTranslate = (int) ((mTopOffset - mMeasuredHeight) * -proportion);
        } else if (proportion > 0f) {
            mTopTranslate = (int) ((mMeasuredHeight - mBottomOffset) * proportion);
        } else if (proportion == 0f) {
            mTopTranslate = 0;
            mPositionState = STATE_EXPAND;
        } else if (proportion == -1f) {
            mTopOffset = mTopOffset - mMeasuredHeight;
            mPositionState = STATE_COLLAPSE_TOP;
        } else if (proportion == 1f) {
            mTopOffset = mMeasuredHeight - mBottomOffset;
            mPositionState = STATE_COLLAPSE_BOTTOM;
        }
        invalidate();
    }

    /**
     * The offset value when flip to top.
     *
     * @return the top offset
     */
    public int getTopOffset() {
        return (int) mTopOffset;
    }

    /**
     * The offset value when flip to bottom.
     *
     * @return the bottom offset
     */
    public int getBottomOffset() {
        return (int) mBottomOffset;
    }

    /**
     * tap top offset area to translate back.
     *
     * @return true if allow tap back
     */
    public boolean isTopTapBack() {
        return mTopTapBack;
    }

    /**
     * tap bottom offset area to translate back.
     *
     * @return true if allow tap back
     */
    public boolean isBottomTapBack() {
        return mBottomTapBack;
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
     * translate to top immediately, without animation.
     */
    public void top() {
        mTopTranslate = (int) (mTopOffset - mMeasuredHeight);
        mPositionState = STATE_COLLAPSE_TOP;
        invalidate();
    }

    /**
     * Flip bottom immediately, without animation.
     */
    public void bottom() {
        mTopTranslate = (int) (mMeasuredHeight - mBottomOffset);
        mPositionState = STATE_COLLAPSE_BOTTOM;
        invalidate();
    }

    /**
     * Open host view when flipped.
     */
    public void open() {
        mTopTranslate = 0;
        mPositionState = STATE_EXPAND;
        invalidate();
    }

    /**
     * Animation version of flipping
     */
    public void animateTop() {
        if (canTop()) {
            mAnimator.animateTop(-mAnimator.kVelocity);
        }
    }

    /**
     * Animation version of flipping
     */
    public void animateBottom() {
        if (canBottom()) {
            mAnimator.animateBottom(mAnimator.kVelocity);
        }
    }

    /**
     * Animation version of flipping
     */
    public void animateOpen() {
        switch (mPositionState) {
            case STATE_COLLAPSE_TOP:
                mAnimator.animateTopOpen(mAnimator.kVelocity);
                break;
            case STATE_COLLAPSE_BOTTOM:
                mAnimator.animateBottomOpen(-mAnimator.kVelocity);
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

    public void setTopAnimationListener(OnTopAnimationListener listener) {
        mOnTopAnimationListener = listener;
    }

    public void setBottomAnimationListener(OnBottomAnimationListener listener) {
        mOnBottomAnimationListener = listener;
    }

    public void addOpenAnimationListener(OnOpenAnimationListener listener) {
        mOnOpenAnimationListener.add(listener);
    }

    public void removeOpenAnimationListener(OnOpenAnimationListener listener) {
        mOnOpenAnimationListener.remove(listener);
    }

    public void setTopTrackListener(OnTopTrackListener listener) {
        mOnTopTrackListener = listener;
    }

    public void setBottomTrackListener(OnBottomTrackListener listener) {
        mOnBottomTrackListener = listener;
    }

    public void setVerticalTrackListener(OnVerticalTrackListener listener) {
        mOnVerticalTrackListener = listener;
    }

    private boolean canTop() {
        return mTopOffset != -1 && mPositionState == STATE_EXPAND;
    }

    private boolean canBottom() {
        return mBottomOffset != -1 && mPositionState == STATE_EXPAND;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, mTopTranslate);
        Log.d(TAG, "@dispatchDraw " + mTopTranslate);

        // draw background here
        canvas.drawRect(0, 0, mMeasuredWidth, mMeasuredHeight, mBackgroundPaint);

        super.dispatchDraw(canvas);
        canvas.restore();
    }

    public int getTopTranslate() {
        return mTopTranslate;
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

        ev.offsetLocation(0, -mTopTranslate);
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastDownX = x;
                mLastDownY = y;

                // 停止所有的动画
                mHandler.removeMessages(MSG_ANIMATE_TOP);
                mHandler.removeMessages(MSG_ANIMATE_TOP_OPEN);
                mHandler.removeMessages(MSG_ANIMATE_BOTTOM);
                mHandler.removeMessages(MSG_ANIMATE_BOTTOM_OPEN);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "@interceptInterceptTouchEvent");

                if (mPositionState == STATE_EXPAND) {
                    if (x < mLastDownX - mTouchThreshold || x > mLastDownX + mTouchThreshold) {
                        return false;
                    }

                    if ((y < mLastDownY - mTouchThreshold || y > mLastDownY + mTouchThreshold)) {
                        switch (mTrackDirection) {
                            case top:
                                return mTracker.prepareTopTrack();
                            case bottom:
                                return mTracker.prepareBottomTrack();
                            case vertical:
                                return mTracker.prepareVerticalTrack(y - mLastDownY);
                            default:
                                break;
                        }
                    }
                    return false;
                } else {
                    switch (mTrackDirection) {
                        case top:
                            return mTracker.prepareTopTrack();
                        case bottom:
                            return mTracker.prepareBottomTrack();
                        case vertical:
                            return mTracker.prepareVerticalTrack(y - mLastDownY);
                    }
                }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mLastDownY - mTouchThreshold < y && y < mLastDownY + mTouchThreshold) {
                    if (mTopTapBack && mTopFrameForTap.contains(x, y)
                            && mPositionState == STATE_COLLAPSE_TOP) {
                        mAnimator.animateTopOpen(mAnimator.kVelocity);
                    } else if (mBottomTapBack && mBottomFrameForTap.contains(x, y)
                            && mPositionState == STATE_COLLAPSE_BOTTOM) {
                        mAnimator.animateBottomOpen(-mAnimator.kVelocity);
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
        Log.d(TAG, String.format("@interceptTouch x %d, y %d", x, y));
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mTracker.tracking) {
                    if (!mLastMoveXBeenSet) {
                        if (mPositionState != STATE_EXPAND) {
                            mLastMoveY = mLastDownX + mTopTranslate;
                            mLastMoveYBeenSet = true;
                        } else if (y > mLastDownY) {
                            mLastMoveX = mLastDownX + mTouchThreshold;
                            mLastMoveXBeenSet = true;
                        } else {
                            mLastMoveX = mLastDownX - mTouchThreshold;
                            mLastMoveXBeenSet = true;
                        }
                    }

                    mTracker.move(mLastMoveX - x);
                    mLastMoveX = x;
                    //ev.offsetLocation(mTopTranslate, 0);
                    mTracker.velocityTracker.addMovement(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "@onTouchEvent up");
                // 当不在展开的状态下的时候，我们要判断是否可以通过单击侧边区域做展开动画。
                // 只有在侧边区域的点击才能进行计算。
                mLastMoveXBeenSet = false;
                if (mPositionState != STATE_EXPAND) {
                    if (mTopTapBack && mPositionState == STATE_COLLAPSE_TOP
                            && mTopFrameForTap.contains(x, y)) {
                        mTracker.stopTracking();
                        Log.d(TAG, "@onTouchEvent top open");
                        mAnimator.animateTopOpen(mAnimator.kVelocity);
                        return true;
                    } else if (mBottomTapBack && mPositionState == STATE_COLLAPSE_BOTTOM
                            && mBottomFrameForTap.contains(x, y)) {
                        mTracker.stopTracking();
                        Log.d(TAG, "@onTouchEvent bottom open");
                        mAnimator.animateBottomOpen(-mAnimator.kVelocity);
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
            if (mTopOffset != -1) {
                mTopFrameForTap.set((int) (r - mTopOffset), t, r, b);
            }

            if (mBottomOffset != -1) {
                mBottomFrameForTap.set(l, t, (int) (l + mBottomOffset), b);
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
        assert widthSize >= mTopOffset :
                "top offset should not be larger than the view's width";
        assert widthSize >= mBottomOffset :
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
                mTopTranslate = 0;
                invalidate();
                break;
            case STATE_COLLAPSE_TOP:
                mTopTranslate = (int) (mTopOffset - getMeasuredWidth());
                invalidate();
                break;
            case STATE_COLLAPSE_BOTTOM:
                mTopTranslate = (int) (getMeasuredWidth() - mBottomOffset);
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
                case MSG_ANIMATE_TOP:
                    mAnimator.computeTopAnimation();
                    break;
                case MSG_ANIMATE_BOTTOM:
                    mAnimator.computeBottomAnimation();
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

        boolean prepareTopTrack() {
            if (mPositionState != STATE_EXPAND &&
                    mPositionState != STATE_COLLAPSE_TOP) {
                return false;
            }
            prepareTracking();
            direction = TrackDirection.top;
            return true;
        }

        boolean prepareBottomTrack() {
            if (mPositionState != STATE_EXPAND &&
                    mPositionState != STATE_COLLAPSE_BOTTOM) {
                return false;
            }
            prepareTracking();
            direction = TrackDirection.bottom;
            return true;
        }

        boolean prepareVerticalTrack(int d) {
            prepareTracking();
            direction = TrackDirection.vertical;
            if (mOnVerticalTrackListener != null) {
                final OnVerticalTrackListener listener = mOnVerticalTrackListener;
                listener.onVerticalTrackListener(d);
            }
            return true;
        }

        void move(int yOffset) {
            if (!tracking) {
                return;
            }
            final int top = mTopTranslate - yOffset;
            switch (direction) {
                case top:
                    Log.d(TAG, "@move top");
                    if (top > mTopOffset - mMeasuredHeight && top < 0) {
                        mTopTranslate -= yOffset;
                        invalidate();
                    }
                    break;
                case bottom:
                    Log.d(TAG, "@move bottom");
                    if (top < mMeasuredHeight - mBottomOffset && top > 0) {
                        mTopTranslate -= yOffset;
                        invalidate();
                    }
                    break;
                case vertical:
                    Log.d(TAG, "@move vertical");
                    if (top >= mTopOffset - mMeasuredHeight
                            && top <= mMeasuredHeight - mBottomOffset) {
                        mTopTranslate -= yOffset;
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
            float yVelocity = velocityTracker.getYVelocity();

            Log.d(TAG, "@fling y " + yVelocity);

            if (yVelocity < 0) {
                yVelocity = Math.min(yVelocity, -minVelocity);
            } else {
                yVelocity = Math.max(yVelocity, minVelocity);
            }

            switch (direction) {
                case vertical:
                    verticalFling(yVelocity);
                    break;
                case top:
                    topFling(yVelocity);
                    break;
                case bottom:
                    bottomFling(yVelocity);
                    break;
                default:
                    break;
            }

            velocityTracker.recycle();
            velocityTracker = null;
        }

        private void verticalFling(float velocity) {
            Log.d(TAG, "@verticalFling");
            final int top = mTopTranslate;
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

        private void topFling(float velocity) {
            Log.d(TAG, "@topFling");
            if (velocity < 0) {
                Log.d(TAG, "@topFling animateTop " + velocity);
                mAnimator.animateTop(velocity);
            } else {
                Log.d(TAG, "@topFling animateTopOpen " + velocity);
                mAnimator.animateTopOpen(velocity);
            }
        }

        private void bottomFling(float velocity) {
            Log.d(TAG, "@bottomFling");
            if (velocity < 0) {
                mAnimator.animateBottomOpen(velocity);
            } else {
                mAnimator.animateBottom(velocity);
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

        void computeTopAnimation() {
            compute();
            if (iAnimatingPosition <= iAnimationDistance) {
                final OnTopAnimationListener listener = mOnTopAnimationListener;
                if (listener != null) {
                    listener.onTopAnimationEnd();
                }
                iAnimating = false;
                mPositionState = STATE_COLLAPSE_TOP;
                offset();
            } else {
                float offset = AnimationConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
                Log.d(TAG, "@computeTopAnimation " + offset);
                mTopTranslate = (int) (offset + iAnimationStart);
                invalidate();
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP, iCurrentAnimationTime);
            }
        }

        void computeBottomAnimation() {
            compute();
            if (iAnimatingPosition >= iAnimationDistance) {
                final OnBottomAnimationListener listener = mOnBottomAnimationListener;
                if (listener != null) {
                    listener.onBottomAnimationEnd();
                }
                iAnimating = false;
                mPositionState = STATE_COLLAPSE_BOTTOM;
                offset();
            } else {
                float offset = AnimationConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
                mTopTranslate = (int) (offset + iAnimationStart);
                invalidate();
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM, iCurrentAnimationTime);
            }
        }

        void computeTopOpenAnimation() {
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
                mTopTranslate = (int) (offset + iAnimationStart);
                invalidate();
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP_OPEN, iCurrentAnimationTime);
            }
        }

        void computeBottomOpenAnimation() {
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
                mTopTranslate = (int) (offset + iAnimationStart);
                invalidate();
                mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM_OPEN, iCurrentAnimationTime);
            }
        }

        void animateTopOpen(float velocity) {
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
            iAnimationDistance = 0 - mTopTranslate;
            iAnimationStart = mTopTranslate;
            mHandler.removeMessages(MSG_ANIMATE_TOP_OPEN);
            Log.d(TAG, "@animateTopOpen " + iAnimationDistance);
            Log.d(TAG, "@animateTopOpen " + velocity);
            mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP_OPEN, iCurrentAnimationTime);
        }

        void animateBottomOpen(float velocity) {
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
            iAnimationDistance = 0 - mTopTranslate;
            iAnimationStart = mTopTranslate;
            Log.d(TAG, "@animateBottomOpen " + iAnimationDistance);
            Log.d(TAG, "@animateBottomOpen " + velocity);
            mHandler.removeMessages(MSG_ANIMATE_BOTTOM_OPEN);
            mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM_OPEN, iCurrentAnimationTime);
        }

        void animateTop(float velocity) {
            final OnTopAnimationListener listener = mOnTopAnimationListener;
            if (listener != null) {
                listener.onLeftAnimationStart();
            }
            iAnimating = true;
            final long now = SystemClock.uptimeMillis();
            iLastAnimationTime = now;
            iCurrentAnimationTime = now + AnimationConfig.ANIMATION_FRAME_DURATION;
            iAnimatingVelocity = velocity;
            iAnimatingPosition = 0;
            iAnimationDistance = -mMeasuredHeight + mTopOffset - mTopTranslate;
            iAnimationStart = mTopTranslate;
            Log.d(TAG, "@animateTop " + iAnimationDistance);
            Log.d(TAG, "@animateTop " + velocity);
            mHandler.removeMessages(MSG_ANIMATE_TOP);
            mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_TOP, iCurrentAnimationTime);
        }

        void animateBottom(float velocity) {
            final OnBottomAnimationListener listener = mOnBottomAnimationListener;
            if (listener != null) {
                listener.onRightAnimationStart();
            }
            iAnimating = true;
            final long now = SystemClock.uptimeMillis();
            iLastAnimationTime = now;
            iCurrentAnimationTime = now + AnimationConfig.ANIMATION_FRAME_DURATION;
            iAnimatingVelocity = velocity;
            iAnimatingPosition = 0;
            iAnimationDistance = (mMeasuredHeight - mBottomOffset) - mTopTranslate;
            iAnimationStart = mTopTranslate;
            Log.d(TAG, "@animateBottom " + iAnimationDistance);
            Log.d(TAG, "@animateBottom " + velocity);
            mHandler.removeMessages(MSG_ANIMATE_BOTTOM);
            mHandler.sendEmptyMessageAtTime(MSG_ANIMATE_BOTTOM, iCurrentAnimationTime);
        }
    }

    public void dump() {
        Log.d(TAG, "@dump top offset " + mTopOffset);
        Log.d(TAG, "@dump bottom offset " + mBottomOffset);
        Log.d(TAG, "@dump track " + mTrackDirection);
        Log.d(TAG, "@dump top tap " + mTopTapBack);
        Log.d(TAG, "@dump bottom tap " + mBottomOffset);
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

    interface OnTopAnimationListener {
        /**
         * fire when top animation starts.
         */
        void onLeftAnimationStart();

        /**
         * fire when top animation ends.
         */
        void onTopAnimationEnd();
    }

    interface OnBottomAnimationListener {
        /**
         * fire when bottom animation starts.
         */
        void onRightAnimationStart();

        /**
         * fire when bottom animation ends.
         */
        void onBottomAnimationEnd();
    }

    interface OnTopTrackListener {
        void onTopTrackStart();
    }

    interface OnBottomTrackListener {
        void onBottomTrackStart();
    }

    interface OnVerticalTrackListener {
        void onVerticalTrackListener(int direction);
    }
}
