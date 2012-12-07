package com.bbsimon.android.demo.views.awesome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.bbsimon.android.demo.R;

/**
 * The implementation of Awesome interface and the interceptions for host view methods.
 */
@SuppressWarnings("unused")
class IAwesomeImpl extends AwesomeInterceptor implements IAwesome {
    /**
     * TAG for logcat
     */
    private static final String TAG = "IAwesomeImpl";

    private final static int MSG_ANIMATE_LEFT = -100;
    private final static int MSG_ANIMATE_RIGHT = -101;
    private final static int MSG_ANIMATE_TOP = -102;
    private final static int MSG_ANIMATE_BOTTOM = -103;
    private final static int MSG_ANIMATE_LEFT_OPEN = -104;
    private final static int MSG_ANIMATE_RIGHT_OPEN = -105;
    private final static int MSG_ANIMATE_TOP_OPEN = -106;
    private final static int MSG_ANIMATE_BOTTOM_OPEN = -107;

    private Context mContext;
    private View mHost;

    private float mLeftOffset;
    private float mRightOffset;
    private float mTopOffset;
    private float mBottomOffset;

    private boolean mLeftTrackable;
    private boolean mRightTrackable;
    private boolean mTopTrackable;
    private boolean mBottomTrackable;

    private boolean mLeftTapBack;
    private boolean mRightTapBack;
    private boolean mTopTapBack;
    private boolean mBottomTapBack;

    private int mState;

    private Rect mLeftFrame = new Rect();
    private Rect mLeftTopFrame = new Rect();
    private Rect mTopFrame = new Rect();
    private Rect mTopRightFrame = new Rect();
    private Rect mRightFrame = new Rect();
    private Rect mRightBottomFrame = new Rect();
    private Rect mBottomFrame = new Rect();
    private Rect mBottomLeftFrame = new Rect();

    /* We store the host's properties to avoid to many get...() methods calling. */
    private int mHostLeft;
    private int mHostRight;
    private int mHostTop;
    private int mHostBottom;
    private int mHostWidth;
    private int mHostHeight;

    private AnimationHandler mHandler;
    private Animator mAnimator;

    IAwesomeImpl(View host) {
        mHost = host;
        mContext = mHost.getContext();
        mHandler = new AnimationHandler();
        mAnimator = new Animator();
        mState = STATE_EXPAND;
    }

    /**
     * Load xml attributes
     * @param attrs the attributes set from xml
     */
    @Override
    public void loadAttrs(AttributeSet attrs) {
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.Awesome);

        mLeftOffset = a.getDimension(R.styleable.Awesome_left_offset, -1f);
        mRightOffset = a.getDimension(R.styleable.Awesome_right_offset, -1f);
        mTopOffset = a.getDimension(R.styleable.Awesome_top_offset, -1f);
        mBottomOffset = a.getDimension(R.styleable.Awesome_bottom_offset, -1f);

        final String trackableArea = a.getString(R.styleable.Awesome_trackable_area);
        if (trackableArea != null && trackableArea.length() > 0) {
            final String[] trackables = trackableArea.split("|");
            for (String s : trackables) {
                if ("left".equals(s) && mLeftOffset != -1) {
                    mLeftTrackable = true;
                } else if ("right".equals(s) && mRightOffset != -1) {
                    mRightTrackable = true;
                } else if ("top".equals(s) && mTopOffset != -1) {
                    mTopTrackable = true;
                } else if ("bottom".equals(s) && mBottomOffset != -1) {
                    mBottomTrackable = true;
                } else {
                    throw new RuntimeException("trackable_area value illegal");
                }
            }
        }

        final String tapBackArea = a.getString(R.styleable.Awesome_tap_back_area);
        if (tapBackArea != null && tapBackArea.length() > 0) {
            final String[] taps = tapBackArea.split("|");
            for (String s : taps) {
                if ("left".equals(s) && mLeftOffset != -1) {
                    mLeftTapBack = true;
                } else if ("right".equals(s) && mRightOffset != -1) {
                    mRightTapBack = true;
                } else if ("top".equals(s) && mTopOffset != -1) {
                    mTopTapBack = true;
                } else if ("bottom".equals(s) && mBottomOffset != -1) {
                    mBottomTapBack = true;
                } else {
                    throw new RuntimeException("tap_back_area value illegal");
                }
            }
        }

