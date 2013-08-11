package com.simon.catkins.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.ViewConfiguration;

/**
 * @author Simon Yu
 */
public class RoundCornerDecor extends Drawable {
    private static final int BOTTOM_SHADER_HEIGHT = 1; //dp

    private final int mWidth;
    private final int mHeight;
    private final int mRadius;
    private final int mTopShaderHeight;
    private final int mBottomShaderHeight;

    private final Paint mCanvasPaint;
    private final Paint mTopShaderPaint;
    private final Paint mBottomShaderPaint;

    private final Path mTopShaderPath;
    private final Path mBottomShaderPath;
    private final PorterDuffXfermode mXferMode;

    private final Bitmap mRoundCornerMask;


    public RoundCornerDecor(Context context, int width, int height, int radius) {
        mWidth = Math.max(0, width);
        mHeight = Math.max(0, height);
        mRadius = Math.max(0, radius);
        mTopShaderHeight = mRadius >> 1;

        final float density = context.getResources().getDisplayMetrics().density;
        mBottomShaderHeight = (int) (BOTTOM_SHADER_HEIGHT * density + 0.5f);

        mXferMode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

        mCanvasPaint = new Paint();
        mCanvasPaint.setDither(true);
        mCanvasPaint.setAntiAlias(true);
        mCanvasPaint.setXfermode(mXferMode);


        mTopShaderPaint = new Paint();
        mTopShaderPaint.setDither(true);
        mTopShaderPaint.setAntiAlias(true);
        mTopShaderPaint.setColor(0xFF000000);
        mTopShaderPaint.setShader(makeTopGradient());

        mBottomShaderPaint = new Paint();
        mBottomShaderPaint.setDither(true);
        mBottomShaderPaint.setAntiAlias(true);
        mBottomShaderPaint.setColor(0x99FFFFFF);

        mRoundCornerMask = makeSrc();

        mTopShaderPath = makeTopShaderPath();
        mBottomShaderPath = makeBottomShaderPath();
    }

    private Bitmap makeSrc() {
        Bitmap bm = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rect = new RectF(0, 0, mWidth, mHeight - mBottomShaderHeight);
        c.drawRoundRect(rect, mRadius, mRadius, paint);
        return bm;
    }

    private Path makeTopShaderPath() {
        Path path = new Path();
        path.moveTo(0, mRadius);
        path.quadTo(0, 0, mRadius, 0);
        path.lineTo(mWidth - mRadius, 0);
        path.quadTo(mWidth, 0, mWidth, mRadius);
        path.quadTo(mWidth, mTopShaderHeight, mWidth - mRadius, mTopShaderHeight);
        path.lineTo(mRadius, mTopShaderHeight);
        path.quadTo(0, mTopShaderHeight, 0, mRadius);
        path.close();
        return path;
    }

    private Path makeBottomShaderPath() {
        Path path = new Path();
        final int height = mHeight - mBottomShaderHeight;
        path.moveTo(0, height - mRadius);
        path.quadTo(0, height, mRadius, height);
        path.lineTo(mWidth - mRadius, height);
        path.quadTo(mWidth, height, mWidth, height - mRadius);
        path.quadTo(mWidth, height + mBottomShaderHeight, mWidth - mRadius, height + mBottomShaderHeight);
        path.lineTo(mRadius, height + mBottomShaderHeight);
        path.quadTo(0, height + mBottomShaderHeight, 0, height - mRadius);
        path.close();
        return path;
    }

    private LinearGradient makeTopGradient() {
        return new LinearGradient(0, 0, 0, mTopShaderHeight, 0x99000000, 0x11000000, Shader.TileMode.REPEAT);
    }

    /**
     * Draw decorator to canvas, call Canvas.save() before this, and call Canvas.restore after this.
     * @param canvas the canvas to be decorated
     */
    @Override
    public void draw(Canvas canvas) {
        if (mWidth == 0 || mHeight <= mBottomShaderHeight) {
            return;
        }
        canvas.drawBitmap(mRoundCornerMask, 0, 0, mCanvasPaint);
        canvas.drawPath(mTopShaderPath, mTopShaderPaint);
        canvas.drawPath(mBottomShaderPath, mBottomShaderPaint);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
