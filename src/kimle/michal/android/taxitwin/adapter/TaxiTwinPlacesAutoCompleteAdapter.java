package kimle.michal.android.taxitwin.adapter;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TaxiTwinPlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

    private ArrayList<String> resultList;
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    public static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
    public static final String PLACES_API_KEY = "AIzaSyAD5dc-7yTVvWhKFRQ-OC48dPlLnAvy5hU";
    private static final String LOG = "TaxiTwinPlacesAutoCompleteAdapter";

    public TaxiTwinPlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                resultList = new ArrayList<String>();
                if (constraint != null) {
                    // Retrieve the autocomplete results.
                    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) {
                            request.setParser(new JsonObjectParser(JSON_FACTORY));
                        }
                    }
                    );

                    GenericUrl url = new GenericUrl(PLACES_API_BASE);
                    url.put("input", constraint.toString());
                    url.put("key", PLACES_API_KEY);
                    url.put("sensor", false);

                    HttpRequest request;
                    HttpResponse httpResponse;
                    PlacesResult directionsResult = null;
                    try {
                        request = requestFactory.buildGetRequest(url);
                        httpResponse = request.execute();
                        directionsResult = httpResponse.parseAs(PlacesResult.class);
                    } catch (IOException ex) {
                        Log.e(LOG, ex.getMessage());
                    }

                    Log.d(LOG, "directionsResult: " + directionsResult);
                    if (directionsResult != null) {
                        List<Prediction> predictions = directionsResult.predictions;
                        Log.d(LOG, "predictions: " + predictions);
                        for (Prediction prediction : predictions) {
                            resultList.add(prediction.description);
                        }
                    }
                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    public static class PlacesResult {

        @Key("predictions")
        public List<Prediction> predictions;
    }

    public static class Prediction {

        @Key("description")
        public String description;
        @Key("id")
        public String id;
    }
}
