package com.simon.catkins.app;

import android.app.Activity;
import android.app.Instrumentation;

import com.jayway.android.robotium.solo.Condition;
import com.jayway.android.robotium.solo.Solo;

/**
 * @author Simon Yu
 */
public class ExtendedSolo extends Solo {

    private Condition mFalseCondition = new Condition() {
        @Override
        public boolean isSatisfied() {
            return false;
        }
    };

    private static final int DEFAULT_WAIT_TIME = 1000;

    public ExtendedSolo(Instrumentation instrumentation, Activity activity) {
        super(instrumentation, activity);
    }

    public ExtendedSolo(Instrumentation instrumentation) {
        super(instrumentation);
    }

    public void waitForTime(int millisecond) {
        waitForCondition(mFalseCondition, millisecond);
    }

    public void waitForTime() {
        waitForTime(DEFAULT_WAIT_TIME);
    }

    public int getScreenWidth() {
        return getCurrentActivity().getResources().getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return getCurrentActivity().getResources().getDisplayMetrics().heightPixels;
    }


}
