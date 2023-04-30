package teamarmada.oxtor.Ui.DialogFragment;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import teamarmada.oxtor.R;

public class ProgressDialog extends DialogFragment {


    public ProgressDialog() {
        super();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Oxtor_AlertDialog)
                .setView(R.layout.fragment_dialog_progress);
        setCancelable(false);
        return builder.create();
    }

}
