package kimle.michal.android.taxitwin.application;

import android.app.Application;
import kimle.michal.android.taxitwin.enumerate.UserState;
import static kimle.michal.android.taxitwin.enumerate.UserState.NO_RIDE;
import kimle.michal.android.taxitwin.gcm.GcmHandler;

public class TaxiTwinApplication extends Application {

    private static int pendingNotificationsCount = 0;
    private static GcmHandler gcmHandler;
    private static UserState userState = NO_RIDE;

    @Override
    public void onCreate() {
        super.onCreate();
        gcmHandler = new GcmHandler(this);
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
}
