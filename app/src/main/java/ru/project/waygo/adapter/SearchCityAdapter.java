package ru.project.waygo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class SearchCityAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private List<String> cities;
    public SearchCityAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public Object getItem(int position) {
        return cities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder holder;
        if(view == null) {
            holder = new ViewHolder();
        }
        return null;
    }

    public class ViewHolder {
        TextView name;
    }
}
