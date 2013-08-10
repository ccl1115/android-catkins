package com.simon.catkins.demo.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simon.catkins.demo.R;
import com.simon.catkins.views.IndicatorView;

/**
 * @author Simon
 */
public class IndicatorDemoActivity extends FragmentActivity {
    private static final String TAG = "IndicatorDemoActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(TAG);

        setContentView(R.layout.indicator_demo);

        final IndicatorView indicatorView = (IndicatorView) findViewById(R.id.indicator);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new PagerAdapter());
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                indicatorView.setPosition(position + positionOffset);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            return new Fragment() {
                @Override
                public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                    View view = new View(container.getContext());
                    view.setBackgroundColor(0xff329231);
                    return view;
                }
            };
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}