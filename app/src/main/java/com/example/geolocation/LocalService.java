package com.example.geolocation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalService extends SQLiteOpenHelper {

    private static final String DB_NAME = "Geolocation.db";
    private static final String DB_TABLE = "Geolocation_Table";
    private static final String DATA = "DATA";
    private static final String CREATE_TABLE = "CREATE TABLE " + DB_TABLE + " (" + DATA + " TEXT " + ")";

    public LocalService(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
        onCreate(sqLiteDatabase);
    }

    public boolean insertData(String data) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATA, data);

        long result = sqLiteDatabase.insert(DB_TABLE, null, contentValues);
        return result != -1;
    }

    public Cursor viewData() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "Select * from " + DB_TABLE;
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

        return cursor;
    }

    public void deleteData() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.execSQL("delete from " + DB_TABLE);
        sqLiteDatabase.close();
    }
}
