package com.example.karim.lahga;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Created by karim on 3/18/2018.
 */

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "historyDB.db";
    private static final int DB_VER = 1;

    public DataBaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history (" + "id INTEGER PRIMARY KEY, "
                + " title TEXT , date DATE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS history");
            onCreate(db);
        }
        catch (SQLException e) {
        }
    }

    public ArrayList<history_item> getAllHistories() {
        ArrayList<history_item> historyList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM history";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                history_item item = new history_item();
                try {
                    SimpleDateFormat spf=new SimpleDateFormat("dd/MM/yyyy");
                    Date newDate = spf.parse(c.getString(c.getColumnIndex("date")));
                    spf= new SimpleDateFormat("dd MMM yyyy");
                    String date = spf.format(newDate);
                    item.setData(c.getInt(c.getColumnIndex("id")), c.getString(c.getColumnIndex("title")), date);
                    historyList.add(item);
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

            } while (c.moveToNext());
        }
        Collections.reverse(historyList);
        return historyList;
    }

    public void deleteHistory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("history", "id" + " = ?", new String[] { String.valueOf(id) });
    }

    public long addHistory(String title, String date) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("date", date);

        long id = db.insert("history", null, values);

        return id;
    }
}