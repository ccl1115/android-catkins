package com.simon.catkins.demo.app.mvc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Simon Yu
 */
public abstract class BaseController extends Fragment {

    private Object mHolder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ViewHolderType viewHolderType = getClass().getAnnotation(ViewHolderType.class);
        if (viewHolderType == null) {
            throw new RuntimeException("no id to bind");
        }
        mHolder = ViewBinder.bind(viewHolderType.value(), view);
    }

    private int getLayoutId() {
        Layout layout = getClass().getAnnotation(Layout.class);
        if (layout == null) {
            throw new RuntimeException("no layout to inflate");
        }

        return layout.value();
    }

    protected Object getViewHolder() {
        return mHolder;
    }

}

