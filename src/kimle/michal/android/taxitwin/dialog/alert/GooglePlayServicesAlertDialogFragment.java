package kimle.michal.android.taxitwin.dialog.alert;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class GooglePlayServicesAlertDialogFragment extends DialogFragment {

    private Dialog dialog;

    public GooglePlayServicesAlertDialogFragment() {
        super();
        dialog = null;
    }

    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return dialog;
    }
}
