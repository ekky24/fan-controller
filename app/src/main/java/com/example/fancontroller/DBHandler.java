package com.example.fancontroller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "db_iveco";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "tb_rpm_data";
    static final String ID_COL = "id";
    static final String GEAR_POS_COL = "gear_pos";
    static final String IN_RPM_COL = "in_rpm";
    static final String OUT_RPM_COL = "out_rpm";
    static final String TEMP_COL = "temperature";
    static final String PRESSURE1_COL = "pressure_1";
    static final String PRESSURE2_COL = "pressure_2";
    static final String PRESSURE3_COL = "pressure_3";
    static final String PRESSURE4_COL = "pressure_4";
    static final String TIMESTAMP_COL = "timestamp";

    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + GEAR_POS_COL + " TEXT,"
                + IN_RPM_COL + " TEXT,"
                + OUT_RPM_COL + " TEXT,"
                + TEMP_COL + " TEXT,"
                + PRESSURE1_COL + " TEXT,"
                + PRESSURE2_COL + " TEXT,"
                + PRESSURE3_COL + " TEXT,"
                + PRESSURE4_COL + " TEXT,"
                + TIMESTAMP_COL + " TEXT)";
        db.execSQL(query);
    }

    public void addNewData(String gearPos, String inRpm, String outRpm, String temp, String pressure1,
                           String pressure2, String pressure3, String pressure4) {
        SQLiteDatabase db = this.getWritableDatabase();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String curr_datetime = sdf.format(new Date());

        ContentValues values = new ContentValues();
        values.put(GEAR_POS_COL, gearPos);
        values.put(IN_RPM_COL, inRpm);
        values.put(OUT_RPM_COL, outRpm);
        values.put(TEMP_COL, temp);
        values.put(PRESSURE1_COL, pressure1);
        values.put(PRESSURE2_COL, pressure2);
        values.put(PRESSURE3_COL, pressure3);
        values.put(PRESSURE4_COL, pressure4);
        values.put(TIMESTAMP_COL, curr_datetime);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public Cursor fetch() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null,
                null, null, null, null, null);
        cursor.moveToFirst();

        db.close();
        return cursor;
    }

    public void clearData() {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);
        db.close();
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
