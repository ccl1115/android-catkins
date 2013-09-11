package com.simon.catkins.demo.app;

import com.simon.catkins.demo.R;
import com.simon.catkins.demo.app.mvc.BaseController;
import com.simon.catkins.demo.app.mvc.Layout;
import com.simon.catkins.demo.app.mvc.ViewId;
import com.simon.catkins.views.HorizontalTranslateLayout;

/**
 */
@Layout(R.layout.horizontal_translate_layout_demo)
public class HorizontalTranslateLayoutController extends BaseController<HorizontalTranslateLayoutController.ViewHolder> {

    @Override
    protected Class<ViewHolder> getViewHolderType() {
        return ViewHolder.class;
    }

    public static class ViewHolder {
        @ViewId(R.id.container)
        public HorizontalTranslateLayout content;
    }
}
