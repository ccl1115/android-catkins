package com.bbsimon.android.demo.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.bbsimon.android.demo.R;
import com.bbsimon.android.demo.data.json.models.Demo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.akquinet.android.androlog.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

  private static final String TAG = "HomeActivity";

  private List<Demo> mDemos;

  @Override
  @SuppressWarnings("unchecked")
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.init(this);
    Log.v(TAG, "@onCreate");
    setContentView(R.layout.main);

    ListView listView = (ListView) findViewById(R.id.list);

    ObjectMapper mapper = new ObjectMapper();
    try {
      mDemos = mapper.readValue(getAssets().open("demos.json"), new TypeReference<List<Demo>>() {});
    } catch (IOException e) {
      Log.e(TAG, "@onCreate - Fail to load demos list.");
      e.printStackTrace();
      mDemos = new ArrayList<Demo>();
    }

    listView.setAdapter(new Adapter());
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(HomeActivity.this, mDemos.get(position).activityClass));
      }
    });
  }

  private static class ViewHolder {
    TextView mName;
    TextView mDescription;
  }

  private class Adapter extends BaseAdapter {
    private ViewHolder mHolder = new ViewHolder();

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
      if (convertView == null) {
        convertView = View.inflate(HomeActivity.this, R.layout.demo_list_item, null);
        mHolder.mName = (TextView) convertView.findViewById(R.id.name);
        mHolder.mDescription = (TextView) convertView.findViewById(R.id.description);
      }
      final Demo demo = mDemos.get(position);
      mHolder.mName.setText(demo.name);
      mHolder.mDescription.setText(demo.description);
      return convertView;
    }
  }
}

