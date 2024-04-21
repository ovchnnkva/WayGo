package ru.project.waygo.ar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import lombok.RequiredArgsConstructor;
import ru.project.waygo.R;

@RequiredArgsConstructor
public class DownloadConfirmDialog extends DialogFragment {
    private AlertDialog.Builder builder ;
    private Context context;

    public DownloadConfirmDialog(Context context){
        builder = new AlertDialog.Builder(context);
        this.context = context;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction.

        // Create the AlertDialog object and return it.
        return builder.create();
    }

    public void preBuild(DialogInterface.OnClickListener onOk, DialogInterface.OnClickListener onNo,Context context){
        builder.setPositiveButton("Да", onOk);
        builder.setNegativeButton("Нет",onNo);
    }

    public void setText(String modelInfo){
        builder.setMessage(context.getResources().getString(R.string.arConfirm)+modelInfo+context.getResources().getString(R.string.arContinue));
    }

}
