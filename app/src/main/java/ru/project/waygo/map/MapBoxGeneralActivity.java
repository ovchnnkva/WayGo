package ru.project.waygo.map;

import static com.mapbox.maps.plugin.animation.CameraAnimationsUtils.getCamera;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;
import static com.mapbox.navigation.base.extensions.RouteOptionsExtensions.applyDefaultNavigationOptions;
import static ru.project.waygo.utils.Base64Util.stringToByte;
import static ru.project.waygo.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.waygo.utils.BitMapUtils.getBitmapFromDrawable;
import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.CITY_USER_AUTH_FILE;
import static ru.project.waygo.utils.CacheUtils.getFileCache;
import static ru.project.waygo.utils.CacheUtils.getFileName;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.Bearing;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.trip.session.LocationMatcherResult;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources;
import com.smarteist.autoimageslider.SliderView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.var;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.adapter.SliderAdapter;
import ru.project.waygo.fragment.SliderFragment;
import ru.project.waygo.user_profile.UserProfileActivity;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.favorite.FavoriteActivity;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.PointService;

public class MapBoxGeneralActivity extends BaseActivity {
    MapView mapView;
    FloatingActionButton focusLocationBtn;
    private final NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();
    private MapboxRouteLineView routeLineView;
    private MapboxRouteLineApi routeLineApi;
    boolean focusLocation = true;
    private MapboxNavigation mapboxNavigation;
    private BottomNavigationView bottomNavigationView;
    private RetrofitConfiguration retrofit;
    private ProgressBar loader;
    private ScrollView pointLayout;
    private TextView pointName;
    private TextView pointDescription;
    private SliderView slider;

    private List<PointDTO> pointDTOS;
    private final LocationObserver locationObserver = new LocationObserver() {
        @Override
        public void onNewRawLocation(@NonNull Location location) {

        }

        @Override
        public void onNewLocationMatcherResult(@NonNull LocationMatcherResult locationMatcherResult) {
            Location location = locationMatcherResult.getEnhancedLocation();
            navigationLocationProvider.changePosition(location, locationMatcherResult.getKeyPoints(), null, null);
            if (focusLocation) {
                updateCamera(Point.fromLngLat(location.getLongitude(), location.getLatitude()), (double) location.getBearing());
            }
        }
    };
    private final RoutesObserver routesObserver = new RoutesObserver() {
        @Override
        public void onRoutesChanged(@NonNull RoutesUpdatedResult routesUpdatedResult) {
            routeLineApi.setNavigationRoutes(routesUpdatedResult.getNavigationRoutes(), routeLineErrorRouteSetValueExpected ->
                    Optional.ofNullable(mapView.getMapboxMap().getStyle()).ifPresent(style -> {
                        routeLineView.renderRouteDrawData(style, routeLineErrorRouteSetValueExpected);
                    }));
        }
    };

