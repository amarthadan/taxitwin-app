package kimle.michal.android.taxitwin.popup;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.skd.centeredcontentbutton.CenteredContentButton;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.view.TaxiTwinAutoCompleteTextView;

public class SettingsPopup extends PopupWindow {

    public static final int DEFAULT_RADIUS = 200;
    public static final int DEFAULT_PASSENGERS = 4;
    public static final int OFFSET = 32;
    private final Context context;
    private final View popupView;

    public SettingsPopup(Context context) {
        super(context);
        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.popup, null);
        setContentView(popupView);
        TaxiTwinAutoCompleteTextView ttactv = (TaxiTwinAutoCompleteTextView) popupView.findViewById(R.id.address_content);
        ttactv.setSuperView(((Activity) context).getWindow().getDecorView());
        ttactv.setDropDownAnchor(context.getResources().getIdentifier("action_bar_container", "id", "android"));
        ttactv.setDropDownVerticalOffset(OFFSET);
        //TODO: add adapter
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setTouchable(true);

        createDefaultPreferences();
        loadPreferences();
        createListeners();
    }

    private void createDefaultPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        if (!pref.contains(context.getResources().getString(R.string.pref_radius))) {
            editor.putInt(context.getResources().getString(R.string.pref_radius), DEFAULT_RADIUS);
        }
        if (!pref.contains(context.getResources().getString(R.string.pref_passengers))) {
            editor.putInt(context.getResources().getString(R.string.pref_passengers), DEFAULT_PASSENGERS);
        }
        editor.commit();
    }

    private void loadPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        EditText addressContent = (EditText) popupView.findViewById(R.id.address_content);
        addressContent.setText(pref.getString(context.getResources().getString(R.string.pref_address), null));
        TextView passengersContent = (TextView) popupView.findViewById(R.id.nop_content);
        passengersContent.setText(String.valueOf(pref.getInt(context.getResources().getString(R.string.pref_passengers), DEFAULT_PASSENGERS)));
        SeekBar passengersSeekBar = (SeekBar) popupView.findViewById(R.id.nop_seekbar);
        passengersSeekBar.setProgress(pref.getInt(context.getResources().getString(R.string.pref_passengers), DEFAULT_PASSENGERS) - 1);
        TextView radiusContent = (TextView) popupView.findViewById(R.id.radius_content);
        radiusContent.setText(String.valueOf(pref.getInt(context.getResources().getString(R.string.pref_radius), DEFAULT_RADIUS)) + "m");
        SeekBar radiusSeekBar = (SeekBar) popupView.findViewById(R.id.radius_seekbar);
        radiusSeekBar.setProgress(pref.getInt(context.getResources().getString(R.string.pref_radius), DEFAULT_RADIUS) / 10 - 10);
    }

    private void createListeners() {
        CenteredContentButton cancel = (CenteredContentButton) popupView.findViewById(R.id.cancel_button);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                loadPreferences();
            }
        });

        SeekBar passengersSeekBar = (SeekBar) popupView.findViewById(R.id.nop_seekbar);
        passengersSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView passengersContent = (TextView) popupView.findViewById(R.id.nop_content);
                passengersContent.setText(String.valueOf(progress + 1));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar radiusSeekBar = (SeekBar) popupView.findViewById(R.id.radius_seekbar);
        radiusSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView passengersContent = (TextView) popupView.findViewById(R.id.radius_content);
                passengersContent.setText(String.valueOf(progress * 10 + 10) + "m");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        CenteredContentButton accept = (CenteredContentButton) popupView.findViewById(R.id.accept_button);
        accept.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = pref.edit();

                EditText addressContent = (EditText) popupView.findViewById(R.id.address_content);
                editor.putString(context.getResources().getString(R.string.pref_address), addressContent.getText().toString());
                SeekBar passengersSeekBar = (SeekBar) popupView.findViewById(R.id.nop_seekbar);
                editor.putInt(context.getResources().getString(R.string.pref_passengers), passengersSeekBar.getProgress() + 1);
                SeekBar radiusSeekBar = (SeekBar) popupView.findViewById(R.id.radius_seekbar);
                editor.putInt(context.getResources().getString(R.string.pref_radius), radiusSeekBar.getProgress() * 10 + 10);

                editor.commit();

                dismiss();
            }
        });
    }
}
