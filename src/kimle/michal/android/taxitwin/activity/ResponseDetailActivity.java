package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skd.centeredcontentbutton.CenteredContentButton;
import java.util.ArrayList;
import java.util.List;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;
import kimle.michal.android.taxitwin.dialog.alert.ServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.error.ResponseErrorDialogFragment;
import kimle.michal.android.taxitwin.enumerate.UserState;
import kimle.michal.android.taxitwin.gcm.GcmHandler;
import kimle.michal.android.taxitwin.gcm.GcmIntentService;
import kimle.michal.android.taxitwin.services.ServicesManagement;

public class ResponseDetailActivity extends Activity {

    public static final String RESPONSE_ID = "response_id";
    private Uri responseUri;
    private static final int PADDING = 60;
    private LatLng start;
    private LatLng end;
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.response_detail);

        Bundle extras = getIntent().getExtras();
        Uri taskUri = (icicle == null) ? null : (Uri) icicle.getParcelable(TaxiTwinContentProvider.RESPONSE_CONTENT_ITEM_TYPE);
        if (extras != null) {
            taskUri = extras.getParcelable(TaxiTwinContentProvider.RESPONSE_CONTENT_ITEM_TYPE);
        }
        fillData(taskUri);
        responseUri = taskUri;

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED)) {
                    showTaxiTwinDialog();
                }
                if (intent.hasCategory(ServicesManagement.CATEGORY_GPS_DISABLED)) {
                    DialogFragment alertFragment = new ServicesAlertDialogFragment(R.string.services_gps_alert_message);
                    alertFragment.show(getFragmentManager(), "gps_alert");
                }
                if (intent.hasCategory(ServicesManagement.CATEGORY_NETWORK_DISABLED)) {
                    DialogFragment alertFragment = new ServicesAlertDialogFragment(R.string.services_network_alert_message);
                    alertFragment.show(getFragmentManager(), "network_alert");
                }
            }
        };

        MapsInitializer.initialize(getApplicationContext());

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        final GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                List<Marker> markers = new ArrayList<Marker>();

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(start);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                markerOptions.anchor(0.34f, 0.92f);
                markers.add(map.addMarker(markerOptions));

                markerOptions.position(end);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                markerOptions.anchor(0.11f, 0.93f);
                markers.add(map.addMarker(markerOptions));

                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                markerOptions.position(current);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
                markerOptions.anchor(0.14f, 0.67f);
                markers.add(map.addMarker(markerOptions));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }

                LatLngBounds bounds = builder.build();

                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, PADDING));
            }
        });

        CenteredContentButton accept = (CenteredContentButton) findViewById(R.id.accept_button);
        accept.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String[] projection = {DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN};
                Cursor cursor = getContentResolver().query(responseUri, projection, null, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    long taxitwinId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN));

                    deleteResponse(taxitwinId);
                    acceptResponse(taxitwinId);

                    cursor.close();
                } else {
                    DialogFragment errorFragment = new ResponseErrorDialogFragment();
                    errorFragment.show(getFragmentManager(), "response_error");
                    setResult(RESULT_CANCELED, null);
                }

                finish();
            }
        });

        CenteredContentButton decline = (CenteredContentButton) findViewById(R.id.decline_button);
        decline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String[] projection = {DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN};
                Cursor cursor = getContentResolver().query(responseUri, projection, null, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();

                    long taxitwinId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN));

                    deleteResponse(taxitwinId);
                    declineResponse(taxitwinId);

                    cursor.close();
                }

                finish();
            }
        });

        TaxiTwinApplication.setPendingNotificationsCount(0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GcmIntentService.NOTIFICATION_RESPONSE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MyTaxiTwinActivity.isInTaxiTwin(this)) {
            showTaxiTwinDialog();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GcmIntentService.ACTION_TAXITWIN);
        intentFilter.addCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED);
        intentFilter.addCategory(ServicesManagement.CATEGORY_GPS_DISABLED);
        intentFilter.addCategory(ServicesManagement.CATEGORY_NETWORK_DISABLED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void fillData(Uri taskUri) {
        String[] projection = {
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN};

        Cursor cursor = getContentResolver().query(taskUri, projection, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();

            ((TextView) findViewById(R.id.name_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.TAXITWIN_NAME_COLUMN)));
            ((TextView) findViewById(R.id.start_address_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN)));
            ((TextView) findViewById(R.id.end_address_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN)));

            start = new LatLng(cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN)), cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN)));
            end = new LatLng(cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN)), cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN)));

            cursor.close();
        } else {
            finish();
        }
    }

    private void showTaxiTwinDialog() {
        MyTaxiTwinActivity.showTaxiTwinDialog(this);
    }

    private void deleteResponse(long taxitwinId) {
        String[] projection = {DbContract.DbEntry.RESPONSE_ID_COLUMN};
        String selection = DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN + " = ?";
        String[] selectionArgs = {String.valueOf(taxitwinId)};

        Cursor cursor = getContentResolver().query(TaxiTwinContentProvider.RESPONSES_URI, projection, selection, selectionArgs, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            long responseId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));
            Uri uri = Uri.parse(TaxiTwinContentProvider.RESPONSES_URI + "/" + responseId);
            getContentResolver().delete(uri, null, null);

            cursor.close();
        }
    }

    private void acceptResponse(long taxitwinId) {
        GcmHandler gcmHandler = new GcmHandler(this);
        gcmHandler.acceptResponse(taxitwinId);

        Intent intent = new Intent(this, MyTaxiTwinActivity.class);
        TaxiTwinApplication.setUserState(UserState.OWNER);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void declineResponse(long taxitwinId) {
        GcmHandler gcmHandler = new GcmHandler(this);
        gcmHandler.declineResponse(taxitwinId);
    }
}
