package ru.project.waygo.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.project.waygo.R;
import ru.project.waygo.dto.point.PointDTO;

@Getter
@Setter
@NoArgsConstructor
public class PointFragment extends Fragment {
    private String name;
    private String description;
    private Bitmap image;

    public PointFragment(PointDTO point, Bitmap image) {
        this.name = point.getPointName();
        this.description = point.getDescription();
        this.image = image;
    }

}