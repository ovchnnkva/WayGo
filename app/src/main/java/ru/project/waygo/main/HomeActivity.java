package ru.project.waygo.main;

import static ru.project.waygo.utils.CacheUtils.cacheFiles;
import static ru.project.waygo.utils.CacheUtils.getFileName;
import static ru.project.waygo.utils.IntentExtraUtils.getPointsExtra;
import static ru.project.waygo.utils.IntentExtraUtils.getRoutesExtra;
import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.CITY_USER_AUTH_FILE;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import ru.project.waygo.dto.CityDto;
import ru.project.waygo.user_profile.UserProfileActivity;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.adapter.LocationAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.favorite.FavoriteActivity;
import ru.project.waygo.fragment.LocationFragment;
import ru.project.waygo.map.MapBoxGeneralActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.CityService;
import ru.project.waygo.retrofit.services.PointService;
import ru.project.waygo.retrofit.services.RouteService;
import ru.project.waygo.retrofit.services.UserService;

public class HomeActivity extends BaseActivity implements TabLayout.OnTabSelectedListener {

    private RecyclerView recyclerView;

    private RetrofitConfiguration retrofit;

    private TabLayout tabLayout;

    private ListView cityListView;
    private EditText locationSearch;

    private MaterialButton cityButton;

    private ConstraintLayout homeLayout;
    private ConstraintLayout citySearchLayout;

    private EditText searchCity;

    private ArrayAdapter<String> cityAdapter;
    private String cityCurrent;
    private List<LocationFragment> currentRoutes = new ArrayList<>();

