package ru.project.waygo.map;

import static com.mapbox.maps.plugin.animation.CameraAnimationsUtils.getCamera;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;
import static com.mapbox.navigation.base.extensions.RouteOptionsExtensions.applyDefaultNavigationOptions;
import static ru.project.waygo.utils.Base64Util.stringToByte;
import static ru.project.waygo.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.waygo.utils.BitMapUtils.getBitmapFromDrawable;
import static ru.project.waygo.utils.CacheUtils.getFileCache;
import static ru.project.waygo.utils.CacheUtils.getFileName;
import static ru.project.waygo.utils.IntentExtraUtils.getPointsFromExtra;
import static ru.project.waygo.utils.StringUtils.getAudioTimeString;
import static ru.project.waygo.retrofit.RetrofitConfiguration.SERVER_URL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

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
import com.mapbox.turf.TurfMeasurement;
import com.smarteist.autoimageslider.SliderView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.var;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.fragment.SliderFragment;
import ru.project.waygo.adapter.SliderAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.rating.RatingActivity;

public class MapBoxActivity extends BaseActivity {
    private MapView mapView;
    private FloatingActionButton focusLocationBtn;
    private final NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();
    private MapboxRouteLineView routeLineView;
    private MapboxRouteLineApi routeLineApi;
    boolean focusLocation = true;
    private MapboxNavigation mapboxNavigation;
    private SliderView slider;
    private ToggleButton playButton;
    private MaterialButton backwardButton;
    private MaterialButton forwardButton;
    private TextView currentTimeText;
    private TextView allTimeText;

    private TextView nameText;
    private TextView descriptionText;
    private MediaPlayer player;
    private Handler handler;
    private SeekBar seekBar;
    private ProgressBar loader;
    private ToggleButton speedAudioButton;
    private MaterialButton nextPointButton;
    private List<PointDTO> pointsDto;
    private long routeId;
    private boolean isFromRoute;
    private ConstraintLayout layoutPlayer;

