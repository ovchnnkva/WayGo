package ru.project.waygo.fragment;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import ru.project.utils.BitMapUtils;
import ru.project.waygo.Constants.TypeLocation;
import ru.project.waygo.R;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;

@Setter
@Getter
public class LocationFragment extends Fragment {

    private long pointId;

    private List<Bitmap> image;

    private String name;

    private String description;

    private String routeLength = "";

    private boolean isFavorite = false;

    private double longitude;

    private double latitude;

    private TypeLocation typeLocation= TypeLocation.POINT;

    public LocationFragment() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public LocationFragment(PointDTO pointDTO) {
        this.pointId = pointDTO.getId();
        this.name = pointDTO.getPointName();
        this.longitude = pointDTO.getLongitude();
        this.latitude = pointDTO.getLatitude();
        this.description = pointDTO.getDescription();
        this.typeLocation = TypeLocation.POINT;
        this.image = pointDTO
                    .getPhotos()
                    .stream()
                    .map(image ->
                        getBitmapFromBytes(stringToByte(image)))
                    .collect(Collectors.toList());
    }

    public LocationFragment(RouteDTO routeDTO) {
        this.pointId = routeDTO.getId();
        this.name = routeDTO.getRouteName();
        this.routeLength = routeDTO.getLength() + " км";
        this.description = routeDTO.getDescription();
        this.typeLocation =TypeLocation.ROUTE;
    }

    public void setRouteLength(String routeLength) {
        this.routeLength = routeLength + " км";
    }


}