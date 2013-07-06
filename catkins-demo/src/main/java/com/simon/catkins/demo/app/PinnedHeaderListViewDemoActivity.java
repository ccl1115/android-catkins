package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import com.simon.catkins.demo.R;
import com.simon.catkins.views.PinnedHeaderListView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.simon.catkins.views.PinnedHeaderListView.PinnedHeaderListAdapter;

/**
 * @author Simon
 */
public class PinnedHeaderListViewDemoActivity extends Activity {
    private static final String TAG = "PinnedHeaderListViewDemoActivity";
    private static final String[] DATA = {
            "no pinned", "pinned 10", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned 3", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned 44444444", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned 19", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned 3999", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned"
    };
    private PinnedHeaderListView listView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = new PinnedHeaderListView(this);
        listView.setAdapter(new TestAdapter());
        setContentView(listView);

        getActionBar().setTitle(TAG);
    }

    private class TestAdapter extends PinnedHeaderListAdapter {
        private static final int PINNED_TYPE = 1;
        private static final int NORMAL_TYPE = 0;

        private int mLastPinnedPosition;

        @Override
        public int getPinnedHeaderViewType() {
            return PINNED_TYPE;
        }

        @Override
        public View getPinnedHeaderView() {
            TextView view = new TextView(PinnedHeaderListViewDemoActivity.this);
            view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 100));
            view.setBackgroundColor(0x75ff0000);
            view.setText("pinned");
            view.setTextColor(0x75ffffff);
            view.setGravity(Gravity.CENTER_VERTICAL);
            return view;
        }

        @Override
        public void updatePinnedHeaderView(View header, int position) {
            Log.d(TAG, "update header = " + position);
            if (position == -1) {
                position = seekNextPinnedHeader(position);
                final View view = listView.findViewWithTag(position);
                if (view != null) {
                    view.findViewById(R.id.text).setVisibility(VISIBLE);
                }
                return;
            }
            ((TextView) header).setText(DATA[position]);
            View view = listView.findViewWithTag(position);
            if (view != null) {
                view.findViewById(R.id.text).setVisibility(INVISIBLE);
            }
            mLastPinnedPosition = position;
            position = seekNextPinnedHeader(position);
            view = listView.findViewWithTag(position);
            if (view != null) {
                view.findViewById(R.id.text).setVisibility(VISIBLE);
            }
        }

        private int seekNextPinnedHeader(int position) {
            final int count = getCount();
            for (int i = position + 1; i < count; i++) {
                if (getItemViewType(i) == getPinnedHeaderViewType()) {
                    position = i;
                    break;
                }
            }
            return position;
        }

        @Override
        public int getCount() {
            return DATA.length;
        }

        @Override
        public Object getItem(int position) {
            return DATA[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.pinned_header_list_item, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.text)).setText(DATA[position]);
            switch (getItemViewType(position)) {
                case PINNED_TYPE:
                    convertView.setBackgroundColor(0xff333333);
                    convertView.findViewById(R.id.text).setBackgroundColor(0xffff0000);
                    ((TextView) convertView.findViewById(R.id.item)).setText("item count");
                    if (mLastPinnedPosition < position) {
                        convertView.findViewById(R.id.text).setVisibility(VISIBLE);
                    }
                    break;
            }
            convertView.setTag(position);
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (DATA[position].startsWith("pinned")) {
                return PINNED_TYPE;
            } else {
                return NORMAL_TYPE;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }
}