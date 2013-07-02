package com.simon.catkins.demo.app;

import android.os.Bundle;
import android.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import com.simon.catkins.app.BaseActivity;
import com.simon.catkins.demo.R;

/**
 * @author Simon
 */
public class IndicatorDemoActivity extends BaseActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.indicator_demo);
    }

    class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}