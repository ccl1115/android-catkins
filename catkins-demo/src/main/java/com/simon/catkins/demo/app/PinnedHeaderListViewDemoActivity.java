package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simon.catkins.demo.R;
import com.simon.catkins.views.PinnedHeaderListView;

import static com.simon.catkins.views.PinnedHeaderListView.PinnedHeaderListAdapter;

/**
 * @author Simon
 */
public class PinnedHeaderListViewDemoActivity extends Activity {
    private static final String TAG = "PinnedHeaderListViewDemoActivity";
    private static final String[] DATA = {
            "pinned 100", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PinnedHeaderListView listView = new PinnedHeaderListView(this);
        listView.setAdapter(new TestAdapter());
        setContentView(listView);
    }

    private class TestAdapter extends PinnedHeaderListAdapter {
        private static final int PINNED_TYPE = 1;
        private static final int NORMAL_TYPE = 0;

        @Override
        public int getPinnedHeaderViewType() {
            return PINNED_TYPE;
        }

        @Override
        public View getPinnedHeaderView() {
            TextView view = new TextView(PinnedHeaderListViewDemoActivity.this);
            view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 100));
            view.setBackgroundColor(0xffff0000);
            view.setText("pinned");
            view.setGravity(Gravity.CENTER_VERTICAL);
            return view;
        }

        @Override
        public void updatePinnedHeaderView(View header, int position) {
            Log.d(TAG, "update header = " + position);
            ((TextView)header).setText(DATA[position]);
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
                    ((TextView)convertView.findViewById(R.id.item)).setText("item count");
                    break;
            }
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