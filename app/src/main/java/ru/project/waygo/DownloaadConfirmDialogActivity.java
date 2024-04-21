package ru.project.waygo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

public class DownloaadConfirmDialogActivity extends DialogFragment {


    private AlertDialog dialog;
    private TextView tittle;
    private MaterialButton yesButton;
    private MaterialButton noButton;
    private Context context;

    public DownloaadConfirmDialogActivity(Context context) {
        this.context = context;

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.activity_downloaad_confirm_dialog, null);


        dialog = new AlertDialog.Builder(context).create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);


        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        tittle = view.findViewById(R.id.tittle);
        yesButton = view.findViewById(R.id.button_yes);
        noButton = view.findViewById(R.id.button_no);

        dialog.setView(view);

        setListeners();
    }

    public void show() {
        dialog.show();
    }

    public void close() {
        dialog.cancel();
        dialog.dismiss();
    }

    private void setListeners() {
        noButton.setOnClickListener(view -> {
            Log.i("close_dialog", "setListeners: close");
            dialog.cancel();
            dialog.dismiss();
        });
    }

    public void preBuild(View.OnClickListener onOk) {
        yesButton.setOnClickListener(onOk);
    }

    public void setText(String modelInfo) {
        String tittleString = context.getResources().getString(R.string.arConfirm) + modelInfo + context.getResources().getString(R.string.arContinue);
        tittle.setText(tittleString);
    }

}