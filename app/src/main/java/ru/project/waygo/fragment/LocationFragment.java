package ru.project.waygo.fragment;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import ru.project.waygo.Constants.TypeLocation;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;

@Setter
@Getter
public class LocationFragment extends Fragment {

    private long locationId;

    private List<Bitmap> images = new ArrayList<>();

    private String name;

    private String description;

    private String routeLength = "";

    private boolean isFavorite = false;

    private double longitude;

    private double latitude;

    private TypeLocation typeLocation= TypeLocation.POINT;
    private List<PointDTO> points;
    private String imageName;
    private String extra;

    public LocationFragment() {
    }

    public LocationFragment(PointDTO pointDTO, String routeExtra) {
        this.locationId = pointDTO.getId();
        this.name = pointDTO.getPointName();
        this.longitude = pointDTO.getLongitude();
        this.latitude = pointDTO.getLatitude();
        this.description = pointDTO.getDescription();
        this.typeLocation = TypeLocation.POINT;
        this.images.addAll(pointDTO.getPhoto()
                .stream()
                .map(p -> getBitmapFromBytes(stringToByte(p)))
                .collect(Collectors.toList()));
        this.extra = routeExtra;
    }

    public LocationFragment(RouteDTO routeDTO, String pointExtra) {
        this.locationId = routeDTO.getId();
        this.name = routeDTO.getRouteName();
        this.routeLength = routeDTO.getLength() + " км";
        this.description = routeDTO.getDescription();
        this.typeLocation = TypeLocation.ROUTE;
        this.points = routeDTO.getStopsOnRoute();
        this.extra = pointExtra;
        routeDTO.getStopsOnRoute()
                .forEach(point -> images.add(getBitmapFromBytes(stringToByte(point.getPhoto().get(0)))));

    }

    public void setRouteLength(String routeLength) {
        this.routeLength = routeLength + " км";
    }


}