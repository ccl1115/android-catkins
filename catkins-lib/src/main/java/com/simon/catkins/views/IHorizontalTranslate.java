package com.simon.catkins.views;

/**
 * 本接口定义了一个View或者ViewGroup具有四向滑动功能时的方法。
 *
 * @see HorizontalTranslateLayout
 */
public interface IHorizontalTranslate {
  public static final int STATE_COLLAPSE_LEFT = 10000;
  public static final int STATE_COLLAPSE_RIGHT = 10001;
  public static final int STATE_EXPAND = 10004;

  public static final String LEFT = "left";
  public static final String RIGHT = "right";
  public static final String HORIZONTAL = "horizontal";

  void setProportion(float proportion);

  /**
   * @return the left offset when animated to left.
   */
  int getLeftOffset();

  /**
   * @return the right offset when animated to right.
   */
  int getRightOffset();

  /**
   * @return true if tap left will animatePullBack back.
   */
  boolean isLeftTapBack();

  /**
   * @return true if tap right will animatePullBack back.
   */
  boolean isRightTapBack();

  /**
   * @param tapBack set true if tap left will animatePullBack back
   */
  void setLeftTapBack(boolean tapBack);

  /**
   * @param tapBack set true if tap right will animatePullBack back
   */
  void setRightTapBack(boolean tapBack);

  /**
   * go to left without animation
   */
  void left();

  /**
   * go to right without animation
   */
  void right();

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
   * register a listener fire when opening animation starts or ends.
   *
   * @param listener a listener
   */
  void addOpenAnimationListener(OnOpenAnimationListener listener);

  void removeOpenAnimationListener(OnOpenAnimationListener listener);

  void setLeftTrackListener(OnLeftTrackListener listener);

  void setRightTrackListener(OnRightTrackListener listener);

  void setHorizontalTrackListener(OnHorizontalTrackListener listener);

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

  interface OnLeftTrackListener {
    void onLeftTrackStart();
  }

  interface OnRightTrackListener {
    void onRightTrackStart();
  }

  interface OnHorizontalTrackListener {
    void onHorizontalTrackListener(int direction);
  }
}

