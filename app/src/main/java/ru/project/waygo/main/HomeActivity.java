package ru.project.waygo.main;

import static ru.project.utils.CacheUtils.cacheFiles;
import static ru.project.utils.CacheUtils.getFileName;
import static ru.project.utils.IntentExtraUtils.getPointsExtra;
import static ru.project.utils.IntentExtraUtils.getRoutesExtra;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import ru.project.waygo.Constants;
import ru.project.waygo.R;
import ru.project.waygo.adapter.LocationAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.fragment.LocationFragment;
import ru.project.waygo.map.MapBoxGeneralActivity;
import ru.project.waygo.map.MapBoxView;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.CityService;
import ru.project.waygo.retrofit.services.PointService;
import ru.project.waygo.retrofit.services.RouteService;

public class HomeActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    private RecyclerView recyclerView;

    private RetrofitConfiguration retrofit;

    private TabLayout tabLayout;

    private ListView cityListView;
    private EditText locationSearch;

    private EditText citySearch;

    private ArrayAdapter<String> cityAdapter;
    private String cityCurrent;
    private List<LocationFragment> currentRoutes = new ArrayList<>();
    private boolean isExcursion = true;
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

        BottomNavigationView bottomNavigationView=findViewById(R.id.navigation_bar);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.action_main);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId())
            {
                case R.id.action_map:
                    Intent intent = new Intent(getApplicationContext(), MapBoxGeneralActivity.class);
                    intent.putExtra("points", getPointsExtra(getPointsDtoFromExcursion()));
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_main:
                    return true;
                case R.id.action_favorites:
                    return true;
                case R.id.action_account:
                    return true;
            }
            return false;
        });

        if(currentRoutes.isEmpty()) getExcursions();
        setListeners();
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
        LocationAdapter adapter = new LocationAdapter(HomeActivity.this, fragments);
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

    private List<PointDTO> getPointsDtoFromExcursion() {
        Set<PointDTO> points = new HashSet<>();

        currentRoutes.forEach(route -> points.addAll(route.getPoints()));
        return new ArrayList<>(points);
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
                    fillRecyclePoint(points.stream()
                            .map(point ->
                                    new LocationFragment(point,
                                    getRoutesExtra(getRoutesFragmentIncludePoint(point))))
                            .collect(Collectors.toList()));
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
                        cacheImages(generalPoint.getPhoto(), route.getId(), "route");
                    });
                    currentRoutes = routes.stream()
                            .map(route -> new LocationFragment(route, getPointsExtra(route.getStopsOnRoute())))
                            .collect(Collectors.toList());
                    fillRecyclePoint(currentRoutes);
                } else {
                    Log.i("POINT", "onResponse: " + "404 not found");
                }
            }

            @Override
            public void onFailure(Call<List<RouteDTO>> call, Throwable t) {

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
                else fillRecyclePoint(currentRoutes);
                break;
            }
            case 1: {
                isExcursion = false;
                List<LocationFragment> points = getPointsFromExcursion();
                if (points.isEmpty()) getPoints();
                else fillRecyclePoint(points);
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
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

}