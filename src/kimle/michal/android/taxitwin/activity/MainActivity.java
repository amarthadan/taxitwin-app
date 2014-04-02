package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
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
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.MapsInitializer;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.db.DbHelper;
import kimle.michal.android.taxitwin.dialog.alert.GPSAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.GooglePlayServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.InternetAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.error.GooglePlayServicesErrorDialogFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinListFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinMapFragment;
import kimle.michal.android.taxitwin.gcm.GcmHandler;
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
    public static final String CATEGORY_NEW_DATA = "kimle.michal.android.taxitwin.CATEGORY_NEW_DATA";
    private static final String SAVED_NAVIGATION_POSITION = "savedNavigationPosition";
    private static final String SAVED_MAP_FRAGMENT = "savedMapFragment";
    private static final String SAVED_LIST_FRAGMENT = "savedListFragment";
    private LocationManager locationManager;
    private boolean gpsEnabled = false;
    private TaxiTwinMapFragment mapViewFragment;
    private TaxiTwinListFragment listViewFragment;
    private SettingsPopup settingsPopup;
    private MenuItem settingsMenuItem;
    private GcmHandler gcmHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.d(LOG, "savedInstanceState: " + savedInstanceState);

        settingsPopup = new SettingsPopup(this);
        settingsPopup.setOnDismissListener(new OnDismissListener() {

            public void onDismiss() {
                settingsMenuItem.setIcon(R.drawable.ic_action_expand);
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        MapsInitializer.initialize(getApplicationContext());
        gcmHandler = new GcmHandler(this);
        checkServices();
        buildGUI(savedInstanceState);

        Log.d(LOG, "end of onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServices();
    }

    private void buildGUI(Bundle savedInstanceState) {
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item);

        FragmentManager fm = getFragmentManager();

        if (savedInstanceState != null) {
            Log.d(LOG, "loading fragments form bundle...");
            mapViewFragment = (TaxiTwinMapFragment) fm.getFragment(savedInstanceState, SAVED_MAP_FRAGMENT);
            listViewFragment = (TaxiTwinListFragment) fm.getFragment(savedInstanceState, SAVED_LIST_FRAGMENT);
        } else {
            if (mapViewFragment == null) {
                mapViewFragment = new TaxiTwinMapFragment();
            }
            if (listViewFragment == null) {
                listViewFragment = new TaxiTwinListFragment();
            }
        }

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.main_fragment, mapViewFragment, "Map view");
        ft.add(R.id.main_fragment, listViewFragment, "List view");
        ft.hide(listViewFragment);

        ft.commit();
        fm.executePendingTransactions();

        ActionBar actionBar = getActionBar();
        actionBar.setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                if (position == LIST_VIEW_POSITION) {
                    ft.hide(mapViewFragment);
                    ft.show(listViewFragment);
                    listViewFragment.updateView();
                    Log.d(LOG, "in if, position:" + position);
                } else if (position == MAP_VIEW_POSITION) {
                    ft.hide(listViewFragment);
                    ft.show(mapViewFragment);
                    mapViewFragment.loadData();
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
        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SAVED_NAVIGATION_POSITION));
        }

        addWaitForGPSSignal();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(LOG, "in onSaveInstanceState");
        savedInstanceState.putInt(SAVED_NAVIGATION_POSITION, getActionBar().getSelectedNavigationIndex());
        getFragmentManager().putFragment(savedInstanceState, SAVED_MAP_FRAGMENT, mapViewFragment);
        getFragmentManager().putFragment(savedInstanceState, SAVED_LIST_FRAGMENT, listViewFragment);
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

        checkServices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        settingsMenuItem = menu.findItem(R.id.action_show_options);
        if (!settingsPopup.hasAddress()) {
            settingsMenuItem.setIcon(R.drawable.ic_action_collapse);
            settingsPopup.showAsDropDown(getActionBarView());
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_options:
                if (settingsPopup.isShowing()) {
                    settingsMenuItem.setIcon(R.drawable.ic_action_expand);
                    settingsPopup.dismiss();
                } else {
                    settingsMenuItem.setIcon(R.drawable.ic_action_collapse);
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

    private boolean checkGooglePlayServices() {
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
            DialogFragment errorFragment = new GooglePlayServicesErrorDialogFragment();
            errorFragment.show(getFragmentManager(), "google_play_services_error");
            return false;
        }
    }

    public void onDialogNeutralClick(DialogFragment dialog) {
        exit();
    }

    private void exit() {
        gcmHandler.unsubscribe();
        DbHelper dbHelper = new DbHelper(this);
        dbHelper.deleteTables(dbHelper.getWritableDatabase());
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private boolean checkGPS() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (enabled) {
            gpsEnabled = true;
            Log.d(LOG, "GPS is enabled");
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

    public void onDialogNegativeClick(DialogFragment dialog) {
        exit();
    }

    private boolean checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Log.d(LOG, "connected to the Internet");
            return true;
        } else {
            DialogFragment alertFragment = new InternetAlertDialogFragment();
            alertFragment.show(getFragmentManager(), "internet_alert");
            return false;
        }
    }

    private void checkServices() {
        gcmHandler.setGoodToGo(checkGooglePlayServices() && checkGPS() && checkInternet());
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
                mapViewFragment.updateCurrentLocation(location);
                gcmHandler.locationChanged(location);
                removeWaitForGPSSignal();
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
                checkServices();
                Log.d(LOG, "onProviderDisabled");
            }
        };
        //every 10 seconds and at least 3 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, locationListener);
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
        if (gpsEnabled) {
            requestLocationChanges();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Log.d(LOG, "in onNewIntent");

        if (intent.hasCategory(CATEGORY_NEW_DATA)) {
            notifyNewData();
        }
    }

    private void notifyNewData() {
        listViewFragment.updateView();
        mapViewFragment.loadData();
    }
}
