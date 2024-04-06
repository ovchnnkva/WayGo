package ru.project.waygo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.project.waygo.R;
import ru.project.waygo.fragment.RoutePhotosFragment;

public class RoutePhotosAdapter extends RecyclerView.Adapter<RoutePhotosAdapter.ViewHolder>{
    private final List<RoutePhotosFragment> fragments;
    private final LayoutInflater inflater;
    private final Context context;

    public RoutePhotosAdapter(Context context, List<RoutePhotosFragment> fragments){
        this.fragments = fragments;
        this.context =  context;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_route_photo, parent,false);
        return new RoutePhotosAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutePhotosAdapter.ViewHolder holder, int position) {
        RoutePhotosFragment fragment = fragments.get(position);
        holder.image.setImageBitmap(fragment.getImage());
        holder.name.setText(fragment.getName());
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        final ImageView image;
        final TextView name;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_location);
            name = itemView.findViewById(R.id.name);
        }
    }
}