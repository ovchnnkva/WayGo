package ru.project.waygo.details;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.utils.CacheUtils.getFileCache;
import static ru.project.utils.CacheUtils.getFileName;
import static ru.project.utils.IntentExtraUtils.getRoutesFromExtra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.adapter.RoutePhotosAdapter;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.fragment.RoutePhotosFragment;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.RouteService;

public class PointDetailsActivity extends BaseActivity {

    private RecyclerView container;

    private TextView namePointField;
    private TextView descriptionField;
    private ImageView generalImage;
    private ToggleButton favorite;
    private long pointId;
    private RetrofitConfiguration retrofit;
    private List<RouteDTO> routesWithPoint = new ArrayList<>();
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
        generalImage = findViewById(R.id.image_point);
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
            getRoutesByPointId(pointId);
        } else {
            fillRecycleFromCache();
        }
    }

    private void fillFromIntent() {
        Intent intent = getIntent();
        namePointField.setText(intent.getStringExtra("name"));
        descriptionField.setText(intent.getStringExtra("description"));
        pointId = intent.getLongExtra("id", 0);
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
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointId));

        if(bytes != null) { //TODO: добавить получение изображения напрямую после того как будут добавлены изображения к маршрутам
            String base64Photos = new String(bytes, StandardCharsets.UTF_8);
            generalImage.setImageBitmap(getBitmapFromBytes(stringToByte(base64Photos)));
        }
    }

    private void fillRecycle(List<RoutePhotosFragment> fragments) {
        Log.i("POINT_DETAILS", "fillRecycle: count fragments " + fragments.size());
        RoutePhotosAdapter adapter = new RoutePhotosAdapter(PointDetailsActivity.this, fragments);
        container.setAdapter(adapter);
    }
}