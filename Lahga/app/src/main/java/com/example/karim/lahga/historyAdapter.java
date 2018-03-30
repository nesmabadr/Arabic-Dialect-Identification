package com.example.karim.lahga;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by karim on 3/18/2018.
 */

public class historyAdapter extends ArrayAdapter<history_item> {
    public historyAdapter(Context context, ArrayList<history_item> show) {
        super(context, 0, show);
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent){
        history_item history = getItem (position);

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.history_item, parent, false);
        }

        TextView textview = convertView.findViewById(R.id.textView);
        TextView textview2 = convertView.findViewById(R.id.textView2);
        textview.setText(history.title);
        textview2.setText(history.date);
        return convertView;
    }
}