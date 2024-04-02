package ru.project.waygo.main;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.CacheUtils.cacheFiles;
import static ru.project.utils.CacheUtils.getFileName;
import static ru.project.utils.CacheUtils.isExistsCache;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.Constants;
import ru.project.waygo.R;
import ru.project.waygo.adapter.LocationAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.fragment.LocationFragment;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.PointService;

public class HomeActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    private RecyclerView recyclerView;

    private RetrofitConfiguration retrofit;

    private TabLayout tabLayout;

    private ListView cityListView;
    private EditText locationSearch;

    private List<LocationFragment> locationFragments;
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

        retrofit = new RetrofitConfiguration();

        fillRecycleRoute();
        setListeners();
    }

    private void setListeners() {
        locationSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (locationSearch.getText() == null || locationSearch.getText().toString().isBlank()) {
                    fillRecyclePoint(locationFragments);
                } else {
                        fillRecyclePoint(locationFragments
                                .stream()
                                .filter(location -> location.getName()
                                        .toLowerCase()
                                        .contains(locationSearch.getText()
                                                .toString()
                                                .toLowerCase()))
                                .collect(Collectors.toList()));
                    }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void fillRecycleRoute() {
        locationFragments = getFragments();
        LocationAdapter adapter = new LocationAdapter(HomeActivity.this, locationFragments);
        recyclerView.setAdapter(adapter);
    }

    private void fillRecyclePoint(List<LocationFragment> fragments) {
        LocationAdapter adapter = new LocationAdapter(HomeActivity.this, fragments);
        recyclerView.setAdapter(adapter);
    }

    private List<LocationFragment> getFragments() {
        List<LocationFragment> fragments = new ArrayList<>();

        LocationFragment fragment = new LocationFragment();
        fragment.setName("Сталинские высотки");
        fragment.setDescription("Семь высотных зданий, строившихся в Москве в 1947—1957 годах.");
        fragment.setFavorite(true);
        fragment.setImage(List.of(BitmapFactory.decodeResource(getResources(), R.drawable.location_test)));
        fragment.setTypeLocation(Constants.TypeLocation.ROUTE);
        fragment.setRouteLength("3,2");

        fragments.add(fragment);

        LocationFragment fragment2 = new LocationFragment();
        fragment2.setName("Сталинские высотки");
        fragment2.setDescription("Семь высотных зданий, строившихся в Москве в 1947—1957 годах.");
        fragment2.setFavorite(false);
        fragment2.setImage(List.of(BitmapFactory.decodeResource(getResources(), R.drawable.location_test)));
        fragment2.setTypeLocation(Constants.TypeLocation.ROUTE);
        fragment2.setRouteLength("3,2");

        fragments.add(fragment2);

        return fragments;
    }

    private List<LocationFragment> convertToPointFragments(List<PointDTO> points) {
        return points.stream()
               .map(LocationFragment::new)
               .collect(Collectors.toList());
    }
    private void getPoints() {
        List<PointDTO> points = new ArrayList<>();
        PointService pointService = retrofit.createService(PointService.class);
        Call<List<PointDTO>> call = pointService.getByCity("Ростов-на-Дону");
        call.enqueue(new Callback<List<PointDTO>>() {
            @Override
            public void onResponse(@NonNull Call<List<PointDTO>> call, @NonNull Response<List<PointDTO>> response) {
                if(response.isSuccessful()) {
                    points.addAll(response.body());
                    points.forEach(point -> cachePointImages(point.getPhotos(), point.getId()));
                    locationFragments = convertToPointFragments(points);
                    fillRecyclePoint(locationFragments);
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

    private void cachePointImages(List<String> images, long pointId) {
        cacheFiles(HomeActivity.this, getFileName("point", pointId), images);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 0: {
                fillRecycleRoute();
                break;
            }
            case 1: {
                getPoints();
                break;
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        recyclerView.setAdapter(new LocationAdapter(HomeActivity.this, new ArrayList<>()));
    }
}