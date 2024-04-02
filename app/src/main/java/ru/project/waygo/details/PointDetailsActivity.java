package ru.project.waygo.details;

import static ru.project.utils.Base64Util.stringToByte;
import static ru.project.utils.BitMapUtils.getBitmapFromBytes;
import static ru.project.utils.CacheUtils.getFileCache;
import static ru.project.utils.CacheUtils.getFileName;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
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
import java.util.stream.Collectors;

import ru.project.waygo.R;
import ru.project.waygo.adapter.PointPhotosAdapter;
import ru.project.waygo.fragment.PointPhotosFragment;

public class PointDetailsActivity extends AppCompatActivity {

    private RecyclerView container;

    private TextView namePointField;
    private TextView descriptionField;
    private ImageView generalImage;

    private long pointId;
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
    }

    private void fillRecycleFromCache() {
        byte[] bytes = getFileCache(getApplicationContext(), getFileName("point", pointId));

        if(bytes != null) {
            List<String> base64Photos = Arrays.asList(new String(bytes, StandardCharsets.UTF_8).split(";"));
            List<PointPhotosFragment> fragments = base64Photos.stream().map(photo ->
                    new PointPhotosFragment(getBitmapFromBytes(stringToByte(photo))))
                    .collect(Collectors.toList());

            fillRecycle(fragments);
            if(!fragments.isEmpty()) {
                generalImage.setImageBitmap(fragments.get(0).getImage());
            }
        }
    }

    private void fillRecycle(List<PointPhotosFragment> fragments) {
        Log.i("POINT_DETAILS", "fillRecycle: count fragments " + fragments.size());
        PointPhotosAdapter adapter = new PointPhotosAdapter(PointDetailsActivity.this, fragments);
        container.setAdapter(adapter);
    }
}