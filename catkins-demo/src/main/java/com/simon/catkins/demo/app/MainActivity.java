package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;

import com.simon.catkins.demo.R;

import de.akquinet.android.androlog.Log;

/**
 * @author Simon
 */
public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.init();
        setContentView(R.layout.main);
    }
}