    private void updateCamera(Point point, Double bearing) {
        MapAnimationOptions animationOptions = new MapAnimationOptions.Builder()
                .duration(1500L)
                .build();

        CameraOptions cameraOptions = new CameraOptions.Builder()
                .center(point)
                .zoom(18.0)
                .bearing(bearing)
                .pitch(45.0)
                .padding(new EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build();

        getCamera(mapView).easeTo(cameraOptions, animationOptions);
    }
    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
            focusLocation = false;
            getGestures(mapView).removeOnMoveListener(this);
            focusLocationBtn.show();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector moveGestureDetector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

        }
    };
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            Toast.makeText(MapBoxGeneralActivity.this, "Permission granted! Restart this app", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_box_general);

        mapView = findViewById(R.id.mapView);
        focusLocationBtn = findViewById(R.id.focusLocation);
        bottomNavigationView = findViewById(R.id.navigation_bar_map);
        loader = findViewById(R.id.loading);
        pointLayout = findViewById(R.id.scroll_general);
        pointName = findViewById(R.id.name_point);
        pointDescription = findViewById(R.id.descriprion_point);
        slider = findViewById(R.id.slider_map);
        slider.setAutoCycleDirection(SliderView.LAYOUT_DIRECTION_LTR);
        slider.setScrollTimeInSec(4);
        slider.setAutoCycle(true);
        slider.startAutoCycle();

        retrofit = new RetrofitConfiguration();

        bottomNavigationView.setSelectedItemId(R.id.action_map);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId())
            {
                case R.id.action_map:
                    return true;
                case R.id.action_main:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_favorites:
                    startActivity(new Intent(getApplicationContext(), FavoriteActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_account:
                    startActivity(new Intent(getApplicationContext(), UserProfileActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
            }
            return false;
        });

        RouteLineColorResources colorResources = new RouteLineColorResources.Builder()
                .routeDefaultColor(Color.parseColor("#7A67FE"))
                .build();

        MapboxRouteLineOptions options = new MapboxRouteLineOptions.Builder(this)
                .withRouteLineResources(new RouteLineResources.Builder()
                        .routeLineColorResources(colorResources)
                        .build())
                .withRouteLineBelowLayerId(LocationComponentConstants.LOCATION_INDICATOR_LAYER)
                .build();

        routeLineView = new MapboxRouteLineView(options);
        routeLineApi = new MapboxRouteLineApi(options);

        NavigationOptions navigationOptions = new NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .build();

        MapboxNavigationApp.setup(navigationOptions);
        mapboxNavigation = new MapboxNavigation(navigationOptions);

        mapboxNavigation.registerRoutesObserver(routesObserver);
        mapboxNavigation.registerLocationObserver(locationObserver);

        mapboxNavigation.startTripSession();

        focusLocationBtn.hide();
        LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
        getGestures(mapView).addOnMoveListener(onMoveListener);

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .zoom(20.0)
                    .build());
            locationComponentPlugin.setEnabled(true);
            locationComponentPlugin.setLocationProvider(navigationLocationProvider);
            getGestures(mapView).addOnMoveListener(onMoveListener);
            locationComponentPlugin.updateSettings(locationComponentSettings -> {
                locationComponentSettings.setEnabled(true);
                locationComponentSettings.setPulsingEnabled(true);
                return null;
            });

            getPoints();


            focusLocationBtn.setOnClickListener(view -> {
                focusLocation = true;
                getGestures(mapView).addOnMoveListener(onMoveListener);
                focusLocationBtn.hide();
            });

            locationComponentPlugin.addOnIndicatorPositionChangedListener(point -> {
                var result = routeLineApi.updateTraveledRouteLine(point);
                routeLineView.renderRouteLineUpdate(style, result);
            });
        });
    }

    private void getPoints() {
        PointService pointService = retrofit.createService(PointService.class);
        Call<List<PointDTO>> call = pointService.getByCity(getCity());
        showIndicator();
        call.enqueue(new Callback<List<PointDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<PointDTO>> call, @NonNull Response<List<PointDTO>> response) {
                if(response.isSuccessful()) {
                    pointDTOS = response.body();
                    createPoints();
                }
                hideIndicator();
            }

            @Override
            public void onFailure(@NonNull Call<List<PointDTO>> call, @NonNull Throwable t) {
                Log.i("POINT", "onFailure " + t.getLocalizedMessage());
                hideIndicator();
            }
        });
    }

    private String getCity() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return preferences.getString(CITY_USER_AUTH_FILE, "");
    }

    private void createPoints() {
        Bitmap bitmap = getBitmapFromDrawable(getApplicationContext(), R.drawable.dot_icon);
        AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
        PointAnnotationManager pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);
        pointAnnotationManager.addClickListener(pointAnnotation -> {
            pointLayout.setVisibility(View.GONE);
            List<Point> points = new ArrayList<>();
            points.add(pointAnnotation.getPoint());
            fetchRoute(points);
            createPointCard(findDtoByCoordinates(pointAnnotation.getPoint()));
            return true;
        });

        List<Point> points = pointDTOS.stream()
                .map(p -> Point.fromLngLat(Objects.requireNonNull(p).getLongitude(), p.getLatitude()))
                .collect(Collectors.toList());
        pointAnnotationManager.deleteAll();

        points.forEach(p -> {
            PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                    .withTextAnchor(TextAnchor.CENTER)
                    .withIconImage(bitmap)
                    .withPoint(p);
            pointAnnotationManager.create(pointAnnotationOptions);
        });
    }

    private PointDTO findDtoByCoordinates(Point point) {
        Log.i("FIND_POINT", "findDtoByCoordinates: "+ point.latitude() + ";" + point.longitude());
        return pointDTOS.stream()
                .filter(dto -> (point.latitude() == dto.getLatitude()) && (point.longitude() == dto.getLongitude()))
                .findFirst().orElse(null);
    }

    private void createPointCard(PointDTO dto) {
        if(dto == null) return;
        pointLayout.setVisibility(View.VISIBLE);
        pointName.setText(dto.getPointName());
        pointDescription.setText(dto.getDescription());
        fillSlider(getPointAllImages(dto)
                .stream()
                .map(SliderFragment::new)
                .collect(Collectors.toList()));
        Log.i("VISIBILITY", "createPointCard: " + pointLayout.getVisibility());
    }

    private void fillSlider(List<SliderFragment> fragments) {
        SliderAdapter adapter = new SliderAdapter(MapBoxGeneralActivity.this, fragments);
        slider.setSliderAdapter(adapter);
    }

    private List<Bitmap> getPointAllImages(PointDTO dto) {
        if(!dto.getPhoto().isEmpty()) {
            return dto.getPhoto()
                    .stream()
                    .map(s -> getBitmapFromBytes(stringToByte(s)))
                    .collect(Collectors.toList());
        }

        return List.of(getBitmapFromDrawable(getApplicationContext(), R.drawable.location_test));
    }

    @SuppressLint("MissingPermission")
    private void fetchRoute(List<Point> points) {
        showIndicator();
        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(MapBoxGeneralActivity.this);
        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();

                if(!checkUserLocation(location)) {
                    return;
                }

                List<Bearing> bearings = new ArrayList<>();
                bearings.add(Bearing.builder()
                        .angle(location.getBearing())
                        .degrees(45.0)
                        .build());

                for(int i = 0; i < points.size(); i++) {
                    bearings.add(null);
                }

                Point origin = Point.fromLngLat(Objects.requireNonNull(location).getLongitude(), location.getLatitude());
                points.add(origin);

                RouteOptions.Builder builder = RouteOptions.builder()
                        .coordinatesList(points)
                        .steps(true)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .alternatives(false)
                        .profile(DirectionsCriteria.PROFILE_WALKING)
                        .bearingsList(bearings);

                mapboxNavigation.requestRoutes(builder.build(), new NavigationRouterCallback() {
                    @Override
                    public void onRoutesReady(@NonNull List<NavigationRoute> list, @NonNull RouterOrigin routerOrigin) {
                        mapboxNavigation.setNavigationRoutes(list);
                        focusLocationBtn.performClick();
                        hideIndicator();
                    }

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {
                        Toast.makeText(MapBoxGeneralActivity.this, "Route request failed", Toast.LENGTH_SHORT).show();
                        hideIndicator();
                    }

                    @Override
                    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {
                        hideIndicator();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                hideIndicator();
            }
        });
    }

    private boolean checkUserLocation(Location location) {
        if(location == null || location.getLatitude() == 0.0 || location.getLongitude() == 0.0) {
            Toast.makeText(MapBoxGeneralActivity.this, "Включите геолокацию", Toast.LENGTH_LONG).show();
            return false;
        } else return true;
    }

    protected void showIndicator() {
        loader.setVisibility(View.VISIBLE);
    }

    protected void hideIndicator() {
        loader.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapboxNavigation.onDestroy();
        mapboxNavigation.unregisterRoutesObserver(routesObserver);
        mapboxNavigation.unregisterLocationObserver(locationObserver);
    }
}