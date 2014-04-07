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
    private static final String LOG = "GcmHandler";
    private boolean subscribed;
    private Place current;
    private Place destination;
    private int radius;
    private int passengers;
    private final Context context;
    private boolean goodToGo;
    private final GcmConnector gcmConnector;

    public GcmHandler(Context context, boolean subscribed, Place current) {
        this.context = context;
        this.subscribed = subscribed;
        this.current = current;

        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        loadPreferences();
        goodToGo = false;
        gcmConnector = new GcmConnector(context);
    }

    public GcmHandler(Context context) {
        this(context, false, null);
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

    public Place getCurrentLocation() {
        return current;
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
        data.putString(GCM_DATA_START_LATITUDE, current.getLatitude().toString());
        data.putString(GCM_DATA_START_LONGITUDE, current.getLongitude().toString());
        sendChangedOffer(data);
    }

    private boolean hasAllData() {
        return (current.isFilled() && destination.isFilled() && radius > 0 && passengers > 0);
    }

    public void sendNewOffer() {
        Log.d(LOG, "in sendNewOffer");
        if (!hasAllData() || !goodToGo) {
            Log.w(LOG, "cannot send new offer - missing data or service");
            return;
        }

        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_SUBSCRIBE);
        data.putString(GCM_DATA_START_LATITUDE, current.getLatitude().toString());
        data.putString(GCM_DATA_START_LONGITUDE, current.getLongitude().toString());
        data.putString(GCM_DATA_END_LATITUDE, destination.getLatitude().toString());
        data.putString(GCM_DATA_END_LONGITUDE, destination.getLongitude().toString());
        data.putString(GCM_DATA_PASSENGERS, String.valueOf(passengers));
        data.putString(GCM_DATA_RADIUS, String.valueOf(radius));
        data.putString(GCM_DATA_NAME, getUserName());

        gcmConnector.send(data);
        subscribed = true;

        Log.d(LOG, "sending subscribe: " + data);
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
            data.putString(GCM_DATA_END_LATITUDE, destination.getLatitude().toString());
            data.putString(GCM_DATA_END_LONGITUDE, destination.getLongitude().toString());

            sendChangedOffer(data);
            return;
        }

        if (key.equals(context.getResources().getString(R.string.pref_passengers))) {
            passengers = sharedPreferences.getInt(key, passengers);

            Bundle data = new Bundle();
            data.putString(GCM_DATA_PASSENGERS, String.valueOf(passengers));

            sendChangedOffer(data);
            return;
        }

        if (key.equals(context.getResources().getString(R.string.pref_radius))) {
            radius = sharedPreferences.getInt(key, radius);

            Bundle data = new Bundle();
            data.putString(GCM_DATA_RADIUS, String.valueOf(radius));

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
        gcmConnector.send(data);

        Log.d(LOG, "sending changes: " + data);
    }
//
//    private String getDeviceId() {
//        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
//    }

    public String getUserName() {
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
        //FIXME giving email not name
    }

    public void unsubscribe() {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_UNSUBSCRIBE);

        if (!isSubscribed() || !goodToGo) {
            Log.w(LOG, "cannot unsubscribe - missing service");
            return;
        }

        gcmConnector.send(data);
        subscribed = false;
    }

    public void acceptOffer(long taxitwinId) {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_ACCEPT_OFFER);
        data.putString(GCM_DATA_TAXITWIN_ID, String.valueOf(taxitwinId));

        if (!isSubscribed() || !goodToGo) {
            Log.w(LOG, "cannot accept an offer - missing service");
            return;
        }

        gcmConnector.send(data);
    }

    public void acceptResponse(long taxitwinId) {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_ACCEPT_RESPONSE);
        data.putString(GCM_DATA_TAXITWIN_ID, String.valueOf(taxitwinId));

        if (!isSubscribed() || !goodToGo) {
            Log.w(LOG, "cannot accept a response - missing service");
            return;
        }

        gcmConnector.send(data);
    }

    public void declineResponse(long taxitwinId) {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_DECLINE_RESPONSE);
        data.putString(GCM_DATA_TAXITWIN_ID, String.valueOf(taxitwinId));

        if (!isSubscribed() || !goodToGo) {
            Log.w(LOG, "cannot decline a response - missing service");
            return;
        }

        gcmConnector.send(data);
    }

    public void leaveTaxiTwin() {
        Bundle data = new Bundle();
        data.putString(GCM_DATA_TYPE, GCM_DATA_TYPE_LEAVE_TAXITWIN);

        if (!isSubscribed() || !goodToGo) {
            Log.w(LOG, "cannot decline a response - missing service");
            return;
        }

        subscribed = false;
        gcmConnector.send(data);
    }
}
