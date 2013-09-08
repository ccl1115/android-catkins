package com.simon.catkins.demo.app;

import com.simon.catkins.demo.R;
import com.simon.catkins.demo.app.mvc.BaseController;
import com.simon.catkins.demo.app.mvc.Layout;
import com.simon.catkins.demo.app.mvc.ViewHolderType;
import com.simon.catkins.demo.app.mvc.ViewId;
import com.simon.catkins.views.HorizontalTranslateLayout;

import static com.simon.catkins.demo.app.HorizontalTranslateLayoutController.ViewHolder;

/**
 */
@Layout(R.layout.horizontal_translate_layout_demo)
@ViewHolderType(ViewHolder.class)
public class HorizontalTranslateLayoutController extends BaseController {

    public static class ViewHolder {
        @ViewId(R.id.container)
        public HorizontalTranslateLayout content;
    }
}
