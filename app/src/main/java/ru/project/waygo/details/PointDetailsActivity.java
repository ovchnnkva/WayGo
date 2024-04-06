package ru.project.waygo.details;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.utils.CacheUtils.getFileCache;
import static ru.project.utils.CacheUtils.getFileName;
import static ru.project.utils.IntentExtraUtils.getRoutesFromExtra;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.project.waygo.R;
import ru.project.waygo.adapter.PointPhotosAdapter;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.fragment.RoutePhotosFragment;

public class PointDetailsActivity extends AppCompatActivity {

    private RecyclerView container;

    private TextView namePointField;
    private TextView descriptionField;
    private ImageView generalImage;

    private long pointId;
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

        container = findViewById(R.id.photos_container);
        container.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        container.setLayoutManager(linearLayoutManager);

        container.setAdapter(new PointPhotosAdapter(PointDetailsActivity.this, new ArrayList<>()));
        fillFromIntent();
        fillRecycleFromCache();
    }

    private void fillFromIntent() {
        Intent intent = getIntent();
        namePointField.setText(intent.getStringExtra("name"));
        descriptionField.setText(intent.getStringExtra("description"));
        pointId = intent.getLongExtra("id", 0);
        routesWithPoint = getRoutesFromExtra(
                Objects.requireNonNullElse(intent.getStringExtra("routes"), "")
        );

        setPointImage();
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

        if(bytes != null) {
            String base64Photos = new String(bytes, StandardCharsets.UTF_8);
            generalImage.setImageBitmap(getBitmapFromBytes(stringToByte(base64Photos)));
        }
    }

    private void fillRecycle(List<RoutePhotosFragment> fragments) {
        Log.i("POINT_DETAILS", "fillRecycle: count fragments " + fragments.size());
        PointPhotosAdapter adapter = new PointPhotosAdapter(PointDetailsActivity.this, fragments);
        container.setAdapter(adapter);
    }
}