package ru.project.waygo.main;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.PASS_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.UID_USER_AUTH_FILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.login.LoginActivity;
import ru.project.waygo.login.RegistrationActivity;

public class MainActivity extends BaseActivity {
    private MaterialButton loginButton;
    private MaterialButton registrationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (isAuthorizedUser()) {
            Log.i("AUTH", "isAuthorizedUser: " + true);
            logIn();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.button_login);
        registrationButton = findViewById(R.id.button_registration);

        addListeners();
    }

    private void addListeners() {
        loginButton.setOnClickListener(e ->
            this.startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        registrationButton.setOnClickListener(e ->
            this.startActivity(new Intent(MainActivity.this, RegistrationActivity.class)));
    }
    private boolean isAuthorizedUser() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return preferences.contains(EMAIL_FROM_AUTH_FILE);
    }

    private void logIn() {
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
    }
}