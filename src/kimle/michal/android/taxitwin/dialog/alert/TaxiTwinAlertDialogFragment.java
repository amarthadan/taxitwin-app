package kimle.michal.android.taxitwin.dialog.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.MyTaxiTwinActivity;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;

public class TaxiTwinAlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.taxitwin_alert_message)
                .setTitle(R.string.taxitwin_alert_title)
                .setPositiveButton(R.string.enter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(getActivity(), MyTaxiTwinActivity.class);
                        getActivity().startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.leave, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TaxiTwinApplication.getGcmHandler().leaveTaxiTwin();
                        getActivity().getContentResolver().delete(TaxiTwinContentProvider.RIDES_URI, null, null);
                        getActivity().getContentResolver().delete(TaxiTwinContentProvider.TAXITWINS_URI, null, null);
                        TaxiTwinApplication.getGcmHandler().sendNewOffer();

                        dismiss();
                    }
                });
        return builder.create();
    }
}
