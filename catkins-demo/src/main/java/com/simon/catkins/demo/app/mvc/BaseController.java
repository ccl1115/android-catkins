package com.simon.catkins.demo.app.mvc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Simon Yu
 */
public abstract class BaseController<ViewHolder> extends Fragment {

    private ViewHolder mHolder;

    private boolean mUseMerge;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, mUseMerge);
    }

    protected abstract Class<ViewHolder> getViewHolderType();

    @Override
    @SuppressWarnings("unchecked")
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mHolder = ViewBinder.bind(getViewHolderType(), view);
    }

    private int getLayoutId() {
        Layout layout = getClass().getAnnotation(Layout.class);
        if (layout == null) {
            throw new RuntimeException("no layout to inflate");
        }

        mUseMerge = layout.useMerge();

        return layout.value();
    }

    protected ViewHolder getViewHolder() {
        return mHolder;
    }

}

