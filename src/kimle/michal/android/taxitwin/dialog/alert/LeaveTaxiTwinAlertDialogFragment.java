package kimle.michal.android.taxitwin.dialog.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.MainActivity;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;

public class LeaveTaxiTwinAlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.leave_taxitwin_alert_message)
                .setTitle(R.string.leave_taxitwin_alert_title)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TaxiTwinApplication.getGcmHandler().leaveTaxiTwin();
                        getActivity().getContentResolver().delete(TaxiTwinContentProvider.RIDES_URI, null, null);
                        getActivity().getContentResolver().delete(TaxiTwinContentProvider.TAXITWINS_URI, null, null);
                        TaxiTwinApplication.getGcmHandler().sendNewOffer();

                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        getActivity().startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        return builder.create();
    }
}
