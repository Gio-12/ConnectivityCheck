package com.lam.lam_project.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.lam.lam_project.models.RecordType;

import java.util.UUID;

public class ContextDB extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "LAMDB.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "Records";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_CONDITION = "condition";
    private static final String COLUMN_GRIDZONE = "gridzone";
    private static final String COLUMN_SQUARE = "square";
    private static final String COLUMN_EASTING = "easting";
    private static final String COLUMN_NORTHING = "northing";
    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_SAMPLE_STATE = "STATE";



    public ContextDB(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String localRecordsTablequery = "CREATE TABLE " +  TABLE_NAME +
                " ("  +  COLUMN_TYPE +  " VARCHAR(30) NOT NULL," +
                COLUMN_TIMESTAMP + " TIMESTAMP NOT NULL," +
                COLUMN_VALUE + " REAL NOT NULL," +
                COLUMN_CONDITION + " INTEGER NOT NULL," +
                COLUMN_GRIDZONE +  " VARCHAR(3) NOT NULL," +
                COLUMN_SQUARE +  " VARCHAR(2) NOT NULL," +
                COLUMN_EASTING +  " VARCHAR(5) NOT NULL," +
                COLUMN_NORTHING +  " VARCHAR(5) NOT NULL, " +
                COLUMN_ID + " VARCHAR(60) primary key, "+
                COLUMN_SAMPLE_STATE + " VARCHAR(30) NOT NULL DEFAULT 'LOCAL' " +
                ");";

        db.execSQL(localRecordsTablequery);
    }

    public void addRecord(
            String type,
            String timeStamp,
            double value,
            int condition,
            String gridzone,
            String square,
            String easting,
            String northing)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TYPE, type);
        cv.put(COLUMN_TIMESTAMP, timeStamp);
        cv.put(COLUMN_VALUE, value);
        cv.put(COLUMN_CONDITION, condition);
        cv.put(COLUMN_GRIDZONE, gridzone);
        cv.put(COLUMN_SQUARE, square);
        cv.put(COLUMN_EASTING, easting);
        cv.put(COLUMN_NORTHING, northing);
        cv.put(COLUMN_ID, UUID.randomUUID().toString());

        long result = db.insert(TABLE_NAME, null, cv);

        if (result == -1)
            System.out.println("Errore durante la scrittura nel DB");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public Cursor getMissingRecordTypes(int accuracy, String gridzoneInput, String squareInput, String eastingInput, String northingInput)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT type_enum " +
                "FROM (SELECT 'Signal' AS type_enum " +
                "UNION " +
                "SELECT 'Wifi' " +
                "UNION " +
                "SELECT 'Noise') AS type_list " +
                "WHERE type_enum NOT IN (" +
                "SELECT  DISTINCT type " +
                "from records " +
                "where date(timestamp) =  date('now') " +
                "and  gridzone = '" +  gridzoneInput + "' "+
                "and square = '" + squareInput  +  "' " +
                "and substr(easting, 1," + accuracy + ") = substr('" +  eastingInput + "'"+", 1," + accuracy + ") " +
                "and  substr(northing, 1," + accuracy + ") = substr('" +  northingInput + "'"+", 1, " + accuracy + "));";
        return db.rawQuery(query, null);
    }

    public Cursor getAvgConditionByAccuracy(String recordType, int accuracy, int dateLimit){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_GRIDZONE + ", "
                + COLUMN_SQUARE +"," +
                "substr(" + COLUMN_EASTING + ", 1," +  accuracy + ") AS easting_cast," +
                "substr(" + COLUMN_NORTHING + ", 1," +  accuracy + ") AS northing_cast," +
                "AVG(" + COLUMN_CONDITION+ ") AS avg_cond " +
                "FROM (" +
                "    SELECT timestamp, type, value, condition, gridzone, square, easting, northing," +
                "           ROW_NUMBER() OVER (PARTITION BY substr(easting, 1," +  accuracy + "), substr(northing, 1," +  accuracy +
                ") ORDER BY timestamp desc) AS rn " +
                "    FROM records" +
                ") AS subquery " +
                "WHERE rn <= " + dateLimit + " AND type = '" + recordType +
                "' " +
                "GROUP BY gridzone, square, easting_cast, northing_cast";
        return db.rawQuery(query, null);
    }
}
