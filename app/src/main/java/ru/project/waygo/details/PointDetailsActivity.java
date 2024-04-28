package ru.project.waygo.details;

import static ru.project.waygo.utils.Base64Util.stringToByte;
import static ru.project.waygo.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.waygo.utils.CacheUtils.getFileCache;
import static ru.project.waygo.utils.CacheUtils.getFileName;
import static ru.project.waygo.utils.IntentExtraUtils.getPointsExtra;
import static ru.project.waygo.utils.IntentExtraUtils.getRoutesFromExtra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarteist.autoimageslider.SliderView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.adapter.RoutePhotosAdapter;
import ru.project.waygo.adapter.SliderAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.fragment.RoutePhotosFragment;
import ru.project.waygo.fragment.SliderFragment;
import ru.project.waygo.map.MapBoxActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.RouteService;

public class PointDetailsActivity extends BaseActivity {

    private RecyclerView container;

    private TextView namePointField;
    private TextView descriptionField;
    private SliderView slider;
    private ToggleButton favorite;
    private RetrofitConfiguration retrofit;
    private List<RouteDTO> routesWithPoint = new ArrayList<>();
    private MaterialButton goToExcurssion;
    private PointDTO pointDTO;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_point_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        namePointField = findViewById(R.id.name_point);
        descriptionField = findViewById(R.id.descriprion_point);
        goToExcurssion = findViewById(R.id.go_to_excursion);
        slider = findViewById(R.id.slider_points);
        slider.setAutoCycleDirection(SliderView.LAYOUT_DIRECTION_LTR);
        slider.setScrollTimeInSec(4);
        slider.setAutoCycle(true);
        slider.startAutoCycle();
        favorite = findViewById(R.id.toggle_favorite);
        retrofit = new RetrofitConfiguration();

        container = findViewById(R.id.photos_container);
        container.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        container.setLayoutManager(linearLayoutManager);

        container.setAdapter(new RoutePhotosAdapter(PointDetailsActivity.this, new ArrayList<>()));
        fillFromIntent();

        if(routesWithPoint.isEmpty()) {
            getRoutesByPointId(pointDTO.getId());
        } else {
            fillRecycleFromCache();
        }

        setListeners();
    }

    private void setListeners() {
        Context context = PointDetailsActivity.this;
        Intent intent = new Intent(context, MapBoxActivity.class);
        goToExcurssion.setOnClickListener(e -> {
            if(checkPermissions()) {
                intent.putExtra("name", namePointField.getText().toString());
                intent.putExtra("description", descriptionField.getText().toString());
                intent.putExtra("points", getPointsExtra(List.of(pointDTO)));
                intent.putExtra("fromRoute", false);
                context.startActivity(intent);
            } else {
                launchPermissions();
            }
        });
    }
    private boolean checkPermissions() {
        boolean notificationPermission = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission = (ActivityCompat.checkSelfPermission(PointDetailsActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        }
        return notificationPermission
                && (ActivityCompat.checkSelfPermission(PointDetailsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(PointDetailsActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

    }

    private void launchPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        activityResultLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private void fillFromIntent() {
        Intent intent = getIntent();
        pointDTO = PointDTO.builder()
                .pointName(intent.getStringExtra("name"))
                .description(intent.getStringExtra("description"))
                .latitude(intent.getDoubleExtra("latitude", 0.0))
                .longitude(intent.getDoubleExtra("longitude", 0.0))
                .id(intent.getLongExtra("id", 0))
                .build();

        namePointField.setText(pointDTO.getPointName());
        descriptionField.setText(pointDTO.getDescription());
        favorite.setChecked(intent.getBooleanExtra("favorite", false));
        routesWithPoint = getRoutesFromExtra(
                Objects.requireNonNullElse(intent.getStringExtra("routes"), "")
        );

        setPointImage();
    }

    private void getRoutesByPointId(long id) {
        RouteService service = retrofit.createService(RouteService.class);
        Call<List<RouteDTO>> routes = service.getRoutesByPointId(id);

        routes.enqueue(new Callback<List<RouteDTO>>() {
            @Override
            public void onResponse(Call<List<RouteDTO>> call, Response<List<RouteDTO>> response) {
                if(response.isSuccessful()) {
                    routesWithPoint = response.body();
                }
            }

            @Override
            public void onFailure(Call<List<RouteDTO>> call, Throwable t) {

            }
        });
    }

    private void fillRecycleFromCache() {
        List<RoutePhotosFragment> fragments = new ArrayList<>();
        routesWithPoint.forEach(route -> {
            byte[] bytes = getFileCache(getApplicationContext(), getFileName("route", route.getId()));

            if(bytes != null) {
                String base64Photos = new String(bytes, StandardCharsets.UTF_8);
                fragments.add(new RoutePhotosFragment(getBitmapFromBytes(stringToByte(base64Photos)), route.getRouteName()));
            }
        });

        fillRecycle(fragments);
    }

    private void setPointImage() {
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointDTO.getId()));

        if(bytes != null) { //TODO: добавить получение изображения напрямую после того как будут добавлены изображения к маршрутам
            String[] base64Photos = new String(bytes, StandardCharsets.UTF_8).split(";");

            List<SliderFragment> fragments = Arrays.stream(base64Photos)
                    .map(s -> new SliderFragment(getBitmapFromBytes(stringToByte(s))))
                    .collect(Collectors.toList());

            Log.i("ROUTE_DETAILS", "fillSlider: count fragments " + fragments.size());
            SliderAdapter adapter = new SliderAdapter(PointDetailsActivity.this, fragments);
            slider.setSliderAdapter(adapter);
        }
    }

    private void fillRecycle(List<RoutePhotosFragment> fragments) {
        Log.i("POINT_DETAILS", "fillRecycle: count fragments " + fragments.size());
        RoutePhotosAdapter adapter = new RoutePhotosAdapter(PointDetailsActivity.this, fragments);
        container.setAdapter(adapter);
    }
}