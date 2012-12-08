package com.bbsimon.android.demo.test;

import android.test.ActivityInstrumentationTestCase2;
import com.bbsimon.android.demo.app.HomeActivity;

public class HelloAndroidActivityTest extends ActivityInstrumentationTestCase2<HomeActivity> {

    public HelloAndroidActivityTest() {
        super(HomeActivity.class);
    }

    public void testActivity() {
        HomeActivity activity = getActivity();
        assertNotNull(activity);
    }
}

