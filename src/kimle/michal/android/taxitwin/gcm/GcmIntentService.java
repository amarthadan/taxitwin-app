package kimle.michal.android.taxitwin.gcm;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import java.util.List;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.MainActivity;
import kimle.michal.android.taxitwin.activity.ResponseDetailActivity;
import kimle.michal.android.taxitwin.activity.ResponsesActivity;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;

public class GcmIntentService extends IntentService {

    private static final String LOG = "GcmIntentService";
    public static final String ACTION_TAXITWIN = "kimle.michal.android.taxitwin.ACTION_TAXITWIN";

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG, "in onHandleIntent");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.w(LOG, "gcm obtained send error mesage");
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.w(LOG, "gcm obtained deleted messages mesage");
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.d(LOG, "message: " + extras.toString());
                if (extras.containsKey(GcmHandler.GCM_DATA_TYPE)) {
                    String type = extras.getString(GcmHandler.GCM_DATA_TYPE);
                    if (type.equals(GcmHandler.GCM_DATA_TYPE_OFFER)) {
                        offerReceived(extras);
                    }
                    if (type.equals(GcmHandler.GCM_DATA_TYPE_INVALIDATE)) {
                        taxitwinInvalidate(extras);
                    }
                    if (type.equals(GcmHandler.GCM_DATA_TYPE_MODIFY)) {
                        offerModify(extras);
                    }
                    if (type.equals(GcmHandler.GCM_DATA_TYPE_RESPONSE)) {
                        responseReceived(extras);
                    }
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void offerReceived(Bundle extras) {
        ContentValues values = new ContentValues();
        //storing start point
        values.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_LATITUDE));
        values.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_LONGITUDE));
        values.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_TEXTUAL));
        long startId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, values));

        //storing end point
        values = new ContentValues();
        values.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_LATITUDE));
        values.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_LONGITUDE));
        values.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_TEXTUAL));
        long endId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, values));

        //storing taxitwin
        values = new ContentValues();
        values.put(DbContract.DbEntry._ID, extras.getString(GcmHandler.GCM_DATA_ID));
        values.put(DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN, startId);
        values.put(DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN, endId);
        values.put(DbContract.DbEntry.TAXITWIN_NAME_COLUMN, extras.getString(GcmHandler.GCM_DATA_NAME));
        getContentResolver().insert(TaxiTwinContentProvider.TAXITWINS_URI, values);

        //storing offer
        values = new ContentValues();
        values.put(DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN, extras.getString(GcmHandler.GCM_DATA_ID));
        values.put(DbContract.DbEntry.OFFER_PASSENGERS_COLUMN, extras.getString(GcmHandler.GCM_DATA_PASSENGERS));
        values.put(DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_PASSENGERS_TOTAL));
        getContentResolver().insert(TaxiTwinContentProvider.OFFERS_URI, values);

        Intent intent = new Intent(ACTION_TAXITWIN);
        intent.addCategory(MainActivity.CATEGORY_OFFER_DATA_CHANGED);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void taxitwinInvalidate(Bundle extras) {
        String taxitwinId = extras.getString(GcmHandler.GCM_DATA_ID);
        String[] projection = {
            DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN,
            DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN};
        Uri uri = Uri.parse(TaxiTwinContentProvider.TAXITWINS_URI + "/" + extras.getString(GcmHandler.GCM_DATA_ID));

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();

            uri = Uri.parse(TaxiTwinContentProvider.TAXITWINS_URI + "/" + taxitwinId);
            getContentResolver().delete(uri, null, null);

            int pointId = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN));
            uri = Uri.parse(TaxiTwinContentProvider.POINTS_URI + "/" + pointId);
            getContentResolver().delete(uri, null, null);

            pointId = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN));
            uri = Uri.parse(TaxiTwinContentProvider.POINTS_URI + "/" + pointId);
            getContentResolver().delete(uri, null, null);

            cursor.close();
        }

        Intent intent = new Intent(ACTION_TAXITWIN);
        intent.addCategory(MainActivity.CATEGORY_OFFER_DATA_CHANGED);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void offerModify(Bundle extras) {
        ContentValues values = new ContentValues();
        //modify start position
        if (extras.containsKey(GcmHandler.GCM_DATA_START_LATITUDE)) {
            ContentValues startValues = new ContentValues();
            startValues.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_LATITUDE));
            startValues.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_LONGITUDE));
            startValues.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_TEXTUAL));
            long startId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, startValues));
            values.put(DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN, startId);
        }
        //modify end position
        if (extras.containsKey(GcmHandler.GCM_DATA_END_LATITUDE)) {
            ContentValues endValues = new ContentValues();
            endValues.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_LATITUDE));
            endValues.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_LONGITUDE));
            endValues.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_TEXTUAL));
            long endId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, endValues));
            values.put(DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN, endId);
        }

        if (values.size() != 0) {
            Uri uri = Uri.parse(TaxiTwinContentProvider.TAXITWINS_URI + "/" + extras.getString(GcmHandler.GCM_DATA_ID));
            getContentResolver().update(uri, values, null, null);
        }

        values = new ContentValues();
        //modify number of passengers
        if (extras.containsKey(GcmHandler.GCM_DATA_PASSENGERS)) {
            values.put(DbContract.DbEntry.OFFER_PASSENGERS_COLUMN, extras.getString(GcmHandler.GCM_DATA_PASSENGERS));
        }
        //modify number of total passengers
        if (extras.containsKey(GcmHandler.GCM_DATA_PASSENGERS_TOTAL)) {
            values.put(DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_PASSENGERS_TOTAL));
        }

        if (values.size() != 0) {
            String[] projection = {DbContract.DbEntry.OFFER_ID_COLUMN};
            String selection = DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN + " = ?";
            String[] selectionArgs = {extras.getString(GcmHandler.GCM_DATA_ID)};

            Cursor cursor = getContentResolver().query(TaxiTwinContentProvider.OFFERS_URI, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                int offerId = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));

                Uri uri = Uri.parse(TaxiTwinContentProvider.OFFERS_URI + "/" + offerId);
                getContentResolver().update(uri, values, null, null);

                cursor.close();
            }
        }

        Intent intent = new Intent(ACTION_TAXITWIN);
        intent.addCategory(MainActivity.CATEGORY_OFFER_DATA_CHANGED);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void responseReceived(Bundle extras) {
        String taxitwinId = extras.getString(GcmHandler.GCM_DATA_ID);
        String[] projection = {DbContract.DbEntry.TAXITWIN_ID_COLUMN};
        Uri uri = Uri.parse(TaxiTwinContentProvider.TAXITWINS_URI + "/" + taxitwinId);
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            //storing start point
            values.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_LATITUDE));
            values.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_LONGITUDE));
            values.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_START_TEXTUAL));
            long startId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, values));

            //storing end point
            values = new ContentValues();
            values.put(DbContract.DbEntry.POINT_LATITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_LATITUDE));
            values.put(DbContract.DbEntry.POINT_LONGITUDE_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_LONGITUDE));
            values.put(DbContract.DbEntry.POINT_TEXTUAL_COLUMN, extras.getString(GcmHandler.GCM_DATA_END_TEXTUAL));
            long endId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.POINTS_URI, values));

            //storing taxitwin
            values = new ContentValues();
            values.put(DbContract.DbEntry._ID, taxitwinId);
            values.put(DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN, startId);
            values.put(DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN, endId);
            values.put(DbContract.DbEntry.TAXITWIN_NAME_COLUMN, extras.getString(GcmHandler.GCM_DATA_NAME));
            getContentResolver().insert(TaxiTwinContentProvider.TAXITWINS_URI, values);
        }

        ContentValues values = new ContentValues();
        values.put(DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN, taxitwinId);
        long responseId = ContentUris.parseId(getContentResolver().insert(TaxiTwinContentProvider.RESPONSES_URI, values));

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        if (componentInfo.getClassName().equals(ResponsesActivity.class.getName())) {
            Intent intent = new Intent(ACTION_TAXITWIN);
            intent.addCategory(MainActivity.CATEGORY_OFFER_DATA_CHANGED);

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            NotificationCompat.Builder mBuilder
                    = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(getResources().getString(R.string.notification_title))
                    .setContentText(getResources().getString(R.string.notification_content));

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            Intent resultIntent;
            if (TaxiTwinApplication.getPendingNotificationsCount() == 0) {
                resultIntent = new Intent(this, ResponseDetailActivity.class);
                Uri taskUri = Uri.parse(TaxiTwinContentProvider.RESPONSES_URI + "/" + responseId);
                resultIntent.putExtra(TaxiTwinContentProvider.RESPONSE_CONTENT_ITEM_TYPE, taskUri);
                stackBuilder.addParentStack(ResponseDetailActivity.class);
            } else {
                resultIntent = new Intent(this, ResponsesActivity.class);
                stackBuilder.addParentStack(ResponsesActivity.class);
            }

            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(TaxiTwinApplication.getPendingNotificationsCount(), mBuilder.build());
            TaxiTwinApplication.setPendingNotificationsCount(TaxiTwinApplication.getPendingNotificationsCount() + 1);
        }
    }
}