    private final Runnable updateSongTime = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            if(seekBar.getProgress() == seekBar.getMax()) {
                seekBar.setProgress(0);
                playButton.setChecked(false);
                currentTimeText.setText("00:00");
            } else {
                int currentTime = player.getCurrentPosition();
                currentTimeText.setText(getAudioTimeString(currentTime));
                seekBar.setProgress(currentTime);
                handler.postDelayed(this, 100);
            }
        }
    };
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
            Toast.makeText(MapBoxActivity.this, "Permission granted! Restart this app", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        mapView = findViewById(R.id.mapView);
        focusLocationBtn = findViewById(R.id.focusLocation);
        slider = findViewById(R.id.slider_map);
        slider.setAutoCycleDirection(SliderView.LAYOUT_DIRECTION_LTR);
        slider.setScrollTimeInSec(2);
        slider.setAutoCycle(true);
        slider.startAutoCycle();
        playButton = findViewById(R.id.toggle_play);
        backwardButton = findViewById(R.id.button_backward);
        forwardButton = findViewById(R.id.button_forward);
        currentTimeText = findViewById(R.id.current_time);
        allTimeText = findViewById(R.id.all_time);
        nameText = findViewById(R.id.name_point);
        descriptionText = findViewById(R.id.descriprion_point);
        seekBar = findViewById(R.id.seek_bar);
        loader = findViewById(R.id.loading);
        speedAudioButton = findViewById(R.id.toggle_speed);
        layoutPlayer = findViewById(R.id.layout_player);
        nextPointButton = findViewById(R.id.button_next_point);
        player = new MediaPlayer();

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(MapBoxActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (ActivityCompat.checkSelfPermission(MapBoxActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MapBoxActivity.this,
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

            pointsDto = getFromIntent();
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

            fetchRoute();

            nextPointButton.setOnClickListener(view -> {
                if(!pointsDto.isEmpty()) {
                    fetchRoute();
                } else if(isFromRoute){
                    Intent intent = new Intent(getApplicationContext(), RatingActivity.class);
                    intent.putExtra("routeId", routeId);
                    intent.putExtra("name", nameText.getText().toString());
                    playerStop();
                    startActivity(intent);
                } else {
                    playerStop();
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                }
            });
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

    private void playerStop() {
        if(player.isPlaying()) {
            player.stop();
        }
    }

    @SuppressLint("DefaultLocale")
    private void createAudioPlayer(long pointId) {
        player.reset();
        handler = new Handler();

        player.setAudioAttributes(new AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).
                setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        player.setWakeMode(MapBoxActivity.this, PowerManager.PARTIAL_WAKE_LOCK);
        try {
            player.setDataSource(SERVER_URL + "api/point/audio?pointId=" + pointId);
            player.setOnPreparedListener(player -> layoutPlayer.setVisibility(View.VISIBLE));
            player.setOnErrorListener((player, what, extra)  -> {
                layoutPlayer.setVisibility(View.GONE);
                return false;
            });
            player.prepareAsync();
        } catch (IOException e) {

            Log.e("AUDIO_PLAYER", "createAudioPlayer: ", e);
            return;
        }


        playButton.setOnClickListener(view -> {
            if(playButton.isChecked()) {
                player.start();
                int allTime = player.getDuration();
                allTimeText.setText(getAudioTimeString(allTime));
                seekBar.setMax(allTime);
                handler.postDelayed(updateSongTime, 100);
            } else {
                player.pause();
            }
        });

        backwardButton.setOnClickListener(view -> {
            int currentPosition = player.getCurrentPosition();

            if((currentPosition - 15000) > 0) {
                player.seekTo(currentPosition - 15000);
            } else {
                player.seekTo(0);
            }
        });

        forwardButton.setOnClickListener(view -> {
            int currentPosition = player.getCurrentPosition();

            if((currentPosition + 30000) <= player.getDuration()) {
                player.seekTo(currentPosition + 30000);
            } else {
                player.seekTo(player.getDuration());
            }
        });

        speedAudioButton.setOnClickListener(view -> {
            float speed = 1.0F;
            if(speedAudioButton.isChecked()) {
                speed = 2.0F;
            }

            player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(seekBar.getProgress());
            }
        });
    }

    private List<PointDTO> getFromIntent() {
        Intent intent = getIntent();
        List<PointDTO> points = new ArrayList<>();
        List<SliderFragment> images = new ArrayList<>();

        if(intent != null) {
            String extraPoints = intent.getStringExtra("points");
            points = getPointsFromExtra(extraPoints);
            nameText.setText(intent.getStringExtra("name"));
            descriptionText.setText(intent.getStringExtra("description"));
            isFromRoute = intent.getBooleanExtra("fromRoute", false);
            if (isFromRoute) {
                routeId = intent.getLongExtra("routeId", 0);
                images.addAll(points
                        .stream()
                        .map(p -> new SliderFragment(getPointImage(p.getId())))
                        .collect(Collectors.toList()));
            } else {
                PointDTO point = points.get(0);
                List<Bitmap> bitmaps = getPointAllImages(point.getId());
                images.addAll(bitmaps
                        .stream()
                        .map(SliderFragment::new)
                        .collect(Collectors.toList()));
            }
        }

        fillSlider(images);

        return points;
    }

    private Point findNextPoint(Point originPoint) {
        double min = 100000.0;
        PointDTO nextPoint = null;
        for(PointDTO dto : pointsDto) {
            Point point = Point.fromLngLat(Objects.requireNonNull(dto).getLongitude(), dto.getLatitude());
            double currentDistance = TurfMeasurement.distance(originPoint, point);

            if (currentDistance < min) {
                min = currentDistance;
                nextPoint = dto;
            }
        }
        pointsDto.remove(nextPoint);
        if(pointsDto.isEmpty()) {
            nextPointButton.setText(getResources().getString(R.string.end_excursion));
        }

        createAudioPlayer(nextPoint.getId());

        return Point.fromLngLat(Objects.requireNonNull(nextPoint).getLongitude(), nextPoint.getLatitude());
    }

    private Bitmap getPointImage(long pointId) {
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointId));

        if(bytes != null) {
            String base64Photos = new String(bytes, StandardCharsets.UTF_8).split(";")[0];
            return getBitmapFromBytes(stringToByte(base64Photos));
        }

        return getBitmapFromDrawable(getApplicationContext(), R.drawable.location_test);
    }

    private List<Bitmap> getPointAllImages(long pointId) {
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointId));

        if(bytes != null) {
            String[] base64Photos = new String(bytes, StandardCharsets.UTF_8).split(";");
            return Arrays.stream(base64Photos)
                    .map(s -> getBitmapFromBytes(stringToByte(s)))
                    .collect(Collectors.toList());
        }

        return List.of(getBitmapFromDrawable(getApplicationContext(), R.drawable.location_test));
    }

    private void fillSlider(List<SliderFragment> fragments) {
        Log.i("MAP_SLIDER", "fillSlider: count fragments " + fragments.size());
        SliderAdapter adapter = new SliderAdapter(MapBoxActivity.this, fragments);
        slider.setSliderAdapter(adapter);

    }
    @SuppressLint("MissingPermission")
    private void fetchRoute() {
        showIndicator();
        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(MapBoxActivity.this);
        locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();

                List<Bearing> bearings = new ArrayList<>();
                bearings.add(Bearing.builder()
                        .angle(location.getBearing())
                        .degrees(45.0)
                        .build());

                bearings.add(null);

                Point origin = Point.fromLngLat(Objects.requireNonNull(location).getLongitude(), location.getLatitude());
                Point nextPoint = findNextPoint(origin);
                List<Point> currentPoints = new ArrayList<>(List.of(nextPoint, origin));

                RouteOptions.Builder builder = RouteOptions.builder()
                        .coordinatesList(currentPoints)
                        .alternatives(false)
                        .steps(true)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
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
                        Toast.makeText(MapBoxActivity.this, "Route request failed", Toast.LENGTH_SHORT).show();
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
        player.stop();
    }
}