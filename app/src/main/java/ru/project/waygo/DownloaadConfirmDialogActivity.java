package ru.project.waygo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

public class DownloaadConfirmDialogActivity {
    private Dialog dialog;
    private TextView tittle;
    private MaterialButton yesButton;
    private MaterialButton noButton;
    private Context context;

    public DownloaadConfirmDialogActivity(Context context){
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        dialog.setContentView(R.layout.activity_downloaad_confirm_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        tittle = dialog.findViewById(R.id.tittle);
        yesButton = dialog.findViewById(R.id.button_yes);
        noButton = dialog.findViewById(R.id.button_no);

        setListeners();
    }
    public void show(){
        dialog.show();
    }

    public void close() {
        dialog.dismiss();
    }
    private void setListeners() {
        noButton.setOnClickListener(view -> {
            Log.i("close_dialog", "setListeners: close");
            dialog.dismiss();
        });
    }

    public void preBuild(View.OnClickListener onOk){
        yesButton.setOnClickListener(onOk);
    }

    public void setText(String modelInfo){
        String tittleString = context.getResources().getString(R.string.arConfirm) + modelInfo + context.getResources().getString(R.string.arContinue);
        tittle.setText(tittleString);
    }

}