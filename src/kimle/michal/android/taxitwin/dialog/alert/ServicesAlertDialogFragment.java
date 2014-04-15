package kimle.michal.android.taxitwin.dialog.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;

public class ServicesAlertDialogFragment extends DialogFragment {

    private int serviceId;

    public ServicesAlertDialogFragment(int serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(serviceId)
                .setTitle(R.string.services_alert_title)
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        return builder.create();
    }
}
