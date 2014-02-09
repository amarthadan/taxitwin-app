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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.dialog.GPSAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.GooglePlayServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.GooglePlayServicesErrorDialogFragment;
import kimle.michal.android.taxitwin.dialog.InternetAlertDialogFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinListFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinMapFragment;

public class MainActivity extends Activity implements
        GooglePlayServicesErrorDialogFragment.GooglePlayServicesErrorDialogListener,
        GPSAlertDialogFragment.GPSAlertDialogListener,
        InternetAlertDialogFragment.InternetAlertDialogListener {

    private static final String LOG = "MainActivity";
    private static final int MAP_VIEW_POSITION = 0;
    private static final int LIST_VIEW_POSITION = 1;
    private static final int PLAY_SERVICES_REQUEST = 9000;
    private static final int GPS_REQUEST = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        checkGooglePlayServices();
        checkGPS();
        checkInternet();

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
                    TaxiTwinListFragment ttlf = new TaxiTwinListFragment();
                    ft.replace(R.id.main_fragment, ttlf, strings[position]);
                    Log.d(LOG, "in if, position:" + position);
                } else if (position == MAP_VIEW_POSITION) {
                    TaxiTwinMapFragment ttmf = new TaxiTwinMapFragment();
                    ft.replace(R.id.main_fragment, ttmf, strings[position]);
                    Log.d(LOG, "in else, position:" + position);
                }
                ft.commit();
                return true;
            }
        });
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        Log.d(LOG, "end of onCreate");
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
}
