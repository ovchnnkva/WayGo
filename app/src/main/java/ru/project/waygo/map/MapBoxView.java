package ru.project.waygo.map;

import static com.mapbox.maps.plugin.animation.CameraAnimationsUtils.getCamera;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.addOnMapClickListener;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;
import static com.mapbox.navigation.base.extensions.RouteOptionsExtensions.applyDefaultNavigationOptions;
import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.utils.BitMapUtils.getBitmapFromDrawable;
import static ru.project.utils.CacheUtils.getFileCache;
import static ru.project.utils.CacheUtils.getFileName;
import static ru.project.utils.IntentExtraUtils.getPointsFromExtra;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources;
import com.smarteist.autoimageslider.SliderView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.var;
import ru.project.waygo.R;
import ru.project.waygo.SliderFragment;
import ru.project.waygo.adapter.SliderAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.fragment.PointFragment;

public class MapBoxView extends AppCompatActivity {
    MapView mapView;
    FloatingActionButton focusLocationBtn;
    private final NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();
    private MapboxRouteLineView routeLineView;
    private MapboxRouteLineApi routeLineApi;
    boolean focusLocation = true;
    private MapboxNavigation mapboxNavigation;
    private SliderView slider;
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
            Toast.makeText(MapBoxView.this, "Permission granted! Restart this app", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        mapView = findViewById(R.id.mapView);
        focusLocationBtn = findViewById(R.id.focusLocation);
        slider = findViewById(R.id.slider_map);

        getIntent();

        MapboxRouteLineOptions options = new MapboxRouteLineOptions.Builder(this)
                .withRouteLineResources(new RouteLineResources.Builder().build())
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(MapBoxView.this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (ActivityCompat.checkSelfPermission(MapBoxView.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MapBoxView.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            activityResultLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            mapboxNavigation.startTripSession();
        }

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

            Bitmap bitmap = getBitmapFromDrawable(getApplicationContext(), R.drawable.dot_icon);
            AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
            PointAnnotationManager pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);

            List<PointDTO> pointsDto = getPoints();
            if(!pointsDto.isEmpty()) {
                List<Point> points = pointsDto.stream()
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

                fetchRoute(points);
            } else {
                addOnMapClickListener(mapView.getMapboxMap(), point -> {
                    pointAnnotationManager.deleteAll();
                    PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                            .withTextAnchor(TextAnchor.CENTER)
                            .withIconImage(bitmap)
                            .withPoint(point);
                    pointAnnotationManager.create(pointAnnotationOptions);

                    return true;
                });
            }

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

    private List<PointDTO> getPoints() {
        Intent intent = getIntent();
        List<PointDTO> points = new ArrayList<>();
        if(intent != null) {
            String extraPoints = intent.getStringExtra("points");
            points = getPointsFromExtra(extraPoints);
        }
        List<PointFragment> fragments = points
                .stream()
                .map(point -> new PointFragment(point, getPointImage(point.getId())))
                .collect(Collectors.toList());
        List<SliderFragment> images = fragments
                .stream()
                .map(fragment -> new SliderFragment(fragment.getImage()))
                .collect(Collectors.toList());

        fillSlider(images);

        return points;
    }

    private Bitmap getPointImage(long pointId) {
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointId));

        if(bytes != null) {
            String base64Photos = new String(bytes, StandardCharsets.UTF_8);
            return getBitmapFromBytes(stringToByte(base64Photos));
        }

        return getBitmapFromDrawable(getApplicationContext(), R.drawable.location_test);
    }

    private void fillSlider(List<SliderFragment> fragments) {
        Log.i("MAP_SLIDER", "fillSlider: count fragments " + fragments.size());
        SliderAdapter adapter = new SliderAdapter(MapBoxView.this, fragments);
        slider.setSliderAdapter(adapter);

    }
    @SuppressLint("MissingPermission")
    private void fetchRoute(List<Point> points) {
        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(MapBoxView.this);
        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();

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
                        .alternatives(false)
                        .profile(DirectionsCriteria.PROFILE_WALKING)
                        .bearingsList(bearings);
                applyDefaultNavigationOptions(builder);

                mapboxNavigation.requestRoutes(builder.build(), new NavigationRouterCallback() {
                    @Override
                    public void onRoutesReady(@NonNull List<NavigationRoute> list, @NonNull RouterOrigin routerOrigin) {
                        mapboxNavigation.setNavigationRoutes(list);
                        focusLocationBtn.performClick();
                    }

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {
                        Toast.makeText(MapBoxView.this, "Route request failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {

                    }
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapboxNavigation.onDestroy();
        mapboxNavigation.unregisterRoutesObserver(routesObserver);
        mapboxNavigation.unregisterLocationObserver(locationObserver);
    }
}