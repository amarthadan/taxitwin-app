package kimle.michal.android.taxitwin.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.HashMap;
import java.util.Map;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;

public class TaxiTwinMapFragment extends MapFragment {

    public interface MapViewListener {

        public abstract void onMapCreated();
    }

    private static final String LOG = "TaxiTwinMapFragment";
    private MapViewListener mapViewListener;
    private GoogleMap map;
    private Map<Marker, Long> markers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Log.d(LOG, "in onCreateView");
        Log.d(LOG, "mapViewListener: " + mapViewListener);
        if (mapViewListener != null) {
            mapViewListener.onMapCreated();
        }
        map = getMap();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(LOG, "in onAttach");
        try {
            mapViewListener = (MapViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MapViewListener");
        }
    }

    public void loadData() {
        if (markers != null) {
            for (Marker m : markers.keySet()) {
                m.remove();
            }
        }
        markers = new HashMap<Marker, Long>();

        String[] projection = {
            DbContract.DbEntry.OFFER_ID_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN};

        Cursor cursor = getActivity().getContentResolver().query(TaxiTwinContentProvider.OFFERS_URI, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.POINT_LONGITUDE_COLUMN));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.POINT_LATITUDE_COLUMN));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));

                MarkerOptions markerOptions = new MarkerOptions();
                LatLng markerPos = new LatLng(latitude, longitude);
                markerOptions.position(markerPos);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                markerOptions.anchor(0.11f, 0.93f);
                markerOptions.title("lat: " + latitude + " long: " + longitude);
                Marker marker = map.addMarker(markerOptions);
                Log.d(LOG, "marker: " + marker.toString());

                markers.put(marker, id);
            } while (cursor.moveToNext());
            cursor.close();
        }
        Log.d(LOG, "markers: " + markers);
    }
}
