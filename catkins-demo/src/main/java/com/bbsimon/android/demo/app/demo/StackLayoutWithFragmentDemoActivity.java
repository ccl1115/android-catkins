/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.app.demo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.app.BaseActivity;
import com.bbsimon.android.demo.views.StackLayout;

import java.util.Stack;

import static android.widget.FrameLayout.LayoutParams;

/**
 * @author bb.simon.yu@gmail.com
 */
public class StackLayoutWithFragmentDemoActivity extends BaseActivity
    implements View.OnClickListener {

  private static final String TAG = "StackLayoutWithFragmentDemoActivity";
  private StackLayout mStackLayout;
  private Stack<ItemFragment> mItemFragments = new Stack<ItemFragment>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getTitleBar().setTitle(TAG);
    setContentView(R.layout.stack_layout);

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
        ItemFragment fragment = new ItemFragment();
        mItemFragments.push(fragment);
        getSupportFragmentManager().beginTransaction()
            .add(R.id.stack_layout, fragment, TAG)
            .commit();
        break;
      case R.id.pop:
        getSupportFragmentManager().beginTransaction()
            .remove(mItemFragments.pop()).commit();
        break;
      case R.id.clear:
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (ItemFragment f : mItemFragments) {
          ft.remove(f);
        }
        ft.commit();
        mItemFragments.clear();
        mStackLayout.clear();
        break;
    }
  }

  private class ItemFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      TextView child = new TextView(getActivity());
      child.setGravity(Gravity.CENTER);
      child.setText("#" + mItemFragments.size());
      child.setLayoutParams(
          new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT));
      child.setBackgroundColor((int) (0x00FFFFFF * Math.random()) | 0xFF000000);
      return child;
    }
  }
}
