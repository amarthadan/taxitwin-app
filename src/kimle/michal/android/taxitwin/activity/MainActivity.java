package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.dialog.GPSAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.GooglePlayServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.GooglePlayServicesErrorDialogFragment;
import kimle.michal.android.taxitwin.dialog.InternetAlertDialogFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinListFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinMapFragment;
import kimle.michal.android.taxitwin.popup.SettingsPopup;

public class MainActivity extends Activity implements
        GooglePlayServicesErrorDialogFragment.GooglePlayServicesErrorDialogListener,
        GPSAlertDialogFragment.GPSAlertDialogListener,
        InternetAlertDialogFragment.InternetAlertDialogListener,
        TaxiTwinMapFragment.MapViewListener {

    private static final String LOG = "MainActivity";
    private static final int MAP_VIEW_POSITION = 0;
    private static final int LIST_VIEW_POSITION = 1;
    private static final int PLAY_SERVICES_REQUEST = 9000;
    private static final int GPS_REQUEST = 10000;
    private Location currentLocation = null;
    private LocationManager locationManager;
    private GoogleMap map;
    private boolean gpsEnabled = false;
    private TaxiTwinMapFragment mapViewFragment;
    private TaxiTwinListFragment listViewFragment;
    private SettingsPopup settingsPopup;
    private MenuItem settingsMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mapViewFragment = new TaxiTwinMapFragment();
        listViewFragment = new TaxiTwinListFragment();
        settingsPopup = new SettingsPopup(this);
        settingsPopup.setOnDismissListener(new OnDismissListener() {

            public void onDismiss() {
                settingsMenuItem.setIcon(R.drawable.ic_action_expand);
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (GooglePlayServicesNotAvailableException ex) {
            Log.e(LOG, ex.toString());
        }

        checkServices();
        buildGUI();

        if (currentLocation == null) {
            addWaitForGPSSignal();
        }

        //TODO: check if there is a home address set and if not force seting it
        Log.d(LOG, "end of onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServices();
    }

    private void buildGUI() {
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item);

        ActionBar actionBar = getActionBar();
        actionBar.setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
            String[] strings = getResources().getStringArray(R.array.action_list);

            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                FragmentManager fm = getFragmentManager();
                Fragment f = fm.findFragmentById(R.id.main_fragment);

                FragmentTransaction ft = fm.beginTransaction();
                if (f != null) {
                    ft.hide(f);
                }
                if (position == LIST_VIEW_POSITION) {
                    ft.replace(R.id.main_fragment, listViewFragment, strings[position]);
                    Log.d(LOG, "in if, position:" + position);
                } else if (position == MAP_VIEW_POSITION) {
                    ft.replace(R.id.main_fragment, mapViewFragment, strings[position]);
                    Log.d(LOG, "in else, position:" + position);
                }
                ft.commit();
                fm.executePendingTransactions();

                return true;
            }
        });
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PLAY_SERVICES_REQUEST:
                Log.d(LOG, "result of google play services request");
                checkGooglePlayServices();
                break;
            case GPS_REQUEST:
                Log.d(LOG, "result of gps request");
                checkGPS();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_options:
                if (settingsPopup.isShowing()) {
                    item.setIcon(R.drawable.ic_action_expand);
                    settingsPopup.dismiss();
                } else {
                    item.setIcon(R.drawable.ic_action_collapse);
                    settingsMenuItem = item;
                    settingsPopup.showAsDropDown(getActionBarView());
                    //WindowManager.LayoutParams p = (WindowManager.LayoutParams) settingsPopup.getContentView().getLayoutParams();
                    //p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                    //WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    //windowManager.updateViewLayout(settingsPopup.getContentView(), p);
                }
                return true;
            case R.id.action_responses:
                return true;
            case R.id.action_exit:
                exit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private View getActionBarView() {
        Window window = getWindow();
        View v = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", "android");
        return v.findViewById(resId);
    }

    private void checkGooglePlayServices() {
        if (!googlePlayServicesAvailable()) {
            DialogFragment errorFragment = new GooglePlayServicesErrorDialogFragment();
            errorFragment.show(getFragmentManager(), "google_play_services_error");
        }
    }

    private boolean googlePlayServicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(LOG, "Google Play services is available");
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
                    alertFragment.show(getFragmentManager(), "Google Play Services alert");
                }
            }
            return false;
        }
    }

    public void onDialogNeutralClick(DialogFragment dialog) {
        exit();
    }

    private void exit() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void checkGPS() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (enabled) {
            gpsEnabled = true;
            Log.d(LOG, "GPS is enabled");
        } else {
            DialogFragment alertFragment = new GPSAlertDialogFragment();
            alertFragment.show(getFragmentManager(), "gps_alert");
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

    public void onDialogNegativeClick(DialogFragment dialog) {
        exit();
    }

    private void checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Log.d(LOG, "connected to the Internet");
        } else {
            DialogFragment alertFragment = new InternetAlertDialogFragment();
            alertFragment.show(getFragmentManager(), "internet_alert");
        }
    }

    private void checkServices() {
        checkGooglePlayServices();
        checkGPS();
        checkInternet();
    }

    private void requestLocationChanges() {
//        LocationRequest lr = LocationRequest.create();
//        lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        //every 20 seconds
//        lr.setInterval(20000);
//        //every 5 seconds top
//        lr.setFastestInterval(5000);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the gps location provider.
                updateCurrentLocation(location);
                Log.d(LOG, "onLocationChanged");
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
                gpsEnabled = true;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, this);
                Log.d(LOG, "onProviderEnabled");
            }

            public void onProviderDisabled(String provider) {
                gpsEnabled = false;
                locationManager.removeUpdates(this);
                checkGPS();
                Log.d(LOG, "onProviderDisabled");
            }
        };
        //every 10 seconds and at least 3 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, locationListener);
    }

    private void updateCurrentLocation(Location location) {
        if (currentLocation == null) {
            removeWaitForGPSSignal();
        }

        currentLocation = location;
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        MarkerOptions currentMarkerOptions = new MarkerOptions();
        currentMarkerOptions.position(currentLatLng);
        currentMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
        currentMarkerOptions.anchor(0.14f, 0.67f);
        Marker currentMarker = map.addMarker(currentMarkerOptions);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 8));
        map.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
    }

    private void addWaitForGPSSignal() {
        TextView bottomText = (TextView) findViewById(R.id.bottom_text);
        bottomText.setText(R.string.waiting_for_signal);
        bottomText.setTextColor(Color.GRAY);
    }

    private void removeWaitForGPSSignal() {
        TextView bottomText = (TextView) findViewById(R.id.bottom_text);
        bottomText.setText("");
    }

    public void onMapCreated() {
        map = mapViewFragment.getMap();
        Log.d(LOG, "map: " + map);

        if (gpsEnabled) {
            requestLocationChanges();
        }
    }
}
