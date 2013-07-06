/*
 * Copyright (c) 2013. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.demo.app.refresher;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.simon.catkins.demo.R;
import com.simon.catkins.views.refresh.IRefreshable;
import com.simon.catkins.views.refresh.RefresherView;

/**
 */
public class RefresherViewFromXmlActivity extends Activity {
  private static final String TAG = "RefresherViewFromXmlActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.refresher_demo);

    final RefresherView refresherView = (RefresherView) findViewById(R.id.refresher);
    final TextView headerText = (TextView) findViewById(R.id.refresher_header);

    refresherView.setOnRefreshListener(new IRefreshable.OnRefreshListener() {
      @Override
      public void onStateChanged(IRefreshable.State state) {
        headerText.setText(String.valueOf(state));
      }

      @Override
      public void onPreRefresh() {
      }

      @Override
      public void onRefreshData() {
        try {
          synchronized (this) {
            wait(2000);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onRefreshUI() {
      }
    });

    final Button refreshButton = (Button) findViewById(R.id.refresh);
    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        refresherView.refreshShowingHeader();
      }
    });
  }
}
