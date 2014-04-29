package kimle.michal.android.taxitwin.dialog.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.MainActivity;

public class TaxiTwinNoLongerAlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.taxitwin_no_longer_alert_content)
                .setTitle(R.string.taxitwin_no_longer_alert_title)
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                        dismiss();
                    }
                });
        return builder.create();
    }

}
