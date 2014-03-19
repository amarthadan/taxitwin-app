package kimle.michal.android.taxitwin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;

public class TaxiTwinAutoCompleteTextView extends AutoCompleteTextView {

    private static final String LOG = "TaxiTwinAutoCompleteTextView";
    private View superView;

    public TaxiTwinAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                setCursorVisible(false);
                setCursorVisible(true);
            }
        });

//        setOnFocusChangeListener(new OnFocusChangeListener() {
//
//            public void onFocusChange(View v, boolean hasFocus) {
//                setCursorVisible(hasFocus);
//            }
//        });
    }

    public void setSuperView(View view) {
        superView = view;
    }

    @Override
    public View getRootView() {
        Log.d(LOG, "in getRootView");

        if (superView == null) {
            View parent = this;
            while (parent.getParent() != null && getParent().getParent() instanceof View) {
                parent = (View) parent.getParent();
            }
            return parent;

        }
        return superView;
    }
}
