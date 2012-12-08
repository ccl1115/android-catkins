package com.bbsimon.android.demo.app.demo;

import android.app.Activity;
import android.os.Bundle;
import com.bbsimon.android.demo.R;
import de.akquinet.android.androlog.Log;

/**
 * @author simon.yu
 */
public class FlipperLayoutDemoActivity extends Activity {
    private static final String TAG = "FlipperLayoutDemoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.init(this);
        Log.v(TAG, "@onCreate");
        setContentView(R.layout.flipper_layout_demo);
    }
}
