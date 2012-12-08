package com.bbsimon.android.demo.app;

import android.app.Activity;
import android.os.Bundle;
import com.bbsimon.android.demo.R;
import de.akquinet.android.androlog.Log;

public class HomeActivity extends Activity {

  private static final String TAG = "HomeActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.init(this);
    Log.v(TAG, "@onCreate");
    setContentView(R.layout.main);
  }

}

