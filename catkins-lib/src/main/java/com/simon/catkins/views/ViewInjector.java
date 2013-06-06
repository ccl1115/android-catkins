package com.simon.catkins.views;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * @author Simon
 */
public interface ViewInjector {
    void measure(int widthMeasureSpec, int heightMeasureSpec);

    void draw(Canvas canvas);

    boolean dispatchTouchEvent(MotionEvent event);

    boolean interceptionTouchEvent(MotionEvent event);

    boolean touchEvent(MotionEvent event);

    void animate(int msg);

    boolean isAnimating();
}
