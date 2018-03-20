package com.example.karim.lahga.fragments;

import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.example.karim.lahga.MainActivity;
import com.example.karim.lahga.R;
import com.example.karim.lahga.historyAdapter;
import com.example.karim.lahga.history_item;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by karim on 2/24/2018.
 */

public class history_frag extends Fragment {

    private SwipeMenuListView listView;
    private ArrayList<history_item> arrayOfHistory = new ArrayList<>();
    private historyAdapter adapter;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.history_frag, container, false);
        listView = rootView.findViewById(R.id.listView);
        arrayOfHistory = ((MainActivity)this.getActivity()).DBHelper.getAllHistories();
        adapter = new historyAdapter(getActivity(), arrayOfHistory);
        listView.setAdapter(adapter);
        createSwipeList();
        return rootView;
    }

    private void createSwipeList(){
        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getActivity().getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.RED));
                // set item width
                deleteItem.setWidth(200);
                // set a icon
                deleteItem.setIcon(R.drawable.ic_delete_white_48dp);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };
        listView.setMenuCreator(creator);


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3) {
                listView.smoothOpenMenu(index);
                return false;
            }
        });
        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_RIGHT);
        listView.setOpenInterpolator(new BounceInterpolator());

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        ((MainActivity)history_frag.this.getActivity()).DBHelper.deleteHistory(arrayOfHistory.get(position).id);
                        arrayOfHistory.remove(position);
                        adapter.notifyDataSetChanged();
                        listView.setAdapter(adapter); //to fix menu not closing after delete
                        break;
                }
                return false;
            }
        });
    }
}
