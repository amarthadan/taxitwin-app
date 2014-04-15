package kimle.michal.android.taxitwin.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.MainActivity;
import kimle.michal.android.taxitwin.activity.OfferDetailActivity;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;

public class TaxiTwinMapFragment extends MapFragment implements OnMarkerClickListener {

    private static final String LOG = "TaxiTwinMapFragment";
    private static final int PADDING = 50;
    private Map<Marker, Long> markers;
    private Marker currentMarker;
    private Location currentLocation;
    private boolean mapReady = false;
    private boolean updateMap = false;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Log.d(LOG, "in onCreateView");
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (getMap() != null) {
            getMap().setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mapReady = true;
                    if (updateMap) {
                        updateCamera();
                    }
                }
            });
            getMap().setOnMarkerClickListener(this);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestLocationChanges();
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private void requestLocationChanges() {
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                updateCurrentLocation(location);
                Log.d(LOG, "onLocationChanged");
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, this);
                Log.d(LOG, "onProviderEnabled");
            }

            public void onProviderDisabled(String provider) {
                //locationManager.removeUpdates(this);
                Log.d(LOG, "onProviderDisabled");
            }
        };
        //every 10 seconds and at least 3 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, locationListener);
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
                Marker marker = getMap().addMarker(markerOptions);
                Log.d(LOG, "marker: " + marker.toString());

                markers.put(marker, id);
            } while (cursor.moveToNext());
            cursor.close();
        }

        updateCamera();
        Log.d(LOG, "markers: " + markers);
    }

    public void updateCurrentLocation(Location location) {
        Log.d(LOG, "location: " + location);
        currentLocation = location;
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        if (currentMarker == null) {
            MarkerOptions currentMarkerOptions = new MarkerOptions();
            currentMarkerOptions.position(currentLatLng);
            currentMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
            currentMarkerOptions.anchor(0.14f, 0.67f);
            currentMarker = getMap().addMarker(currentMarkerOptions);
        } else {
            currentMarker.setPosition(currentLatLng);
        }

        TextView bottomText = (TextView) getActivity().findViewById(R.id.bottom_text);
        bottomText.setText("");

        if (!mapReady) {
            updateMap = true;
            return;
        }
        updateCamera();
    }

    private void updateCamera() {
        if (!mapReady) {
            updateMap = true;
            return;
        }

        List<Marker> tmpList = new ArrayList<Marker>();
        if (currentMarker != null) {
            tmpList.add(currentMarker);
        }

        if (markers != null) {
            tmpList.addAll(markers.keySet());
        }

        if (tmpList.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : tmpList) {
            builder.include(marker.getPosition());
        }

        LatLngBounds bounds = builder.build();

        if (tmpList.size() == 1) {
            getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(currentMarker.getPosition(), 15f));
        } else {
            getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, PADDING));
        }
    }

    public boolean onMarkerClick(Marker marker) {
        Log.d(LOG, "clicked marker: " + marker);
        if (!markers.containsKey(marker)) {
            return false;
        }

        long id = markers.get(marker);

        Intent i = new Intent(getActivity(), OfferDetailActivity.class);
        Uri taskUri = Uri.parse(TaxiTwinContentProvider.OFFERS_URI + "/" + id);
        i.putExtra(TaxiTwinContentProvider.OFFER_CONTENT_ITEM_TYPE, taskUri);

        getActivity().startActivityForResult(i, MainActivity.OFFER_DETAIL);
        return true;
    }
}
