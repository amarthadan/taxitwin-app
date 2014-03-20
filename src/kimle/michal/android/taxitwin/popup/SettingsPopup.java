package kimle.michal.android.taxitwin.popup;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;
import com.skd.centeredcontentbutton.CenteredContentButton;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.adapter.TaxiTwinPlacesAutoCompleteAdapter;
import kimle.michal.android.taxitwin.dialog.alert.AddressAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.error.PlaceErrorDialogFragment;
import kimle.michal.android.taxitwin.entity.Place;
import kimle.michal.android.taxitwin.view.TaxiTwinAutoCompleteTextView;

public class SettingsPopup extends PopupWindow {

    public static final int DEFAULT_RADIUS = 200;
    public static final int DEFAULT_PASSENGERS = 4;
    public static final int OFFSET = 32;
    private final Context context;
    private final View popupView;
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    public static final String GEOCODE_API_BASE = "https://maps.googleapis.com/maps/api/geocode/json";
    public static final String GEOCODE_API_KEY = "AIzaSyAD5dc-7yTVvWhKFRQ-OC48dPlLnAvy5hU";
    private static final String LOG = "SettingsPopup";
    private static final String STATUS_OK = "OK";

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
        ttactv.setAdapter(new TaxiTwinPlacesAutoCompleteAdapter(context, android.R.layout.simple_dropdown_item_1line));
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setTouchable(true);
        //setBackgroundDrawable(null);
        setOutsideTouchable(false);

        createDefaultPreferences();
        loadPreferences();
        createListeners();
    }

    @Override
    public void showAsDropDown(View v) {
        super.showAsDropDown(v);
        loadPreferences();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (!hasAddress()) {
            Log.d(LOG, "in dismiss");
            DialogFragment errorFragment = new AddressAlertDialogFragment();
            errorFragment.show(((Activity) context).getFragmentManager(), "address_alert");
        }
        loadPreferences();
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
        TaxiTwinAutoCompleteTextView addressContent = (TaxiTwinAutoCompleteTextView) popupView.findViewById(R.id.address_content);
        addressContent.setText(pref.getString(context.getResources().getString(R.string.pref_address), null));
        addressContent.dismissDropDown();
        addressContent.setText("");
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

                TaxiTwinAutoCompleteTextView addressContent = (TaxiTwinAutoCompleteTextView) popupView.findViewById(R.id.address_content);
                String rawAddress = addressContent.getText().toString();

                GeocodeTask task = new GeocodeTask();
                task.execute(rawAddress);
                Place place = null;
                try {
                    place = task.get(2, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Log.e(LOG, ex.getMessage());
                } catch (ExecutionException ex) {
                    Log.e(LOG, ex.getMessage());
                } catch (TimeoutException ex) {
                    Log.e(LOG, ex.getMessage());
                }

                if (place == null) {
                    DialogFragment errorFragment = new PlaceErrorDialogFragment();
                    errorFragment.show(((Activity) context).getFragmentManager(), "place_error");

                    return;
                }

                Log.d(LOG, "place: " + place);

                editor.putString(context.getResources().getString(R.string.pref_address), place.getAddress());
                editor.putLong(context.getResources().getString(R.string.pref_address_long), place.getLongitude());
                editor.putLong(context.getResources().getString(R.string.pref_address_lat), place.getLatitude());
                SeekBar passengersSeekBar = (SeekBar) popupView.findViewById(R.id.nop_seekbar);
                editor.putInt(context.getResources().getString(R.string.pref_passengers), passengersSeekBar.getProgress() + 1);
                SeekBar radiusSeekBar = (SeekBar) popupView.findViewById(R.id.radius_seekbar);
                editor.putInt(context.getResources().getString(R.string.pref_radius), radiusSeekBar.getProgress() * 10 + 10);

                editor.commit();

                dismiss();
            }
        });
    }

    public boolean hasAddress() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String address = pref.getString(context.getResources().getString(R.string.pref_address), null);

        return address != null;
    }

    private class GeocodeTask extends AsyncTask<String, Void, Place> {

        @Override
        protected Place doInBackground(String... input) {
            Place result = null;

            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                }
            }
            );

            GenericUrl url = new GenericUrl(GEOCODE_API_BASE);
            url.put("address", input[0]);
            url.put("key", GEOCODE_API_KEY);
            url.put("sensor", false);

            HttpRequest request;
            HttpResponse httpResponse;
            GeocodeResult geocodeResult;
            try {
                request = requestFactory.buildGetRequest(url);
                httpResponse = request.execute();
                geocodeResult = httpResponse.parseAs(GeocodeResult.class);

                if (!geocodeResult.status.equals(STATUS_OK)) {
                    Log.e(LOG, "status: " + geocodeResult.status);
                    return result;
                }

                Log.d(LOG, "results: " + geocodeResult.results.get(0).geometry);
                //return null;
                Result firstResult = geocodeResult.results.get(0);
                result = new Place(firstResult.address, firstResult.geometry.location.latitude, firstResult.geometry.location.longitude);

            } catch (IOException ex) {
                Log.e(LOG, ex.getMessage());
            }
            return result;
        }
    }

    public static class GeocodeResult {

        @Key("results")
        public List<Result> results;
        @Key("status")
        public String status;
    }

    public static class Result {

        @Key("formatted_address")
        public String address;
        @Key("geometry")
        public Geometry geometry;
    }

    public static class Geometry {

        @Key("location")
        public Location location;
    }

    public static class Location {

        @Key("lat")
        public Long latitude;
        @Key("lng")
        public Long longitude;
    }
}
