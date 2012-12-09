/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bbsimon.android.demo.R;
import de.akquinet.android.androlog.Log;

/**
 * @author bb.simon.yu@gmail.com
 */
public abstract class BaseActivity extends FragmentActivity {
  private static final String TAG = "BaseActivity";

  private TitleBar mTitleBar;
  private LinearLayout mRoot;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    super.setContentView(buildContentView());
  }

  public interface ITitleBar {
    void setTitle(String text);
  }

  protected ITitleBar getTitleBar() {
    return mTitleBar;
  }

  private View buildContentView() {
    mRoot = new LinearLayout(this);
    mRoot.setOrientation(LinearLayout.VERTICAL);
    mTitleBar = new TitleBar(this);
    mRoot.addView(mTitleBar,
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
    return mRoot;
  }

  @Override
  public void setContentView(int layoutResID) {
    View content = View.inflate(this, layoutResID, null);
    if (content == null) {
      Log.e(TAG, "@setContentView content view layout id doesn't exist.");
      return;
    }
    View added = mRoot.findViewById(R.id.content);
    if (added != null) {
      mRoot.removeView(added);
    }
    content.setId(R.id.content);
    mRoot.addView(content,
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
  }

  @Override
  public void setContentView(View view, ViewGroup.LayoutParams params) {
    setContentView(view);
  }

  @Override
  public void setContentView(View view) {
    if (view == null) {
      return;
    }
    mRoot.removeView(view);
    view.setId(R.id.content);
    mRoot.addView(view,
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
  }

  /**
   * @author bb.simon.yu@gmail.com
   */
  private static class TitleBar extends ViewGroup implements ITitleBar {
    private static final int HEIGHT = 100;

    private final int mHeight;

    private TextView mTextView;

    public TitleBar(Context context) {
      this(context, null, 0);
    }

    public TitleBar(Context context, AttributeSet attrs) {
      this(context, attrs, 0);
    }

    public TitleBar(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);

      final float density = getResources().getDisplayMetrics().density;
      mHeight = (int) (0.5f + HEIGHT * density);

      mTextView = new TextView(context);
      addView(mTextView, new LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      final int widthSize = widthMeasureSpec & ~(0x3 << 30);

      measureChild(mTextView, widthSize + MeasureSpec.AT_MOST,
          mHeight + MeasureSpec.AT_MOST);

      setMeasuredDimension(widthSize, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
      final int w = r - l;
      final int h = b - t;
      int measuredWidth = mTextView.getMeasuredWidth();
      int measuredHeight = mTextView.getMeasuredHeight();
      mTextView.layout((w - measuredWidth) / 2, (h - measuredHeight) / 2,
          (w + measuredWidth) / 2, (h + measuredHeight) / 2);
    }

    public void setTitle(String title) {
      mTextView.setText(title);
    }
  }
}
