package com.bbsimon.android.demo.views.fakechrome;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.bbsimon.android.demo.views.fakechrome.scalable.ScalableLayout;

class TabView extends ScalableLayout {

    private View mContent;
    private View mTitle;

    public TabView(Context context) {
        this(context, null, 0);
    }

    public TabView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setContentView(View view) {
        if (mContent == null) {
            mContent = view;
        } else {
            removeView(mContent);
            mContent = view;
        }
        if (mContent.getLayoutParams() == null) {
            FrameLayout.LayoutParams lp =
                    new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
            lp.gravity = Gravity.CENTER;
            addView(mContent, lp);
        } else {
            addView(mContent, mContent.getLayoutParams());
        }
    }

    public void setTitleView(View view) {
        if (mTitle == null) {
            mTitle = view;
        } else {
            removeView(mTitle);
            mTitle = view;
        }
        FrameLayout.LayoutParams lp =
                new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT | Gravity.TOP;
        addView(mTitle, lp);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

}
