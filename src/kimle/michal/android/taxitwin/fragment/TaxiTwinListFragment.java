package kimle.michal.android.taxitwin.fragment;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.OfferDetailActivity;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;

public class TaxiTwinListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupAdapter();
    }

    private void setupAdapter() {
        String[] columns = new String[]{
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN};
        //FIXME not showing correct data, showing only end point name

        int[] to = new int[]{
            R.id.name_text,
            R.id.from_text,
            R.id.to_text};

        getLoaderManager().initLoader(0, null, this);

        adapter = new SimpleCursorAdapter(
                getActivity(), R.layout.offer,
                null,
                columns,
                to,
                0);

        setListAdapter(adapter);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
            DbContract.DbEntry.OFFER_ID_COLUMN,
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN};

        Uri uri = TaxiTwinContentProvider.OFFERS_URI;

        CursorLoader cursorLoader = new CursorLoader(getActivity(), uri, projection, null, null, null);
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
        Intent i = new Intent(getActivity(), OfferDetailActivity.class);
        Uri taskUri = Uri.parse(TaxiTwinContentProvider.OFFERS_URI + "/" + id);
        i.putExtra(TaxiTwinContentProvider.OFFER_CONTENT_ITEM_TYPE, taskUri);

        startActivity(i);
    }
}
