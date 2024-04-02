package ru.project.waygo.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.project.waygo.R;
import ru.project.waygo.fragment.LocationFragment;
import ru.project.waygo.fragment.PointPhotosFragment;

public class PointPhotosAdapter extends RecyclerView.Adapter<PointPhotosAdapter.ViewHolder>{
    private final List<PointPhotosFragment> fragments;
    private final LayoutInflater inflater;
    private final Context context;

    public PointPhotosAdapter(Context context, List<PointPhotosFragment> fragments){
        this.fragments = fragments;
        this.context =  context;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_point_photo, parent,false);
        return new PointPhotosAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PointPhotosAdapter.ViewHolder holder, int position) {
        PointPhotosFragment fragment = fragments.get(position);
        Log.i("POINT_ADAPTER", "onBindViewHolder: image != null " + (fragment.getImage() != null));
        holder.image.setImageBitmap(fragment.getImage());
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        final ImageView image;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_location);
        }
    }
}
