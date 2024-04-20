package ru.project.user_profile;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;
import static ru.project.waygo.Constants.NAME_USER_AUTH_FILE;
import static ru.project.waygo.Constants.PASS_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.UID_USER_AUTH_FILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ru.project.waygo.R;
import ru.project.waygo.dto.user.UserDTO;
import ru.project.waygo.main.MainActivity;

public class UserProfileActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private ConstraintLayout accountLayout;
    private ConstraintLayout feedbackLayout;
    private ConstraintLayout subscribeLayout;
    private TextInputEditText nameField;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private TabLayout tabLayout;

    private MaterialButton signOutButton;
    private MaterialButton saveChanges;
    private UserDTO userDTO;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        accountLayout = findViewById(R.id.account_layout);
        feedbackLayout = findViewById(R.id.feedback_layout);
        subscribeLayout = findViewById(R.id.subscribe_layout);
        nameField = findViewById(R.id.name_field);
        emailField = findViewById(R.id.email_field);
        passwordField = findViewById(R.id.password_field);
        signOutButton = findViewById(R.id.button_sign_out);
        saveChanges = findViewById(R.id.button_save);
        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setOnTabSelectedListener(this);

        fillFromPreferences();
        setListeners();
    }

    private void setListeners() {
        signOutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            clearPreferences();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        nameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
               if(!nameField.getText().toString().equals(userDTO.getName())) {
                   saveChanges.setTextColor(getResources().getColor(R.color.white));
               }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
    private void sendVerificationEmail()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // email sent

                            // after email is sent just logout the user and finish this activity
                            FirebaseAuth.getInstance().signOut();
                            finish();
                        }
                        else
                        {
                            // email not sent, so display message and restart the activity or do whatever you wish to do

                            //restart this activity
                            overridePendingTransition(0, 0);
                            finish();
                            overridePendingTransition(0, 0);
                            startActivity(getIntent());

                        }
                    }
                });
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 0: {
                configureAccountLayout();
                break;
            }
            case 1: {
                configureFeedbackLayout();
                break;
            }
            case 2: {
                configureSubscribeLayout();
                break;
            }
        }
    }

    private void configureAccountLayout() {
        feedbackLayout.setVisibility(View.INVISIBLE);
        subscribeLayout.setVisibility(View.INVISIBLE);
        accountLayout.setVisibility(View.VISIBLE);

    }

    private void configureFeedbackLayout() {
        accountLayout.setVisibility(View.INVISIBLE);
        subscribeLayout.setVisibility(View.INVISIBLE);
        feedbackLayout.setVisibility(View.VISIBLE);
    }

    private void configureSubscribeLayout() {
        accountLayout.setVisibility(View.INVISIBLE);
        feedbackLayout.setVisibility(View.INVISIBLE);
        subscribeLayout.setVisibility(View.VISIBLE);
    }

    private void fillFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        userDTO = UserDTO.builder()
                .email(preferences.getString(EMAIL_FROM_AUTH_FILE, ""))
                .id(Long.parseLong(preferences.getString(ID_USER_AUTH_FILE, "")))
                .uid(preferences.getString(UID_USER_AUTH_FILE, ""))
                .build();


        nameField.setText(userDTO.getName());
        emailField.setText(userDTO.getEmail());
        passwordField.setText(preferences.getString(PASS_FROM_AUTH_FILE,""));
    }

    private void clearPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }
}