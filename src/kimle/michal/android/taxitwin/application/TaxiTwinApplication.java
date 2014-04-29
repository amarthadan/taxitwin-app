package kimle.michal.android.taxitwin.application;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import kimle.michal.android.taxitwin.db.DbHelper;
import kimle.michal.android.taxitwin.enumerate.UserState;
import static kimle.michal.android.taxitwin.enumerate.UserState.NOT_SUBSCRIBED;
import kimle.michal.android.taxitwin.gcm.GcmHandler;
import kimle.michal.android.taxitwin.services.ServicesManagement;

public class TaxiTwinApplication extends Application {

    private static int pendingNotificationsCount = 0;
    private static UserState userState = NOT_SUBSCRIBED;
    private ServicesManagement servicesManagement;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void unregister() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
    }

    public void register() {
        servicesManagement = new ServicesManagement(this);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                locationUpdate(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, this);
            }

            public void onProviderDisabled(String provider) {
            }
        };
        //every 10 seconds and at least 3 meters
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, locationListener);
    }

    private void locationUpdate(Location location) {
        GcmHandler gcmHandler = new GcmHandler(this);
        gcmHandler.locationChanged(location);
    }

    public static int getPendingNotificationsCount() {
        return pendingNotificationsCount;
    }

    public static void setPendingNotificationsCount(int pendingNotifications) {
        pendingNotificationsCount = pendingNotifications;
    }

    public static UserState getUserState() {
        return userState;
    }

    public static void setUserState(UserState userState) {
        TaxiTwinApplication.userState = userState;
    }

    public static void exit(Context context) {
        GcmHandler gcmHandler = new GcmHandler(context);
        if (userState == UserState.OWNER || userState == UserState.PARTICIPANT) {
            gcmHandler.leaveTaxiTwin();
        }
        gcmHandler.unsubscribe();

        TaxiTwinApplication app = (TaxiTwinApplication) context.getApplicationContext();
        app.unregister();

        DbHelper dbHelper = new DbHelper(context);
        dbHelper.deleteTables(dbHelper.getWritableDatabase());
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
