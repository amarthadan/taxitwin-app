package kimle.michal.android.taxitwin.gcm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import static android.content.Context.ACCOUNT_SERVICE;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.entity.Place;

public class GcmHandler implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String GCM_DATA_TYPE = "type";
    public static final String GCM_DATA_START_LONGITUDE = "start_long";
    public static final String GCM_DATA_START_LATITUDE = "start_lat";
    public static final String GCM_DATA_END_LONGITUDE = "end_long";
    public static final String GCM_DATA_END_LATITUDE = "end_lat";
    public static final String GCM_DATA_RADIUS = "radius";
    public static final String GCM_DATA_NAME = "name";
    public static final String GCM_DATA_PASSENGERS = "passengers";
    public static final String GCM_DATA_TAXITWIN_ID = "taxitwin_id";
    public static final String GCM_DATA_TYPE_SUBSCRIBE = "subscribe";
    public static final String GCM_DATA_TYPE_MODIFY = "modify";
    private static final String LOG = "GcmHandler";
    private boolean subscribed;
    private Place current;
    private Place destination;
    private int radius;
    private int passengers;
    private final Context context;
    private boolean goodToGo;
    private final GcmConnector gcmConnector;
    private Long taxiTwinId;

    public GcmHandler(Context context) {
        this.context = context;

        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        loadPreferences();
        goodToGo = false;
        gcmConnector = new GcmConnector(context);
        getTaxiTwinId();
    }

    public void setGoodToGo(boolean status) {
        goodToGo = status;
    }

    private void loadPreferences() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        destination = new Place();
        destination.setAddress(pref.getString(context.getResources().getString(R.string.pref_address), null));
        destination.setLatitude(Double.valueOf(pref.getFloat(context.getResources().getString(R.string.pref_address_lat), 0)));
        destination.setLongitude(Double.valueOf(pref.getFloat(context.getResources().getString(R.string.pref_address_long), 0)));
        current = new Place();
        radius = pref.getInt(context.getResources().getString(R.string.pref_radius), 0);
        passengers = pref.getInt(context.getResources().getString(R.string.pref_passengers), 0);
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void locationChanged(Location location) {
        Log.d(LOG, "in locationChanged");
        if (location != null) {
            if (current == null) {
                current = new Place();
            }
            current.setLatitude(location.getLatitude());
            current.setLongitude(location.getLongitude());
        }

        Bundle data = new Bundle();
        data.putDouble(GCM_DATA_START_LATITUDE, current.getLatitude());
        data.putDouble(GCM_DATA_START_LONGITUDE, current.getLongitude());
        sendChangedOffer(data);
    }

    private boolean hasAllData() {
        return (current.isFilled() && destination.isFilled() && radius > 0 && passengers > 0);
    }

    private void sendNewOffer() {
        Log.d(LOG, "in sendNewOffer");
        if (!hasAllData() || !goodToGo) {
            Log.w(LOG, "cannot send new offer - missing data or service");
            return;
        }

        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_SUBSCRIBE);
        data.putDouble(GCM_DATA_START_LATITUDE, current.getLatitude());
        data.putDouble(GCM_DATA_START_LONGITUDE, current.getLongitude());
        data.putDouble(GCM_DATA_END_LATITUDE, destination.getLatitude());
        data.putDouble(GCM_DATA_END_LONGITUDE, destination.getLongitude());
        data.putInt(GCM_DATA_PASSENGERS, passengers);
        data.putInt(GCM_DATA_RADIUS, radius);
        data.putString(GCM_DATA_NAME, getUserName());

        gcmConnector.send(data);
        subscribed = true;

        Log.d(LOG, "sending subscribe...");
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(LOG, "in onSharedPreferenceChanged");
        if (key.equals(context.getResources().getString(R.string.pref_address))) {
            if (destination == null) {
                destination = new Place();
            }

            destination.setLatitude(Double.valueOf(sharedPreferences.getFloat(context.getResources().getString(R.string.pref_address_lat), destination.getLatitude().floatValue())));
            destination.setLongitude(Double.valueOf(sharedPreferences.getFloat(context.getResources().getString(R.string.pref_address_long), destination.getLongitude().floatValue())));

            Bundle data = new Bundle();
            data.putDouble(GCM_DATA_END_LATITUDE, destination.getLatitude());
            data.putDouble(GCM_DATA_END_LONGITUDE, destination.getLongitude());

            sendChangedOffer(data);
            return;
        }

        if (key.equals(context.getResources().getString(R.string.pref_passengers))) {
            passengers = sharedPreferences.getInt(key, passengers);

            Bundle data = new Bundle();
            data.putInt(GCM_DATA_PASSENGERS, passengers);

            sendChangedOffer(data);
            return;
        }

        if (key.equals(context.getResources().getString(R.string.pref_radius))) {
            radius = sharedPreferences.getInt(key, radius);

            Bundle data = new Bundle();
            data.putInt(GCM_DATA_RADIUS, radius);

            sendChangedOffer(data);
        }
    }

    private void sendChangedOffer(Bundle data) {
        if (!isSubscribed()) {
            sendNewOffer();
            return;
        }
        if (!goodToGo) {
            Log.w(LOG, "cannot send new changes to offer - missing service");
            return;
        }

        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_MODIFY);
        if (taxiTwinId == null) {
            getTaxiTwinId();
        }
        data.putLong(GCM_DATA_TAXITWIN_ID, taxiTwinId);
        gcmConnector.send(data);

        Log.d(LOG, "sending changes...");
    }
//
//    private String getDeviceId() {
//        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
//    }

    private String getUserName() {
//        Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
//        c.moveToFirst();
//        String name = c.getString(c.getColumnIndex("display_name"));
//        c.close();
//
//        return name;
        AccountManager manager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();

        for (Account account : list) {
            if (account.type.equalsIgnoreCase("com.google")) {
                return account.name;
            }
        }

        Log.w(LOG, "no name was found");
        return "";
    }

    private void getTaxiTwinId() {
        /**
         * **TESTING***
         */
        taxiTwinId = 5l;
        /**
         * **TESTING***
         */
        //TODO - fetch db for your own taxitwin id
    }
}
