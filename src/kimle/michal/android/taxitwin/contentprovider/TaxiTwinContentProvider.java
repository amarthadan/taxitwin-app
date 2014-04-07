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
import android.text.TextUtils;
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
    private static final int TAXITWINS_ID = 17;
    private static final int POINTS = 11;
    private static final int POINTS_ID = 16;
    private static final int OFFERS = 12;
    private static final int OFFERS_ID = 15;
    private static final int RESPONSES = 13;
    private static final int RESPONSES_ID = 18;
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
    public static final String OFFER_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/offers";
    public static final String OFFER_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/offer";
    public static final String RESPONSE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/responses";
    public static final String RESPONSE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/response";
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String VND = "vnd.kimle.michal.android.taxitwin.contentprovider";

    static {
        sURIMatcher.addURI(AUTHORITY, TAXITWINS_PATH, TAXITWINS);
        sURIMatcher.addURI(AUTHORITY, TAXITWINS_PATH + "/#", TAXITWINS_ID);
        sURIMatcher.addURI(AUTHORITY, POINTS_PATH, POINTS);
        sURIMatcher.addURI(AUTHORITY, POINTS_PATH + "/#", POINTS_ID);
        sURIMatcher.addURI(AUTHORITY, OFFERS_PATH, OFFERS);
        sURIMatcher.addURI(AUTHORITY, OFFERS_PATH + "/#", OFFERS_ID);
        sURIMatcher.addURI(AUTHORITY, RESPONSES_PATH, RESPONSES);
        sURIMatcher.addURI(AUTHORITY, RESPONSES_PATH + "/#", RESPONSES_ID);
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
            case TAXITWINS_ID:
                queryBuilder.appendWhere(DbContract.DbEntry.TAXITWIN_ID_COLUMN + "="
                        + uri.getLastPathSegment());
            case TAXITWINS:
                queryBuilder.setTables(DbContract.DbEntry.TAXITWIN_TABLE);
                break;
            case RESPONSES_ID:
                queryBuilder.appendWhere(DbContract.DbEntry.RESPONSE_ID_COLUMN + "="
                        + uri.getLastPathSegment());
            case RESPONSES:
                queryBuilder.setTables(DbContract.DbEntry.RESPONSE_TABLE
                        + " inner join " + DbContract.DbEntry.TAXITWIN_TABLE
                        + " on " + DbContract.DbEntry.TAXITWIN_ID_COLUMN
                        + " = " + DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN
                        + " inner join " + DbContract.DbEntry.POINT_TABLE
                        + " as " + DbContract.DbEntry.POINT_START_TABLE
                        + " on " + DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN
                        + " = " + DbContract.DbEntry.POINT_START_ID_COLUMN
                        + " inner join " + DbContract.DbEntry.POINT_TABLE
                        + " as " + DbContract.DbEntry.POINT_END_TABLE
                        + " on " + DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN
                        + " = " + DbContract.DbEntry.POINT_END_ID_COLUMN);
                break;
            case RIDES:
                queryBuilder.setTables(DbContract.DbEntry.RIDE_TABLE
                        + " inner join " + DbContract.DbEntry.OFFER_TABLE
                        + " on " + DbContract.DbEntry.OFFER_ID_COLUMN
                        + " = " + DbContract.DbEntry.RIDE_OFFER_ID_COLUMN
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
            case TAXITWINS_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND + "." + TAXITWINS_PATH;
            case POINTS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + POINTS_PATH;
            case POINTS_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND + "." + POINTS_PATH;
            case OFFERS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + OFFERS_PATH;
            case OFFERS_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND + "." + OFFERS_PATH;
            case RESPONSES:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND + "." + RESPONSES_PATH;
            case RESPONSES_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND + "." + RESPONSES_PATH;
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
        int uriType = sURIMatcher.match(uri);
        db = dbHelper.getWritableDatabase();
        int rowsDeleted;
        String tableName;

        switch (uriType) {
            case OFFERS_ID:
                tableName = DbContract.DbEntry.OFFER_TABLE;
                break;
            case POINTS_ID:
                tableName = DbContract.DbEntry.POINT_TABLE;
                break;
            case TAXITWINS_ID:
                tableName = DbContract.DbEntry.TAXITWIN_TABLE;
                break;
            case RESPONSES_ID:
                tableName = DbContract.DbEntry.RESPONSE_TABLE;
                break;
            case RIDES:
                return db.delete(DbContract.DbEntry.RIDE_TABLE, null, null);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        String id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
            rowsDeleted = db.delete(tableName,
                    DbContract.DbEntry._ID + "=" + id,
                    null);
        } else {
            rowsDeleted = db.delete(tableName,
                    DbContract.DbEntry._ID + "=" + id
                    + " and " + selection,
                    selectionArgs);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        db = dbHelper.getWritableDatabase();
        int rowsUpdated;
        String tableName;

        switch (uriType) {
            case OFFERS_ID:
                tableName = DbContract.DbEntry.OFFER_TABLE;
                break;
            case POINTS_ID:
                tableName = DbContract.DbEntry.POINT_TABLE;
                break;
            case TAXITWINS_ID:
                tableName = DbContract.DbEntry.TAXITWIN_TABLE;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        String id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
            rowsUpdated = db.update(tableName, values,
                    DbContract.DbEntry._ID + "=" + id,
                    null);
        } else {
            rowsUpdated = db.update(tableName, values,
                    DbContract.DbEntry._ID + "=" + id
                    + " and " + selection,
                    selectionArgs);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
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
            DbContract.DbEntry.POINT_END_ID_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry._ID + " as " + DbContract.DbEntry.AS_START_POINT_ID_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry._ID + " as " + DbContract.DbEntry.AS_END_POINT_ID_COLUMN
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection: " + projection);
            }
        }
    }
}
