package kimle.michal.android.taxitwin.db;

import android.provider.BaseColumns;

public class DbContract {

    private DbContract() {
    }

    public static abstract class DbEntry implements BaseColumns {

        //tables
        public static final String TAXITWIN_TABLE = "taxitwin";
        public static final String POINT_TABLE = "point";
        public static final String POINT_START_TABLE = "startpoint";
        public static final String POINT_END_TABLE = "endpoint";
        public static final String RESPONSE_TABLE = "response";
        public static final String OFFER_TABLE = "offer";
        public static final String RIDE_TABLE = "ride";

        //taxitwin table
        public static final String TAXITWIN_ID_COLUMN = TAXITWIN_TABLE + "." + _ID;
        public static final String TAXITWIN_START_POINT_ID_COLUMN = "start_point_id";
        public static final String TAXITWIN_END_POINT_ID_COLUMN = "end_point_id";
        public static final String TAXITWIN_NAME_COLUMN = "name";

        //point table
        public static final String POINT_ID_COLUMN = POINT_TABLE + "." + _ID;
        public static final String POINT_START_ID_COLUMN = POINT_START_TABLE + "." + _ID;
        public static final String POINT_END_ID_COLUMN = POINT_END_TABLE + "." + _ID;
        public static final String POINT_LATITUDE_COLUMN = "latitude";
        public static final String POINT_LONGITUDE_COLUMN = "longitude";
        public static final String POINT_TEXTUAL_COLUMN = "textual";

        //response table
        public static final String RESPONSE_ID_COLUMN = RESPONSE_TABLE + "." + _ID;
        public static final String RESPONSE_TAXITWIN_ID_COLUMN = "taxitwin_id";

        //offer table
        public static final String OFFER_ID_COLUMN = OFFER_TABLE + "." + _ID;
        public static final String OFFER_TAXITWIN_ID_COLUMN = "taxitwin_id";
        public static final String OFFER_PASSENGERS_TOTAL_COLUMN = "passengers_total";
        public static final String OFFER_PASSENGERS_COLUMN = "passengers";

        //ride table
        public static final String RIDE_ID_COLUMN = RIDE_TABLE + "." + _ID;
        public static final String RIDE_OFFER_ID_COLUMN = "offer_id";

        //as nicknames
        public static final String AS_START_POINT_LATITUDE_COLUMN = "start_latitude";
        public static final String AS_START_POINT_LONGITUDE_COLUMN = "start_longitude";
        public static final String AS_START_POINT_TEXTUAL_COLUMN = "start_textual";
        public static final String AS_END_POINT_LATITUDE_COLUMN = "end_latitude";
        public static final String AS_END_POINT_LONGITUDE_COLUMN = "end_longitude";
        public static final String AS_END_POINT_TEXTUAL_COLUMN = "end_textual";
    }

}
