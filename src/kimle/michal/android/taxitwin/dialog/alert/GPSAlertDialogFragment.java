package kimle.michal.android.taxitwin.dialog.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;

public class GPSAlertDialogFragment extends DialogFragment {

    public interface GPSAlertDialogListener {

        public void onDialogPositiveClick(DialogFragment dialog);

        public void onDialogNegativeClick(DialogFragment dialog);
    }

    private GPSAlertDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.gps_alert_message)
                .setTitle(R.string.gps_alert_title)
                .setPositiveButton(R.string.location_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogPositiveClick(GPSAlertDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogNegativeClick(GPSAlertDialogFragment.this);
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (GPSAlertDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement GPSAlertDialogListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        listener.onDialogNegativeClick(GPSAlertDialogFragment.this);
    }
}
