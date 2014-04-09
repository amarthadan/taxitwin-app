package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import static android.content.Context.LOCATION_SERVICE;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;
import kimle.michal.android.taxitwin.db.DbHelper;
import kimle.michal.android.taxitwin.dialog.alert.GPSAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.GooglePlayServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.InternetAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.LeaveTaxiTwinAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.TaxiTwinAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.TaxiTwinNoLongerAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.error.GooglePlayServicesErrorDialogFragment;
import kimle.michal.android.taxitwin.gcm.GcmIntentService;

public class MyTaxiTwinActivity extends Activity implements
        GooglePlayServicesErrorDialogFragment.GooglePlayServicesErrorDialogListener,
        GPSAlertDialogFragment.GPSAlertDialogListener,
        InternetAlertDialogFragment.InternetAlertDialogListener {

    private static final String LOG = "MyTaxiTwinActivity";
    public static final String CATEGORY_TAXITWIN_OWNER = "kimle.michal.android.taxitwin.CATEGORY_TAXITWIN_OWNER";
    public static final String CATEGORY_TAXITWIN_DATA_CHANGED = "kimle.michal.android.taxitwin.CATEGORY_TAXITWIN_DATA_CHANGED";
    public static final String CATEGORY_TAXITWIN_NO_LONGER = "kimle.michal.android.taxitwin.CATEGORY_TAXITWIN_NO_LONGER";
    private boolean owner = false;
    private MenuItem responsesMenuItem;
    private BroadcastReceiver broadcastReceiver;
    private static final int PADDING = 60;
    private LatLng start;
    private LatLng end;
    private boolean mapReady = false;
    private List<Marker> markers;
    private GoogleMap map;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean gpsEnabled = false;
    private static final int PLAY_SERVICES_REQUEST = 9000;
    private static final int GPS_REQUEST = 10000;
    private Geocoder geocoder;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.taxitwin);

        if (getIntent().hasCategory(CATEGORY_TAXITWIN_OWNER)) {
            owner = true;
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        geocoder = new Geocoder(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED)
                        || intent.hasCategory(MainActivity.CATEGORY_OFFER_DATA_CHANGED)) {
                    fillData();
                }
                if (intent.hasCategory(CATEGORY_TAXITWIN_NO_LONGER)) {
                    showTaxiTwinNoLongerDialog();
                }
            }
        };

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

        fillData();

        MapsInitializer.initialize(getApplicationContext());
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mapReady = true;
                if (gpsEnabled) {
                    requestLocationChanges();
                }
                fillMap();
            }
        });

        checkServices();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GcmIntentService.NOTIFICATION_TAXITWIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapReady = false;
        fillData();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GcmIntentService.ACTION_TAXITWIN);
        intentFilter.addCategory(CATEGORY_TAXITWIN_DATA_CHANGED);
        intentFilter.addCategory(CATEGORY_TAXITWIN_NO_LONGER);
        intentFilter.addCategory(MainActivity.CATEGORY_OFFER_DATA_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
        checkServices();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        //locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkServices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.taxitwin, menu);
        responsesMenuItem = menu.findItem(R.id.action_leave_taxitwin);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_leave_taxitwin:
                DialogFragment alertFragment = new LeaveTaxiTwinAlertDialogFragment();
                alertFragment.show(getFragmentManager(), "leave_taxitwin_alert");
                return true;
            case R.id.action_responses:
                Intent i = new Intent(this, ResponsesActivity.class);
                startActivity(i);
                return true;
            case R.id.action_exit:
                exit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        responsesMenuItem.setVisible(owner);
        return true;
    }

    private boolean checkGooglePlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Dialog alertDialog = GooglePlayServicesUtil.getErrorDialog(
                        resultCode,
                        this,
                        PLAY_SERVICES_REQUEST);
                if (alertDialog != null) {
                    GooglePlayServicesAlertDialogFragment alertFragment = new GooglePlayServicesAlertDialogFragment();
                    alertFragment.setDialog(alertDialog);
                    alertFragment.show(getFragmentManager(), "google_play_services_alert");
                }
            }
            DialogFragment errorFragment = new GooglePlayServicesErrorDialogFragment();
            errorFragment.show(getFragmentManager(), "google_play_services_error");
            return false;
        }
    }

    private boolean checkGPS() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (enabled) {
            gpsEnabled = true;
            return true;
        } else {
            DialogFragment alertFragment = new GPSAlertDialogFragment();
            alertFragment.show(getFragmentManager(), "gps_alert");
            return false;
        }
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        if (dialog instanceof GPSAlertDialogFragment) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, GPS_REQUEST);
        }
        if (dialog instanceof InternetAlertDialogFragment) {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
        }
    }

    public void onDialogNeutralClick(DialogFragment dialog) {
        exit();
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        exit();
    }

    private void exit() {
        TaxiTwinApplication.getGcmHandler().leaveTaxiTwin();
        TaxiTwinApplication.getGcmHandler().unsubscribe();
        DbHelper dbHelper = new DbHelper(this);
        dbHelper.deleteTables(dbHelper.getWritableDatabase());
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private boolean checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            return true;
        } else {
            DialogFragment alertFragment = new InternetAlertDialogFragment();
            alertFragment.show(getFragmentManager(), "internet_alert");
            return false;
        }
    }

    private void checkServices() {
        TaxiTwinApplication.getGcmHandler().setGoodToGo(checkGooglePlayServices() && checkGPS() && checkInternet());
    }

    private void requestLocationChanges() {
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                String addressString = null;
                if (Geocoder.isPresent()) {
                    Log.d(LOG, "geocoder present");
                    try {
                        Address address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
                        addressString = address.getAddressLine(0);
                        for (int i = 1; i <= address.getMaxAddressLineIndex(); i++) {
                            addressString = addressString + ", " + address.getAddressLine(i);
                        }
                    } catch (IOException ex) {
                        Log.e(LOG, ex.getMessage());
                    }
                }

                ContentValues values = new ContentValues();
                values.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, location.getLatitude());
                values.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, location.getLongitude());
                values.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, addressString);
                long startId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, values));

                values = new ContentValues();
                values.put(DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN, startId);

                String[] projection = {DbContract.DbEntry.TAXITWIN_ID_COLUMN};
                long taxitwinId;
                Cursor cursor = getContentResolver().query(TaxiTwinContentProvider.RIDES_URI, projection, null, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    taxitwinId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));
                    Uri uri = Uri.parse(TaxiTwinContentProvider.TAXITWINS_URI + "/" + taxitwinId);
                    getContentResolver().update(uri, values, null, null);

                    cursor.close();
                }

                fillData();
                TaxiTwinApplication.getGcmHandler().locationChanged(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
                gpsEnabled = true;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, this);
                checkServices();
            }

            public void onProviderDisabled(String provider) {
                gpsEnabled = false;
                locationManager.removeUpdates(this);
                checkServices();
            }
        };
        //every 10 seconds and at least 3 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, locationListener);
    }

    private void fillData() {
        String[] projection = {
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN,
            DbContract.DbEntry.OFFER_PASSENGERS_COLUMN};

        Cursor cursor = getContentResolver().query(TaxiTwinContentProvider.RIDES_URI, projection, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();

            ((TextView) findViewById(R.id.name_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.TAXITWIN_NAME_COLUMN)));
            ((TextView) findViewById(R.id.start_address_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN)));
            ((TextView) findViewById(R.id.end_address_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN)));
            String passengersText = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry.OFFER_PASSENGERS_COLUMN)) + "/" + cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN));
            ((TextView) findViewById(R.id.passengers_content)).setText(passengersText);

            start = new LatLng(cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN)), cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN)));
            end = new LatLng(cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN)), cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN)));

            cursor.close();
        }

        if (mapReady) {
            fillMap();
        }
    }

    private void fillMap() {
        if (start == null || end == null) {
            return;
        }

        if (markers != null) {
            for (Marker m : markers) {
                m.remove();
            }
        }

        markers = new ArrayList<Marker>();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(start);
        if (owner) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
            markerOptions.anchor(0.14f, 0.67f);
        } else {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
            markerOptions.anchor(0.34f, 0.92f);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions otherMarkerOptions = new MarkerOptions();
            otherMarkerOptions.position(current);
            otherMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
            otherMarkerOptions.anchor(0.14f, 0.67f);
            markers.add(map.addMarker(otherMarkerOptions));
        }
        markers.add(map.addMarker(markerOptions));

        markerOptions.position(end);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
        markerOptions.anchor(0.11f, 0.93f);
        markers.add(map.addMarker(markerOptions));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }

        LatLngBounds bounds = builder.build();

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, PADDING));
    }

    private void showTaxiTwinNoLongerDialog() {
        DialogFragment alertFragment = new TaxiTwinNoLongerAlertDialogFragment();
        alertFragment.show(getFragmentManager(), "taxitwin_no_longer_alert");
    }

    public static boolean isInTaxiTwin(Context context) {
        String[] projection = {DbContract.DbEntry.OFFER_ID_COLUMN};
        Cursor cursor = context.getContentResolver().query(TaxiTwinContentProvider.RIDES_URI, projection, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.close();
            return true;
        }
        return false;
    }

    public static void showTaxiTwinDialog(Activity activity) {
        DialogFragment alertFragment = new TaxiTwinAlertDialogFragment();
        alertFragment.show(activity.getFragmentManager(), "taxitwin_alert");
    }
}
