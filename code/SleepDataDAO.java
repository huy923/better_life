package com.yourapp.healthapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.yourapp.healthapp.models.SleepData;
import com.yourapp.healthapp.models.SleepSession;
import com.yourapp.healthapp.models.SleepStage;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO)
 * Xử lý tất cả các thao tác CRUD với database
 */
public class SleepDataDAO {
    
    private SleepDatabaseHelper dbHelper;
    
    public SleepDataDAO(Context context) {
        dbHelper = SleepDatabaseHelper.getInstance(context);
    }
    
    // ==================== SLEEP SESSION OPERATIONS ====================
    
    /**
     * Tạo một phiên ngủ mới
     * @return ID của phiên ngủ vừa tạo
     */
    public long createSession(SleepSession session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(SleepDatabaseHelper.COLUMN_START_TIME, session.getStartTime());
        values.put(SleepDatabaseHelper.COLUMN_END_TIME, session.getEndTime());
        
        long sessionId = db.insert(SleepDatabaseHelper.TABLE_SESSIONS, null, values);
        db.close();
        
        return sessionId;
    }
    
    /**
     * Cập nhật phiên ngủ
     */
    public int updateSession(SleepSession session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(SleepDatabaseHelper.COLUMN_END_TIME, session.getEndTime());
        values.put(SleepDatabaseHelper.COLUMN_TOTAL_DURATION, session.getTotalDuration());
        values.put(SleepDatabaseHelper.COLUMN_DEEP_DURATION, session.getDeepSleepDuration());
        values.put(SleepDatabaseHelper.COLUMN_LIGHT_DURATION, session.getLightSleepDuration());
        values.put(SleepDatabaseHelper.COLUMN_REM_DURATION, session.getRemSleepDuration());
        values.put(SleepDatabaseHelper.COLUMN_AWAKE_DURATION, session.getAwakeDuration());
        values.put(SleepDatabaseHelper.COLUMN_SLEEP_QUALITY, session.getSleepQuality());
        values.put(SleepDatabaseHelper.COLUMN_NUM_AWAKENINGS, session.getNumberOfAwakenings());
        
        int rowsAffected = db.update(
            SleepDatabaseHelper.TABLE_SESSIONS,
            values,
            SleepDatabaseHelper.COLUMN_ID + " = ?",
            new String[]{String.valueOf(session.getId())}
        );
        
        db.close();
        return rowsAffected;
    }
    
