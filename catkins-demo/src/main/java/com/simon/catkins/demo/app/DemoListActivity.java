package com.simon.catkins.demo.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.simon.catkins.demo.R;
import com.simon.catkins.demo.app.mvc.BaseController;
import com.simon.catkins.views.ViewHolderInjector;

/**
 * @author Simon Yu
 */
public class DemoListActivity extends FragmentActivity implements AdapterView.OnItemClickListener {

    private int mSeletedPosition;
    private DemoListActivity.ControllerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.demo_list_activity);

        ViewHolder holder = ViewHolderInjector.mapping(ViewHolder.class, findViewById(android.R.id.content));

        mAdapter = new ControllerAdapter();
        holder.listView.setAdapter(mAdapter);
        holder.listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSeletedPosition = position;
        mAdapter.notifyDataSetChanged();
        Config.ControllerEntry entry = Config.LIST.get(position);

        getActionBar().setTitle(entry.content);

        try {
            BaseController controller = entry.clazz.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.wrapper, controller)
                    .commit();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static class ViewHolder {
        @ViewHolderInjector.ViewId(R.id.list)
        public ListView listView;

        @ViewHolderInjector.ViewId(R.id.wrapper)
        public FrameLayout wrapper;
    }


    private class ControllerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return Config.LIST.size();
        }

        @Override
        public Object getItem(int position) {
            return Config.LIST.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.demo_list_item, parent, false);
            }

            convertView.setSelected(position == mSeletedPosition);

            ((TextView) convertView.findViewById(R.id.name)).setText(Config.LIST.get(position).content);

            return convertView;
        }
    }
}
