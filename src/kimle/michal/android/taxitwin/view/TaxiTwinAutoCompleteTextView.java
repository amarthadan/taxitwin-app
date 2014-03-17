package kimle.michal.android.taxitwin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;

public class TaxiTwinAutoCompleteTextView extends AutoCompleteTextView {

    private View superView;

    public TaxiTwinAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSuperView(View view) {
        superView = view;
    }

    @Override
    public View getRootView() {

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
