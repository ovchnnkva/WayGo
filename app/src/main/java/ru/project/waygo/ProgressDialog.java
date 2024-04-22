package ru.project.waygo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FileDownloadTask;

public class ProgressDialog extends DialogFragment {
    private AlertDialog dialog;

    private TextView progressText;

    private ProgressBar bar;

    private MaterialButton cancelButton;

    private Context context;


    public ProgressDialog(Context context) {
        this.context = context;

        LayoutInflater layoutInflater = LayoutInflater.from(context);

        View view = layoutInflater.inflate(R.layout.activity_progress_dialog, null);

        dialog = new AlertDialog.Builder(context).create();
        dialog.requestWindowFeature(Window.FEATURE_PROGRESS);
        dialog.setCancelable(false);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        progressText = view.findViewById(R.id.progress_text);

        bar = view.findViewById(R.id.bar_progress);

        bar.setMax(100);

        cancelButton = view.findViewById(R.id.button_cancel);

        dialog.setView(view);
    }

    public void show() {
        dialog.show();
    }

    public void close() {
        bar.setActivated(false);
        dialog.cancel();
        dialog.dismiss();
    }

    public void preBuild(FileDownloadTask.TaskSnapshot task,View.OnClickListener onOk) {
        cancelButton.setOnClickListener(onOk);
    }

    @SuppressLint("SetTextI18n")
    public void setProgress(int progress) {
        progressText.setText("Сцена скачивается \n"+progress + "%");
        bar.setProgress(progress, true);
    }
}
