package ru.project.waygo.login;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;

public class RegistrationActivity extends BaseActivity {
    private TextInputEditText password;
    private TextInputEditText email;

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

        password = findViewById(R.id.password_field);
        email = findViewById(R.id.email_field);

        fillFromIntent();
    }

    private void fillFromIntent() {
        Intent intent = getIntent();

        if(intent == null) return;

        password.setText(intent.getStringExtra("password"));
        email.setText(intent.getStringExtra("email"));
    }
}