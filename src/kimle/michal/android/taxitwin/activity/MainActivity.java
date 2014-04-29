package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.google.android.gms.maps.MapsInitializer;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.dialog.alert.ServicesAlertDialogFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinListFragment;
import kimle.michal.android.taxitwin.fragment.TaxiTwinMapFragment;
import kimle.michal.android.taxitwin.gcm.GcmHandler;
import kimle.michal.android.taxitwin.gcm.GcmIntentService;
import kimle.michal.android.taxitwin.popup.SettingsPopup;
import kimle.michal.android.taxitwin.services.ServicesManagement;

public class MainActivity extends Activity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int MAP_VIEW_POSITION = 0;
    private static final int LIST_VIEW_POSITION = 1;
    public static final int OFFER_DETAIL = 11000;
    public static final String CATEGORY_OFFER_DATA_CHANGED = "kimle.michal.android.taxitwin.CATEGORY_OFFER_DATA_CHANGED";
    public static final String CATEGORY_OFFER_ACCEPTED = "kimle.michal.android.taxitwin.CATEGORY_OFFER_ACCEPTED";
    private static final String SAVED_NAVIGATION_POSITION = "savedNavigationPosition";
    private static final String SAVED_MAP_FRAGMENT = "savedMapFragment";
    private static final String SAVED_LIST_FRAGMENT = "savedListFragment";
    public static final int RESULT_ACCEPT_OFFER = 42;
    private TaxiTwinMapFragment mapViewFragment;
    private TaxiTwinListFragment listViewFragment;
    private SettingsPopup settingsPopup;
    private MenuItem settingsMenuItem;
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        settingsPopup = new SettingsPopup(this);
        settingsPopup.setOnDismissListener(new OnDismissListener() {

            public void onDismiss() {
                settingsMenuItem.setIcon(R.drawable.ic_action_expand);
            }
        });

        MapsInitializer.initialize(getApplicationContext());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasCategory(CATEGORY_OFFER_DATA_CHANGED)) {
                    notifyChangedData();
                }
                if (intent.hasCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED)) {
                    showTaxiTwinDialog();
                }
                if (intent.hasCategory(ServicesManagement.CATEGORY_GPS_DISABLED)) {
                    DialogFragment alertFragment = new ServicesAlertDialogFragment(R.string.services_gps_alert_message);
                    alertFragment.show(getFragmentManager(), "gps_alert");
                }
                if (intent.hasCategory(ServicesManagement.CATEGORY_NETWORK_DISABLED)) {
                    DialogFragment alertFragment = new ServicesAlertDialogFragment(R.string.services_network_alert_message);
                    alertFragment.show(getFragmentManager(), "network_alert");
                }
            }
        };

        buildGUI(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MyTaxiTwinActivity.isInTaxiTwin(this)) {
            showTaxiTwinDialog();
        }
        notifyChangedData();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GcmIntentService.ACTION_TAXITWIN);
        intentFilter.addCategory(CATEGORY_OFFER_DATA_CHANGED);
        intentFilter.addCategory(MyTaxiTwinActivity.CATEGORY_TAXITWIN_DATA_CHANGED);
        intentFilter.addCategory(ServicesManagement.CATEGORY_GPS_DISABLED);
        intentFilter.addCategory(ServicesManagement.CATEGORY_NETWORK_DISABLED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
        ServicesManagement.initialCheck(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void buildGUI(Bundle savedInstanceState) {
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item);

        FragmentManager fm = getFragmentManager();

        if (savedInstanceState != null) {
            mapViewFragment = (TaxiTwinMapFragment) fm.getFragment(savedInstanceState, SAVED_MAP_FRAGMENT);
            listViewFragment = (TaxiTwinListFragment) fm.getFragment(savedInstanceState, SAVED_LIST_FRAGMENT);
        } else {
            if (mapViewFragment == null) {
                mapViewFragment = new TaxiTwinMapFragment();
            }
            if (listViewFragment == null) {
                listViewFragment = new TaxiTwinListFragment();
            }
        }

        FragmentTransaction ft = fm.beginTransaction();
        if (!mapViewFragment.isAdded()) {
            ft.add(R.id.main_fragment, mapViewFragment, "Map view");
        }
        if (!listViewFragment.isAdded()) {
            ft.add(R.id.main_fragment, listViewFragment, "List view");
        }
        ft.hide(listViewFragment);

        ft.commit();
        fm.executePendingTransactions();

        ActionBar actionBar = getActionBar();
        actionBar.setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                if (position == LIST_VIEW_POSITION) {
                    ft.hide(mapViewFragment);
                    ft.show(listViewFragment);
                    listViewFragment.updateView();
                } else if (position == MAP_VIEW_POSITION) {
                    ft.hide(listViewFragment);
                    ft.show(mapViewFragment);
                    mapViewFragment.loadData();
                }
                ft.commit();
                fm.executePendingTransactions();

                return true;
            }
        });
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SAVED_NAVIGATION_POSITION));
        }

        TextView bottomText = (TextView) findViewById(R.id.bottom_text);
        bottomText.setText(R.string.waiting_for_signal);
        bottomText.setTextColor(Color.GRAY);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SAVED_NAVIGATION_POSITION, getActionBar().getSelectedNavigationIndex());
        getFragmentManager().putFragment(savedInstanceState, SAVED_MAP_FRAGMENT, mapViewFragment);
        getFragmentManager().putFragment(savedInstanceState, SAVED_LIST_FRAGMENT, listViewFragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OFFER_DETAIL:
                if (resultCode == RESULT_ACCEPT_OFFER) {
                    acceptOffer(data.getLongExtra(GcmHandler.GCM_DATA_TAXITWIN_ID, 0));
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        settingsMenuItem = menu.findItem(R.id.action_show_options);
        if (!settingsPopup.hasAddress()) {
            settingsMenuItem.setIcon(R.drawable.ic_action_collapse);
            settingsPopup.showAsDropDown(getActionBarView());
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_options:
                if (settingsPopup.isShowing()) {
                    settingsMenuItem.setIcon(R.drawable.ic_action_expand);
                    settingsPopup.dismiss();
                } else {
                    settingsMenuItem.setIcon(R.drawable.ic_action_collapse);
                    settingsPopup.showAsDropDown(getActionBarView());
                }
                return true;
            case R.id.action_responses:
                Intent i = new Intent(this, ResponsesActivity.class);
                startActivity(i);
                return true;
            case R.id.action_exit:
                TaxiTwinApplication.exit(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        GcmHandler gcmHandler = new GcmHandler(this);
        gcmHandler.onSharedPreferenceChanged(sharedPreferences, key);
    }

    private View getActionBarView() {
        Window window = getWindow();
        View v = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", "android");
        return v.findViewById(resId);
    }

    private void notifyChangedData() {
        listViewFragment.updateView();
        mapViewFragment.loadData();
    }

    private void acceptOffer(long taxitwinId) {
        GcmHandler gcmHandler = new GcmHandler(this);
        gcmHandler.acceptOffer(taxitwinId);
    }

    private void showTaxiTwinDialog() {
        MyTaxiTwinActivity.showTaxiTwinDialog(this);
    }
}