        a.recycle();
    }

    /**
     * The offset value when flip to left.
     * @return the left offset
     */
    @Override
    public int getLeftOffset() {
        return (int) mLeftOffset;
    }

    /**
     * The offset value when flip to right.
     * @return the top offset
     */
    @Override
    public int getRightOffset() {
        return (int) mRightOffset;
    }

    /**
     * The top value when flip to top.
     * @return the top offset
     */
    @Override
    public int getTopOffset() {
        return (int) mTopOffset;
    }

    /**
     * The bottom value when flip to bottom
     * @return the bottom offset
     */
    @Override
    public int getBottomOffset() {
        return (int) mBottomOffset;
    }

    /**
     * Is the left offset area trackable.
     * @return return true if left area is trackable.
     */
    @Override
    public boolean getLeftTrackable() {
        return mLeftTrackable;
    }

    /**
     * Is the right offset area trackable.
     * @return return true if right area is trackable.
     */
    @Override
    public boolean getRightTrackable() {
        return mRightTrackable;
    }

    /**
     * Is the top offset area trackable.
     * @return return true if top area is trackable.
     */
    @Override
    public boolean getTopTrackable() {
        return mTopTrackable;
    }

    /**
     * Is the bottom offset area trackable.
     * @return return true if bottom area is trackable.
     */
    @Override
    public boolean getBottomTrackable() {
        return mBottomTrackable;
    }

    /**
     * Set left area trackable.
     * @param trackable trackable if true
     */
    @Override
    public void setLeftTrackable(boolean trackable) {
        mLeftTrackable = trackable;
    }

    /**
     * Set right area trackable.
     * @param trackable trackable if true
     */
    @Override
    public void setRightTrackable(boolean trackable) {
        mRightTrackable = trackable;
    }

    /**
     * Set top area trackable.
     * @param trackable trackable if true
     */
    @Override
    public void setTopTrackable(boolean trackable) {
        mTopTrackable = trackable;
    }

    /**
     * Set bottom area trackable.
     * @param trackable trackable if true
     */
    @Override
    public void setBottomTrackable(boolean trackable) {
        mBottomTrackable = trackable;
    }

    /**
     * tap left offset area to flip back.
     * @return true if allow tap back
     */
    @Override
    public boolean getLeftTapBack() {
        return mLeftTapBack;
    }

    /**
     * tap right offset area to flip back.
     * @return true if allow tap back
     */
    @Override
    public boolean getRightTapBack() {
        return mRightTapBack;
    }

    /**
     * tap top offset area to flip back.
     * @return true if allow tap back
     */
    @Override
    public boolean getTopTapBack() {
        return mTopTapBack;
    }

    /**
     * tap bottom offset area to flip back.
     * @return true if allow tap back
     */
    @Override
    public boolean getBottomTapBack() {
        return mBottomTapBack;
    }

    /**
     * Set left offset area could tap back.
     * @param tapBack tap back
     */
    @Override
    public void setLeftTapBack(boolean tapBack) {
        mLeftTapBack = tapBack;
    }

    /**
     * Set right offset area could tap back.
     * @param tapBack tap back
     */
    @Override
    public void setRightTapBack(boolean tapBack) {
        mRightTapBack = tapBack;
    }

    /**
     * Set top offset area could tap back.
     * @param tapBack tap back
     */
    @Override
    public void setTopTapBack(boolean tapBack) {
        mTopTapBack = tapBack;
    }

    /**
     * Set bottom offset area could tap back.
     * @param tapBack tap back
     */
    @Override
    public void setBottomTapBack(boolean tapBack) {
        mBottomTapBack = tapBack;
    }

    /**
     * Flip left immediately, without animation.
     */
    @Override
    public void left() {
        mHost.offsetLeftAndRight((int) (mLeftOffset - mHostLeft - mHostWidth));
    }

    /**
     * Flip right immediately, without animation.
     */
    @Override
    public void right() {
        mHost.offsetLeftAndRight((int) (mHostWidth - mRightOffset - mHostLeft));
    }

    /**
     * Flip top immediately, without animation.
     */
    @Override
    public void top() {
        mHost.offsetTopAndBottom(-mHostTop);
    }

    /**
     * Flip bottom immediately, without animation.
     */
    @Override
    public void bottom() {
        mHost.offsetTopAndBottom(mHostTop);
    }

    /**
     * Open host view when flipped.
     */
    @Override
    public void open() {
        mHost.offsetLeftAndRight(mHostLeft);
        mHost.offsetTopAndBottom(mHostTop);
    }

    /**
     * Animation version of flipping
     */
    @Override
    public void animateLeft() {
        mAnimator.animateLeft(0, -mAnimator.velocity);
    }

    /**
     * Animation version of flipping
     */
    @Override
    public void animateRight() {
        mAnimator.animateRight(0, mAnimator.velocity);
    }

    /**
     * Animation version of flipping
     */
    @Override
    public void animateTop() {
        mAnimator.animateTop(0, -mAnimator.velocity);
    }

    /**
     * Animation version of flipping
     */
    @Override
    public void animateBottom() {
        mAnimator.animateBottom(0, mAnimator.velocity);
    }

    /**
     * Animation version of flipping
     */
    @Override
    public void animateOpen() {
        switch (mState) {
            case STATE_COLLAPSE_LEFT:
                mAnimator.animateOpen(mHostLeft, mAnimator.velocity);
                break;
            case STATE_COLLAPSE_RIGHT:
                mAnimator.animateOpen(mHostLeft, -mAnimator.velocity);
                break;
            case STATE_COLLAPSE_TOP:
                mAnimator.animateOpen(mHostTop, mAnimator.velocity);
                break;
            case STATE_COLLAPSE_BOTTOM:
                mAnimator.animateOpen(mHostTop, -mAnimator.velocity);
                break;
        }
    }

    /**
     * Get flipping state
     * @return the state
     */
    @Override
    public int getState() {
        return mState;
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
            }
        }
    }

    private class Animator {
        private static final String TAG = "IAwesomeImpl$Animator";

        private final int FRAME_ANIMATION_DURATION = 1000 / 60;

        static final int VELOCITY = 1500;
        static final int MIN_VELOCITY = 300;
        static final int ACCELERATION = 2500;

        float left;
        float top;
        final float velocity;
        final float minVelocity;
        final float acceleration;

        float animatingPosition;
        float animatingVelocity;
        float animatingAcceleration;
        float lastAnimationTime;
        long currentAnimationTime;
        boolean animating;

        Animator() {
            final float density = mContext.getResources().getDisplayMetrics().density;
            velocity = VELOCITY * density + 0.5f;
            minVelocity = MIN_VELOCITY * density + 0.5f;
            acceleration = ACCELERATION * density + 0.5f;
        }

        private void compute() {
            final float p = animatingPosition, v = animatingVelocity;
            final long now = SystemClock.uptimeMillis();
            final float t = (now - lastAnimationTime) / 1000f;
            animatingPosition = p + v * t + animatingAcceleration * t * t * 0.5f;
            animatingVelocity = animatingVelocity < 0 ? Math.min(-minVelocity, v + animatingAcceleration * t) :
                    Math.max(v + animatingAcceleration * t, minVelocity);
            lastAnimationTime = now;
            currentAnimationTime += FRAME_ANIMATION_DURATION;

            Log.d(TAG, "@compute position " + animatingPosition + " velocity " + animatingVelocity);
        }

        void computeLeftAnimation() {
            compute();
            left = animatingPosition;
            if (left <= mLeftOffset - mHostWidth) {
                animating = false;
                left = mLeftOffset - mHostWidth;
                mState = STATE_COLLAPSE_LEFT;
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_LEFT), currentAnimationTime);
            }
            mHost.offsetLeftAndRight((int) left - mHostLeft);
            mHost.invalidate();
        }

        void computeRightAnimation() {
            compute();
            left = animatingPosition;
            if (left >= mHostWidth - mRightOffset) {
                animating = false;
                left = mHostWidth - mRightOffset;
                mState = STATE_COLLAPSE_RIGHT;
                mHandler.removeMessages(MSG_ANIMATE_RIGHT);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_RIGHT), currentAnimationTime);
            }
            mHost.offsetLeftAndRight((int) left - mHostLeft);
            mHost.invalidate();
        }

        void computeTopAnimation() {
            compute();
            top = animatingPosition;
            if (top <= mTopOffset - mHostHeight) {
                animating = false;
                top = mTopOffset - mHostHeight;
                mState = STATE_COLLAPSE_TOP;
                mHandler.removeMessages(MSG_ANIMATE_TOP);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_TOP), currentAnimationTime);
            }
            mHost.offsetTopAndBottom((int) top - mHostTop);
            mHost.invalidate();
        }

        void computeBottomAnimation() {
            compute();
            top = animatingPosition;
            if (top >= mHostHeight - mBottomOffset) {
                animating = false;
                top = mHostHeight - mBottomOffset;
                mState = STATE_COLLAPSE_BOTTOM;
                mHandler.removeMessages(MSG_ANIMATE_BOTTOM);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_BOTTOM), currentAnimationTime);
            }
            mHost.offsetTopAndBottom((int) top - mHostTop);
            mHost.invalidate();
        }

        void computeLeftOpenAnimation() {
            compute();
            left = animatingPosition;
            if (left >= 0) {
                animating = false;
                left = 0;
                mState = STATE_EXPAND;
                mHandler.removeMessages(MSG_ANIMATE_LEFT_OPEN);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_LEFT_OPEN), currentAnimationTime);
            }
            mHost.offsetLeftAndRight((int) left - mHostLeft);
            mHost.invalidate();
        }

        void computeRightOpenAnimation() {
            compute();
            left = animatingPosition;
            if (left <= 0) {
                animating = false;
                left = 0;
                mState = STATE_EXPAND;
                mHandler.removeMessages(MSG_ANIMATE_RIGHT_OPEN);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_RIGHT_OPEN), currentAnimationTime);
            }
            mHost.offsetLeftAndRight((int) left - mHostLeft);
            mHost.invalidate();
        }

        void computeTopOpenAnimation() {
            compute();
            top = animatingPosition;
            if (top >= 0) {
                animating = false;
                top = 0;
                mState = STATE_EXPAND;
                mHandler.removeMessages(MSG_ANIMATE_TOP_OPEN);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_TOP_OPEN), currentAnimationTime);
            }
            mHost.offsetTopAndBottom((int) top - mHostTop);
            mHost.invalidate();
        }

        void computeBottomOpenAnimation() {
            compute();
            top = animatingPosition;
            if (top <= 0) {
                animating = false;
                top = 0;
                mState = STATE_EXPAND;
                mHandler.removeMessages(MSG_ANIMATE_BOTTOM_OPEN);
            } else {
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_BOTTOM_OPEN), currentAnimationTime);
            }
            mHost.offsetTopAndBottom((int) top - mHostTop);
            mHost.invalidate();
        }

        void animateOpen(int position, float velocity) {
            if (mState == STATE_EXPAND) return;
            animating = true;
            currentAnimationTime = SystemClock.uptimeMillis();
            lastAnimationTime = currentAnimationTime;
            currentAnimationTime += FRAME_ANIMATION_DURATION;
            animatingPosition = position;
            animatingVelocity = velocity;
            switch (mState) {
                case STATE_COLLAPSE_LEFT:
                    animatingAcceleration = -acceleration;
                    left = animatingPosition;
                    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_LEFT_OPEN), currentAnimationTime);
                    break;
                case STATE_COLLAPSE_RIGHT:
                    animatingAcceleration = acceleration;
                    left = animatingPosition;
                    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_RIGHT_OPEN), currentAnimationTime);
                    break;
                case STATE_COLLAPSE_TOP:
                    animatingAcceleration = -acceleration;
                    top = animatingPosition;
                    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_TOP_OPEN), currentAnimationTime);
                    break;
                case STATE_COLLAPSE_BOTTOM:
                    animatingAcceleration = acceleration;
                    top = animatingPosition;
                    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_BOTTOM_OPEN), currentAnimationTime);
                    break;
            }
        }

        void animateLeft(int position, float velocity) {
            if (mState != STATE_EXPAND) return;
            animating = true;
            currentAnimationTime = SystemClock.uptimeMillis();
            lastAnimationTime = currentAnimationTime;
            currentAnimationTime += FRAME_ANIMATION_DURATION;
            animatingPosition = position;
            animatingVelocity = velocity;
            animatingAcceleration = acceleration;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_LEFT), currentAnimationTime);
        }

        void animateRight(int position, float velocity) {
            if (mState != STATE_EXPAND) return;
            animating = true;
            currentAnimationTime = SystemClock.uptimeMillis();
            lastAnimationTime = currentAnimationTime;
            currentAnimationTime += FRAME_ANIMATION_DURATION;
            animatingPosition = position;
            animatingVelocity = velocity;
            animatingAcceleration = -acceleration;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_RIGHT), currentAnimationTime);
        }

        void animateTop(int position, float velocity) {
            if (mState != STATE_EXPAND) return;
            animating = true;
            currentAnimationTime = SystemClock.uptimeMillis();
            lastAnimationTime = currentAnimationTime;
            currentAnimationTime += FRAME_ANIMATION_DURATION;
            animatingPosition = position;
            animatingVelocity = velocity;
            animatingAcceleration = acceleration;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_TOP), currentAnimationTime);
        }

        void animateBottom(int position, float velocity) {
            if (mState != STATE_EXPAND) return;
            animating = true;
            currentAnimationTime = SystemClock.uptimeMillis();
            lastAnimationTime = currentAnimationTime;
            currentAnimationTime += FRAME_ANIMATION_DURATION;
            animatingPosition = position;
            animatingVelocity = velocity;
            animatingAcceleration = -acceleration;
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_BOTTOM), currentAnimationTime);
        }
    }

    /**
     * call this method at the end of the onMeasure method body or after the setMeasuredDimension() call.
     *
     * @param widthMeasureSpec  MeasureSpec of width
     * @param heightMeasureSpec MeasureSpec of height
     */
    @Override
    void postInterceptMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = mHost.getMeasuredWidth();
        final int height = mHost.getMeasuredHeight();

        //check the offsets' sizes are not larger than the view's dimension
        assert width >= mLeftOffset : "left offset should not be larger than the view's width";
        assert width >= mRightOffset : "right offset should not be larger than the view's width";
        assert height >= mTopOffset : "top offset should not be larger than the view's height";
        assert height >= mBottomOffset : "bottom offset should not be larger than the view's height";
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
    void postInterceptLayout(boolean changed, int l, int t, int r, int b) {
        mHostLeft = mHost.getLeft();
        mHostTop = mHost.getTop();
        mHostRight = mHost.getRight();
        mHostBottom = mHost.getBottom();
        mHostWidth = mHost.getWidth();
        mHostHeight = mHost.getHeight();
        if (changed) {
            //recreate the frames only when layout changed
            if (mLeftOffset != -1) {
                mLeftFrame.set(l, t, (int) (l + mLeftOffset), b);
            }

            if (mTopOffset != -1) {
                mTopFrame.set(l, t, r, (int) (t + mTopOffset));
            }

            if (mRightOffset != -1) {
                mRightFrame.set((int) (r - mRightOffset), t, r, b);
            }

            if (mBottomOffset != -1) {
                mBottomFrame.set(l, (int) (b - mBottomOffset), r, b);
            }

            if (mLeftOffset != -1 && mTopOffset != -1) {
                    mLeftTopFrame.set(l, t, (int) (l + mLeftOffset), (int) (t + mTopOffset));
            }

            if (mTopOffset != -1 && mRightOffset != -1) {
                    mTopRightFrame.set((int) (r - mRightOffset), t, r, (int) (t + mTopOffset));
            }

            if (mLeftOffset != -1 && mBottomOffset != -1) {
                    mBottomLeftFrame.set(l, (int) (b - mBottomOffset), (int) (l + mLeftOffset), b);
            }

            if (mRightOffset != -1 && mBottomOffset != -1) {
                    mRightBottomFrame.set((int) (r - mRightOffset), (int) (b - mBottomOffset), r, b);
            }
        }
    }

    /**
     * This method must be invoked at the beginning of the host view's method dispatchDraw().
     * @param canvas the canvas.
     */
    @Override
    void interceptDispatchDraw(Canvas canvas) {
        //if (mAnimator.animating) {
        //    canvas.save();
        //    canvas.translate(-mAnimator.left, -mAnimator.top);
        //}
    }

    /**
     * This method must be invoked at the end of the host view's method dispatchDraw().
     * @param canvas the canvas
     */
    @Override
    void postInterceptDispatchDraw(Canvas canvas) {
        //if (mAnimator.animating) {
        //    canvas.restore();
        //}
    }

    /**
     * This method must be invoked at the beginning of the host view's method dispatchTouchEvent().
     * @param ev the event.
     * @return true if consumed
     */
    @Override
    boolean interceptDispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    /**
     * This method must be invoked at the beginning of the host view's method onInterceptTouchEvent().
     * @param ev the event.
     * @return true if intercepted
     */
    @Override
    boolean interceptInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    /**
     * This method must be invoked at the beginning of the host view's method onTouchEvent().
     * @param ev the event.
     * @return true if handled
     */
    @Override
    boolean interceptTouch(MotionEvent ev) {
        return false;
    }
}
