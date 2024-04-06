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
import ru.project.waygo.fragment.PointFragment;
import ru.project.waygo.fragment.RoutePhotosFragment;

public class PointAdapter extends RecyclerView.Adapter<PointAdapter.ViewHolder>{
    private final List<PointFragment> fragments;
    private final LayoutInflater inflater;
    private final Context context;

    public PointAdapter(Context context, List<PointFragment> fragments){
        this.fragments = fragments;
        this.context =  context;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public PointAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_point, parent,false);
        return new PointAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PointAdapter.ViewHolder holder, int position) {
        PointFragment fragment = fragments.get(position);
        holder.image.setImageBitmap(fragment.getImage());
        holder.name.setText(fragment.getName());
        holder.description.setText(fragment.getDescription());
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        final ImageView image;
        final TextView name;
        final TextView description;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_location);
            name = itemView.findViewById(R.id.name);
            description = itemView.findViewById(R.id.descriprion);
        }
    }
}
