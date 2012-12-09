package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;
import com.bbsimon.android.demo.views.StackLayout;

/**
 * @author bb.simon.yu@gmail.com
 */
public class StackLayoutDemoActivity extends BaseActivity
    implements View.OnClickListener {

  private static final String TAG = "StackLayoutDemoActivity";

  private StackLayout mStackLayout;

  private int mCount;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getTitleBar().setTitle(TAG);
    setContentView(R.layout.stack_layout_demo);

    View view = findViewById(R.id.push);
    view.setOnClickListener(this);
    view = findViewById(R.id.pop);
    view.setOnClickListener(this);
    view = findViewById(R.id.clear);
    view.setOnClickListener(this);

    mStackLayout = (StackLayout) findViewById(R.id.stack_layout);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.push:
        mCount++;
        TextView child = new TextView(this);
        child.setGravity(Gravity.CENTER);
        child.setText("#" + mCount);
        child.setBackgroundColor((int) (0x00FFFFFF * Math.random()) | 0xFF000000);
        mStackLayout.addView(child);
        break;
      case R.id.pop:
        if (mCount == 0) {
          break;
        }
        mCount--;
        mStackLayout.removeView(mStackLayout.getChildAt(mCount));
        break;
      case R.id.clear:
        mCount = 0;
        mStackLayout.clear();
        break;
    }
  }
}
