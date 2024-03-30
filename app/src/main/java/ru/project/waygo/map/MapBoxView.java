package ru.project.waygo.map;

import static com.mapbox.maps.plugin.animation.CameraAnimationsUtils.getCamera;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.addOnMapClickListener;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;
import static com.mapbox.navigation.base.extensions.RouteOptionsExtensions.applyDefaultNavigationOptions;

import static ru.project.utils.BitMapUtils.getBitmapFromDrawable;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
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
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.button.MaterialButton;
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
import com.mapbox.navigation.core.RoutesRefreshData;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import lombok.var;
import ru.project.waygo.R;
import ru.project.waygo.dto.point.PointDTO;

public class MapBoxView extends AppCompatActivity {
    MapView mapView;
    MaterialButton setRoute;
    FloatingActionButton focusLocationBtn;
    private final NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();
    private MapboxRouteLineView routeLineView;
    private MapboxRouteLineApi routeLineApi;
    boolean focusLocation = true;
    private MapboxNavigation mapboxNavigation;
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
            routeLineApi.setNavigationRoutes(routesUpdatedResult.getNavigationRoutes(), routeLineErrorRouteSetValueExpected -> {
                Optional.ofNullable(mapView.getMapboxMap().getStyle()).ifPresent(style -> {
                    routeLineView.renderRouteDrawData(style, routeLineErrorRouteSetValueExpected);
                });
            });
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
        setRoute = findViewById(R.id.setRoute);
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
                && ActivityCompat.checkSelfPermission(MapBoxView.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            activityResultLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            mapboxNavigation.startTripSession();
        }

        focusLocationBtn.hide();
        LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
        getGestures(mapView).addOnMoveListener(onMoveListener);


        setRoute.setOnClickListener(view ->
                Toast.makeText(MapBoxView.this, "Please select a location in map", Toast.LENGTH_SHORT).show()
        );

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

            PointDTO location = getPoint();
            if(location != null) {
                Point point = Point.fromLngLat(Objects.requireNonNull(location).getLongitude(), location.getLatitude());
                pointAnnotationManager.deleteAll();
                PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                        .withTextAnchor(TextAnchor.CENTER)
                        .withIconImage(bitmap)
                        .withPoint(point);
                pointAnnotationManager.create(pointAnnotationOptions);
                fetchRoute(point);
            } else {
                addOnMapClickListener(mapView.getMapboxMap(), point -> {
                    pointAnnotationManager.deleteAll();
                    PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                            .withTextAnchor(TextAnchor.CENTER)
                            .withIconImage(bitmap)
                            .withPoint(point);
                    pointAnnotationManager.create(pointAnnotationOptions);

                    setRoute.setOnClickListener(view -> fetchRoute(point));
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

    private PointDTO getPoint() {
        Intent intent = getIntent();
        PointDTO pointDTO = null;

        if(intent != null) {
            pointDTO = PointDTO.builder()
                    .id(intent.getLongExtra("id", 0))
                    .longitude(intent.getDoubleExtra("longitude", 0))
                    .latitude(intent.getDoubleExtra("latitude", 0))
                    .build();
        }

        return pointDTO;
    }

    @SuppressLint("MissingPermission")
    private void fetchRoute(Point point) {
        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(MapBoxView.this);
        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();
                setRoute.setEnabled(false);
                setRoute.setText("Fetching route...");
                Point origin = Point.fromLngLat(Objects.requireNonNull(location).getLongitude(), location.getLatitude());

                RouteOptions.Builder builder = RouteOptions.builder()
                        .coordinatesList(Arrays.asList(origin, point))
                        .alternatives(false)
                        .profile(DirectionsCriteria.PROFILE_WALKING)
                        .bearingsList(Arrays.asList(Bearing.builder()
                                .angle(location.getBearing())
                                .degrees(45.0)
                                .build(),
                                null));
                applyDefaultNavigationOptions(builder);

                mapboxNavigation.requestRoutes(builder.build(), new NavigationRouterCallback() {
                    @Override
                    public void onRoutesReady(@NonNull List<NavigationRoute> list, @NonNull RouterOrigin routerOrigin) {
                        mapboxNavigation.setNavigationRoutes(list);
                        focusLocationBtn.performClick();
                        setRoute.setEnabled(true);
                        setRoute.setText("Set route");
                    }

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {
                        setRoute.setEnabled(true);
                        setRoute.setText("Set route");
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