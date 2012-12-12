package com.bbsimon.android.demo.views.refresh;

/**
 */
public interface IRefreshable {
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
  boolean getEnable();

  /**
   */
  void refresh();

  void showEmptyView();

  void hideEmptyView();

  void setRefreshIndicator(IRefreshIndicator indicator);
}
