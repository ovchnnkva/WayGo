package ru.project.waygo.user_profile;

import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;
import static ru.project.waygo.Constants.PASS_FROM_AUTH_FILE;
import static ru.project.waygo.utils.VerificationCodeGenerator.generate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;
import java.util.regex.Pattern;

import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.favorite.FavoriteActivity;
import ru.project.waygo.login.RegistrationActivity;
import ru.project.waygo.mail.MailSenderAsync;
import ru.project.waygo.main.HomeActivity;
import ru.project.waygo.map.MapBoxGeneralActivity;

public class ChangePasswordActivity extends BaseActivity {
    private TextInputEditText emailField;
    private TextInputEditText verificationCodeField;
    private TextInputEditText newPasswordField;
    private TextInputEditText repeatPasswordField;
    private ConstraintLayout verificationLayout;
    private ConstraintLayout newPasswordLayout;
    private MaterialButton nextButton;
    private MaterialButton saveChangeButton;
    private MaterialButton getCodeButton;
    private String currentCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailField = findViewById(R.id.email_field);
        verificationCodeField = findViewById(R.id.code_field);
        newPasswordField = findViewById(R.id.new_password_field);
        repeatPasswordField = findViewById(R.id.repeat_password_field);
        newPasswordLayout = findViewById(R.id.change_password_layout);
        saveChangeButton = findViewById(R.id.button_save_change);
        verificationLayout = findViewById(R.id.verification_layout);
        nextButton = findViewById(R.id.button_next);
        getCodeButton = findViewById(R.id.button_get_code);
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

        fillEmail();
        setListeners();
    }

    private void fillEmail() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        emailField.setText(preferences.getString(EMAIL_FROM_AUTH_FILE, ""));
    }

    private void setListeners() {
        getCodeButton.setOnClickListener(view ->sendCode());
        nextButton.setOnClickListener(view -> checkVerification());
        saveChangeButton.setOnClickListener(view -> {
            if(checkValidPass() && checkRepeatPassword()){
                changePasswordFirebase();
            }
        });
    }

    private void sendCode() {
        currentCode = generate();
        MailSenderAsync mailSenderAsync = new MailSenderAsync(getResources().getString(R.string.verification_code),
                currentCode, getResources().getString(R.string.project_mail), emailField.getText().toString());

        mailSenderAsync.execute();
        Toast.makeText(this, "Код отправлен вам на почту", Toast.LENGTH_LONG).show();
    }

    private void checkVerification() {
        if(emailField.getText() == null || emailField.getText().toString().isBlank()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_LONG).show();
        }else if(verificationCodeField.getText() == null || verificationCodeField.getText().toString().isBlank()) {
           Toast.makeText(this, "Введите код", Toast.LENGTH_LONG).show();
       } else if (!verificationCodeField.getText().toString().equals(currentCode)) {
           Toast.makeText(this, "Неверный код", Toast.LENGTH_LONG).show();
       } else {
           verificationLayout.setVisibility(View.INVISIBLE);
           newPasswordLayout.setVisibility(View.VISIBLE);
       }
    }

    private boolean checkValidPass(){
        String pass = Objects.requireNonNull(newPasswordField.getText())
                .toString()
                .replaceAll(" ","");

        Pattern patternDegits = Pattern.compile("[1-9]");

        if((newPasswordField.getText() != null) && (pass.length() >= 8) && (patternDegits.matcher(pass).find())) {
            return true;
        } else if (pass.length() < 8){
            Toast.makeText(ChangePasswordActivity.this, "Пароль слишком короткий", Toast.LENGTH_LONG).show();
        } else if (!patternDegits.matcher(pass).find()) {
            Toast.makeText(ChangePasswordActivity.this, "Пароль должен содержать цифры", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private boolean checkRepeatPassword() {
        String password = Objects.requireNonNullElse(repeatPasswordField.getText(), "").toString();
        String passwordRepeat = Objects.requireNonNull(repeatPasswordField.getText(), "").toString();

        if(!password.isBlank() && !passwordRepeat.isBlank() && password.equals(passwordRepeat)) {
            return true;
        } else {
            repeatPasswordField.setError("Пароли не совпадают");
        }
        return false;
    }

    private void changePasswordFirebase() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(getEmailFromPreferences(), getPasswordFromPreferences());

        firebaseUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    Log.d("FIREBASE_CHANGE_PASS", "onComplete: user reauth");
                    firebaseUser.updatePassword(emailField.getText().toString()).addOnCompleteListener(task1 -> {
                        if(task1.isSuccessful()) {
                            saveNewPasswordPreferences();
                            setEnable();
                        }
                    });
                });
    }

    private void setEnable() {
        saveChangeButton.setTextColor(getResources().getColor(R.color.black));
        saveChangeButton.getBackground().setColorFilter(Color.parseColor("#DDDDDD"), PorterDuff.Mode.SRC);
        saveChangeButton.setEnabled(false);
        saveChangeButton.setText(getResources().getString(R.string.change_password_success));

        newPasswordField.setEnabled(false);
        repeatPasswordField.setEnabled(false);
    }
    private void saveNewPasswordPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PASS_FROM_AUTH_FILE, newPasswordField.getText().toString());
        editor.apply();
    }
    private String getPasswordFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return preferences.getString(PASS_FROM_AUTH_FILE,"");
    }

    private String getEmailFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(AUTH_FILE_NAME, MODE_PRIVATE);
        return preferences.getString(EMAIL_FROM_AUTH_FILE,"");
    }

}