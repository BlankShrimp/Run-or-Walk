package com.blankshrimp.runorwalk.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DataAccessObject {

    private Context context;
    private DBHelper dbHelper;

    public DataAccessObject(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.close();
    }

    /**
     * Query the daily movements.
     * @param date Formatted String of date.
     * @return
     */
    public List<Integer> queryMovements(String date) {
        List<Integer> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from ParsedMovement where day=? order by period asc",
                new String[]{date});
        int initPeriod = -1;
        if (cursor.moveToFirst()) {
            do {
                while (true) {
                    initPeriod ++;
                    if (cursor.getInt(cursor.getColumnIndex("period")) == initPeriod) {
                        result.add(cursor.getInt(cursor.getColumnIndex("seconds")));
                        break;
                    } else if (initPeriod < 24)
                        result.add(0);
                    else break;
                }
            } while (cursor.moveToNext());
        }
        while (result.size()<24)
            result.add(0);
        cursor.close();
        db.close();
        return result;
    }

    /**
     * Update move type. This operation will add running seconds to specific time slot.
     * @param date Formatted String of date.
     * @param period Time slot.
     * @param seconds Time span.
     */
    public void updateEntry(String date, String period, String seconds) {
        // If that date has no data at all, create blank slots.
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from ParsedMovement where day=? order by period asc",
                new String[]{date});
        if (!cursor.moveToNext()) {
            for (int i = 0; i < 24; i++) {
                db.execSQL("insert into ParsedMovement values (?,?,?);", new String[]{date, String.valueOf(i), String.valueOf(0)});
            }
        }
        cursor.close();
        db.close();
        // Then update value to each slot.
        SQLiteDatabase db1 = dbHelper.getWritableDatabase();
        db1.execSQL("update ParsedMovement set seconds=seconds+? where day=? and (period=?)", new String[]{seconds, date, period});
        db1.close();
    }
}
