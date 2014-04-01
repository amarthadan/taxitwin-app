package kimle.michal.android.taxitwin.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import kimle.michal.android.taxitwin.db.DbContract;
import kimle.michal.android.taxitwin.db.DbHelper;

public class TaxiTwinContentProvider extends ContentProvider {

    private static final String LOG = "TaxiTwinContentProvider";
    private DbHelper dbHelper;
    private SQLiteDatabase db;
    private static final String AUTHORITY = "kimle.michal.android.taxitwin.contentprovider";
    private static final int TAXITWINS = 10;
    private static final int POINTS = 11;
    private static final int OFFERS = 12;
    private static final int OFFERS_ID = 15;
    private static final int RESPONSES = 13;
    private static final int RIDES = 14;
    private static final String TAXITWINS_PATH = "taxitwins";
    private static final String POINTS_PATH = "points";
    private static final String OFFERS_PATH = "offers";
    private static final String RESPONSES_PATH = "responses";
    private static final String RIDES_PATH = "rides";
    public static final Uri TAXITWINS_URI = Uri.parse("content://" + AUTHORITY + "/" + TAXITWINS_PATH);
    public static final Uri POINTS_URI = Uri.parse("content://" + AUTHORITY + "/" + POINTS_PATH);
    public static final Uri OFFERS_URI = Uri.parse("content://" + AUTHORITY + "/" + OFFERS_PATH);
    public static final Uri RESPONSES_URI = Uri.parse("content://" + AUTHORITY + "/" + RESPONSES_PATH);
    public static final Uri RIDES_URI = Uri.parse("content://" + AUTHORITY + "/" + RIDES_PATH);
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String VND = "vnd.kimle.michal.android.taxitwin.contentprovider";

    static {
        sURIMatcher.addURI(AUTHORITY, TAXITWINS_PATH, TAXITWINS);
        sURIMatcher.addURI(AUTHORITY, POINTS_PATH, POINTS);
        sURIMatcher.addURI(AUTHORITY, OFFERS_PATH, OFFERS);
        sURIMatcher.addURI(AUTHORITY, OFFERS_PATH + "/#", OFFERS_ID);
        sURIMatcher.addURI(AUTHORITY, RESPONSES_PATH, RESPONSES);
        sURIMatcher.addURI(AUTHORITY, RIDES_PATH, RIDES);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int uriType = sURIMatcher.match(uri);
        db = dbHelper.getReadableDatabase();
        Cursor cursor;
        checkColumns(projection);

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (uriType) {
            case OFFERS_ID:
                queryBuilder.appendWhere(DbContract.DbEntry.OFFER_ID_COLUMN + "="
                        + uri.getLastPathSegment());
            case OFFERS:
                queryBuilder.setTables(DbContract.DbEntry.OFFER_TABLE
                        + " inner join " + DbContract.DbEntry.TAXITWIN_TABLE
                        + " on " + DbContract.DbEntry.TAXITWIN_ID_COLUMN
                        + " = " + DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN
                        + " inner join " + DbContract.DbEntry.POINT_TABLE
                        + " as " + DbContract.DbEntry.POINT_START_TABLE
                        + " on " + DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN
                        + " = " + DbContract.DbEntry.POINT_START_ID_COLUMN
                        + " inner join " + DbContract.DbEntry.POINT_TABLE
                        + " as " + DbContract.DbEntry.POINT_END_TABLE
                        + " on " + DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN
                        + " = " + DbContract.DbEntry.POINT_END_ID_COLUMN);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        Log.d(LOG, queryBuilder.buildQuery(projection, selection, selectionArgs, null, null, sortOrder, null));
        cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder, null);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case TAXITWINS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + TAXITWINS_PATH;
            case POINTS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + POINTS_PATH;
            case OFFERS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + OFFERS_PATH;
            case OFFERS_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND + "." + OFFERS_PATH;
            case RESPONSES:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + RESPONSES_PATH;
            case RIDES:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + RIDES_PATH;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        db = dbHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case TAXITWINS:
                id = db.insert(DbContract.DbEntry.TAXITWIN_TABLE, null, values);
                break;
            case POINTS:
                id = db.insert(DbContract.DbEntry.POINT_TABLE, null, values);
                break;
            case OFFERS:
                id = db.insert(DbContract.DbEntry.OFFER_TABLE, null, values);
                break;
            case RESPONSES:
                id = db.insert(DbContract.DbEntry.RESPONSE_TABLE, null, values);
                break;
            case RIDES:
                id = db.insert(DbContract.DbEntry.RIDE_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void checkColumns(String[] projection) {
        String[] available = {
            DbContract.DbEntry._ID,
            DbContract.DbEntry.OFFER_ID_COLUMN,
            DbContract.DbEntry.OFFER_PASSENGERS_COLUMN,
            DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN,
            DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN,
            DbContract.DbEntry.POINT_ID_COLUMN,
            DbContract.DbEntry.POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.RESPONSE_ID_COLUMN,
            DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN,
            DbContract.DbEntry.RIDE_ID_COLUMN,
            DbContract.DbEntry.RIDE_OFFER_ID_COLUMN,
            DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN,
            DbContract.DbEntry.TAXITWIN_ID_COLUMN,
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_ID_COLUMN,
            DbContract.DbEntry.POINT_END_ID_COLUMN
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
