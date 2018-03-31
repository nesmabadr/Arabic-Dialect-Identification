package com.example.karim.lahga;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

/**
 * Created by karim on 6/18/2017.
 */

public class OpenSansSBTextView extends  android.support.v7.widget.AppCompatTextView {

    public OpenSansSBTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public OpenSansSBTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OpenSansSBTextView(Context context) {
        super(context);
        init();
    }

    public void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/OpenSans-Semibold.ttf");
        setTypeface(tf ,0);

    }

}