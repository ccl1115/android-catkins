/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.bbsimon.android.demo.views.fakechrome;

import android.database.DataSetObserver;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class FakeChromeAdapter {
  private static final String TAG = "FakeChromeAdapter";

  private FragmentManager mManager;
  private List<DataSetObserver> mObservers;

  public FakeChromeAdapter(FragmentManager manager) {
    mManager = manager;
    mObservers = new ArrayList<DataSetObserver>();
  }

  public void add(TabFragment fragment) {
    if (getList().add(fragment)) {
      dataSetChanged();
    }
  }

  public void remove(TabFragment fragment) {
    if (getList().remove(fragment)) {
      destroyItem(fragment);
    }
  }

  public void remove(int position) {
    TabFragment fragment = getList().remove(position);
    destroyItem(fragment);
  }

  protected final void dataSetChanged() {
    for (DataSetObserver observer : mObservers) {
      observer.onChanged();
    }
  }

  protected final void dataSetInvalidate() {
    for (DataSetObserver observer : mObservers) {
      observer.onInvalidated();
    }
  }

  public final int getCount() {
    return getList().size();
  }

  public final TabFragment getFragment(int position) {
    return getList().get(position);
  }

  protected abstract List<TabFragment> getList();

  public final void registerDataSetObserver(DataSetObserver observer) {
    mObservers.add(observer);
  }

  public final void unregisterDataSetObserver(DataSetObserver observer) {
    mObservers.remove(observer);
  }

  final void update(FakeChromeLayout layout) {
    final int id = layout.getId();
    if (id == View.NO_ID) {
      throw new FakeChromeError("FakeChromeLayout must have an id");
    }

    if (!getList().isEmpty()) {
      final int count = getList().size();
      TabFragment fragment;
      final FragmentTransaction ft = mManager.beginTransaction();
      for (int i = 0; i < count; i++) {
        fragment = getList().get(i);
        if (!fragment.isAdded()) {
          ft.add(id, fragment, "fakeChrome:tab:" + i);
        }
      }
      ft.commitAllowingStateLoss();
    }
  }

  final void destroyItem(TabFragment fragment) {
//        Log.d(TAG, "@destroyItem");
    mManager.beginTransaction().detach(fragment).commitAllowingStateLoss();
  }
}
