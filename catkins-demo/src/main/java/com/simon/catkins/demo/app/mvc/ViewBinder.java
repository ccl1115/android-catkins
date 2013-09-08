package com.simon.catkins.demo.app.mvc;

import android.view.View;

import java.lang.reflect.Field;

/**
 * @author Simon Yu
 */
class ViewBinder {
    static <T> T bind(Class<T> clazz, View parent) {
        T holder;
        try {
            holder = clazz.newInstance();
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }

        Field[] fields = clazz.getFields();

        for (Field f : fields) {
            final ViewId viewId = f.getAnnotation(ViewId.class);
            if (viewId != null) {
                final int id = viewId.value();
                try {
                    f.set(holder, parent.findViewById(id));
                } catch (IllegalAccessException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return holder;
    }
}
