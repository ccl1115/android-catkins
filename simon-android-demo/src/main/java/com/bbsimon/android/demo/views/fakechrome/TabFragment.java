package com.bbsimon.android.demo.views.fakechrome;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public abstract class TabFragment extends Fragment {
  private TabView mTabView;

  /**
   * The content view you want to place in this fragment.
   *
   * @param inflater Use this if you have a XML layout
   * @return the content view you create
   */
  protected abstract View onCreateContent(LayoutInflater inflater);

  /**
   * The title view you want to place into this fragment.
   *
   * @param inflater Use this if you have a XML layout.
   * @return the title view you created.
   */
  protected abstract View onCreateTitle(LayoutInflater inflater);

  /**
   * Implement this method to load data and update UI.
   * <b>Use threading or AsyncTask for long time data loading.</b>
   */
  protected abstract void onLoadData();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    mTabView = new TabView(getActivity());
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT);
    mTabView.setLayoutParams(lp);
    mTabView.setContentView(onCreateContent(inflater));
    mTabView.setTitleView(onCreateTitle(inflater));
    return mTabView;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    view.setClickable(true);
  }

  public final TabView getTabView() {
    return mTabView;
  }
}
