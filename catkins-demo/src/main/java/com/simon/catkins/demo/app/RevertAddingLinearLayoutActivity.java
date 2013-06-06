package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.simon.catkins.demo.R;
import com.simon.catkins.views.RevertAddingLinearLayout;

import java.util.Random;

/**
 * @author Simon
 */
public class RevertAddingLinearLayoutActivity extends Activity implements View.OnClickListener {

  private RevertAddingLinearLayout mLayout;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.revert_adding_linear_layout_demo);

    mLayout = (RevertAddingLinearLayout) findViewById(R.id.container);

    findViewById(R.id.add).setOnClickListener(this);
    findViewById(R.id.remove).setOnClickListener(this);
  }


  @Override
  public void onClick(View view) {
    final int id = view.getId();
    switch (id) {
      case R.id.add:
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(Math.random()));
        mLayout.addView(tv);
        break;
      case R.id.remove:
        if (mLayout.getChildCount() > 1)
          mLayout.removeView(mLayout.getChildAt(new Random().nextInt(mLayout.getChildCount() - 1)));
        break;
    }
  }
}