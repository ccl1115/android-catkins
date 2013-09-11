package com.simon.catkins.demo.app;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.simon.catkins.demo.R;
import com.simon.catkins.demo.app.mvc.BaseController;
import com.simon.catkins.demo.app.mvc.Layout;
import com.simon.catkins.demo.app.mvc.ViewId;
import com.simon.catkins.views.IndicatorView;

/**
 * @author Simon
 *
 */
@Layout(R.layout.indicator_demo)
public class IndicatorController extends BaseController<IndicatorController.ViewHolder> {
    private static final String TAG = "IndicatorController";

    @Override
    protected Class<ViewHolder> getViewHolderType() {
        return ViewHolder.class;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViewHolder().viewPager.setAdapter(new DebugPagerAdapter());
        getViewHolder().viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                getViewHolder().indicatorView.setPosition(position + positionOffset);
            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    public static class ViewHolder {
        @ViewId(R.id.indicator)
        public IndicatorView indicatorView;
        @ViewId(R.id.pager)
        public ViewPager viewPager;
    }

    @Override
    protected ViewHolder getViewHolder() {
        return (ViewHolder) super.getViewHolder();
    }

    class DebugPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view.equals(o);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = new View(container.getContext());
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}