package kimle.michal.android.taxitwin.application;

import android.app.Application;
import android.content.Context;
import kimle.michal.android.taxitwin.db.DbHelper;
import kimle.michal.android.taxitwin.enumerate.UserState;
import static kimle.michal.android.taxitwin.enumerate.UserState.NO_RIDE;
import kimle.michal.android.taxitwin.gcm.GcmHandler;
import kimle.michal.android.taxitwin.services.ServicesManagement;

public class TaxiTwinApplication extends Application {

    private static int pendingNotificationsCount = 0;
    private static GcmHandler gcmHandler;
    private static UserState userState = NO_RIDE;
    private ServicesManagement servicesManagement;

    @Override
    public void onCreate() {
        super.onCreate();
        gcmHandler = new GcmHandler(this);
        servicesManagement = new ServicesManagement(this);
    }

    public static int getPendingNotificationsCount() {
        return pendingNotificationsCount;
    }

    public static void setPendingNotificationsCount(int pendingNotifications) {
        pendingNotificationsCount = pendingNotifications;
    }

    public static GcmHandler getGcmHandler() {
        return gcmHandler;
    }

    public static UserState getUserState() {
        return userState;
    }

    public static void setUserState(UserState userState) {
        TaxiTwinApplication.userState = userState;
    }

    public static void exit(Context context) {
        if (userState != UserState.NO_RIDE) {
            TaxiTwinApplication.getGcmHandler().leaveTaxiTwin();
        }
        TaxiTwinApplication.getGcmHandler().unsubscribe();
        DbHelper dbHelper = new DbHelper(context);
        dbHelper.deleteTables(dbHelper.getWritableDatabase());
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
