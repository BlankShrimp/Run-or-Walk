package com.blankshrimp.runorwalk.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    private Context mContext;
    // The data is stored as this: period stands for each hour, for example, 5 refers to 5am-6am.
    // For each period, seconds stores how many seconds is running.
    private static final String CREATE_PARSED = "create table ParsedMovement (" +
            "day date, " +
            "period integer, " +
            "seconds integer)";
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "move.db";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_PARSED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
