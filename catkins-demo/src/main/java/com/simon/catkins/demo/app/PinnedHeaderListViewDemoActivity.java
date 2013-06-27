package com.simon.catkins.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import com.simon.catkins.views.PinnedHeaderListView;

import static com.simon.catkins.views.PinnedHeaderListView.PinnedHeaderListAdapter;

/**
 * @author Simon
 */
public class PinnedHeaderListViewDemoActivity extends Activity {

    private static final String[] DATA = {
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned",
            "no pinned", "pinned", "no pinned", "no pinned", "no pinned", "no pinned", "no pinned"
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
            convertView = new TextView(parent.getContext());
            convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));
            convertView.setBackgroundColor((int) (Math.random() * 0xffffff)| 0xff000000);
            ((TextView) convertView).setText(DATA[position]);
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (DATA[position].equals("pinned")) {
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