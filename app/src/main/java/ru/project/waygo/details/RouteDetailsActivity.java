package ru.project.waygo.details;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.utils.BitMapUtils.getBitmapFromDrawable;
import static ru.project.utils.CacheUtils.getFileCache;
import static ru.project.utils.CacheUtils.getFileName;
import static ru.project.utils.IntentExtraUtils.getPointsFromExtra;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarteist.autoimageslider.SliderView;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.fragment.SliderFragment;
import ru.project.waygo.adapter.PointAdapter;
import ru.project.waygo.adapter.SliderAdapter;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.fragment.PointFragment;
import ru.project.waygo.map.MapBoxActivity;

public class RouteDetailsActivity extends BaseActivity {
    private SliderView slider;
    private TextView name;
    private ToggleButton favorite;
    private TextView length;
    private TextView description;
    private RecyclerView container;
    private MaterialButton goToExcurssion;
    private String pointsExtra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_route_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        slider = findViewById(R.id.slider);

        slider.setAutoCycleDirection(SliderView.LAYOUT_DIRECTION_LTR);
        slider.setScrollTimeInSec(2);
        slider.setAutoCycle(true);
        slider.startAutoCycle();

        name = findViewById(R.id.name_route);
        description = findViewById(R.id.descriprion_route);
        favorite = findViewById(R.id.toggle_favorite);
        length = findViewById(R.id.route_length);
        goToExcurssion = findViewById(R.id.go_to_excursion);

        container = findViewById(R.id.points_container);
        container.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        container.setLayoutManager(linearLayoutManager);

        fillFromIntent();
        setListeners();
    }

    private void setListeners() {
        Context context = RouteDetailsActivity.this;
        Intent intent = new Intent(context, MapBoxActivity.class);
        goToExcurssion.setOnClickListener(e -> {
            intent.putExtra("name", name.getText().toString());
            intent.putExtra("description", description.getText().toString());
            intent.putExtra("points", pointsExtra);
            context.startActivity(intent);
        });
    }
    private void fillFromIntent() {
        Intent intent = getIntent();
        long routeId = intent.getLongExtra("id", 0);
        name.setText(intent.getStringExtra("name"));
        description.setText(intent.getStringExtra("description"));
        length.setText(intent.getStringExtra("length"));
        favorite.setChecked(intent.getBooleanExtra("favorite", false));
        pointsExtra = Objects.requireNonNullElse(intent.getStringExtra("points"), "");
        fillPoints(getPointsFromExtra(pointsExtra));
    }

    private void fillPoints(List<PointDTO> points) {
        List<PointFragment> fragments = points
                .stream()
                .map(point -> new PointFragment(point, getPointImage(point.getId())))
                .collect(Collectors.toList());

        fillRecycle(fragments);

        List<SliderFragment> images = fragments
                .stream()
                .map(fragment -> new SliderFragment(fragment.getImage()))
                .collect(Collectors.toList());

        fillSlider(images);
    }

    private void fillRecycle(List<PointFragment> fragments) {
        Log.i("ROUTE_DETAILS", "fillRecycle: count fragments " + fragments.size());
        PointAdapter adapter = new PointAdapter(RouteDetailsActivity.this, fragments);
        container.setAdapter(adapter);
    }

    private void fillSlider(List<SliderFragment> fragments) {
        Log.i("ROUTE_DETAILS", "fillSlider: count fragments " + fragments.size());
        SliderAdapter adapter = new SliderAdapter(RouteDetailsActivity.this, fragments);
        slider.setSliderAdapter(adapter);

    }

    private Bitmap getPointImage(long pointId) {
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointId));

        if(bytes != null) {
            String base64Photos = new String(bytes, StandardCharsets.UTF_8).split(";")[0];
            return getBitmapFromBytes(stringToByte(base64Photos));
        }

        return getBitmapFromDrawable(getApplicationContext(), R.drawable.location_test);
    }
}