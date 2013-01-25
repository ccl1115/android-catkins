package com.bbsimon.android.demo.views.refresh;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.views.Facade;
import de.akquinet.android.androlog.Log;

import java.util.ArrayList;
import java.util.List;

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
