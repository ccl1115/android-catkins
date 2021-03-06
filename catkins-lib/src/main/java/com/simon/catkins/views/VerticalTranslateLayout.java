package com.simon.catkins.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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
    private float mTopHeight;
    private float mBottomOffset;
    private float mBottomHeight;

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

    private Drawable mTopDivider;
    private Drawable mBottomDivider;
    private int mTopDividerHeight;
    private int mBottomDividerHeight;

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

        mTopOffset = a.getDimensionPixelSize(R.styleable.VerticalTranslateLayout_topOffset, -1);
        mBottomOffset = a.getDimensionPixelSize(R.styleable.VerticalTranslateLayout_bottomOffset, -1);
        mTopHeight = a.getDimensionPixelSize(R.styleable.VerticalTranslateLayout_topHeight, -1);
        mBottomHeight = a.getDimensionPixelSize(R.styleable.VerticalTranslateLayout_bottomHeight, -1);


        final String track = a.getString(R.styleable.VerticalTranslateLayout_track);
        if (track != null && track.length() > 0) {
            if (canTop() && canBottom() && VERTICAL.equals(track)) {
                Log.d(TAG, "@parseTrack vertical");
                mTrackDirection = TrackDirection.vertical;
            } else if (canBottom() && BOTTOM.equals(track)) {
                Log.d(TAG, "@parseTrack bottom");
                mTrackDirection = TrackDirection.bottom;
            } else if (canTop() && TOP.equals(track)) {
                Log.d(TAG, "@parseTrack top");
                mTrackDirection = TrackDirection.top;
            } else {
                mTrackDirection = TrackDirection.none;
                Log.d(TAG, "@parseTrack no direction");
            }
        }

        final String tapBackArea =
                a.getString(R.styleable.VerticalTranslateLayout_tapBack);
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

        mTopDivider = a.getDrawable(R.styleable.VerticalTranslateLayout_topDivider);
        if (mTopDivider != null) {
            mTopDividerHeight = a.getDimensionPixelSize(R.styleable.VerticalTranslateLayout_topDividerHeight, 0);
            if (mTopDividerHeight == 0) {
                Log.d(TAG, "has height divider but height no set.");
                mTopDividerHeight = mTopDivider.getIntrinsicHeight();
                Log.d(TAG, "height set to intrinsic height = " + mTopDividerHeight);
            }
        }

        mBottomDivider = a.getDrawable(R.styleable.VerticalTranslateLayout_bottomDivider);
        if (mBottomDivider != null) {
            mBottomDividerHeight = a.getDimensionPixelSize(R.styleable.VerticalTranslateLayout_bottomDividerHeight, 0);
            if (mBottomDividerHeight == 0) {
                Log.d(TAG, "has height divider but height no set.");
                mBottomDividerHeight = mBottomDivider.getIntrinsicHeight();
                Log.d(TAG, "height set to intrinsic height = " + mBottomDividerHeight);
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
     * Collapse drawer to top
     */
    public void collapseTop(boolean anim) {
        if (canTop()) {
            if (anim) {
                mAnimator.animateTop(-mAnimator.kVelocity);
            } else {
                mTopTranslate = (int) (mTopOffset - mMeasuredHeight);
                mPositionState = STATE_COLLAPSE_TOP;
                invalidate();
            }
        }
    }

    /**
     * Collapse drawer to bottom.
     */
    public void collapseBottom(boolean anim) {
        if (canBottom()) {
            if (anim) {
                mAnimator.animateBottom(mAnimator.kVelocity);
            } else {
                mTopTranslate = (int) (mMeasuredHeight - mBottomOffset);
                mPositionState = STATE_COLLAPSE_BOTTOM;
                invalidate();
            }
        }
    }

    /**
     * Open drawer.
     */
    public void expand(boolean anim) {
        if (anim) {
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
        } else {
            mTopTranslate = 0;
            mPositionState = STATE_EXPAND;
            invalidate();
        }
    }


    /**
     * Get position state
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
        return mTopOffset != -1 || mBottomHeight != -1;
    }

    private boolean canBottom() {
        return mBottomOffset != -1 || mTopHeight != -1;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, mTopTranslate);

        // draw dividers if set.
        if (mTopDividerHeight != 0) {
            mTopDivider.draw(canvas);
        }
        if (mBottomDividerHeight != 0) {
            mBottomDivider.draw(canvas);
        }

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

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (mPositionState == STATE_EXPAND) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mLastDownX = x;
                    mLastDownY = y;

                    // 停止所有的动画
                    mHandler.removeMessages(MSG_ANIMATE_TOP);
                    mHandler.removeMessages(MSG_ANIMATE_TOP_OPEN);
                    mHandler.removeMessages(MSG_ANIMATE_BOTTOM);
                    mHandler.removeMessages(MSG_ANIMATE_BOTTOM_OPEN);
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "@interceptInterceptTouchEvent");
                    ev.offsetLocation(0, -mTopTranslate);
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
        return !(x < mLastDownX - mTouchThreshold || x > mLastDownX + mTouchThreshold)
                && (y < mLastDownY - mTouchThreshold || y > mLastDownY + mTouchThreshold)
                && mTracker.prepareTracking(y - mLastDownY);

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
                        if (!mLastMoveYBeenSet) {
                            if (y > mLastDownY) {
                                mLastMoveY = mLastDownY + mTouchThreshold;
                                mLastMoveYBeenSet = true;
                            } else {
                                mLastMoveY = mLastDownY - mTouchThreshold;
                                mLastMoveYBeenSet = true;
                            }
                        }

                        mTracker.move(mLastMoveY - y);
                        mLastMoveY = y;
                        mTracker.velocityTracker.addMovement(ev);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "@onTouchEvent up");
                    mLastMoveYBeenSet = false;

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
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (((mPositionState == STATE_COLLAPSE_TOP) && mTopFrameForTap.contains(x, y))
                            || ((mPositionState == STATE_COLLAPSE_BOTTOM) && mBottomFrameForTap.contains(x, y))) {
                        if (!mTracker.tracking) {
                            mLastMoveY = y;
                            mTracker.prepareTracking(y);
                        }
                    } else {
                        // Do not consume this event, dispatch to its siblings.
                        return false;
                    }
                case MotionEvent.ACTION_MOVE:
                    if (mTracker.tracking) {
                        mTracker.move(mLastMoveY - y);
                        mLastMoveY = y;
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
            if (mTopOffset != -1) {
                mTopFrameForTap.set(l, t, r, (int) (t + mTopOffset));
            }

            if (mBottomOffset != -1) {
                mBottomFrameForTap.set(l, (int) (b - mBottomOffset), r, b);
            }

            if (mTopDividerHeight != 0) {
                Log.d(TAG, "has top divider");
                mTopDivider.setBounds(0, -mTopDividerHeight, mMeasuredWidth, 0);
            }

            if (mBottomDividerHeight != 0) {
                Log.d(TAG, "has bottom divider");
                mBottomDivider.setBounds(0, b, mMeasuredWidth, b + mBottomDividerHeight);
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
        final int heightSize = heightMeasureSpec & ~(0x3 << 30);

        if (mTopHeight != -1) {
            mTopOffset = heightSize - mBottomHeight;
        }

        if (mBottomHeight != -1) {
            mBottomOffset = heightSize - mTopHeight;
        }

        //check the offsets' sizes are not larger than the view's dimension
        assert heightSize >= mTopOffset : "top offset should not be larger than the view's width";
        assert heightSize >= mBottomOffset : "bottom offset should not be larger than the view's width";

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
                mTopTranslate = (int) (mTopOffset - mMeasuredHeight);
                invalidate();
                break;
            case STATE_COLLAPSE_BOTTOM:
                mTopTranslate = (int) (mMeasuredHeight - mBottomOffset);
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
     * MotionEventTracker can handle the dragging of the view.
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
                case top:
                    if (mPositionState != STATE_EXPAND &&
                            mPositionState != STATE_COLLAPSE_TOP) {
                        return false;
                    }
                    break;
                case bottom:
                    if (mPositionState != STATE_EXPAND &&
                            mPositionState != STATE_COLLAPSE_BOTTOM) {
                        return false;
                    }
                    break;
                case vertical:
                    if (mOnVerticalTrackListener != null) {
                        final OnVerticalTrackListener listener = mOnVerticalTrackListener;
                        listener.onStartVerticalTrack(start);
                    }
            }
            velocityTracker = VelocityTracker.obtain();
            tracking = true;
            return true;
        }

        void stopTracking() {
            tracking = false;
        }

        void move(int yOffset) {
            if (!tracking) {
                return;
            }
            final int top = mTopTranslate - yOffset;
            switch (mTrackDirection) {
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

            switch (mTrackDirection) {
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
                mAnimator.animateTop(velocity);
            } else {
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
                float offset = ViewConfig.computeInterpolator(iAnimationDistance, iAnimatingPosition, false);
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
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
            iCurrentAnimationTime = now + ViewConfig.ANIMATION_FRAME_DURATION;
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
        void onStartVerticalTrack(int direction);
    }
}
