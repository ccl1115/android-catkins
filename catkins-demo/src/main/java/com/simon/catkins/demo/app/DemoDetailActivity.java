package com.simon.catkins.demo.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * @author Simon Yu
 */
public class DemoDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String id = getIntent().getStringExtra(Config.ENTRY_ID);

    }

}
