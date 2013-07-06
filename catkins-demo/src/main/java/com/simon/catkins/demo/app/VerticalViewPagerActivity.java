package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.simon.catkins.demo.R;
import com.simon.catkins.views.VerticalViewPager;

/**
 * @author bb.simon.yu@gmail.com
 */
public class VerticalViewPagerActivity extends Activity {
  private static final String TAG = "VerticalViewPagerActivity";

  private static final String[] DATA = {
      "This is a hello world",
      "This is a second hello world",
      "This is not a hello world",
      "This is not the second hello world"
  };

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.vertical_view_pager);

    getActionBar().setTitle(TAG);

    VerticalViewPager verticalViewPager = (VerticalViewPager) findViewById(R.id.view_pager);

    verticalViewPager.setAdapter(new Adapter());
  }

  public class Adapter extends BaseAdapter {

    @Override
    public int getCount() {
      return DATA.length;
    }

    @Override
    public Object getItem(int position) {
      return DATA[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        Log.d(TAG, "@getView convertView created");
        convertView = new TextView(VerticalViewPagerActivity.this);
        convertView.setBackgroundColor(0xFF333333);
        ((TextView) convertView).setGravity(Gravity.CENTER);
      }
      ((TextView) convertView).setText(DATA[position]);
      return convertView;
    }
  }
}