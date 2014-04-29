package kimle.michal.android.taxitwin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

public class TaxiTwinAutoCompleteTextView extends AutoCompleteTextView {

    private static final String LOG = "TaxiTwinAutoCompleteTextView";
    private View superView;

    public TaxiTwinAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusableInTouchMode(true);
        setFocusable(true);
        setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                setCursorVisible(false);
                setCursorVisible(true);
            }
        });

//        setOnFocusChangeListener(new OnFocusChangeListener() {
//
//            public void onFocusChange(View v, boolean hasFocus) {
//                Log.d(LOG, "in onFocusChange : " + hasFocus);
//                if (hasFocus) {
//                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
//                    //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//                } else {
//                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
//                }
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
