package com.example.karim.lahga.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.example.karim.lahga.MainActivity;
import com.example.karim.lahga.OpenSansSBTextView;
import com.example.karim.lahga.R;
import com.example.karim.lahga.historyAdapter;
import com.example.karim.lahga.history_item;

import java.util.ArrayList;

/**
 * Created by karim on 2/24/2018.
 */

public class history_frag extends Fragment {

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.history_frag, container, false);
        ((MainActivity)getActivity()).listView = rootView.findViewById(R.id.listView);
        ((MainActivity)getActivity()).arrayOfHistory = ((MainActivity)this.getActivity()).DBHelper.getAllHistories();
        ((MainActivity)getActivity()).adapter = new historyAdapter(getActivity(), ((MainActivity)getActivity()).arrayOfHistory);
        ((MainActivity)getActivity()).listView.setAdapter(((MainActivity)getActivity()).adapter);
        OpenSansSBTextView emptyText = rootView.findViewById(R.id.emptyText);
        ((MainActivity)getActivity()).listView.setEmptyView(emptyText);
        createSwipeList();
        return rootView;
    }

    private void createSwipeList(){
        ((MainActivity)getActivity()).creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getActivity().getApplicationContext());
                deleteItem.setBackground(new ColorDrawable(Color.parseColor("#FF6666")));
                deleteItem.setWidth(250);
                deleteItem.setIcon(R.drawable.ic_delete_white_24dp);
               // deleteItem.setTitle("Delete");
                //deleteItem.setTitleSize(12);
                //deleteItem.setTitleColor(Color.WHITE);

                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };
        ((MainActivity)getActivity()).listView.setMenuCreator(((MainActivity)getActivity()).creator);


        ((MainActivity)getActivity()).listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3) {
                ((MainActivity)getActivity()).listView.smoothOpenMenu(index);
                return false;
            }
        });
        ((MainActivity)getActivity()).listView.setSwipeDirection(SwipeMenuListView.DIRECTION_RIGHT);
        //((MainActivity)getActivity()).listView.setOpenInterpolator(new BounceInterpolator());

        ((MainActivity)getActivity()).listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        ((MainActivity)history_frag.this.getActivity()).DBHelper.deleteHistory(((MainActivity)getActivity()).arrayOfHistory.get(position).id);
                        ((MainActivity)getActivity()).arrayOfHistory.remove(position);
                        ((MainActivity)getActivity()).adapter.notifyDataSetChanged();
                        ((MainActivity)getActivity()).listView.setAdapter(((MainActivity)getActivity()).adapter); //to fix menu not closing after delete
                        break;
                }
                return false;
            }
        });
    }
}
