package com.simon.catkins.app;

import android.test.ActivityInstrumentationTestCase2;

import com.simon.catkins.demo.app.MainActivity;

/**
 * @author Simon Yu
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public static final int WAIT_TIME = 1000;
    private ExtendedSolo mSolo;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mSolo = new ExtendedSolo(getInstrumentation(), getActivity());
    }

    public void testFlip() throws Exception {
        mSolo.clickOnMenuItem("Flip");
        mSolo.clickOnMenuItem("Flip");
        mSolo.clickOnMenuItem("Flip");
        mSolo.clickOnRadioButton(0);
        mSolo.clickOnMenuItem("Flip");
        mSolo.clickOnMenuItem("Flip");
        mSolo.clickOnRadioButton(1);
        mSolo.clickOnMenuItem("Flip");
    }

    public void testTranslate() throws Exception {
        mSolo.drag(100, 300, 400, 400, 50);
        mSolo.waitForTime();
        mSolo.drag(400, 400, 100, 300, 50);
        mSolo.waitForTime();
        mSolo.drag(mSolo.getScreenWidth() - 100, mSolo.getScreenWidth() - 300, 400, 400, 50);
        mSolo.waitForTime();
    }

    @Override
    public void tearDown() throws Exception {
        mSolo.finishOpenedActivities();
        super.tearDown();
    }
}
