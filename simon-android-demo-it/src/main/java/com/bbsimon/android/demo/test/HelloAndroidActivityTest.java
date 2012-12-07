package com.bbsimon.android.demo.test;

import android.test.ActivityInstrumentationTestCase2;
import com.bbsimon.android.demo.app.HelloAndroidActivity;

public class HelloAndroidActivityTest extends ActivityInstrumentationTestCase2<HelloAndroidActivity> {

    public HelloAndroidActivityTest() {
        super(HelloAndroidActivity.class);
    }

    public void testActivity() {
        HelloAndroidActivity activity = getActivity();
        assertNotNull(activity);
    }
}

