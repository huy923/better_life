package com.example.better_life;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OurDatabase extends SQLiteOpenHelper {
    private Context context;
    public static final String DATABASE_NAM = "BetterLife.db";
    public static int DATABASE_VERSION = 1;
    public static String TABLE_NAME = "users";

    public OurDatabase(@Nullable Context context) {
        super(context, DATABASE_NAM, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
