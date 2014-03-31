package kimle.michal.android.taxitwin.gcm;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;

public class GcmIntentService extends IntentService {

    private static final String LOG = "GcmIntentService";

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
    }
}
