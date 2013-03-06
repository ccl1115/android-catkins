/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.demo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.simon.catkins.app.BaseActivity;
import com.simon.catkins.demo.R;
import com.simon.catkins.demo.data.json.models.Demo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.akquinet.android.androlog.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {

  private static final String TAG = "HomeActivity";

  private List<Demo> mDemos;

  @Override
  @SuppressWarnings("unchecked")
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "@onCreate");

    getTitleBar().setTitle(TAG);

    setContentView(R.layout.home);

    ListView listView = (ListView) findViewById(R.id.content);

    ObjectMapper mapper = new ObjectMapper();
    try {
      mDemos = mapper.readValue(getAssets().open("demos.json"),
          new TypeReference<List<Demo>>() {
          });
    } catch (IOException e) {
      Log.e(TAG, "@onCreate - Fail to load demos list.");
      e.printStackTrace();
      mDemos = new ArrayList<Demo>();
    }

    listView.setAdapter(new Adapter());
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(HomeActivity.this,
            mDemos.get(position).activityClass));
      }
    });
  }

  private static class ViewHolder {
    TextView mName;
    TextView mDescription;
  }

  private class Adapter extends BaseAdapter {

    @Override
    public int getCount() {
      return mDemos.size();
    }

    @Override
    public Object getItem(int position) {
      return mDemos.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder holder;
      if (convertView == null) {
        convertView = View.inflate(HomeActivity.this,
            R.layout.demo_list_item, null);
        holder = new ViewHolder();
        holder.mName = (TextView) convertView.findViewById(R.id.name);
        holder.mDescription =
            (TextView) convertView.findViewById(R.id.description);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }
      final Demo demo = mDemos.get(position);
      holder.mName.setText(demo.name);
      holder.mDescription.setText(demo.description);
      return convertView;
    }
  }
}

