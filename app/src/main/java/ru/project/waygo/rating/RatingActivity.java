package ru.project.waygo.rating;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.R;
import ru.project.waygo.dto.route.RouteGradeDTO;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.UserService;

public class RatingActivity extends AppCompatActivity {

    private RetrofitConfiguration retrofit;
    private RatingBar ratingBar;
    private MaterialButton closeButton;
    private TextView nameExcursion;
    private String nameString = "Экскурсия \"%s\" завершена";
    private long routeId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rating);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        retrofit = new RetrofitConfiguration();

        ratingBar = findViewById(R.id.ratingBar);
        nameExcursion = findViewById(R.id.name_excursion);
        ratingBar.setMax(5);
        ratingBar.setStepSize(1);
        closeButton = findViewById(R.id.close_button);

        fillFromIntent();
        setListeners();
    }

    private void setListeners() {
        closeButton.setOnClickListener(view -> saveRating());
    }

    private void saveRating() {
        UserService service = retrofit.createService(UserService.class);
        Call<Void> call = service.createRateRoute(getUserId(), routeId, new RouteGradeDTO((int)ratingBar.getRating()));
        Context context = RatingActivity.this;
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()){
                    startActivity(new Intent(context, HomeActivity.class));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }

    private void fillFromIntent() {
        Intent intent = getIntent();
        routeId = intent.getLongExtra("routeId", 0);
        nameExcursion.setText(String.format(nameString, intent.getStringExtra("name")));
    }

    private long getUserId() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return Long.parseLong(preferences.getString(ID_USER_AUTH_FILE, ""));
    }

}