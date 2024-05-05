package ru.project.waygo.main;

import static com.google.android.material.internal.ContextUtils.getActivity;
import static ru.project.waygo.Constants.AUTH_FILE_NAME;
import static ru.project.waygo.Constants.EMAIL_FROM_AUTH_FILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.material.button.MaterialButton;

import ru.project.waygo.BaseActivity;
import ru.project.waygo.R;
import ru.project.waygo.login.LoginActivity;
import ru.project.waygo.login.RegistrationActivity;

public class MainActivity extends BaseActivity {
    private MaterialButton loginButton;
    private MaterialButton registrationButton;
    private VideoView videoView;

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
        videoView = findViewById(R.id.videoView);

        addListeners();
        configureVideo();
    }

    private void configureVideo() {
        String packageName = this.getPackageName();
        String path = "android.resource://" + packageName + "/raw/" + R.raw.main_video_start;
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
        videoView.setOnCompletionListener(mediaPlayer -> {
            String path1 = "android.resource://" + packageName + "/" + R.raw.main_video_next;
            videoView.setVideoURI(Uri.parse(path1));
            videoView.start();
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (isAuthorizedUser()) {
            Log.i("AUTH", "isAuthorizedUser: " + true);
            logIn();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAuthorizedUser()) {
            Log.i("AUTH", "isAuthorizedUser: " + true);
            logIn();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.stopPlayback();
        videoView.clearAnimation();
        videoView.suspend();
    }
}