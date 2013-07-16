package com.simon.catkins.views;

import android.content.Context;
import android.view.View;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * @author Simon Yu
 */
public abstract class ViewHolderInjector {

    @Target({ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface ViewId {
        public int value() default 0;
    }

    public static <T> T mapping(Class<T> holderClass, View view) {
        T holderObject;
        try {
            holderObject = holderClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

        Field[] fields = holderClass.getDeclaredFields();

        for (Field field : fields) {
            ViewId childId = field.getAnnotation(ViewId.class);
            if (childId != null) {
                View child = view.findViewById(childId.value());
                try {
                    field.set(holderObject, child);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }

        return holderObject;
    }
}
