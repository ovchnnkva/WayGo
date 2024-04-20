package ru.project.waygo.main;

import static ru.project.waygo.utils.CacheUtils.cacheFiles;
import static ru.project.waygo.utils.CacheUtils.getFileName;
import static ru.project.waygo.utils.IntentExtraUtils.getPointsExtra;
import static ru.project.waygo.utils.IntentExtraUtils.getRoutesExtra;
import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.CITY_USER_AUTH_FILE;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    private EditText citySearch;

    private ArrayAdapter<String> cityAdapter;
    private String cityCurrent;
    private List<LocationFragment> currentRoutes = new ArrayList<>();

    private List<LocationFragment> currentPoints = new ArrayList<>();
    private boolean isExcursion = true;
    private long userId;
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
        tabLayout.setOnTabSelectedListener(this);
        locationSearch = findViewById(R.id.edit_search_location);
        cityListView = findViewById(R.id.city_container);
        citySearch = findViewById(R.id.search_country);

        retrofit = new RetrofitConfiguration();

        cityCurrent = citySearch.getText() != null
                ? citySearch.getText().toString()
                : "";

        savePreferences();

        BottomNavigationView bottomNavigationView=findViewById(R.id.navigation_bar);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.action_main);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId())
            {
                case R.id.action_map:
                    startActivity(new Intent(getApplicationContext(), MapBoxGeneralActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
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

        if(currentRoutes.isEmpty()) getExcursions();
        setListeners();
    }

    private long getUserId() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return Long.parseLong(preferences.getString(ID_USER_AUTH_FILE, ""));
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
                        : getPointsFromExcursion();

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

        citySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getCities(citySearch.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        citySearch.setOnClickListener(e -> getCities(citySearch.getText().toString()));
        citySearch.setOnFocusChangeListener((view, hasFocus) -> {
                if(!hasFocus) citySearch.setText(cityCurrent);
        });
        cityListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                cityCurrent = (String) adapterView.getItemAtPosition(i);
                citySearch.setText(cityCurrent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private List<LocationFragment> filterLocation(List<LocationFragment> locationFragments) {
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
        LocationAdapter adapter = new LocationAdapter(HomeActivity.this, fragments, getUserId());
        recyclerView.setAdapter(adapter);
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
        String cityName = citySearch.getText() != null
                          ? citySearch.getText().toString()
                          : "";
        Call<List<PointDTO>> call = pointService.getByCity(cityName);
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
                    getFavoritesPointsIds(currentPoints);

                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PointDTO>> call, @NonNull Throwable t) {
                Log.i("POINT", "onFailure " + t.getLocalizedMessage());
            }
        });
    }

    private void getFavoritesPointsIds(List<LocationFragment> points) {
        UserService service = retrofit.createService(UserService.class);
        Call<List<Long>> call = service.getFavoritePointsIds(getUserId());
        call.enqueue(new Callback<List<Long>>() {
            @Override
            public void onResponse(Call<List<Long>> call, Response<List<Long>> response) {
                if(response.isSuccessful()) {
                    points.stream()
                            .filter(p -> response.body().contains(p.getId()))
                            .forEach(p -> p.setFavorite(true));
                }

                fillRecyclePoint(currentPoints);
            }

            @Override
            public void onFailure(Call<List<Long>> call, Throwable t) {

            }
        });
    }

    private void getExcursions() {
        RouteService service = retrofit.createService(RouteService.class);
        String cityName = citySearch.getText() != null
                ? citySearch.getText().toString()
                : "";
        Call<List<RouteDTO>> call = service.getByCityName(cityName);
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
                    getFavoritesRoutesIds(currentRoutes);

                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                }
            }

            @Override
            public void onFailure(Call<List<RouteDTO>> call, Throwable t) {

            }
        });
    }

    private void getFavoritesRoutesIds(List<LocationFragment> routes) {
        UserService service = retrofit.createService(UserService.class);
        Call<List<Long>> call = service.getFavoriteRoutesIds(getUserId());
        call.enqueue(new Callback<List<Long>>() {
            @Override
            public void onResponse(Call<List<Long>> call, Response<List<Long>> response) {
                if(response.isSuccessful()) {
                    routes.stream()
                            .filter(p -> response.body().contains(p.getId()))
                            .forEach(p -> p.setFavorite(true));
                }
                fillRecyclePoint(currentRoutes);
            }

            @Override
            public void onFailure(Call<List<Long>> call, Throwable t) {

            }
        });
    }
    private void getCities(String name) {
        CityService service = retrofit.createService(CityService.class);
        Call<List<String>> call = service.getByName(name);

        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if(response.isSuccessful()) {
                    fillCityContainer(response.body());
                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {

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
        cityAdapter = new ArrayAdapter<>(this, R.layout.fragment_city_name, R.id.product_name, cities);
        cityListView.setAdapter(cityAdapter);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 0: {
                isExcursion = true;
                if(currentRoutes.isEmpty()) getExcursions();
                else getFavoritesRoutesIds(currentRoutes);
                break;
            }
            case 1: {
                isExcursion = false;
                currentPoints = getPointsFromExcursion();
                if (currentPoints.isEmpty()) getPoints();
                else getFavoritesPointsIds(currentPoints);

                break;
            }
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
}