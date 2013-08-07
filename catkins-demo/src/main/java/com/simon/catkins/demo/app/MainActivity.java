package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.simon.catkins.demo.R;
import com.simon.catkins.views.Flip3DLayout;
import com.simon.catkins.views.ViewHolderInjector;

import de.akquinet.android.androlog.Log;

import static com.simon.catkins.views.ViewHolderInjector.ViewId;

/**
 * @author Simon
 */
public class MainActivity extends Activity {

    private ViewHolder mHolder;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.init();
        setContentView(R.layout.main);

        mHolder = ViewHolderInjector.mapping(ViewHolder.class, findViewById(android.R.id.content));
        mHolder.mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_vertical:
                        mHolder.mFlipper.setTransition(Flip3DLayout.TRANSITION_HORIZONTAL);
                        break;
                    case R.id.radio_horizontal:
                        mHolder.mFlipper.setTransition(Flip3DLayout.TRANSITION_VERTICAL);
                        break;
                }
            }
        });
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.flip) {
            if (mHolder.mFlipper.getState() == Flip3DLayout.STATE_INITIAL) {
                mHolder.mFlipper.startFlip();
            } else {
                mHolder.mFlipper.startReverseFlip();
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public static class ViewHolder {
        @ViewId(R.id.flip)
        public Flip3DLayout mFlipper;

        @ViewId(R.id.group)
        public RadioGroup mRadioGroup;

        @ViewId(R.id.radio_vertical)
        public RadioButton mRadioVertical;

        @ViewId(R.id.radio_horizontal)
        public RadioButton mRadioHorizontal;
    }
}