    /**
     * Lấy phiên ngủ theo ID
     */
    public SleepSession getSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
            SleepDatabaseHelper.TABLE_SESSIONS,
            null, // Lấy tất cả columns
            SleepDatabaseHelper.COLUMN_ID + " = ?",
            new String[]{String.valueOf(sessionId)},
            null, null, null
        );
        
        SleepSession session = null;
        if (cursor.moveToFirst()) {
            session = cursorToSession(cursor);
        }
        
        cursor.close();
        db.close();
        
        return session;
    }
    
    /**
     * Lấy phiên ngủ đang hoạt động (chưa kết thúc)
     */
    public SleepSession getActiveSession() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
            SleepDatabaseHelper.TABLE_SESSIONS,
            null,
            SleepDatabaseHelper.COLUMN_END_TIME + " = 0",
            null, null, null,
            SleepDatabaseHelper.COLUMN_START_TIME + " DESC",
            "1" // Chỉ lấy 1 kết quả
        );
        
        SleepSession session = null;
        if (cursor.moveToFirst()) {
            session = cursorToSession(cursor);
        }
        
        cursor.close();
        db.close();
        
        return session;
    }
    
    /**
     * Lấy tất cả phiên ngủ
     */
    public List<SleepSession> getAllSessions() {
        List<SleepSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
            SleepDatabaseHelper.TABLE_SESSIONS,
            null, null, null, null, null,
            SleepDatabaseHelper.COLUMN_START_TIME + " DESC" // Mới nhất trước
        );
        
        if (cursor.moveToFirst()) {
            do {
                sessions.add(cursorToSession(cursor));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return sessions;
    }
    
    /**
     * Lấy phiên ngủ trong khoảng thời gian
     */
    public List<SleepSession> getSessionsBetween(long startTime, long endTime) {
        List<SleepSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
            SleepDatabaseHelper.TABLE_SESSIONS,
            null,
            SleepDatabaseHelper.COLUMN_START_TIME + " >= ? AND " +
            SleepDatabaseHelper.COLUMN_START_TIME + " <= ?",
            new String[]{String.valueOf(startTime), String.valueOf(endTime)},
            null, null,
            SleepDatabaseHelper.COLUMN_START_TIME + " DESC"
        );
        
        if (cursor.moveToFirst()) {
            do {
                sessions.add(cursorToSession(cursor));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return sessions;
    }
    
    /**
     * Xóa phiên ngủ
     */
    public int deleteSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        int rowsDeleted = db.delete(
            SleepDatabaseHelper.TABLE_SESSIONS,
            SleepDatabaseHelper.COLUMN_ID + " = ?",
            new String[]{String.valueOf(sessionId)}
        );
        
        db.close();
        return rowsDeleted;
    }
    
    // ==================== SLEEP DATA OPERATIONS ====================
    
    /**
     * Thêm dữ liệu ngủ
     */
    public long addSleepData(SleepData data) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(SleepDatabaseHelper.COLUMN_SESSION_ID, data.getSessionId());
        values.put(SleepDatabaseHelper.COLUMN_TIMESTAMP, data.getTimestamp());
        values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_X, data.getMovementX());
        values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_Y, data.getMovementY());
        values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_Z, data.getMovementZ());
        values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_MAG, data.getMovementMagnitude());
        values.put(SleepDatabaseHelper.COLUMN_STAGE, data.getStage().getValue());
        
        long dataId = db.insert(SleepDatabaseHelper.TABLE_DATA, null, values);
        db.close();
        
        return dataId;
    }
    
    /**
     * Thêm nhiều dữ liệu cùng lúc (nhanh hơn)
     */
    public void addSleepDataBatch(List<SleepData> dataList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        db.beginTransaction();
        try {
            for (SleepData data : dataList) {
                ContentValues values = new ContentValues();
                values.put(SleepDatabaseHelper.COLUMN_SESSION_ID, data.getSessionId());
                values.put(SleepDatabaseHelper.COLUMN_TIMESTAMP, data.getTimestamp());
                values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_X, data.getMovementX());
                values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_Y, data.getMovementY());
                values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_Z, data.getMovementZ());
                values.put(SleepDatabaseHelper.COLUMN_MOVEMENT_MAG, data.getMovementMagnitude());
                values.put(SleepDatabaseHelper.COLUMN_STAGE, data.getStage().getValue());
                
                db.insert(SleepDatabaseHelper.TABLE_DATA, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    
    /**
     * Lấy tất cả dữ liệu của một phiên ngủ
     */
    public List<SleepData> getSleepDataForSession(long sessionId) {
        List<SleepData> dataList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
            SleepDatabaseHelper.TABLE_DATA,
            null,
            SleepDatabaseHelper.COLUMN_SESSION_ID + " = ?",
            new String[]{String.valueOf(sessionId)},
            null, null,
            SleepDatabaseHelper.COLUMN_TIMESTAMP + " ASC" // Sắp xếp theo thời gian
        );
        
        if (cursor.moveToFirst()) {
            do {
                dataList.add(cursorToSleepData(cursor));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return dataList;
    }
    
    /**
     * Xóa dữ liệu cũ hơn số ngày chỉ định
     */
    public int deleteOldData(int daysOld) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
        
        int rowsDeleted = db.delete(
            SleepDatabaseHelper.TABLE_SESSIONS,
            SleepDatabaseHelper.COLUMN_START_TIME + " < ?",
            new String[]{String.valueOf(cutoffTime)}
        );
        
        db.close();
        return rowsDeleted;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Chuyển Cursor thành SleepSession object
     */
    private SleepSession cursorToSession(Cursor cursor) {
        SleepSession session = new SleepSession();
        
        session.setId(cursor.getLong(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_ID)));
        session.setStartTime(cursor.getLong(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_START_TIME)));
        session.setEndTime(cursor.getLong(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_END_TIME)));
        session.setTotalDuration(cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_TOTAL_DURATION)));
        session.setDeepSleepDuration(cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_DEEP_DURATION)));
        session.setLightSleepDuration(cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_LIGHT_DURATION)));
        session.setRemSleepDuration(cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_REM_DURATION)));
        session.setAwakeDuration(cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_AWAKE_DURATION)));
        session.setSleepQuality(cursor.getDouble(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_SLEEP_QUALITY)));
        session.setNumberOfAwakenings(cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_NUM_AWAKENINGS)));
        
        return session;
    }
    
    /**
     * Chuyển Cursor thành SleepData object
     */
    private SleepData cursorToSleepData(Cursor cursor) {
        SleepData data = new SleepData();
        
        data.setId(cursor.getLong(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_DATA_ID)));
        data.setSessionId(cursor.getLong(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_SESSION_ID)));
        data.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_TIMESTAMP)));
        
        double x = cursor.getDouble(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_MOVEMENT_X));
        double y = cursor.getDouble(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_MOVEMENT_Y));
        double z = cursor.getDouble(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_MOVEMENT_Z));
        
        data.setMovementX(x);
        data.setMovementY(y);
        data.setMovementZ(z);
        
        int stageValue = cursor.getInt(cursor.getColumnIndexOrThrow(SleepDatabaseHelper.COLUMN_STAGE));
        data.setStage(SleepStage.fromValue(stageValue));
        
        return data;
    }
}
