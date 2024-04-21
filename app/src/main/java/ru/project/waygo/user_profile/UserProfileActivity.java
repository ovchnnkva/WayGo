package ru.project.waygo.user_profile;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.ID_USER_AUTH_FILE;
import static ru.project.waygo.Constants.NAME_USER_AUTH_FILE;
import static ru.project.waygo.Constants.PASS_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.UID_USER_AUTH_FILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.dto.user.UserDTO;
import ru.project.waygo.favorite.FavoriteActivity;
import ru.project.waygo.mail.MailSenderAsync;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.main.MainActivity;
import ru.project.waygo.map.MapBoxGeneralActivity;

public class UserProfileActivity extends BaseActivity implements TabLayout.OnTabSelectedListener {
    private ConstraintLayout accountLayout;
    private ConstraintLayout feedbackLayout;
    private ConstraintLayout subscribeLayout;
    private TextInputEditText nameField;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private TextInputEditText themeFeedbackField;
    private TextInputEditText messageFeedbackField;
    private TabLayout tabLayout;

    private MaterialButton signOutButton;
    private MaterialButton saveChanges;
    private MaterialButton sendFeedbackButton;
    private MaterialButton changePasswordButton;
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
        themeFeedbackField = findViewById(R.id.theme_field);
        messageFeedbackField = findViewById(R.id.message_field);
        signOutButton = findViewById(R.id.button_sign_out);
        saveChanges = findViewById(R.id.button_save);
        sendFeedbackButton = findViewById(R.id.button_send_feedback);
        changePasswordButton = findViewById(R.id.button_change_password);
        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setOnTabSelectedListener(this);
        BottomNavigationView bottomNavigationView=findViewById(R.id.navigation_bar);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.action_account);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId())
            {
                case R.id.action_map:
                    startActivity(new Intent(getApplicationContext(), MapBoxGeneralActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_main:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_favorites:
                    startActivity(new Intent(getApplicationContext(), FavoriteActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                case R.id.action_account:
                    return true;
            }
            return false;
        });
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
            finishAffinity();
        });

        changePasswordButton.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), ChangePasswordActivity.class)));
        
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(isUserDataNotEmpty() && (isUpdateUserName() || isUpdateUserEmail())) {
                    saveChanges.setTextColor(getResources().getColor(R.color.white));
                    saveChanges.getBackground().setColorFilter(Color.parseColor("#7A67FE"), PorterDuff.Mode.SRC);
                    saveChanges.setEnabled(true);
                } else {
                    saveChanges.setTextColor(getResources().getColor(R.color.black));
                    saveChanges.getBackground().setColorFilter(Color.parseColor("#DDDDDD"), PorterDuff.Mode.SRC);
                    saveChanges.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        
        nameField.addTextChangedListener(textWatcher);
        emailField.addTextChangedListener(textWatcher);
        
        saveChanges.setOnClickListener(view -> {
            if(isUpdateUserEmail()) {
                fireBaseUpdateEmail();
                updatePreferences();
            }
        });

        sendFeedbackButton.setOnClickListener(view -> sendFeedback());
    }
    
    private void fireBaseUpdateEmail() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(userDTO.getEmail(), getPasswordFromPreferences());

        firebaseUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    firebaseUser.sendEmailVerification().addOnSuccessListener(task1 -> {
                        Log.d("FIREBASE_CHANGE_PASS", "onComplete: user reauth");
                        firebaseUser.updateEmail(emailField.getText().toString()).addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                Toast.makeText(UserProfileActivity.this, "Почта успешно изменена", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                });

    }

    private void sendFeedback() {
        if(themeFeedbackField.getText() != null || messageFeedbackField.getText() != null){
            MailSenderAsync mailSenderAsync = new MailSenderAsync(themeFeedbackField.getText().toString(),
                    messageFeedbackField.getText().toString(), userDTO.getEmail(), getResources().getString(R.string.project_mail));

            mailSenderAsync.execute();
            themeFeedbackField.setText("");
            messageFeedbackField.setText("");
        }
    }
    private boolean isUserDataNotEmpty() {
        return nameField.getText() != null 
                || !nameField.getText().toString().equals("")
                || emailField.getText() != null
                || !emailField.getText().toString().equals("");
    }
    private boolean isUpdateUserName() {
        return !Objects.equals(nameField.getText().toString(), userDTO.getName());
    }

    private boolean isUpdateUserEmail() {
        return !emailField.getText().toString().equals(userDTO.getEmail());
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
                .name(preferences.getString(NAME_USER_AUTH_FILE, ""))
                .build();


        nameField.setText(userDTO.getName());
        emailField.setText(userDTO.getEmail());
        passwordField.setText(preferences.getString(PASS_FROM_AUTH_FILE,""));
    }

    private void updatePreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(NAME_USER_AUTH_FILE, nameField.getText().toString());
        editor.putString(EMAIL_FROM_AUTH_FILE, emailField.getText().toString());
    }

    private String getPasswordFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return preferences.getString(PASS_FROM_AUTH_FILE,"");
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