    private List<LocationFragment> currentPoints = new ArrayList<>();
    private boolean isExcursion = true;
    private ConstraintLayout emptyLayout;
    private ProgressBar loader;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.location_container);
        tabLayout = findViewById(R.id.tab_layout);
        emptyLayout = findViewById(R.id.empty_layout);
        tabLayout.setOnTabSelectedListener(this);
        locationSearch = findViewById(R.id.edit_search_location);
        cityListView = findViewById(R.id.city_container);
        cityButton = findViewById(R.id.search_country_button);
        homeLayout = findViewById(R.id.home_layout);
        citySearchLayout = findViewById(R.id.search_city_layout);
        searchCity = findViewById(R.id.search_city);
        loader = findViewById(R.id.loading);

        retrofit = new RetrofitConfiguration();

        cityCurrent = !getCityCurrent().isEmpty()
                ? getCityCurrent()
                : getResources().getString(R.string.rostov);
        cityButton.setText(cityCurrent);

        savePreferences();

        BottomNavigationView bottomNavigationView=findViewById(R.id.navigation_bar);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.action_main);

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

        if(currentRoutes.isEmpty()) {
            showIndicator();
            getExcursions();
        }
        setListeners();
    }

    private boolean checkPermissions() {
        boolean notificationPermission = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission = (ActivityCompat.checkSelfPermission(HomeActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        }
        return notificationPermission
                && (ActivityCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

    }

    private void launchPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }

        ActivityCompat.requestPermissions(
                this,
                permissions,
                1
        );
    }
    private String getCityCurrent() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return preferences.getString(CITY_USER_AUTH_FILE, "");
    }

    private long getUserId() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        String userId = preferences.getString(ID_USER_AUTH_FILE, "");
        return !userId.isEmpty() ? Long.parseLong(userId) : 1L;
    }

    private void setListeners() {
        locationSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                List<LocationFragment> locationFragments = isExcursion
                        ? currentRoutes
                        : currentPoints;

                if (locationSearch.getText() == null || locationSearch.getText().toString().isBlank()) {
                    fillRecyclePoint(locationFragments);
                } else {
                    fillRecyclePoint(filterLocation(locationFragments));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        searchCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getCities(searchCity.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        cityButton.setOnClickListener(e -> {
            homeLayout.setVisibility(View.INVISIBLE);
            hideIndicator();
            citySearchLayout.setVisibility(View.VISIBLE);
            searchCity.setText(cityCurrent);
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            cityCurrent = (String) adapterView.getItemAtPosition(i);
            cityButton.setText(cityCurrent);
            saveCityPreferences();

            citySearchLayout.setVisibility(View.INVISIBLE);
            fillCityContainer(new ArrayList<>());
            fillRecyclePoint(new ArrayList<>());
            homeLayout.setVisibility(View.VISIBLE);

            showIndicator();
            if(isExcursion) getExcursions();
            else getPoints();
        });
    }

    private void saveCityPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CITY_USER_AUTH_FILE, cityCurrent);
        editor.apply();
    }
    private List<LocationFragment> filterLocation(List<LocationFragment> locationFragments) {
        if (locationSearch.getText() == null || locationSearch.getText().toString().isBlank()) return locationFragments;
        return locationFragments
                .stream()
                .filter(location -> location.getName()
                        .toLowerCase()
                        .contains(locationSearch.getText()
                                .toString()
                                .toLowerCase()))
                .collect(Collectors.toList());
    }

    private void fillRecyclePoint(List<LocationFragment> fragments) {
        emptyLayout.setVisibility(View.INVISIBLE);
        LocationAdapter adapter = new LocationAdapter(HomeActivity.this, fragments, getUserId());
        recyclerView.setAdapter(adapter);
        cityListView.setVisibility(View.VISIBLE);
    }

    private List<LocationFragment> getPointsFromExcursion() {
        Set<PointDTO> points = new HashSet<>();

        currentRoutes.forEach(route -> points.addAll(route.getPoints()));
        return points.stream()
                .map(point ->
                        new LocationFragment(point,
                        getRoutesExtra(getRoutesFragmentIncludePoint(point))))
                .collect(Collectors.toList());
    }

    private void getPoints() {
        PointService pointService = retrofit.createService(PointService.class);

        Call<List<PointDTO>> call = pointService.getByCity(cityCurrent);
        call.enqueue(new Callback<List<PointDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<PointDTO>> call, @NonNull Response<List<PointDTO>> response) {
                if(response.isSuccessful()) {
                    List<PointDTO> points =response.body();
                    response.body().forEach(point -> cacheImages(point.getPhoto(), point.getId(), "point"));

                    currentPoints = points.stream()
                            .map(point ->
                                    new LocationFragment(point,
                                            getRoutesExtra(getRoutesFragmentIncludePoint(point))))
                            .collect(Collectors.toList());
                    getFavoritesPointsIds();

                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                    hideIndicator();
                    emptyLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PointDTO>> call, @NonNull Throwable t) {
                Log.i("POINT", "onFailure " + t.getLocalizedMessage());
                hideIndicator();
            }
        });
    }

    private void getFavoritesPointsIds() {
        UserService service = retrofit.createService(UserService.class);
        Call<List<Long>> call = service.getFavoritePointsIds(getUserId());
        call.enqueue(new Callback<List<Long>>() {
            @Override
            public void onResponse(Call<List<Long>> call, Response<List<Long>> response) {
                if(response.isSuccessful()) {
                    currentPoints.stream()
                            .filter(p -> response.body().contains(p.getLocationId()))
                            .forEach(p -> p.setFavorite(true));
                }
                hideIndicator();
                fillRecyclePoint(filterLocation(currentPoints));
            }

            @Override
            public void onFailure(Call<List<Long>> call, Throwable t) {
                hideIndicator();
            }
        });
    }

    private void getExcursions() {
        RouteService service = retrofit.createService(RouteService.class);

        Call<List<RouteDTO>> call = service.getByCityName(cityCurrent);
        call.enqueue(new Callback<List<RouteDTO>>() {
            @Override
            public void onResponse(Call<List<RouteDTO>> call, Response<List<RouteDTO>> response) {
                if(response.isSuccessful()) {
                    List<RouteDTO> routes =response.body();
                    response.body().forEach(route -> {
                        route.getStopsOnRoute()
                             .forEach(point -> cacheImages(point.getPhoto(), point.getId(), "point"));

                        PointDTO generalPoint = route.getStopsOnRoute().get(0);
                        cacheImages(generalPoint.getPhoto().get(0), route.getId(), "route");
                    });
                    currentRoutes = routes.stream()
                            .map(route -> new LocationFragment(route, getPointsExtra(route.getStopsOnRoute())))
                            .collect(Collectors.toList());
                    getFavoritesRoutesIds();

                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                    hideIndicator();
                    emptyLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<RouteDTO>> call, Throwable t) {
                hideIndicator();
                Toast.makeText(HomeActivity.this, "Ошибка соединения", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getFavoritesRoutesIds() {
        UserService service = retrofit.createService(UserService.class);
        Call<List<Long>> call = service.getFavoriteRoutesIds(getUserId());
        call.enqueue(new Callback<List<Long>>() {
            @Override
            public void onResponse(Call<List<Long>> call, Response<List<Long>> response) {
                if(response.isSuccessful()) {
                    currentRoutes.stream()
                            .filter(p -> response.body().contains(p.getLocationId()))
                            .forEach(p -> p.setFavorite(true));
                }
                hideIndicator();
                fillRecyclePoint(filterLocation(currentRoutes));
            }

            @Override
            public void onFailure(Call<List<Long>> call, Throwable t) {
                hideIndicator();
                Toast.makeText(HomeActivity.this, "Ошибка соединения", Toast.LENGTH_LONG).show();
            }
        });
    }
    private void getCities(String name) {
        CityService service = retrofit.createService(CityService.class);
        Call<List<CityDto>> call = service.getByName(name);
        call.enqueue(new Callback<List<CityDto>>() {
            @Override
            public void onResponse(Call<List<CityDto>> call, Response<List<CityDto>> response) {
                if(response.isSuccessful()) {
                    fillCityContainer(response.body().stream().map(c -> c.getCity()).collect(Collectors.toList()));
                } else {
                    Log.i("CITIES", "onResponse: " + "404 not found");
                }
            }

            @Override
            public void onFailure(Call<List<CityDto>> call, Throwable t) {
                hideIndicator();
                Toast.makeText(HomeActivity.this, "Ошибка соединения", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void cacheImages(String image, long id, String type) {
        cacheFiles(HomeActivity.this, getFileName(type, id), image);
    }
    private void cacheImages(List<String> image, long id, String type) {
        cacheFiles(HomeActivity.this, getFileName(type, id), image);
    }
    private void fillCityContainer(List<String> cities) {
        Log.i("CITIES", "fillCityContainer: size " + cities.size());
        Log.i("CITIES", "visivle " + (cityListView.getVisibility() == View.VISIBLE));
        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cities);
        cityListView.setAdapter(cityAdapter);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 0: {
                fillRecyclePoint(new ArrayList<>());
                isExcursion = true;
                showIndicator();
                if(currentRoutes.isEmpty()) getExcursions();
                else getFavoritesRoutesIds();
                break;
            }
            case 1: {
                fillRecyclePoint(new ArrayList<>());
                isExcursion = false;
                showIndicator();
                if (currentPoints.isEmpty()) getPoints();
                else getFavoritesPointsIds();
                break;
            }
        }
    }

    private void setEnableTab(boolean disable) {
        for(int i =0; i < 2; i++) {
            tabLayout.getTabAt(i).view.setEnabled(disable);
        }
    }

    private List<RouteDTO> getRoutesFragmentIncludePoint(PointDTO pointDTO) {
        return currentRoutes.stream()
                .filter(route -> route.getPoints().contains(pointDTO))
                .map(RouteDTO::new)
                .collect(Collectors.toList());
    }

    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CITY_USER_AUTH_FILE, cityCurrent);
        editor.apply();
    }
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

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