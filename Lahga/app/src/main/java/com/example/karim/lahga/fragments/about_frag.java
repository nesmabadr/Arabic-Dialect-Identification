package com.example.karim.lahga.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.karim.lahga.R;

/**
 * Created by karim on 2/24/2018.
 */

public class about_frag extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.about_frag, container, false);
        return rootView;
    }
}