package com.simon.catkins.views.imagebroswer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;

/**
 * @author bb.simon.yu@gmail.com
 */
public class ImageViewer extends SurfaceView {
  public ImageViewer(Context context) {
    this(context, null, 0);
  }

  public ImageViewer(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ImageViewer(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }
}
