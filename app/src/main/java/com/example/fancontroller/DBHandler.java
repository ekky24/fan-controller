package com.example.fancontroller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "db_iveco";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "tb_rpm_data";
    static final String ID_COL = "id";
    static final String GEAR_POS_COL = "gear_pos";
    static final String IN_RPM_COL = "in_rpm";
    static final String OUT_RPM_COL = "out_rpm";

    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + GEAR_POS_COL + " TEXT,"
                + IN_RPM_COL + " TEXT,"
                + OUT_RPM_COL + " TEXT)";
        db.execSQL(query);
    }

    public void addNewData(String gearPos, String inRpm, String outRpm) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(GEAR_POS_COL, gearPos);
        values.put(IN_RPM_COL, inRpm);
        values.put(OUT_RPM_COL, outRpm);

        db.insert(TABLE_NAME, null, values);
    }

    public Cursor fetch() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null,
                null, null, null, null, null);
        return cursor;
    }

    public void clearData() {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);
    }

    public void closeDbConn() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
