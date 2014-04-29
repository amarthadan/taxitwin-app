package kimle.michal.android.taxitwin.activity;

import android.app.Activity;
import android.content.Intent;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import android.os.Bundle;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import static kimle.michal.android.taxitwin.enumerate.UserState.OWNER;
import static kimle.michal.android.taxitwin.enumerate.UserState.PARTICIPANT;

public class LauncherActivity extends Activity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent;
        TaxiTwinApplication app = (TaxiTwinApplication) getApplication();
        app.register();
        switch (TaxiTwinApplication.getUserState()) {
            case NOT_SUBSCRIBED:
            case SUBSCRIBED:
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
                break;
            case PARTICIPANT:
                intent = new Intent(this, MyTaxiTwinActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
                break;
            case OWNER:
                intent = new Intent(this, MyTaxiTwinActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
                break;
            default:
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
        finish();
    }
}
