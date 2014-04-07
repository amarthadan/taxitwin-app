package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;
import kimle.michal.android.taxitwin.gcm.GcmHandler;
import kimle.michal.android.taxitwin.gcm.GcmIntentService;
import static kimle.michal.android.taxitwin.gcm.GcmIntentService.ACTION_TAXITWIN;

public class ResponsesActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;
    public static final int RESPONSE_DETAIL = 20000;
    public static final String CATEGORY_RESPONSE_DATA_CHANGED = "kimle.michal.android.taxitwin.CATEGORY_RESPONSE_DATA_CHANGED";
    public static final int RESULT_ACCEPT_RESPONSE = 74;
    public static final int RESULT_DECLINE_RESPONSE = 75;
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasCategory(CATEGORY_RESPONSE_DATA_CHANGED)) {
                    updateView();
                }
                if (intent.hasCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED)) {
                    showTaxiTwinDialog();
                }
            }
        };

        setupAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MyTaxiTwinActivity.isInTaxiTwin(this)) {
            showTaxiTwinDialog();
        }
        updateView();
        IntentFilter intentFiler = new IntentFilter();
        intentFiler.addAction(GcmIntentService.ACTION_TAXITWIN);
        intentFiler.addCategory(CATEGORY_RESPONSE_DATA_CHANGED);
        intentFiler.addCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFiler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void setupAdapter() {
        String[] columns = new String[]{
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN};

        int[] to = new int[]{
            R.id.name_text,
            R.id.from_text,
            R.id.to_text};

        getLoaderManager().initLoader(0, null, this);

        adapter = new SimpleCursorAdapter(
                this, R.layout.offer,
                null,
                columns,
                to,
                0);

        setListAdapter(adapter);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
            DbContract.DbEntry.RESPONSE_ID_COLUMN,
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN};

        Uri uri = TaxiTwinContentProvider.RESPONSES_URI;
        CursorLoader cursorLoader = new CursorLoader(this, uri, projection, null, null, null);
        return cursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    public void updateView() {
        getLoaderManager().restartLoader(0, null, this);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ResponseDetailActivity.class);
        Uri taskUri = Uri.parse(TaxiTwinContentProvider.RESPONSES_URI + "/" + id);
        i.putExtra(TaxiTwinContentProvider.RESPONSE_CONTENT_ITEM_TYPE, taskUri);

        startActivityForResult(i, RESPONSE_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESPONSE_DETAIL:
                if (resultCode == RESULT_ACCEPT_RESPONSE) {
                    acceptResponse(data.getLongExtra(GcmHandler.GCM_DATA_TAXITWIN_ID, 0));
                }
                if (resultCode == RESULT_DECLINE_RESPONSE) {
                    declineResponse(data.getLongExtra(GcmHandler.GCM_DATA_TAXITWIN_ID, 0));
                }
                if (resultCode == RESULT_ACCEPT_RESPONSE || resultCode == RESULT_DECLINE_RESPONSE) {
                    String[] projection = {DbContract.DbEntry.RESPONSE_ID_COLUMN};
                    String selection = DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN + " = ?";
                    String[] selectionArgs = {String.valueOf(data.getLongExtra(GcmHandler.GCM_DATA_TAXITWIN_ID, 0))};

                    Cursor cursor = getContentResolver().query(TaxiTwinContentProvider.RESPONSES_URI, projection, selection, selectionArgs, null);
                    if (cursor != null && cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        long responseId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));
                        Uri uri = Uri.parse(TaxiTwinContentProvider.RESPONSES_URI + "/" + responseId);
                        getContentResolver().delete(uri, null, null);

                        Intent intent = new Intent(ACTION_TAXITWIN);
                        intent.addCategory(CATEGORY_RESPONSE_DATA_CHANGED);

                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    }
                }
        }
    }

    private void acceptResponse(long taxitwinId) {
        TaxiTwinApplication.getGcmHandler().acceptResponse(taxitwinId);

        Intent intent = new Intent(this, MyTaxiTwinActivity.class);
        intent.addCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_OWNER);
        startActivity(intent);
    }

    private void declineResponse(long taxitwinId) {
        TaxiTwinApplication.getGcmHandler().declineResponse(taxitwinId);
    }

    private void showTaxiTwinDialog() {
        MyTaxiTwinActivity.showTaxiTwinDialog(this);
    }
}
