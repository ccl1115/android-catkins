/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.views.awesome;

import android.util.AttributeSet;

/**
 * 本接口定义了一个View或者ViewGroup具有四向滑动功能时的方法。
 *
 * @see AwesomeLayout
 */
public interface IAwesome {
  public static final int STATE_COLLAPSE_LEFT = 10000;
  public static final int STATE_COLLAPSE_RIGHT = 10001;
  public static final int STATE_COLLAPSE_TOP = 10002;
  public static final int STATE_COLLAPSE_BOTTOM = 10003;
  public static final int STATE_EXPAND = 10004;

  public static final String LEFT = "left";
  public static final String RIGHT = "right";
  public static final String TOP = "top";
  public static final String BOTTOM = "bottom";
  public static final String HORIZONTAL = "horizontal";
  public static final String VERTICAL = "vertical";

  /**
   * Call this in the constructor
   *
   * @param attrs the xml attributes
   */
  void loadAttrs(AttributeSet attrs);

  /**
   * @return the left offset when animated to left.
   */
  int getLeftOffset();

  /**
   * @return the right offset when animated to right.
   */
  int getRightOffset();

  /**
   * @return the top offset when animated to top.
   */
  int getTopOffset();

  /**
   * @return the bottom offset when animated to bottom.
   */
  int getBottomOffset();

  /**
   * @return true if tap left will animate back.
   */
  boolean getLeftTapBack();

  /**
   * @return true if tap right will animate back.
   */
  boolean getRightTapBack();

  /**
   * @return true if tap top will animate back.
   */
  boolean getTopTapBack();

  /**
   * @return true if tap bottom will animate back.
   */
  boolean getBottomTapBack();

  /**
   * @param tapBack set true if tap left will animate back
   */
  void setLeftTapBack(boolean tapBack);

  /**
   * @param tapBack set true if tap right will animate back
   */
  void setRightTapBack(boolean tapBack);

  /**
   * @param tapBack set true if tap top will animate back
   */
  void setTopTapBack(boolean tapBack);

  /**
   * @param tapBack set true if tap bottom will animate back
   */
  void setBottomTapBack(boolean tapBack);

  /**
   * go to left without animation
   */
  void left();

  /**
   * go to right without animation
   */
  void right();

  /**
   * go to top without animation
   */
  void top();

  /**
   * go to bottom without animation
   */
  void bottom();

  /**
   * go back without animation
   */
  void open();

  /**
   * translate to left.
   */
  void animateLeft();

  /**
   * translate to right.
   */
  void animateRight();

  /**
   * translate to top.
   */
  void animateTop();

  /**
   * translate to bottom.
   */
  void animateBottom();

  /**
   * translate back.
   */
  void animateOpen();

  /**
   * get the state of the position
   *
   * @return the state
   */
  int getState();

  /**
   * register a listener fire when left animation starts or ends.
   *
   * @param listener a listener
   */
  void setLeftAnimationListener(OnLeftAnimationListener listener);

  /**
   * register a listener fire when right animation starts or ends.
   *
   * @param listener a listener
   */
  void setRightAnimationListener(OnRightAnimationListener listener);

  /**
   * register a listener fire when top animation starts or ends.
   *
   * @param listener a listener
   */
  void setTopAnimationListener(OnTopAnimationListener listener);

  /**
   * register a listener fire when bottom animation starts or ends.
   *
   * @param listener a listener
   */
  void setBottomAnimationListener(OnBottomAnimationListener listener);

  /**
   * register a listener fire when opening animation starts or ends.
   *
   * @param listener a listener
   */
  void setOpenAnimationListener(OnOpenAnimationListener listener);

  interface OnOpenAnimationListener {
    /**
     * fire when open animation starts.
     */
    void onOpenAnimationStart();

    /**
     * fire when open animation ends;
     */
    void onOpenAnimationEnd();
  }

  interface OnLeftAnimationListener {
    /**
     * fire when left animation starts.
     */
    void onLeftAnimationStart();

    /**
     * fire when left animation ends.
     */
    void onLeftAnimationEnd();
  }

  interface OnRightAnimationListener {
    /**
     * fire when right animation starts.
     */
    void onRightAnimationStart();

    /**
     * fire when right animation ends.
     */
    void onRightAnimationEnd();
  }

  interface OnTopAnimationListener {
    /**
     * fire when top animation starts.
     */
    void onTopAnimationStart();

    /**
     * fire when top animation ends.
     */
    void onTopAnimationEnd();
  }

  interface OnBottomAnimationListener {
    /**
     * fire when bottom animation starts.
     */
    void onBottomAnimationStart();

    /**
     * fire when bottom animation ends.
     */
    void onBottomAnimationEnd();
  }
}
