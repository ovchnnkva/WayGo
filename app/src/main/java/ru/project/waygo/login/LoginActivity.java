package ru.project.waygo.login;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.PASS_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.UID_USER_AUTH_FILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;
import java.util.Optional;

import ru.project.waygo.R;
import ru.project.waygo.main.HomeActivity;

public class LoginActivity extends AppCompatActivity {

    private MaterialButton loginButton;
    private MaterialButton registrationButton;
    private TextInputEditText passwordField;
    private TextInputEditText emailFields;

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

        addListeners();
    }

    private void logIn() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
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
                            savePreferences();
                            logIn();
                        }).addOnFailureListener(e ->
                            Toast.makeText(LoginActivity.this, "Неправильный пароль", Toast.LENGTH_LONG).show()
                        );
            } else {
                passwordField.setError("Введите пароль");
            }
        } else if(email.isEmpty()){
            emailFields.setError("Email не заполнен");
        } else{
            emailFields.setError("Введен некорректный email");
        }
    }

    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PASS_FROM_AUTH_FILE, password);
        editor.putString(EMAIL_FROM_AUTH_FILE, email);
        editor.putString(UID_USER_AUTH_FILE, uid);
        editor.apply();
    }
}