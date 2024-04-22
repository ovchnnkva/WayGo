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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.dto.user.UserDTO;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.retrofit.RetrofitConfiguration;
import ru.project.waygo.retrofit.services.UserService;

public class RegistrationActivity extends BaseActivity {
    private TextInputEditText passwordField;
    private TextInputEditText passwortRepeatField;
    private TextInputEditText emailField;
    private TextInputEditText nameField;
    private MaterialButton registrationButton;
    private RetrofitConfiguration retrofit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        passwordField = findViewById(R.id.password_field);
        passwortRepeatField = findViewById(R.id.repeat_password_field);
        emailField = findViewById(R.id.email_field);
        nameField = findViewById(R.id.name_field);
        registrationButton = findViewById(R.id.button_register);
        retrofit = new RetrofitConfiguration();

        registrationButton.setOnClickListener(view -> {
            if (validation())
                registrationInFirebase();
        });

        fillFromIntent();
    }

    private void fillFromIntent() {
        Intent intent = getIntent();

        if(intent == null) return;

        passwordField.setText(intent.getStringExtra("passwordField"));
        emailField.setText(intent.getStringExtra("emailField"));
    }
    private boolean validation(){

        return (nameField.getText() != null)
                && (emailField.getText() != null)
                && checkValidPass()
                && checkRepeatPassword();
    }
    private boolean checkValidPass(){
        String pass = Objects.requireNonNull(passwordField.getText())
                .toString()
                .replaceAll(" ","");

        Pattern patternDegits = Pattern.compile("[1-9]");

        if((passwordField.getText() != null) && (pass.length() > 8) && (patternDegits.matcher(pass).find())) {
            return true;
        } else {
            passwordField.setError("Пароль должен быть длинее 8-ми символов и содержать хотя бы одну цифру");
        }
        return false;
    }

    private boolean checkRepeatPassword() {
        String password = Objects.requireNonNullElse(passwordField.getText(), "").toString();
        String passwordRepeat = Objects.requireNonNull(passwortRepeatField.getText(), "").toString();

        if(!password.isBlank() && !passwordRepeat.isBlank() && password.equals(passwordRepeat)) {
            return true;
        } else {
            passwortRepeatField.setError("Пароли не совпадают");
        }
        return false;
    }

    private void registrationInFirebase(){
        String mail = emailField.getText().toString();
        String pass = passwordField.getText().toString();

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(mail, pass)
                .addOnSuccessListener(event ->{
                    Toast.makeText(this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                    saveUser(UserDTO.builder()
                            .name(Objects.requireNonNull(nameField.getText()).toString())
                            .email(emailField.getText().toString())
                            .uid(event.getUser().getUid())
                            .build());
                })
                .addOnFailureListener(event -> Toast.makeText(this, "Введены некорректные данные " + event.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveUser(UserDTO userDto) {
        UserService service = retrofit.createService(UserService.class);
        Call<Void> call = service.createUser(userDto);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()) {
                    Log.d("SAVE_USER", "onResponse: " + "user save with uid " + userDto.getUid());
                    getUser(userDto.getUid());
                } else {
                    Log.e("SAVE_USER", "onResponse: " + "user save failure ");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    private void getUser(String uid) {
        UserService service = retrofit.createService(UserService.class);
        Call<UserDTO> call = service.getByUid(uid);
        call.enqueue(new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                if(response.isSuccessful()) {
                    savePreferences(response.body().getId(), uid);
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                } else {
                    Log.d("USER_GET_UID", "onResponse: пользователь не найден");
                }
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {

            }
        });

    }
    private void savePreferences(long id, String uid) {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PASS_FROM_AUTH_FILE, passwordField.getText().toString());
        editor.putString(EMAIL_FROM_AUTH_FILE, emailField.getText().toString());
        editor.putString(UID_USER_AUTH_FILE, uid);
        editor.putString(ID_USER_AUTH_FILE, id + "");
        editor.putString(NAME_USER_AUTH_FILE, nameField.getText().toString());
        editor.putString(CITY_USER_AUTH_FILE, getResources().getString(R.string.moscow));
        editor.apply();
    }
}