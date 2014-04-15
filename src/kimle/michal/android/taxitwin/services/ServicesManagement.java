package kimle.michal.android.taxitwin.services;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import static android.content.Context.LOCATION_SERVICE;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import kimle.michal.android.taxitwin.dialog.alert.GPSAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.GooglePlayServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.alert.InternetAlertDialogFragment;
import kimle.michal.android.taxitwin.dialog.error.GooglePlayServicesErrorDialogFragment;
import static kimle.michal.android.taxitwin.gcm.GcmIntentService.ACTION_TAXITWIN;

public class ServicesManagement {

    private static final String LOG = "ServicesManagement";
    public static final String CATEGORY_GPS_ENABLED = "kimle.michal.android.taxitwin.CATEGORY_GPS_ENABLED";
    public static final String CATEGORY_GPS_DISABLED = "kimle.michal.android.taxitwin.CATEGORY_GPS_DISABLED";
    public static final String CATEGORY_NETWORK_ENABLED = "kimle.michal.android.taxitwin.CATEGORY_NETWORK_ENABLED";
    public static final String CATEGORY_NETWORK_DISABLED = "kimle.michal.android.taxitwin.CATEGORY_NETWORK_DISABLED";
    private static final int PLAY_SERVICES_REQUEST = 9000;
    private final Context context;

    public ServicesManagement(Context c) {
        this.context = c;

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
                Log.d(LOG, "in onProviderEnabled, provider: " + provider);
                if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    Intent intent = new Intent(ACTION_TAXITWIN);
                    intent.addCategory(CATEGORY_GPS_ENABLED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }

            public void onProviderDisabled(String provider) {
                Log.d(LOG, "in onProviderDisabled, provider: " + provider);
                if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    Intent intent = new Intent(ACTION_TAXITWIN);
                    intent.addCategory(CATEGORY_GPS_DISABLED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        };

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, locationListener);

        BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Intent i = new Intent(ACTION_TAXITWIN);
                if (checkNetwork(context)) {
                    i.addCategory(CATEGORY_NETWORK_ENABLED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                } else {
                    i.addCategory(CATEGORY_NETWORK_DISABLED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkStatusReceiver, filter);
    }

    private static boolean checkNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private static boolean checkGps(Context context) {
        LocationManager service = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private static void checkGooglePlayServices(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (ConnectionResult.SUCCESS != resultCode) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Dialog alertDialog = GooglePlayServicesUtil.getErrorDialog(
                        resultCode,
                        activity,
                        PLAY_SERVICES_REQUEST);
                if (alertDialog != null) {
                    GooglePlayServicesAlertDialogFragment alertFragment = new GooglePlayServicesAlertDialogFragment();
                    alertFragment.setDialog(alertDialog);
                    alertFragment.show(activity.getFragmentManager(), "google_play_services_alert");
                }
            } else {
                DialogFragment errorFragment = new GooglePlayServicesErrorDialogFragment();
                errorFragment.show(activity.getFragmentManager(), "google_play_services_error");
            }
        }
    }

    public static void initialCheck(Activity activity) {
        if (!checkNetwork(activity)) {
            DialogFragment alertFragment = new InternetAlertDialogFragment();
            alertFragment.show(activity.getFragmentManager(), "internet_alert");
        }
        if (!checkGps(activity)) {
            DialogFragment alertFragment = new GPSAlertDialogFragment();
            alertFragment.show(activity.getFragmentManager(), "gps_alert");
        }
        checkGooglePlayServices(activity);
    }

    public static boolean checkServices(Context context) {
        return checkGps(context) && checkNetwork(context);
    }
}
