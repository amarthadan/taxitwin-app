package kimle.michal.android.taxitwin.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE = "taxitwin.db";

    //taxitwin table
    private static final String TAXITWIN_TABLE_CREATE
            = "CREATE TABLE " + DbContract.DbEntry.TAXITWIN_TABLE + " ("
            + DbContract.DbEntry._ID + " INTEGER PRIMARY KEY, "
            + DbContract.DbEntry.TAXITWIN_START_POINT_ID_COLUMN + " INTEGER NOT NULL REFERENCES "
            + DbContract.DbEntry.POINT_TABLE + "("
            + DbContract.DbEntry._ID + ") ON DELETE CASCADE, "
            + DbContract.DbEntry.TAXITWIN_END_POINT_ID_COLUMN + " INTEGER NOT NULL REFERENCES "
            + DbContract.DbEntry.POINT_TABLE + "("
            + DbContract.DbEntry._ID + ") ON DELETE CASCADE "
            + DbContract.DbEntry.TAXITWIN_NAME_COLUMN + " TEXT NOT NULL);";
    private static final String TAXITWIN_TABLE_DROP
            = "DROP TABLE IF EXISTS " + DbContract.DbEntry.TAXITWIN_TABLE;

    //point table
    private static final String POINT_TABLE_CREATE
            = "CREATE TABLE " + DbContract.DbEntry.POINT_TABLE + " ("
            + DbContract.DbEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " REAL NOT NULL, "
            + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " REAL NOT NULL, "
            + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " TEXT NOT NULL);";
    private static final String POINT_TABLE_DROP
            = "DROP TABLE IF EXISTS " + DbContract.DbEntry.POINT_TABLE;

    //response table
    private static final String RESPONSE_TABLE_CREATE
            = "CREATE TABLE " + DbContract.DbEntry.RESPONSE_TABLE + " ("
            + DbContract.DbEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DbContract.DbEntry.RESPONSE_TAXITWIN_ID_COLUMN + " INTEGER NOT NULL REFERENCES "
            + DbContract.DbEntry.TAXITWIN_TABLE + "("
            + DbContract.DbEntry._ID + ") ON DELETE CASCADE);";
    private static final String RESPONSE_TABLE_DROP
            = "DROP TABLE IF EXISTS " + DbContract.DbEntry.RESPONSE_TABLE;

    //offer table
    private static final String OFFER_TABLE_CREATE
            = "CREATE TABLE " + DbContract.DbEntry.OFFER_TABLE + " ("
            + DbContract.DbEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN + " INTEGER NOT NULL REFERENCES "
            + DbContract.DbEntry.TAXITWIN_TABLE + "("
            + DbContract.DbEntry._ID + ") ON DELETE CASCADE, "
            + DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN + " INTEGER NOT NULL, "
            + DbContract.DbEntry.OFFER_PASSENGERS_COLUMN + " INTEGER NOT NULL);";
    private static final String OFFER_TABLE_DROP
            = "DROP TABLE IF EXISTS " + DbContract.DbEntry.OFFER_TABLE;

    //ride table
    private static final String RIDE_TABLE_CREATE
            = "CREATE TABLE " + DbContract.DbEntry.RIDE_TABLE + " ("
            + DbContract.DbEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DbContract.DbEntry.RIDE_OFFER_ID_COLUMN + " INTEGER NOT NULL REFERENCES "
            + DbContract.DbEntry.OFFER_TABLE + "("
            + DbContract.DbEntry._ID + ") ON DELETE CASCADE);";
    private static final String RIDE_TABLE_DROP
            = "DROP TABLE IF EXISTS " + DbContract.DbEntry.RIDE_TABLE;

    public DbHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(POINT_TABLE_CREATE);
        db.execSQL(TAXITWIN_TABLE_CREATE);
        db.execSQL(RESPONSE_TABLE_CREATE);
        db.execSQL(OFFER_TABLE_CREATE);
        db.execSQL(RIDE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(RIDE_TABLE_DROP);;
        db.execSQL(OFFER_TABLE_DROP);
        db.execSQL(RESPONSE_TABLE_DROP);
        db.execSQL(TAXITWIN_TABLE_DROP);
        db.execSQL(POINT_TABLE_DROP);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
}
