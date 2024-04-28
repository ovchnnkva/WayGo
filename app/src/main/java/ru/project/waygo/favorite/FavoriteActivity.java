package ru.project.waygo.favorite;

import static ru.project.waygo.utils.CacheUtils.cacheFiles;
import static ru.project.waygo.utils.CacheUtils.getFileName;
import static ru.project.waygo.utils.IntentExtraUtils.getPointsExtra;
import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.user_profile.UserProfileActivity;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.adapter.LocationAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.fragment.LocationFragment;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.map.MapBoxGeneralActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.RouteService;
import ru.project.waygo.retrofit.services.UserService;

public class FavoriteActivity extends BaseActivity implements TabLayout.OnTabSelectedListener{
    private RecyclerView recyclerView;

    private RetrofitConfiguration retrofit;

    private TabLayout tabLayout;
    private List<LocationFragment> currentRoutes = new ArrayList<>();
    private List<LocationFragment> currentPoint = new ArrayList<>();
    private ConstraintLayout emptyLayout;
    private MaterialButton goHomeButton;
    private ProgressBar loader;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorite);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        recyclerView = findViewById(R.id.location_container);
        tabLayout = findViewById(R.id.tab_layout);
        emptyLayout = findViewById(R.id.empty_layout);
        goHomeButton = findViewById(R.id.go_home);
        loader = findViewById(R.id.loading);

        tabLayout.setOnTabSelectedListener(this);

        retrofit = new RetrofitConfiguration();

        BottomNavigationView bottomNavigationView=findViewById(R.id.navigation_bar);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.action_favorites);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId())
            {
                case R.id.action_map:
                    if(checkPermissions()) {
                        startActivity(new Intent(getApplicationContext(), MapBoxGeneralActivity.class));
                        overridePendingTransition(0, 0);
                        finish();
                        return true;
                    } else {
                        launchPermissions();
                    }
                    return  false;
                case R.id.action_main:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_favorites:
                    return true;
                case R.id.action_account:
                    startActivity(new Intent(getApplicationContext(), UserProfileActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
            }
            return false;
        });

        getRouteFavorites();
        goHomeButton.setOnClickListener(view -> bottomNavigationView.setSelectedItemId(R.id.action_main));
    }
    private boolean checkPermissions() {
        boolean notificationPermission = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission = (ActivityCompat.checkSelfPermission(FavoriteActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        }
        return notificationPermission
                && (ActivityCompat.checkSelfPermission(FavoriteActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(FavoriteActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

    }

    private void launchPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        activityResultLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    private long getUserId() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return Long.parseLong(preferences.getString(ID_USER_AUTH_FILE, ""));
    }

    private void getRouteFavorites() {
        UserService service = retrofit.createService(UserService.class);

        Call<Set<RouteDTO>> call = service.getFavoriteRoutes(getUserId());
        showIndicator();
        call.enqueue(new Callback<Set<RouteDTO>>() {
            @Override
            public void onResponse(Call<Set<RouteDTO>> call, Response<Set<RouteDTO>> response) {
                if(response.isSuccessful()) {
                    Set<RouteDTO> routes = response.body();
                    response.body().forEach(route -> {
                        route.getStopsOnRoute()
                                .forEach(point -> cacheImages(point.getPhoto(), point.getId(), "point"));

                        PointDTO generalPoint = route.getStopsOnRoute().get(0);
                        cacheImages(generalPoint.getPhoto(), route.getId(), "route");
                    });
                    currentRoutes = routes.stream()
                            .map(route -> new LocationFragment(route, getPointsExtra(route.getStopsOnRoute())))
                            .collect(Collectors.toList());
                    fillRecyclePoint(currentRoutes);
                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                }
                hideIndicator();
            }

            @Override
            public void onFailure(Call<Set<RouteDTO>> call, Throwable t) {
                hideIndicator();
            }
        });
    }

    private void getPointsFavorite() {
        UserService service = retrofit.createService(UserService.class);

        Call<Set<PointDTO>> call = service.getFavoritePoints(getUserId());
        showIndicator();
        call.enqueue(new Callback<Set<PointDTO>>() {
            @Override
            public void onResponse(Call<Set<PointDTO>> call, Response<Set<PointDTO>> response) {
                if(response.isSuccessful()) {
                    Set<PointDTO> points = response.body();
                    response.body().forEach(point -> cacheImages(point.getPhoto(), point.getId(), "point"));

                    Set<RouteDTO> routes = new HashSet<>();
                    points.forEach(point -> getRouteByPoint(point.getId(), routes));

                    currentPoint = points.stream()
                            .map(point ->
                                    new LocationFragment(point,
                                            ""))
                            .collect(Collectors.toList());
                    hideIndicator();
                    fillRecyclePoint(currentPoint);
                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                    hideIndicator();
                }
            }

            @Override
            public void onFailure(Call<Set<PointDTO>> call, Throwable t) {

            }
        });
    }


    private void getRouteByPoint(long id, Set<RouteDTO> pointRoutes) {
        RouteService service = retrofit.createService(RouteService.class);
        Call<List<RouteDTO>> routes = service.getRoutesByPointId(id);
        showIndicator();
        routes.enqueue(new Callback<List<RouteDTO>>() {
            @Override
            public void onResponse(Call<List<RouteDTO>> call, Response<List<RouteDTO>> response) {
                if(response.isSuccessful()) {
                    pointRoutes.addAll(response.body());
                }
                hideIndicator();
            }

            @Override
            public void onFailure(Call<List<RouteDTO>> call, Throwable t) {
                hideIndicator();
            }
        });
    }
    private void cacheImages(List<String> image, long id, String type) {
        cacheFiles(FavoriteActivity.this, getFileName(type, id), image);
    }
    private void fillRecyclePoint(List<LocationFragment> fragments) {
        if (fragments.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            emptyLayout.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            fragments.forEach(f -> f.setFavorite(true));
            LocationAdapter adapter = new LocationAdapter(FavoriteActivity.this, fragments, getUserId());
            recyclerView.setAdapter(adapter);
        }
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 0: {
                recyclerView.setVisibility(View.INVISIBLE);
                if(currentRoutes.isEmpty() || needReload(currentRoutes)) getRouteFavorites();
                else fillRecyclePoint(currentRoutes);
                break;
            }
            case 1: {
                recyclerView.setVisibility(View.INVISIBLE);
                if (currentPoint.isEmpty() || needReload(currentPoint)) getPointsFavorite();
                else fillRecyclePoint(currentPoint);
                break;
            }
        }
    }

    private boolean needReload(List<LocationFragment> fragments) {
        return fragments.stream().anyMatch(LocationFragment::isNeedReload);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    private void setEnableTab(boolean disable) {
        for(int i =0; i < 2; i++) {
            tabLayout.getTabAt(i).view.setEnabled(disable);
        }
    }

    protected void showIndicator() {
        loader.setVisibility(View.VISIBLE);
        setEnableTab(false);
    }

    protected void hideIndicator() {
        loader.setVisibility(View.INVISIBLE);
        setEnableTab(true);
    }
}