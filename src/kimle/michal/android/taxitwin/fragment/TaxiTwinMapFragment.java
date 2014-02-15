package kimle.michal.android.taxitwin.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.MapFragment;

public class TaxiTwinMapFragment extends MapFragment {

    public interface MapViewListener {

        public abstract void onMapCreated();
    }

    private MapViewListener mapViewListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (mapViewListener != null) {
            mapViewListener.onMapCreated();
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mapViewListener = (MapViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MapViewListener");
        }
    }
}
