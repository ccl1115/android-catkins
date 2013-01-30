package com.simon.catkins.views.refresh;

import android.view.View;
import android.view.ViewGroup;

/**
 */
public interface IRefreshable {

  /**
   * 这里仅仅涉及到下拉的状态，而没有刷新的状态。
   */
  public enum State {
    /**
     * 等待状态
     */
    idle,

    /**
     * 下拉状态，松手不会触发刷新
     */
    pulling_no_refresh,

    /**
     * 回弹动画状态
     */
    animating,

    /**
     * 下拉状态，松手会触发下拉刷新
     */
    pulling_refresh
  }

  /**
   * @param listener listener
   */
  void setOnRefreshListener(OnRefreshListener listener);

  /**
   * @param enable true if refreshable.
   */
  void setEnable(boolean enable);

  /**
   * @return true if refreshable.
   */
  boolean isEnabled();

  State getState();

  /**
   */
  void refresh();

  void refreshShowingHeader();

  void setRefresherContent(ViewGroup view);

  void setRefresherHeader(View view);

  void setEmptyView(View view);

  View getRefresherContent();

  View getRefresherHeader();

  View getEmptyView();

  interface OnRefreshListener {
    void onStateChanged(State state);

    void onPreRefresh();

    void onRefreshData();

    void onRefreshUI();
  }
}
