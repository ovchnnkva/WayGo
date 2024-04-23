package ru.project.waygo.login;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.CITY_USER_AUTH_FILE;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;
import static ru.project.waygo.Constants.NAME_USER_AUTH_FILE;
import static ru.project.waygo.Constants.PASS_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.UID_USER_AUTH_FILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;
import java.util.Optional;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.dto.user.UserDTO;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.UserService;

public class LoginActivity extends BaseActivity {

    private MaterialButton loginButton;
    private MaterialButton registrationButton;
    private TextInputEditText passwordField;
    private TextInputEditText emailFields;
    private RetrofitConfiguration retrofit;

    private String email;
    private String password;
    private String uid;

    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        loginButton = findViewById(R.id.button_login);
        registrationButton = findViewById(R.id.button_registration);
        passwordField = findViewById(R.id.password_field);
        emailFields = findViewById(R.id.email_field);
        retrofit = new RetrofitConfiguration();

        addListeners();
    }

    private void logIn() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }

    private void addListeners() {
        registrationButton.setOnClickListener(e -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);

            Optional.ofNullable(passwordField.getText()).ifPresent(val -> {
                intent.putExtra("passwordField", val.toString());
            });

            Optional.ofNullable(emailFields.getText()).ifPresent(val -> {
                intent.putExtra("emailFields", val.toString());
            });

            this.startActivity(intent);
        });

        loginButton.setOnClickListener(e -> auth());
    }

    private void auth() {
        email = Objects.requireNonNullElse(emailFields.getText(), "").toString();
        password = Objects.requireNonNullElse(passwordField.getText(), "").toString();

        if(!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if(!password.isEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(e -> {
                            uid = e.getUser().getUid();
                            getUser();
                            logIn();
                        }).addOnFailureListener(e ->
                            Toast.makeText(LoginActivity.this, "Неправильный пароль или email", Toast.LENGTH_LONG).show()
                        );
            } else {
                Toast.makeText(LoginActivity.this, "Введите пароль", Toast.LENGTH_LONG).show();
            }
        } else if(email.isEmpty()){
            Toast.makeText(LoginActivity.this, "Email не заполнен", Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(LoginActivity.this, "Введен некорректный email",  Toast.LENGTH_LONG).show();
        }
    }

    private void getUser() {
        UserService service = retrofit.createService(UserService.class);
        Call<UserDTO> call = service.getByUid(uid);
        call.enqueue(new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                if(response.isSuccessful()) {
                    savePreferences(response.body());
                } else {
                    Log.d("USER_GET_UID", "onResponse: пользователь не найден");
                }
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {

            }
        });

    }
    private void savePreferences(UserDTO userDTO) {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PASS_FROM_AUTH_FILE, password);
        editor.putString(EMAIL_FROM_AUTH_FILE, email);
        editor.putString(UID_USER_AUTH_FILE, userDTO.getUid());
        editor.putString(ID_USER_AUTH_FILE, userDTO.getId() + "");
        editor.putString(NAME_USER_AUTH_FILE, userDTO.getName());
        editor.putString(CITY_USER_AUTH_FILE, getResources().getString(R.string.rostov));
        editor.apply();
    }
}