package kimle.michal.android.taxitwin.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;

public class GooglePlayServicesErrorDialogFragment extends DialogFragment {

    public interface GooglePlayServicesErrorDialogListener {

        public void onDialogNeutralClick(DialogFragment dialog);
    }

    private GooglePlayServicesErrorDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.google_play_services_error_message)
                .setTitle(R.string.google_play_services_error_title)
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogNeutralClick(GooglePlayServicesErrorDialogFragment.this);
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (GooglePlayServicesErrorDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement GooglePlayServicesErrorDialogListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        listener.onDialogNeutralClick(GooglePlayServicesErrorDialogFragment.this);
    }
}
