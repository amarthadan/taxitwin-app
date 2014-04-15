package kimle.michal.android.taxitwin.gcm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import static android.content.Context.ACCOUNT_SERVICE;
import static android.content.Context.LOCATION_SERVICE;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.enumerate.UserState;
import kimle.michal.android.taxitwin.services.ServicesManagement;

public class GcmHandler implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String GCM_DATA_TYPE = "type";
    public static final String GCM_DATA_ID = "id";
    public static final String GCM_DATA_START_LONGITUDE = "start_long";
    public static final String GCM_DATA_START_LATITUDE = "start_lat";
    public static final String GCM_DATA_START_TEXTUAL = "start_text";
    public static final String GCM_DATA_END_LONGITUDE = "end_long";
    public static final String GCM_DATA_END_LATITUDE = "end_lat";
    public static final String GCM_DATA_END_TEXTUAL = "end_text";
    public static final String GCM_DATA_RADIUS = "radius";
    public static final String GCM_DATA_NAME = "name";
    public static final String GCM_DATA_PASSENGERS = "passengers";
    public static final String GCM_DATA_PASSENGERS_TOTAL = "passengers_total";
    public static final String GCM_DATA_TAXITWIN_ID = "taxitwin_id";
    public static final String GCM_DATA_OFFER_ID = "offer_id";
    public static final String GCM_DATA_TYPE_SUBSCRIBE = "subscribe";
    public static final String GCM_DATA_TYPE_UNSUBSCRIBE = "unsubscribe";
    public static final String GCM_DATA_TYPE_MODIFY = "modify";
    public static final String GCM_DATA_TYPE_OFFER = "offer";
    public static final String GCM_DATA_TYPE_INVALIDATE = "invalidate";
    public static final String GCM_DATA_TYPE_ACCEPT_OFFER = "accept_offer";
    public static final String GCM_DATA_TYPE_RESPONSE = "response";
    public static final String GCM_DATA_TYPE_ACCEPT_RESPONSE = "accept_response";
    public static final String GCM_DATA_TYPE_DECLINE_RESPONSE = "decline_response";
    public static final String GCM_DATA_TYPE_TAXITWIN = "taxitwin";
    public static final String GCM_DATA_TYPE_LEAVE_TAXITWIN = "leave_taxitwin";
    public static final String GCM_DATA_TYPE_NO_LONGER = "no_longer";
    private static final String LOG = "GcmHandler";
    private final Context context;
    private final GcmConnector gcmConnector;

    public GcmHandler(Context context) {
        this.context = context;
        gcmConnector = new GcmConnector(context);
    }

    public void locationChanged(Location location) {
        Log.d(LOG, "in locationChanged");
        if (location != null) {
            Bundle data = new Bundle();
            data.putString(GCM_DATA_START_LATITUDE, String.valueOf(location.getLatitude()));
            data.putString(GCM_DATA_START_LONGITUDE, String.valueOf(location.getLongitude()));
            sendChangedOffer(data);
        }
    }

    private boolean hasAllData() {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null || location.getLatitude() == 0 || location.getLongitude() == 0) {
            return false;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (pref.getFloat(context.getResources().getString(R.string.pref_address_lat), 0) == 0
                || pref.getFloat(context.getResources().getString(R.string.pref_address_long), 0) == 0) {
            return false;
        }
        if (pref.getInt(context.getResources().getString(R.string.pref_radius), 0) == 0
                || pref.getInt(context.getResources().getString(R.string.pref_passengers), 0) == 0) {
            return false;
        }

        return true;
    }

    public void sendNewOffer() {
        Log.d(LOG, "in sendNewOffer");
        if (!hasAllData() || !ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot send new offer - missing data or service");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_SUBSCRIBE);
        data.putString(GCM_DATA_START_LATITUDE, String.valueOf(location.getLatitude()));
        data.putString(GCM_DATA_START_LONGITUDE, String.valueOf(location.getLongitude()));
        data.putString(GCM_DATA_END_LATITUDE, String.valueOf(pref.getFloat(context.getResources().getString(R.string.pref_address_lat), 0)));
        data.putString(GCM_DATA_END_LONGITUDE, String.valueOf(pref.getFloat(context.getResources().getString(R.string.pref_address_long), 0)));
        data.putString(GCM_DATA_PASSENGERS, String.valueOf(pref.getInt(context.getResources().getString(R.string.pref_passengers), 0)));
        data.putString(GCM_DATA_RADIUS, String.valueOf(pref.getInt(context.getResources().getString(R.string.pref_radius), 0)));
        data.putString(GCM_DATA_NAME, getUserName());

        gcmConnector.send(data);
        TaxiTwinApplication.setUserState(UserState.SUBSCRIBED);

        Log.d(LOG, "sending subscribe: " + data);
    }

    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        Log.d(LOG, "in onSharedPreferenceChanged");
        if (key.equals(context.getResources().getString(R.string.pref_address))) {

            Bundle data = new Bundle();
            data.putString(GCM_DATA_END_LATITUDE, String.valueOf(pref.getFloat(context.getResources().getString(R.string.pref_address_lat), 0)));
            data.putString(GCM_DATA_END_LONGITUDE, String.valueOf(pref.getFloat(context.getResources().getString(R.string.pref_address_long), 0)));

            sendChangedOffer(data);
            return;
        }

        if (key.equals(context.getResources().getString(R.string.pref_passengers))) {

            Bundle data = new Bundle();
            data.putString(GCM_DATA_PASSENGERS, String.valueOf(pref.getInt(context.getResources().getString(R.string.pref_passengers), 0)));

            sendChangedOffer(data);
            return;
        }

        if (key.equals(context.getResources().getString(R.string.pref_radius))) {

            Bundle data = new Bundle();
            data.putString(GCM_DATA_RADIUS, String.valueOf(pref.getInt(context.getResources().getString(R.string.pref_radius), 0)));

            sendChangedOffer(data);
        }
    }

    private void sendChangedOffer(Bundle data) {
        if (TaxiTwinApplication.getUserState() == UserState.NOT_SUBSCRIBED) {
            sendNewOffer();
            return;
        }
        if (!ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot send new changes to offer - missing service");
            return;
        }

        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_MODIFY);
        gcmConnector.send(data);

        Log.d(LOG, "sending changes: " + data);
    }

    private String getUserName() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Cursor cursor = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();

                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Profile.DISPLAY_NAME));
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } else {
            AccountManager manager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
            Account[] list = manager.getAccounts();
            String accountName = "";
            for (Account account : list) {
                if (account.type.equalsIgnoreCase("com.google")) {
                    accountName = account.name;
                }
            }

            if (accountName.contains("@")) {
                String[] parts = accountName.split("@");
                return parts[0];
            }
        }

        return "unknown name";
    }

    public void unsubscribe() {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_UNSUBSCRIBE);

        if (TaxiTwinApplication.getUserState() == UserState.NOT_SUBSCRIBED || !ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot unsubscribe - missing service");
            return;
        }

        gcmConnector.send(data);
        TaxiTwinApplication.setUserState(UserState.NOT_SUBSCRIBED);
    }

    public void acceptOffer(long taxitwinId) {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_ACCEPT_OFFER);
        data.putString(GCM_DATA_TAXITWIN_ID, String.valueOf(taxitwinId));

        if (TaxiTwinApplication.getUserState() == UserState.NOT_SUBSCRIBED || !ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot accept an offer - missing service");
            return;
        }

        gcmConnector.send(data);
    }

    public void acceptResponse(long taxitwinId) {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_ACCEPT_RESPONSE);
        data.putString(GCM_DATA_TAXITWIN_ID, String.valueOf(taxitwinId));

        if (TaxiTwinApplication.getUserState() == UserState.NOT_SUBSCRIBED || !ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot accept a response - missing service");
            return;
        }

        gcmConnector.send(data);
    }

    public void declineResponse(long taxitwinId) {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_DECLINE_RESPONSE);
        data.putString(GCM_DATA_TAXITWIN_ID, String.valueOf(taxitwinId));

        if (TaxiTwinApplication.getUserState() == UserState.NOT_SUBSCRIBED || !ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot decline a response - missing service");
            return;
        }

        gcmConnector.send(data);
    }

    public void leaveTaxiTwin() {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_LEAVE_TAXITWIN);

        if (TaxiTwinApplication.getUserState() == UserState.NOT_SUBSCRIBED || !ServicesManagement.checkServices(context)) {
            Log.w(LOG, "cannot leave taxitwin - missing service");
            return;
        }

        gcmConnector.send(data);
    }
}
