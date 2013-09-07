package com.simon.catkins.demo.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.simon.catkins.canvas.RoundCornerDecor;

/**
 * @author Simon Yu
 */
public class RoundCornerView extends View {
    private RoundCornerDecor mDecor;

    public RoundCornerView(Context context) {
        this(context, null, 0);
    }

    public RoundCornerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundCornerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mDecor = new RoundCornerDecor(getContext(), getMeasuredWidth(), getMeasuredHeight(), 6);
    }

    @Override
    public void draw(Canvas canvas) {
        int sc = canvas.saveLayer(0, 0, getMeasuredWidth(), getMeasuredHeight(), null,
                Canvas.MATRIX_SAVE_FLAG |
                        Canvas.CLIP_SAVE_FLAG |
                        Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                        Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                        Canvas.CLIP_TO_LAYER_SAVE_FLAG);
        super.draw(canvas);
        mDecor.draw(canvas);
        canvas.restoreToCount(sc);
        getParent().requestDisallowInterceptTouchEvent(true);
    }

    //@Override
    //public void draw(Canvas canvas) {
    //    canvas.save();
    //    super.draw(canvas);
    //    mDecor.draw(canvas);
    //    canvas.restore();
    //}
}
