/*
 * Copyright (c) 2012. All rights reserved by bb.simon.yu@gmail.com
 */

package com.simon.catkins.demo.app;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.simon.catkins.demo.R;
import com.simon.catkins.demo.app.mvc.BaseController;
import com.simon.catkins.demo.app.mvc.Layout;
import com.simon.catkins.demo.app.mvc.ViewHolderType;
import com.simon.catkins.demo.app.mvc.ViewId;

import static com.simon.catkins.demo.app.FlipperLayoutController.ViewHolder;

/**
 * @author bb.simon.yu@gmail.com
 */
@Layout(R.layout.flipper_layout)
@ViewHolderType(ViewHolder.class)
public class FlipperLayoutController extends BaseController {

    public static class ViewHolder {
        @ViewId(R.id.content)
        public FrameLayout mContent;
        @ViewId(R.id.head)
        public LinearLayout mHead;
        @ViewId(R.id.handle)
        public Button mHandle;
    }
}
