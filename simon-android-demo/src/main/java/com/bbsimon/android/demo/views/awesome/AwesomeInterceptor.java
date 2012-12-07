package com.bbsimon.android.demo.views.awesome;

import android.graphics.Canvas;
import android.view.MotionEvent;

@SuppressWarnings("unused")
abstract class AwesomeInterceptor {
    abstract void postInterceptMeasure(int widthMeasureSpec, int heightMeasureSpec);

    abstract void postInterceptLayout(boolean changed, int l, int t, int r, int b);

    abstract void interceptDispatchDraw(Canvas canvas);

    abstract void postInterceptDispatchDraw(Canvas canvas);

    abstract boolean interceptDispatchTouchEvent(MotionEvent ev);

    abstract boolean interceptInterceptTouchEvent(MotionEvent ev);

    abstract boolean interceptTouch(MotionEvent ev);
}
